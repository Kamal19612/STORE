import { useEffect } from "react";
import { Navigate, useLocation } from "react-router-dom";
import useAuthStore from "../store/authStore";

/**
 * Composant de protection des routes admin
 * Redirige vers /login si l'utilisateur n'est pas authentifié
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children - Composants enfants à protéger
 * @param {string[]} props.allowedRoles - Rôles autorisés (optionnel)
 */
const PrivateRoute = ({ children, allowedRoles = [] }) => {
  const { isAuthenticated, user, checkAuth } = useAuthStore();
  const location = useLocation();

  // Vérifier l'authentification au montage
  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  if (!isAuthenticated) {
    // Rediriger vers login en conservant la destination souhaitée
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Vérifier les rôles si spécifiés
  if (allowedRoles.length > 0 && user?.role) {
    if (!allowedRoles.includes(user.role)) {
      // Rediriger vers dashboard approprié si rôle non autorisé
      if (user.role === "DELIVERY_AGENT") {
        return <Navigate to="/delivery/dashboard" replace />;
      }
      return <Navigate to="/admin/dashboard" replace />;
    }
  }

  return children;
};

export default PrivateRoute;
