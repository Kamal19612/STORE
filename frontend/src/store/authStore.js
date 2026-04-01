import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import api from "../services/api";

/**
 * Store Zustand pour gérer l'état d'authentification
 * Persiste le token dans localStorage pour que la session survive
 * au redémarrage de la PWA installée (standalone).
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
        // Nouvelle logique avec List<String> depuis le back
        // authData.roles est maintenant un tableau : ["ROLE_ADMIN", "ROLE_LIVREUR"]
        const roles = Array.isArray(authData.roles) ? authData.roles : [];

        // On prend le premier rôle trouvé et on retire le préfixe "ROLE_" pour la compatibilité frontend
        // Ex: "ROLE_ADMIN" -> "ADMIN"
        let role = null;
        if (roles.length > 0) {
          role = roles[0].replace("ROLE_", "");
        }

        const user = {
          username: authData.username,
          role: role,
        };

        // localStorage persiste entre les sessions — nécessaire pour la PWA standalone

        set({
          user,
          token: authData.token,
          isAuthenticated: true,
        });
      },

      /**
       * Déconnecte l'utilisateur et nettoie le state
       */
      logout: async () => {
        try {
          await api.post("/auth/logout");
        } catch (error) {
          console.error("Erreur déconnexion serveur (ignorée):", error);
        } finally {
          localStorage.removeItem("auth-storage");
          set({
            user: null,
            token: null,
            isAuthenticated: false,
          });
        }
      },

      /**
       * Vérifie si l'utilisateur est toujours authentifié
       * @returns {boolean}
       */
      checkAuth: () => {
        const { token, user } = get();

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
      name: "auth-storage",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);

export default useAuthStore;
