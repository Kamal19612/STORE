function deriveStoreFromHostname() {
  const host = typeof window !== "undefined" ? window.location.hostname : "";
  const first = host.split(".")[0];
  if (first && first.length >= 2 && first !== "www" && first !== "localhost") {
    return first.toLowerCase();
  }
  return null;
}

/**
 * Returns an explicitly selected store code (env or localStorage).
 * Use this to set `X-Store-Code` ONLY when strictly needed (e.g. SUPER_ADMIN cross-store).
 *
 * IMPORTANT: Do not derive from hostname here — sending an unmapped subdomain
 * can cause 400 "Unknown store code" in strict tenant mode.
 */
export function getExplicitStoreCode() {
  const env = import.meta.env.VITE_STORE_CODE;
  if (typeof env === "string" && env.trim()) return env.trim().toLowerCase();

  try {
    const v = localStorage.getItem("active_store_code");
    if (v && v.trim()) return v.trim().toLowerCase();
  } catch {
    // ignore
  }

  return null;
}

/**
 * Returns the effective store code for UI purposes (can derive from hostname).
 * Backend should primarily resolve tenant from Host; `X-Store-Code` is optional.
 */
export function getStoreCode() {
  return getExplicitStoreCode() ?? deriveStoreFromHostname() ?? "sucre";
}

