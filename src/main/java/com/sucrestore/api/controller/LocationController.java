package com.sucrestore.api.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
 * Résout les liens Google Maps courts (maps.app.goo.gl) en coordonnées GPS.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public")
public class LocationController {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; Pixel 3) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    // Patterns pour extraire les coordonnées depuis une URL Google Maps
    private static final Pattern COORDS_AT   = Pattern.compile("/@(-?[0-9]{1,3}\\.[0-9]+),(-?[0-9]{1,3}\\.[0-9]+)");
    private static final Pattern COORDS_Q    = Pattern.compile("[?&]q=(-?[0-9]{1,3}\\.[0-9]+),(-?[0-9]{1,3}\\.[0-9]+)");
    private static final Pattern COORDS_LL   = Pattern.compile("[?&]ll=(-?[0-9]{1,3}\\.[0-9]+),(-?[0-9]{1,3}\\.[0-9]+)");
    // Format encodé dans les URLs longues Google Maps : !3dLAT!4dLNG
    private static final Pattern COORDS_3D4D  = Pattern.compile("!3d(-?[0-9]{1,3}\\.[0-9]+)!4d(-?[0-9]{1,3}\\.[0-9]+)");
    // Format des liens courts résolus : /maps/search/LAT,+LNG (le + est optionnel)
    private static final Pattern COORDS_SEARCH = Pattern.compile("/search/(-?[0-9]{1,3}\\.[0-9]+),\\+?(-?[0-9]{1,3}\\.[0-9]+)");

    // Redirection dans le corps HTML : <meta http-equiv="refresh" content="0;url=...">
    private static final Pattern META_REFRESH = Pattern.compile(
        "content=[\"'][^;]*;\\s*url=([^\"'\\s>]+)",
        Pattern.CASE_INSENSITIVE);

    // Redirection JS : window.location = "..." ou window.location.href = "..."
    private static final Pattern JS_LOCATION = Pattern.compile(
        "window\\.location(?:\\.href)?\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE);

    // Détection d'un Plus Code : "9CXR+XVG" ou "9CXR+XVG, Tampouy, Ouagadougou"
    private static final Pattern PLUS_CODE = Pattern.compile(
        "^[0-9A-Z]{4,8}\\+[0-9A-Z]{2,3}(\\s*,.*)?$", Pattern.CASE_INSENSITIVE);

    @GetMapping("/resolve-location")
    public ResponseEntity<Map<String, Object>> resolve(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();
        try {
            double[] coords;
            String finalUrl;

            // Cas 1 : Plus Code brut (ex: "9CXR+XVG, Tampouy, Ouagadougou")
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                coords = geocodeWithNominatim(url.trim());
                finalUrl = (coords != null)
                    ? "https://www.google.com/maps?q=" + coords[0] + "," + coords[1]
                    : url;

            } else {
                // Cas 2 : URL Google Maps (courte ou complète)
                finalUrl = followRedirects(url, 10);

                // Si Google redirige vers sa page de consentement, extraire l'URL cible
                if (finalUrl.contains("consent.google.com")) {
                    String continueUrl = extractQueryParam(finalUrl, "continue");
                    if (continueUrl != null) finalUrl = continueUrl;
                }

                coords = extractCoordinates(finalUrl);

                // Fallback Nominatim : lien de type lieu (ftid=...) sans coordonnées dans l'URL
                if (coords == null) {
                    String q = extractQueryParam(finalUrl, "q");
                    if (q != null && !q.isEmpty()) {
                        coords = geocodeWithNominatim(q);
                        if (coords != null) {
                            finalUrl = "https://www.google.com/maps?q=" + coords[0] + "," + coords[1];
                        }
                    }
                }

                // Fallback 2 : extraire le nom du lieu depuis /maps/place/{name}/
                if (coords == null && finalUrl.contains("/maps/place/")) {
                    Pattern placePath = Pattern.compile("/maps/place/([^/?#]+)");
                    Matcher pm = placePath.matcher(finalUrl);
                    if (pm.find()) {
                        String placeName = URLDecoder.decode(
                            pm.group(1).replace("+", " "), StandardCharsets.UTF_8);
                        coords = geocodeWithNominatim(placeName);
                        if (coords != null) {
                            finalUrl = "https://www.google.com/maps?q=" + coords[0] + "," + coords[1];
                        }
                    }
                }
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

    /**
     * Suit les redirections HTTP (301/302/307/308) ET les méta-refresh / window.location
     * dans le corps HTML, jusqu'à maxHops fois.
     */
    private String followRedirects(String urlStr, int maxHops) throws Exception {
        String current = urlStr;
        for (int i = 0; i < maxHops; i++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8");

            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) break;
                current = resolveUrl(current, location);

            } else if (status == 200) {
                // Lire le corps pour chercher une redirection HTML/JS ou des coordonnées
                String body = readBody(conn, 16384);
                conn.disconnect();

                // Si l'URL actuelle a déjà des coordonnées, on s'arrête
                if (extractCoordinates(current) != null) break;

                // Chercher une redirection dans le corps
                String redirect = extractRedirectFromBody(body, current);
                if (redirect != null && !redirect.equals(current)) {
                    current = redirect;
                } else {
                    // Chercher les coordonnées directement dans le corps de la page
                    double[] bodyCoords = extractCoordinates(body);
                    if (bodyCoords != null) {
                        current = "https://www.google.com/maps?q=" + bodyCoords[0] + "," + bodyCoords[1];
                    }
                    break;
                }
            } else {
                conn.disconnect();
                break;
            }
        }
        return current;
    }

    /** Lit les premiers maxBytes caractères du corps de la réponse. */
    private String readBody(HttpURLConnection conn, int maxBytes) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            char[] buf = new char[1024];
            int total = 0, read;
            while ((read = reader.read(buf)) != -1 && total < maxBytes) {
                sb.append(buf, 0, read);
                total += read;
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Extrait une URL de redirection depuis le corps HTML (meta-refresh ou window.location). */
    private String extractRedirectFromBody(String body, String currentUrl) {
        Matcher m = META_REFRESH.matcher(body);
        if (m.find()) return resolveUrl(currentUrl, m.group(1).trim());
        m = JS_LOCATION.matcher(body);
        if (m.find()) return resolveUrl(currentUrl, m.group(1).trim());
        return null;
    }

    /** Extrait la valeur d'un paramètre de query string et la décode. */
    private String extractQueryParam(String url, String param) {
        try {
            Pattern p = Pattern.compile("[?&]" + param + "=([^&]+)");
            Matcher m = p.matcher(url);
            if (m.find()) return URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        return null;
    }

    /** Résout une URL potentiellement relative par rapport à une URL de base. */
    private String resolveUrl(String base, String location) {
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return location;
        }
        try {
            return new URL(new URL(base), location).toString();
        } catch (Exception e) {
            return location;
        }
    }

    /**
     * Géocode une adresse textuelle via Nominatim (OpenStreetMap).
     * Utilisé comme fallback pour les liens de type lieu (ftid=...) sans coordonnées GPS.
     * Stratégie : essaie la requête complète, puis uniquement premier + dernier terme
     * (ex: "Hôpital Paul VI, Tampouy, Ouagadougou" → "Hôpital Paul VI Ouagadougou")
     * pour éviter que les quartiers inconnus bloquent la recherche.
     */
    private double[] geocodeWithNominatim(String query) {
        // Supprimer les Plus Codes courts (ex: "9CXR+XVG " ou "9CXR XVG " après URL-decode)
        // Le + peut devenir un espace après URLDecoder.decode, donc on accepte les deux
        String cleanQuery = query.replaceAll("^[0-9A-Z]{4}[+\\s][0-9A-Z]{2,3}\\s*", "").trim();
        if (cleanQuery.isEmpty()) cleanQuery = query;

        // Tentative 1 : requête complète
        double[] result = nominatimSearch(cleanQuery);
        if (result != null) return result;

        String[] parts = cleanQuery.split(",");
        if (parts.length >= 2) {
            // Tentative 2 : premier terme + dernier terme
            String reduced = parts[0].trim() + " " + parts[parts.length - 1].trim();
            result = nominatimSearch(reduced);
            if (result != null) return result;

            // Tentative 3 : progressivement de la fin (2ème à last, 3ème à last, …)
            // ex: "Spirit Design, desc, Dassasgo, Ouagadougou" → essaie "Dassasgo Ouagadougou"
            for (int i = parts.length - 2; i >= 1; i--) {
                String candidate = parts[i].trim() + " " + parts[parts.length - 1].trim();
                result = nominatimSearch(candidate);
                if (result != null) return result;
            }
        }
        return result;
    }

    /** Appel HTTP brut à Nominatim, retourne null si aucun résultat. */
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

            if (conn.getResponseCode() != 200) { conn.disconnect(); return null; }
            String body = readBody(conn, 4096);
            conn.disconnect();

            if (body.equals("[]")) return null;

            Matcher latM = Pattern.compile("\"lat\"\\s*:\\s*\"(-?[0-9.]+)\"").matcher(body);
            Matcher lonM = Pattern.compile("\"lon\"\\s*:\\s*\"(-?[0-9.]+)\"").matcher(body);
            if (latM.find() && lonM.find()) {
                double lat = Double.parseDouble(latM.group(1));
                double lon = Double.parseDouble(lonM.group(1));
                if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Tente d'extraire les coordonnées depuis une URL ou un texte Google Maps. */
    private double[] extractCoordinates(String text) {
        // COORDS_3D4D en premier : coordonnées exactes du pin, plus précises que /@ (centre vue carte)
        for (Pattern p : new Pattern[]{COORDS_3D4D, COORDS_SEARCH, COORDS_Q, COORDS_LL, COORDS_AT}) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                try {
                    double lat = Double.parseDouble(m.group(1));
                    double lng = Double.parseDouble(m.group(2));
                    if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
                        return new double[]{lat, lng};
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
