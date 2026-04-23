package com.sucrestore.api.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sucrestore.api.config.AppProperties;

import lombok.RequiredArgsConstructor;

/**
 * Provisions Supabase Auth users via the Admin API (service_role key).
 *
 * NOTE: this key must remain server-side.
 */
@Service
@RequiredArgsConstructor
public class SupabaseAdminService {

    private final AppProperties appProperties;

    /**
     * Creates a Supabase Auth user and returns the auth user id (uuid).
     *
     * Endpoint: POST {SUPABASE_URL}/auth/v1/admin/users
     */
    public String createAuthUser(String email, String password, boolean emailConfirmed) {
        String baseUrl = appProperties.getSupabase().getUrl();
        String serviceKey = appProperties.getSupabase().getServiceRoleKey();

        if (baseUrl == null || baseUrl.isBlank() || serviceKey == null || serviceKey.isBlank()) {
            throw new RuntimeException("Supabase admin credentials not configured (app.supabase.url / app.supabase.service-role-key).");
        }

        String url = baseUrl.replaceAll("/+$", "") + "/auth/v1/admin/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", serviceKey);
        headers.setBearerAuth(serviceKey);

        Map<String, Object> body = Map.of(
            "email", email,
            "password", password,
            "email_confirm", emailConfirmed
        );

        RestTemplate rt = new RestTemplate();
        ResponseEntity<Map> res = rt.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new RuntimeException("Supabase admin create user failed: HTTP " + res.getStatusCode().value());
        }

        Object id = res.getBody().get("id");
        if (id == null) {
            // Some versions return { user: { id: ... } }
            Object user = res.getBody().get("user");
            if (user instanceof Map u && u.get("id") != null) {
                return u.get("id").toString();
            }
            throw new RuntimeException("Supabase admin create user: missing id in response.");
        }
        return id.toString();
    }
}

