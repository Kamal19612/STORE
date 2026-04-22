import axios from "axios";

import useAuthStore from "../store/authStore";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  timeout: 15000, // 15s — évite les requêtes qui pendent indéfiniment sur réseau mobile
});

// Intercepteur pour ajouter le token JWT si présent
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercepteur pour gérer les erreurs d'authentification
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Si le token est expiré ou invalide (401), déconnecter l'utilisateur
    if (error.response?.status === 401) {
      const currentPath = window.location.pathname;

      // Ne pas rediriger si déjà sur la page de login
      if (!currentPath.includes("/login")) {
        // Effacer le state directement sans rappeler l'API (évite la cascade 401)
        localStorage.removeItem("auth-storage");
        useAuthStore.setState({ user: null, token: null, isAuthenticated: false });
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);

export const getPublicSettings = () => api.get("/public/settings");
export const getAdminSettings = () => api.get("/admin/settings");
export const updateSettings = (settings) =>
  api.put("/admin/settings", settings);

export const resetStats = () => api.post("/admin/dashboard/reset-stats");
export const syncProducts = () => api.post("/admin/products/google-sheets-sync");

export default api;
