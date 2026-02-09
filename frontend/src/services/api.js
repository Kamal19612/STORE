import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Intercepteur pour ajouter le token JWT si présent
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
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
      if (!currentPath.includes("/admin/login")) {
        localStorage.removeItem("token");
        localStorage.removeItem("auth-storage");
        window.location.href = "/admin/login";
      }
    }
    return Promise.reject(error);
  },
);

export default api;
