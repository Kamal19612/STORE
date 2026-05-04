package com.sucrestore.api.tenant;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sucrestore.api.entity.Store;
import com.sucrestore.api.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreResolverService {

    public static final String STORE_HEADER = "X-Store-Code";
    public static final String DEFAULT_STORE_CODE = "sucre";

    private final StoreRepository storeRepository;
    private final TenantProperties tenantProperties;

    /**
     * Best-effort resolution without any default fallback.
     * - Host/domain has priority
     * - Then X-Store-Code
     * - Returns empty if both are missing or if domain is unmapped.
     *
     * NOTE: Unknown explicit store codes still throw (caller can map to 400).
     */
    public Optional<Store> resolveByDomainOrCode(String storeCodeHeader, String hostHeader) {
        String host = normalizeHost(hostHeader);
        if (host != null) {
            Optional<Store> byDomain = storeRepository.findByDomain(host);
            if (byDomain.isPresent()) return byDomain;
        }

        String code = normalize(storeCodeHeader);
        if (code != null) {
            return Optional.of(
                storeRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown store code: " + code))
            );
        }

        return Optional.empty();
    }

    public Store resolveByCodeOrDomainOrDefault(String storeCodeHeader, String hostHeader) {
        // 1) Domain mapping (Host / X-Forwarded-Host) has priority in multi-boutique by domain.
        // This prevents ambiguity when clients always send a default X-Store-Code.
        String host = normalizeHost(hostHeader);
        if (host != null) {
            Optional<Store> byDomain = storeRepository.findByDomain(host);
            if (byDomain.isPresent()) {
                return byDomain.get();
            }
        }

        // 2) First hostname label as store code (e.g. sucrestore.example.com → code "sucrestore"),
        //    aligned with storefront env / hostname conventions before falling back to default.
        Optional<Store> byHostLabel = findByFirstHostLabelAsStoreCode(host);
        if (byHostLabel.isPresent()) {
            return byHostLabel.get();
        }

        // 3) Header X-Store-Code (kept for backwards compatibility and super-admin cross-store operations)
        String code = normalize(storeCodeHeader);
        if (code != null) {
            return storeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown store code: " + code));
        }

        // 4) Strict mode: reject if store cannot be resolved
        if (tenantProperties.isRequireExplicitStore()) {
            throw new IllegalArgumentException("Missing store identifier (X-Store-Code or domain mapping)");
        }

        // 5) Default store (backward compatibility)
        return storeRepository.findByCode(DEFAULT_STORE_CODE)
            .orElseThrow(() -> new IllegalStateException("Default store not found: " + DEFAULT_STORE_CODE));
    }

    /**
     * When {@code stores.domain} has no row for this host, try the first hostname label as {@code stores.code}
     * (e.g. {@code spirit.example.com} → code {@code spirit}).
     */
    private Optional<Store> findByFirstHostLabelAsStoreCode(String normalizedHost) {
        if (normalizedHost == null || !normalizedHost.contains(".")) {
            return Optional.empty();
        }
        int dot = normalizedHost.indexOf('.');
        String first = normalizedHost.substring(0, dot);
        if (first.isBlank() || first.length() < 2) {
            return Optional.empty();
        }
        if ("www".equals(first) || "localhost".equals(first)) {
            return Optional.empty();
        }
        return storeRepository.findByCode(first);
    }

    private String normalize(String v) {
        if (v == null) return null;
        String t = v.trim().toLowerCase(Locale.ROOT);
        return t.isBlank() ? null : t;
    }

    private String normalizeHost(String host) {
        if (host == null) return null;
        // Host can include port and/or be a forwarded list: "a.example.com, b.example.com"
        String h = host.trim();
        int comma = h.indexOf(',');
        if (comma > 0) {
            h = h.substring(0, comma);
        }
        h = h.toLowerCase(Locale.ROOT);
        if (h.isBlank()) return null;
        int idx = h.indexOf(':');
        if (idx > 0) h = h.substring(0, idx);
        return h.isBlank() ? null : h;
    }
}

