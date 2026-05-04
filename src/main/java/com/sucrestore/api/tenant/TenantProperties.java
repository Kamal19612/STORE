package com.sucrestore.api.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tenant / multi-store runtime controls.
 * Keep defaults backward-compatible; turn on strictness in production via config.
 */
@Component
@ConfigurationProperties(prefix = "tenant")
public class TenantProperties {

    /**
     * If true, requests must resolve a store explicitly (X-Store-Code or domain mapping).
     * When false, resolver can fall back to DEFAULT_STORE_CODE.
     */
    private boolean requireExplicitStore = false;

    public boolean isRequireExplicitStore() {
        return requireExplicitStore;
    }

    public void setRequireExplicitStore(boolean requireExplicitStore) {
        this.requireExplicitStore = requireExplicitStore;
    }
}

