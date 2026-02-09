import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * Store Zustand pour gérer l'état d'authentification
 * Persiste le token et les informations utilisateur dans localStorage
 */
const useAuthStore = create(
  persist(
    (set, get) => ({
      // État
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,

      /**
       * Connecte l'utilisateur et stocke le token JWT
       * @param {Object} authData - { token, username, roles }
       */
      login: (authData) => {
        // Parser le rôle depuis "[ROLE_ADMIN]" vers "ADMIN"
        const roleString = authData.roles || "";
        const roleMatch = roleString.match(/ROLE_(\w+)/);
        const role = roleMatch ? roleMatch[1] : null;

        const user = {
          username: authData.username,
          role: role,
        };

        // Stocker dans localStorage (persist s'en occupe automatiquement)
        localStorage.setItem("token", authData.token);

        set({
          user,
          token: authData.token,
          isAuthenticated: true,
        });
      },

      /**
       * Déconnecte l'utilisateur et nettoie le state
       */
      logout: () => {
        localStorage.removeItem("token");
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });
      },

      /**
       * Vérifie si l'utilisateur est toujours authentifié
       * @returns {boolean}
       */
      checkAuth: () => {
        const token = localStorage.getItem("token");
        const { user } = get();

        if (token && user) {
          return true;
        }

        // Nettoyer si token invalide
        get().logout();
        return false;
      },

      /**
       * Met à jour l'état de chargement
       * @param {boolean} loading
       */
      setLoading: (loading) => set({ isLoading: loading }),
    }),
    {
      name: "auth-storage", // Nom de la clé dans localStorage
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);

export default useAuthStore;
