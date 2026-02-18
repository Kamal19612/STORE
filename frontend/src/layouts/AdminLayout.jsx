import { useState } from "react";
import { Outlet, Link, useLocation } from "react-router-dom";
import useAuthStore from "../store/authStore";
import {
  Menu,
  X,
  LayoutDashboard,
  ShoppingBag,
  Package,
  Image as ImageIcon,
  Users,
  Settings,
  LogOut,
} from "lucide-react";

/**
 * Composant de layout pour les pages admin
 * Sidebar statique sur Desktop, Tiroir sur Mobile
 */
const AdminLayout = () => {
  const { user, logout } = useAuthStore();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const location = useLocation();

  const handleLogout = () => {
    logout();
    window.location.href = "/admin/login";
  };

  const closeSidebar = () => setIsSidebarOpen(false);

  const isActive = (path) => location.pathname.startsWith(path);

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Mobile Sidebar Overlay */}
      {isSidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={closeSidebar}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`
          fixed lg:static inset-y-0 left-0 z-50 w-64 bg-gradient-to-b from-gray-800 to-gray-900 text-white flex flex-col transition-transform duration-300 ease-in-out
          ${isSidebarOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"}
        `}
      >
        {/* Logo & Close Button */}
        <div className="p-6 border-b border-gray-700 flex justify-between items-center">
          <div>
            <img
              src="/logo-sucre.png"
              alt="Sucre Store"
              className="h-10 w-auto"
            />
          </div>
          <button
            onClick={closeSidebar}
            className="lg:hidden text-gray-400 hover:text-white"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
          <Link
            to="/admin/dashboard"
            onClick={closeSidebar}
            className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
              isActive("/admin/dashboard")
                ? "bg-primary text-secondary font-bold"
                : "hover:bg-gray-700 text-gray-300"
            }`}
          >
            <LayoutDashboard className="w-5 h-5" />
            <span>Dashboard</span>
          </Link>

          <Link
            to="/admin/orders"
            onClick={closeSidebar}
            className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
              isActive("/admin/orders")
                ? "bg-primary text-secondary font-bold"
                : "hover:bg-gray-700 text-gray-300"
            }`}
          >
            <ShoppingBag className="w-5 h-5" />
            <span>Commandes</span>
          </Link>

          <Link
            to="/admin/products"
            onClick={closeSidebar}
            className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
              isActive("/admin/products")
                ? "bg-primary text-secondary font-bold"
                : "hover:bg-gray-700 text-gray-300"
            }`}
          >
            <Package className="w-5 h-5" />
            <span>Produits</span>
          </Link>

          <Link
            to="/admin/slider"
            onClick={closeSidebar}
            className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
              isActive("/admin/slider")
                ? "bg-primary text-secondary font-bold"
                : "hover:bg-gray-700 text-gray-300"
            }`}
          >
            <ImageIcon className="w-5 h-5" />
            <span>Carrousel</span>
          </Link>

          <Link
            to="/admin/settings"
            onClick={closeSidebar}
            className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
              isActive("/admin/settings")
                ? "bg-primary text-secondary font-bold"
                : "hover:bg-gray-700 text-gray-300"
            }`}
          >
            <Settings className="w-5 h-5" />
            <span>Paramètres</span>
          </Link>

          {user?.role === "SUPER_ADMIN" && (
            <Link
              to="/admin/users"
              onClick={closeSidebar}
              className={`flex items-center space-x-3 px-4 py-3 rounded-lg transition-colors ${
                isActive("/admin/users")
                  ? "bg-primary text-secondary font-bold"
                  : "hover:bg-gray-700 text-gray-300"
              }`}
            >
              <Users className="w-5 h-5" />
              <span>Utilisateurs</span>
            </Link>
          )}
        </nav>

        {/* User Info & Logout */}
        <div className="p-4 border-t border-gray-700">
          <div className="flex items-center space-x-3 mb-3">
            <div className="w-10 h-10 rounded-full bg-gradient-to-r from-yellow-400 to-amber-500 flex items-center justify-center">
              <span className="text-white font-bold text-sm">
                {user?.username?.charAt(0).toUpperCase() || "A"}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">
                {user?.username || "Admin"}
              </p>
              <p className="text-xs text-gray-400 truncate">
                {user?.role || "ADMIN"}
              </p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 transition-colors text-sm font-medium"
          >
            <LogOut className="w-4 h-4" />
            <span>Déconnexion</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <header className="bg-white shadow-sm border-b border-gray-200 px-4 lg:px-8 py-4 sticky top-0 z-30">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setIsSidebarOpen(true)}
                className="lg:hidden p-2 rounded-md hover:bg-gray-100"
              >
                <Menu className="h-6 w-6 text-gray-600" />
              </button>
              <h2 className="text-xl lg:text-2xl font-semibold text-gray-800 truncate">
                Tableau de bord
              </h2>
            </div>

            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-600 hidden sm:block">
                {new Date().toLocaleDateString("fr-FR", {
                  weekday: "long",
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                })}
              </span>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 p-4 lg:p-8 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
