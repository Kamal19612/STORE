import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import useAuthStore from "../../store/authStore";
import authService from "../../services/authService";

/**
 * Page de connexion pour les administrateurs
 */
const AdminLogin = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuthStore();

  // Nettoyage au montage : Si on arrive sur cette page, on force un état propre
  // Sauf si on est déjà authentifié et qu'on vient d'arriver (cas d'un refresh ou accès direct)
  useEffect(() => {
    // Si on arrive sur LOGIN alors qu'on est déjà auth, on redirige
    // MAIS on ne dépend plus de [isAuthenticated] pour ne pas déclencher ça APRES le login submit
    const token = localStorage.getItem("token");
    if (token) {
      // Déjà connecté ? On redirige vers le dashboard par défaut (ou on pourrait logout)
      // Pour éviter les boucles, on va plutôt faire un check initial.
      // Si l'utilisateur veut changer de compte, il doit cliquer sur Logout qui nettoie le token.
    }
  }, []);

  // On désactive l'effet "réactif" qui causait le bug.
  // La redirection se fera UNIQUEMENT dans handleSubmit.

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation simple
    if (!formData.username.trim() || !formData.password.trim()) {
      toast.error("Veuillez remplir tous les champs");
      return;
    }

    setIsSubmitting(true);

    try {
      // Appel API
      const authData = await authService.login(
        formData.username,
        formData.password,
      );

      // Stocker dans le store
      login(authData);

      toast.success(`Bienvenue ${authData.username} !`);

      // Redirection dynamique selon le rôle
      // La réponse est maintenant propre : ["ROLE_ADMIN"]
      const roles = Array.isArray(authData.roles) ? authData.roles : [];
      console.log("LOGIN SUCCESS - Roles:", roles);

      let targetPath = "/admin/dashboard";

      // Hiérarchie de redirection
      if (roles.includes("ROLE_DELIVERY_AGENT")) {
        targetPath = "/delivery/dashboard";
      }

      // L'admin surcharge toujours le livreur si présent (sécurité)
      if (
        roles.includes("ROLE_ADMIN") ||
        roles.includes("ROLE_SUPER_ADMIN") ||
        roles.includes("ROLE_MANAGER")
      ) {
        targetPath = "/admin/dashboard";
      }

      // 3. Navigation Force
      navigate(targetPath, { replace: true });
    } catch (error) {
      toast.error(error.message || "Erreur de connexion");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-amber-50 via-white to-yellow-50">
      {/* Container principal */}
      <div className="w-full max-w-md px-6">
        {/* Card de connexion */}
        <div className="bg-white rounded-2xl shadow-2xl p-8 border border-gray-100">
          {/* Logo et titre */}
          <div className="text-center mb-8">
            <div className="inline-block p-3 bg-gradient-to-r from-yellow-400 to-amber-500 rounded-full mb-4">
              <svg
                className="w-10 h-10 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                />
              </svg>
            </div>
            <h1 className="text-3xl font-bold text-gray-800 mb-2">
              SUCRE STORE
            </h1>
            <p className="text-gray-600">Espace Administrateur</p>
          </div>

          {/* Formulaire */}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Champ Username */}
            <div>
              <label
                htmlFor="username"
                className="block text-sm font-medium text-gray-700 mb-2"
              >
                Nom d'utilisateur
              </label>
              <input
                type="text"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-400 focus:border-transparent transition-all"
                placeholder="admin@example.com"
                required
                autoComplete="username"
              />
            </div>

            {/* Champ Password */}
            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700 mb-2"
              >
                Mot de passe
              </label>
              <input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-400 focus:border-transparent transition-all"
                placeholder="••••••••"
                required
                autoComplete="current-password"
              />
            </div>

            {/* Bouton de soumission */}
            <button
              type="submit"
              disabled={isSubmitting}
              className={`
                w-full py-3 px-4 rounded-lg font-semibold text-white
                bg-gradient-to-r from-yellow-400 to-amber-500
                hover:from-yellow-500 hover:to-amber-600
                transform hover:scale-[1.02] active:scale-[0.98]
                transition-all duration-200 shadow-lg
                disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none
                flex items-center justify-center
              `}
            >
              {isSubmitting ? (
                <>
                  <svg
                    className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    ></circle>
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    ></path>
                  </svg>
                  Connexion en cours...
                </>
              ) : (
                "Se connecter"
              )}
            </button>
          </form>

          {/* Footer */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              Accès réservé aux administrateurs uniquement
            </p>
          </div>
        </div>

        {/* Note de sécurité */}
        <div className="mt-4 text-center text-xs text-gray-500">
          Connexion sécurisée par JWT
        </div>
      </div>
    </div>
  );
};

export default AdminLogin;
