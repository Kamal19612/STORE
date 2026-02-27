import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "react-toastify";
import useAuthStore from "../../store/authStore";
import authService from "../../services/authService";
import { LogOut, LayoutDashboard, User, ShieldCheck } from "lucide-react";

/**
 * Page de connexion pour les administrateurs
 */
const AdminLogin = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showLogin, setShowLogin] = useState(false); // Pour forcer l'affichage du login même si connecté

  const navigate = useNavigate();
  const { login, logout, isAuthenticated, user } = useAuthStore();

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated && user && !showLogin) {
      const target =
        user.role === "DELIVERY_AGENT"
          ? "/delivery/dashboard"
          : "/admin/dashboard";
      navigate(target, { replace: true });
    }
  }, [isAuthenticated, user, showLogin, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleLogoutAndSwitch = () => {
    logout();
    setFormData({ username: "", password: "" });
    setShowLogin(true); // Affiche le formulaire
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.username.trim() || !formData.password.trim()) {
      toast.error("Veuillez remplir tous les champs");
      return;
    }

    setIsSubmitting(true);

    try {
      const authData = await authService.login(
        formData.username,
        formData.password,
      );

      login(authData);
      toast.success(`Bienvenue ${authData.username} !`);

      const roles = Array.isArray(authData.roles) ? authData.roles : [];
      let targetPath = "/admin/dashboard";

      if (roles.includes("ROLE_DELIVERY_AGENT")) {
        targetPath = "/delivery/dashboard";
      }
      if (
        roles.includes("ROLE_ADMIN") ||
        roles.includes("ROLE_SUPER_ADMIN") ||
        roles.includes("ROLE_MANAGER")
      ) {
        targetPath = "/admin/dashboard";
      }

      navigate(targetPath, { replace: true });
    } catch (error) {
      toast.error(error.message || "Erreur de connexion");
    } finally {
      setIsSubmitting(false);
    }
  };

  // --- Rendu : Formulaire de Connexion ---
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-amber-50 via-white to-yellow-50">
      <div className="w-full max-w-md px-6">
        <div className="bg-white rounded-2xl shadow-2xl p-8 border border-gray-100">
          <div className="text-center mb-8">
            <img
              src="/logo-sucre.png"
              alt="Sucre Store"
              className="h-20 w-auto mx-auto mb-4"
            />
            <h1 className="text-3xl font-bold text-gray-800 mb-2">
              SUCRE STORE
            </h1>
            <p className="text-gray-600">Espace Administrateur</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
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

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              Accès réservé aux administrateurs uniquement
            </p>
          </div>
        </div>

        <div className="mt-4 text-center text-xs text-gray-500">
          Connexion sécurisée par JWT
        </div>
      </div>
    </div>
  );
};

export default AdminLogin;
