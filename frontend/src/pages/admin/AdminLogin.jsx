import { useState, useEffect } from "react";
import { useNavigate, Navigate } from "react-router-dom";
import { toast } from "react-toastify";
import useAuthStore from "../../store/authStore";
import authService from "../../services/authService";
import { triggerInstallPrompt, isInstallAvailable } from "../../hooks/useInstallPWA";

/**
 * Page de connexion pour les administrateurs
 */
const AdminLogin = () => {
  const [formData, setFormData] = useState({ username: "", password: "" });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [installable, setInstallable] = useState(isInstallAvailable);
  const isStandalone = window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone;
  const isIOS = /iphone|ipad|ipod/i.test(navigator.userAgent);

  useEffect(() => {
    const onInstallable = () => setInstallable(true);
    const onInstalled = () => setInstallable(false);
    window.addEventListener("pwa:installable", onInstallable);
    window.addEventListener("pwa:installed", onInstalled);
    return () => {
      window.removeEventListener("pwa:installable", onInstallable);
      window.removeEventListener("pwa:installed", onInstalled);
    };
  }, []);

  const navigate = useNavigate();
  const { login, isAuthenticated, user } = useAuthStore();

  // Redirect synchronously if already logged in (no useEffect = no double-navigate)
  if (isAuthenticated && user) {
    const target =
      user.role === "DELIVERY_AGENT"
        ? "/delivery/dashboard"
        : "/admin/dashboard";
    return <Navigate to={target} replace />;
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
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

  return (
    <div className="min-h-svh flex items-center justify-center bg-gradient-to-br from-amber-50 via-white to-yellow-50 px-4 py-8 overflow-y-auto">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-2xl p-6 sm:p-8 border border-gray-100">
          <div className="text-center mb-6 sm:mb-8">
            <img
              src="/logo-sucre.png"
              alt="Sucre Store"
              className="h-16 w-16 object-contain mx-auto mb-3 rounded-full"
            />
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-1">
              SUCRE STORE
            </h1>
            <p className="text-gray-600 text-sm">Espace Administrateur</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5 sm:space-y-6" autoComplete="on">
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
                placeholder="admin"
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
              className="w-full py-3 px-4 rounded-lg font-semibold text-gray-900 bg-amber-400 bg-gradient-to-r from-yellow-400 to-amber-500 hover:bg-amber-500 active:bg-amber-600 active:opacity-90 transition-colors duration-200 shadow-lg disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {/* Structure DOM stable : toujours présents, visibilité par CSS
                  évite le crash removeChild causé par les extensions navigateur */}
              <svg
                className={`h-5 w-5 text-gray-900 shrink-0 ${isSubmitting ? "animate-spin" : "hidden"}`}
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              <span>{isSubmitting ? "Connexion en cours..." : "Se connecter"}</span>
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              Accès réservé aux administrateurs uniquement
            </p>
          </div>

          {!isStandalone && (
            <div className="mt-4">
              {installable ? (
                <button
                  type="button"
                  onClick={triggerInstallPrompt}
                  className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg border border-amber-300 bg-amber-50 text-amber-700 hover:bg-amber-100 transition-colors text-sm font-medium"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  Installer l'application
                </button>
              ) : isIOS ? (
                <p className="text-center text-xs text-gray-400">
                  Pour installer : touchez <strong>Partager</strong> puis <strong>Sur l'écran d'accueil</strong>
                </p>
              ) : (
                <p className="text-center text-xs text-gray-400">
                  Pour installer : menu du navigateur <strong>⋮</strong> → <strong>Ajouter à l'écran d'accueil</strong>
                </p>
              )}
            </div>
          )}
        </div>

        <div className="mt-4 text-center text-xs text-gray-500">
          Connexion sécurisée par JWT
        </div>
      </div>
    </div>
  );
};

export default AdminLogin;
