import { useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import useAuthStore from "../store/authStore";
import { Menu } from "lucide-react";
import Sidebar from "../components/Sidebar";

/**
 * Composant de layout pour les pages admin
 * Sidebar statique sur Desktop, Tiroir sur Mobile
 */
const AdminLayout = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate("/admin/login");
  };

  const closeSidebar = () => setIsSidebarOpen(false);

  return (
    <div className="h-screen w-full flex bg-gray-50 dark:bg-[#1c191a] relative overflow-hidden transition-colors duration-300">
      {/* Background Pattern/Gradient for Glassmorphism Context - Optimized (Static for Perf) */}
      <div className="fixed inset-0 z-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-[var(--color-primary)]/5 rounded-full blur-[80px] will-change-transform" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-[var(--color-primary)]/5 rounded-full blur-[80px] will-change-transform" />
        <div className="absolute inset-0 bg-[url('/grid-pattern.svg')] opacity-[0.02] dark:opacity-[0.05]" />
      </div>

      {/* Mobile Sidebar Overlay */}
      <Sidebar
        user={user}
        logout={handleLogout}
        isMobileOpen={isSidebarOpen}
        onMobileClose={closeSidebar}
      />

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 h-full relative z-10 overflow-hidden">
        {/* Header */}
        <header className="bg-white/80 dark:bg-[#242021]/80 backdrop-blur-md shadow-sm border-b border-gray-200/50 dark:border-white/5 px-2 sm:px-4 lg:px-8 py-2 sm:py-3 lg:py-4 shrink-0 transition-colors duration-300 relative z-50">
          <div className="flex items-center justify-between gap-2 sm:gap-4">
            <div className="flex items-center gap-2 min-w-0">
              <button
                type="button"
                onClick={() => setIsSidebarOpen(true)}
                className="lg:hidden p-2 rounded-md hover:bg-gray-100 dark:hover:bg-white/10 dark:text-white shrink-0 flex items-center justify-center"
                aria-label="Ouvrir le menu"
              >
                <Menu className="h-6 w-6 text-gray-700 dark:text-gray-200" />
              </button>
              <h2 className="text-base sm:text-xl lg:text-2xl font-semibold text-gray-800 dark:text-white truncate font-brand-serif">
                Tableau de bord
              </h2>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <span className="text-xs text-gray-500 dark:text-gray-400 hidden sm:block whitespace-nowrap">
                {new Date().toLocaleDateString("fr-FR", {
                  weekday: "short",
                  day: "numeric",
                  month: "short",
                })}
              </span>
            </div>
          </div>
        </header>

        {/* Page Content - Scrollable Area */}
        <main className="flex-1 p-2 sm:p-4 lg:p-6 overflow-y-auto overflow-x-hidden scroll-smooth">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
