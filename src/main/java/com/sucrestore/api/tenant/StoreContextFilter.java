package com.sucrestore.api.tenant;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sucrestore.api.entity.Store;
import com.sucrestore.api.entity.User;
import com.sucrestore.api.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

/**
 * Resolves tenant store for every request and stores it in StoreContext.
 *
 * Security model:
 * - For PUBLIC routes, tenant is derived from X-Store-Code (or Host) with a default fallback.
 * - For AUTHENTICATED store users on *protected* APIs (admin, etc.), tenant is the same
 *   {@link StoreResolverService#resolveByCodeOrDomainOrDefault} result as the public storefront for this Host/header,
 *   and must match the user's store id (403 otherwise). This matches single-catalog behaviour and avoids
 *   "public default store" vs "user store" drift when the domain column is not set yet.
 * - Public storefront APIs (/api/products/**, /api/public/**, …) always use the store from X-Store-Code / Host
 *   so browsing the shop with a JWT (e.g. admin from another store) does not return 403.
 * - Delivery role is GLOBAL and can operate cross-store (tenant context still exists, but is not used to filter).
 */
@Component
@RequiredArgsConstructor
public class StoreContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(StoreContextFilter.class);

    private final StoreResolverService storeResolverService;
    private final UserRepository userRepository;

    private String getEffectiveHost(HttpServletRequest request) {
        if (request == null) return null;
        String xfHost = request.getHeader("X-Forwarded-Host");
        if (xfHost != null && !xfHost.isBlank()) return xfHost;
        String host = request.getHeader("Host");
        if (host != null && !host.isBlank()) return host;
        String server = request.getServerName();
        return (server == null || server.isBlank()) ? null : server;
    }

    /**
     * Storefront and other permitAll routes: resolve tenant from header/host only (see SecurityConfig).
     * Do not overwrite with the authenticated user's store — otherwise JWT + wrong X-Store-Code yields 403.
     */
    private static boolean usesHeaderTenantForPublicApis(String requestUri) {
        if (requestUri == null) return false;
        return requestUri.startsWith("/api/products")
            || requestUri.startsWith("/api/categories")
            || requestUri.startsWith("/api/sliders")
            || requestUri.startsWith("/api/public")
            || requestUri.startsWith("/api/orders")
            // Telegram webhook is unauthenticated but must be tenant-aware (by Host/domain).
            || requestUri.startsWith("/api/telegram");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            String uri = request.getRequestURI();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

            // Always resolve "header/host" tenant for public storefront APIs (multi-store browsing).
            // For protected APIs, we resolve tenant differently depending on role:
            // - Store-scoped roles: same resolver as public catalog for this request, then must equal user's store.
            // - SUPER_ADMIN: may operate cross-store, but MUST specify an explicit store (no silent default fallback).
            Store resolved = null;
            if (usesHeaderTenantForPublicApis(uri)) {
                try {
                    resolved = storeResolverService.resolveByCodeOrDomainOrDefault(
                        request.getHeader(StoreResolverService.STORE_HEADER),
                        getEffectiveHost(request)
                    );
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                    return;
                }
            }

            if (isAuthenticated) {
                boolean isGlobalDelivery = auth.getAuthorities().stream().anyMatch(a ->
                    "ROLE_DELIVERY".equals(a.getAuthority()) || "ROLE_DELIVERY_AGENT".equals(a.getAuthority())
                );

                boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

                if (!isGlobalDelivery && !usesHeaderTenantForPublicApis(uri)) {
                    String username = auth.getName();
                    User u = userRepository.findByUsername(username).orElse(null);
                    Store userStore = u != null ? u.getStore() : null;

                    // SUPER_ADMIN can work cross-store, but should never fall back silently.
                    if (isSuperAdmin) {
                        String headerStore = request.getHeader(StoreResolverService.STORE_HEADER);
                        String host = getEffectiveHost(request);
                        if ((headerStore == null || headerStore.isBlank()) && (host == null || host.isBlank())) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing store identifier (X-Store-Code or domain mapping)");
                            return;
                        }
                        try {
                            resolved = storeResolverService.resolveByCodeOrDomainOrDefault(headerStore, host);
                        } catch (IllegalArgumentException e) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                            return;
                        }
                    } else {
                        // Store-scoped roles: identical effective store as GET /api/products for this Host/header,
                        // then enforce that the user belongs to that store (single catalogue / PHP-style consistency).
                        if (userStore == null) {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User has no store scope");
                            return;
                        }

                        final Store storefrontStore;
                        try {
                            storefrontStore = storeResolverService.resolveByCodeOrDomainOrDefault(
                                request.getHeader(StoreResolverService.STORE_HEADER),
                                getEffectiveHost(request)
                            );
                        } catch (IllegalArgumentException e) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                            return;
                        }

                        if (userStore.getId() == null || !userStore.getId().equals(storefrontStore.getId())) {
                            log.warn("[TENANT] store_mismatch (forbidden) user={} userStore={} storefrontStore={} headerStore={} host={}",
                                username,
                                userStore.getCode(),
                                storefrontStore.getCode(),
                                request.getHeader(StoreResolverService.STORE_HEADER),
                                getEffectiveHost(request)
                            );
                            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Store mismatch: this host resolves to a different store than your account. "
                                    + "Map the domain to your store, align X-Store-Code, or use an admin user for that store.");
                            return;
                        }

                        resolved = storefrontStore;
                    }
                } else if (resolved == null) {
                    // Delivery / other flows still benefit from a tenant context for logging; but do not require it.
                    try {
                        resolved = storeResolverService.resolveByCodeOrDomainOrDefault(
                            request.getHeader(StoreResolverService.STORE_HEADER),
                            getEffectiveHost(request)
                        );
                    } catch (RuntimeException ignore) {
                        resolved = null;
                    }
                }
            }

            StoreContext.set(resolved);
            if (resolved != null) {
                MDC.put("storeCode", resolved.getCode());
                MDC.put("storeId", resolved.getId() == null ? "" : String.valueOf(resolved.getId()));
            }
            filterChain.doFilter(request, response);
        } finally {
            StoreContext.clear();
            MDC.remove("storeCode");
            MDC.remove("storeId");
        }
    }
}

