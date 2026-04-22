package com.sucrestore.api.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Résout les liens Google Maps (courts et longs) en coordonnées GPS.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public")
public class LocationController {

    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Pixel 3) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    // Patterns pour extraire les coordonnées
    private static final Pattern COORDS_AT = Pattern.compile("/@(-?[0-9]{1,3}\\.[0-9]+),(-?[0-9]{1,3}\\.[0-9]+)");
    private static final Pattern COORDS_Q = Pattern.compile("[?&]q=(-?[0-9]{1,3}\\.[0-9]+),(-?[0-9]{1,3}\\.[0-9]+)");
    private static final Pattern COORDS_LL = Pattern.compile("[?&]ll=(-?[0-9]{1,3}\\.[0-9]+),(-?[0-9]{1,3}\\.[0-9]+)");
    private static final Pattern COORDS_3D4D = Pattern.compile("!3d(-?[0-9]{1,3}\\.[0-9]+)!4d(-?[0-9]{1,3}\\.[0-9]+)");
    private static final Pattern COORDS_SEARCH = Pattern
            .compile("/search/(-?[0-9]{1,3}\\.[0-9]+),\\+?(-?[0-9]{1,3}\\.[0-9]+)");
    private static final Pattern META_REFRESH = Pattern.compile("content=[\"'][^;]*;\\s*url=([^\"'\\s>]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_LOCATION = Pattern
            .compile("window\\.location(?:\\.href)?\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_GOOGLE = Pattern
            .compile("(https?://)?(maps\\.app\\.goo\\.gl|goo\\.gl/maps)/([a-zA-Z0-9]+)");

    @GetMapping("/resolve-location")
    public ResponseEntity<Map<String, Object>> resolve(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();
        try {
            double[] coords = null;
            String finalUrl = url;

            // Cas 1 : Plus Code ou texte brut
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                coords = geocodeWithNominatim(url.trim());
                if (coords != null)
                    finalUrl = "https://www.google.com/maps?q=" + coords[0] + "," + coords[1];
            } else {
                // Cas 2 : URL Google Maps (courte ou longue)
                finalUrl = followRedirects(url, 10);
                coords = extractCoordinates(finalUrl);

                // Fallback q param
                if (coords == null) {
                    String q = extractQueryParam(finalUrl, "q");
                    if (q != null && !q.isEmpty())
                        coords = geocodeWithNominatim(q);
                }

                // Fallback place/dir
                if (coords == null) {
                    String placeName = extractPlaceName(finalUrl);
                    if (placeName != null)
                        coords = geocodeWithNominatim(placeName);
                }

                if (coords != null)
                    finalUrl = "https://www.google.com/maps?q=" + coords[0] + "," + coords[1];
            }

            if (coords != null) {
                response.put("success", true);
                response.put("lat", coords[0]);
                response.put("lng", coords[1]);
                response.put("resolvedUrl", finalUrl);
            } else {
                response.put("success", false);
                response.put("message", "Coordonnées introuvables : " + url);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Impossible de résoudre : " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // --- Méthodes utilitaires ---

    private String followRedirects(String urlStr, int maxHops) throws Exception {
        String current = urlStr;
        for (int i = 0; i < maxHops; i++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", USER_AGENT);

            int status = conn.getResponseCode();

            if (status == 301 || status == 302 || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null)
                    break;
                current = resolveUrl(current, location);
            } else if (status == 200) {
                String body = readBody(conn, 16384);
                conn.disconnect();

                // Redirection HTML/JS
                String redirect = extractRedirectFromBody(body, current);
                if (redirect != null && !redirect.equals(current))
                    current = redirect;
                else
                    break;
            } else {
                conn.disconnect();
                break;
            }
        }
        return current;
    }

    private String readBody(HttpURLConnection conn, int maxBytes) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        char[] buf = new char[1024];
        int total = 0, read;
        while ((read = reader.read(buf)) != -1 && total < maxBytes) {
            sb.append(buf, 0, read);
            total += read;
        }
        reader.close();
        return sb.toString();
    }

    private String extractRedirectFromBody(String body, String currentUrl) {
        Matcher m = META_REFRESH.matcher(body);
        if (m.find())
            return resolveUrl(currentUrl, m.group(1).trim());
        m = JS_LOCATION.matcher(body);
        if (m.find())
            return resolveUrl(currentUrl, m.group(1).trim());
        return null;
    }

    private String extractQueryParam(String url, String param) {
        try {
            Pattern p = Pattern.compile("[?&]" + param + "=([^&]+)");
            Matcher m = p.matcher(url);
            if (m.find())
                return URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveUrl(String base, String location) {
        if (location.startsWith("http"))
            return location;
        try {
            return new URL(new URL(base), location).toString();
        } catch (Exception e) {
            return location;
        }
    }

    private String extractPlaceName(String url) {
        try {
            Pattern p = Pattern.compile("/maps/(place|dir)/([^/?#]+)");
            Matcher m = p.matcher(url);
            if (m.find())
                return URLDecoder.decode(m.group(2).replace("+", " "), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return null;
    }

    private double[] extractCoordinates(String text) {
        for (Pattern p : new Pattern[] { COORDS_3D4D, COORDS_SEARCH, COORDS_Q, COORDS_LL, COORDS_AT }) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                try {
                    double lat = Double.parseDouble(m.group(1));
                    double lng = Double.parseDouble(m.group(2));
                    if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180)
                        return new double[] { lat, lng };
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private double[] geocodeWithNominatim(String query) {
        try {
            String cleanQuery = query.replaceAll("^[0-9A-Z]{4}[+\\s][0-9A-Z]{2,3}\\s*", "").trim();
            if (cleanQuery.isEmpty())
                cleanQuery = query;

            double[] result = nominatimSearch(cleanQuery);
            if (result != null)
                return result;

            String noAccent = stripAccents(cleanQuery);
            if (!noAccent.equals(cleanQuery)) {
                result = nominatimSearch(noAccent);
                if (result != null)
                    return result;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String stripAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private double[] nominatimSearch(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URL url = new URL("https://nominatim.openstreetmap.org/search?q=" + encoded
                    + "&format=json&limit=1&accept-language=fr");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "SucreStoreApp/1.0");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return null;
            }
            String body = readBody(conn, 4096);
            conn.disconnect();
            if (body.equals("[]"))
                return null;

            Matcher latM = Pattern.compile("\"lat\"\\s*:\\s*\"(-?[0-9.]+)\"").matcher(body);
            Matcher lonM = Pattern.compile("\"lon\"\\s*:\\s*\"(-?[0-9.]+)\"").matcher(body);
            if (latM.find() && lonM.find()) {
                double lat = Double.parseDouble(latM.group(1));
                double lon = Double.parseDouble(lonM.group(1));
                return new double[] { lat, lon };
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}