package com.sucrestore.api.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DeliveryAgentRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsertDeliveryAgent(String authUserId, Long storeUserId, String role, boolean active) {
        jdbcTemplate.update(
            """
            INSERT INTO public.delivery_agents (auth_user_id, store_user_id, role, is_active)
            VALUES (?::uuid, ?, ?, ?)
            ON CONFLICT (auth_user_id)
            DO UPDATE SET store_user_id = EXCLUDED.store_user_id,
                          role = EXCLUDED.role,
                          is_active = EXCLUDED.is_active
            """,
            authUserId,
            storeUserId,
            role,
            active
        );
    }

    public String findAuthUserIdByStoreUserId(Long storeUserId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT auth_user_id::text FROM public.delivery_agents WHERE store_user_id = ? LIMIT 1",
                String.class,
                storeUserId
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }
}

