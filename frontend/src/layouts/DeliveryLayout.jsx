import { Outlet, Link, useNavigate } from "react-router-dom";
import { LogOut, MapPin, Package } from "lucide-react";
import useAuthStore from "../store/authStore";

const DeliveryLayout = () => {
  const { logout, user } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-gray-100 font-sans pb-20">
      {/* Header Mobile */}
      <header className="bg-primary text-white p-4 shadow-md sticky top-0 z-50">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <img
              src="/logo-sucre.png"
              alt="Sucre Store"
              className="h-8 w-auto bg-white rounded-md p-0.5"
            />
            <span className="font-bold text-lg">Espace Livreur</span>
          </div>
          <div className="text-xs opacity-90">Bonjour, {user?.username}</div>
        </div>
      </header>

      {/* Contenu Principal */}
      <main className="p-4 max-w-md mx-auto">
        <Outlet />
      </main>

      {/* Barre de Navigation Mobile (Bottom) */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex justify-around p-3 z-50 shadow-[0_-2px_10px_rgba(0,0,0,0.05)]">
        <Link
          to="/delivery/dashboard"
          className="flex flex-col items-center gap-1 text-primary"
        >
          <Package className="h-6 w-6" />
          <span className="text-xs font-medium">Commandes</span>
        </Link>

        <button
          onClick={handleLogout}
          className="flex flex-col items-center gap-1 text-gray-400 hover:text-red-500 transition-colors"
        >
          <LogOut className="h-6 w-6" />
          <span className="text-xs font-medium">Déconnexion</span>
        </button>
      </nav>
    </div>
  );
};

export default DeliveryLayout;
