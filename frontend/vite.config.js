import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  // Si tu développes derrière un reverse-proxy HTTPS (ex: https://sucrestore.socialracine.com),
  // tu peux activer un HMR explicite via:
  //   VITE_HMR_HOST=sucrestore.socialracine.com
  //   VITE_HMR_PROTOCOL=wss
  //   VITE_HMR_CLIENT_PORT=443
  const hmrHost = env.VITE_HMR_HOST?.trim();
  const hmr =
    hmrHost && hmrHost.length > 0
      ? {
          host: hmrHost,
          protocol: env.VITE_HMR_PROTOCOL?.trim() || "wss",
          clientPort: Number(env.VITE_HMR_CLIENT_PORT || 443),
        }
      : undefined;

  return {
    plugins: [react(), tailwindcss()],
    server: {
      proxy: {
        "/api": {
          target: "http://localhost:8082",
          changeOrigin: true,
        },
        "/uploads": {
          target: "http://localhost:8082",
          changeOrigin: true,
        },
      },
      // En dev, on accepte tous les hosts. Si tu veux verrouiller, mets VITE_HMR_HOST
      // et configure allowedHosts en conséquence.
      allowedHosts: "all",
      hmr,
    },
  };
});
