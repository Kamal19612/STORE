import {
  useState,
  useEffect,
  createContext,
  useContext,
  useMemo,
  memo,
} from "react";
import { Link, useLocation } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import {
  LayoutDashboard,
  ShoppingBag,
  Package,
  Image as ImageIcon,
  Users,
  Settings,
  LogOut,
  ChevronLeft,
  Sun,
  Moon,
  Menu,
} from "lucide-react";
import { useTheme } from "../context/ThemeContext";

const SidebarContext = createContext();

export default function Sidebar({ user, logout, isMobileOpen, onMobileClose }) {
  const [expanded, setExpanded] = useState(true);
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();

  // Close sidebar on route change (mobile)
  useEffect(() => {
    if (window.innerWidth < 1024) {
      onMobileClose();
    }
  }, [location.pathname]);

  const menuItems = useMemo(() => {
    const items = [
      { path: "/admin/dashboard", icon: LayoutDashboard, text: "Dashboard" },
      { path: "/admin/orders", icon: ShoppingBag, text: "Commandes" },
      { path: "/admin/products", icon: Package, text: "Produits" },
    ];

    if (user?.role === "ADMIN" || user?.role === "SUPER_ADMIN") {
      items.push({ path: "/admin/slider", icon: ImageIcon, text: "Carrousel" });
    }

    if (user?.role === "SUPER_ADMIN") {
      items.push({ path: "/admin/users", icon: Users, text: "Utilisateurs" });
      items.push({
        path: "/admin/settings",
        icon: Settings,
        text: "Paramètres",
      });
    }
    return items;
  }, [user?.role]);

  const contextValue = useMemo(() => ({ expanded, theme }), [expanded, theme]);

  return (
    <>
      {/* Mobile Overlay */}
      <AnimatePresence>
        {isMobileOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 0.6 }}
            exit={{ opacity: 0 }}
            onClick={onMobileClose}
            className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden"
          />
        )}
      </AnimatePresence>

      <SidebarContext.Provider value={contextValue}>
        <motion.aside
          initial={false}
          animate={{
            width: expanded ? 260 : 80,
            transition: { type: "spring", damping: 25, stiffness: 200 },
          }}
          className={`
            fixed lg:static inset-y-0 left-0 z-50 flex flex-col 
            bg-white/75 dark:bg-[#242021]/75 backdrop-blur-md
            border-r border-white/20 dark:border-white/5
            shadow-xl h-full will-change-[width] overflow-hidden
            ${isMobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"}
          `}
        >
          {/* Header (Logo) */}
          <div className="flex items-center px-4 h-20 shrink-0 relative">
            <div className="flex items-center gap-3 min-w-max">
              <div className="relative group cursor-pointer shrink-0">
                <div className="absolute -inset-1 bg-gradient-to-r from-[#f5ad41] to-[#d89a35] rounded-full opacity-75 group-hover:opacity-100 blur transition-opacity duration-200"></div>
                <img
                  src="/logo-sucre.png"
                  alt="Logo"
                  className="relative h-9 w-auto min-w-[36px]"
                />
              </div>
              <span
                className={`font-brand-serif font-bold text-2xl text-[#242021] dark:text-[#f5ad41] tracking-wide whitespace-nowrap transition-opacity duration-200 ${expanded ? "opacity-100 delay-100" : "opacity-0"}`}
              >
                Sucre
              </span>
            </div>

            {/* Toggle Button */}
            <button
              onClick={() => setExpanded((curr) => !curr)}
              className={`
                p-2 rounded-full 
                bg-white/50 dark:bg-[#3a3638]/50 
                hover:bg-[#f5ad41]/20 dark:hover:bg-[#f5ad41]/20
                text-gray-600 dark:text-gray-300 
                border border-white/20 dark:border-white/5
                backdrop-blur-sm shadow-sm
                transition-colors duration-200 hidden lg:flex items-center justify-center
                absolute -right-3 top-1/2 -translate-y-1/2
                z-50
              `}
            >
              {expanded ? <ChevronLeft size={16} /> : <Menu size={20} />}
            </button>

            {/* Mobile Close Button */}
            <button
              onClick={onMobileClose}
              className="p-2 rounded-lg text-gray-600 dark:text-[#f5ad41] lg:hidden ml-auto"
            >
              <ChevronLeft size={24} />
            </button>
          </div>

          {/* Navigation Items */}
          <ul className="flex-1 px-3 space-y-2 py-6 overflow-y-auto overflow-x-hidden no-scrollbar">
            {menuItems.map((item) => (
              <SidebarItem
                key={item.path}
                {...item}
                active={location.pathname.startsWith(item.path)}
              />
            ))}
          </ul>

          {/* Footer Area */}
          <div className="p-4 border-t border-gray-200/30 dark:border-white/10 mx-2 shrink-0 overflow-hidden">
            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className={`
                flex items-center p-3 rounded-xl cursor-pointer transition-colors duration-200
                hover:bg-gradient-to-r hover:from-gray-100/50 hover:to-white/10 dark:hover:from-[#3a3638]/50 dark:hover:to-transparent
                text-[#242021] dark:text-[#f5ad41]
                group relative w-full
                ${expanded ? "justify-start gap-3" : "justify-center"}
              `}
            >
              <div className="absolute inset-0 bg-white/5 dark:bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity" />
              <div className="shrink-0">
                {theme === "light" ? <Moon size={20} /> : <Sun size={20} />}
              </div>
              <span
                className={`transition-opacity duration-200 whitespace-nowrap font-medium ${expanded ? "opacity-100 delay-100" : "opacity-0 w-0"}`}
              >
                {theme === "light" ? "Mode Sombre" : "Mode Clair"}
              </span>
            </button>

            {/* User Profile */}
            <div
              className={`mt-2 flex items-center p-2 rounded-xl bg-white/40 dark:bg-[#3a3638]/40 border border-white/20 dark:border-white/5 shadow-sm backdrop-blur-md transition-all ${expanded ? "gap-3" : "justify-center"}`}
            >
              <div className="w-9 h-9 rounded-full bg-gradient-to-br from-[#f5ad41] to-[#d89a35] flex items-center justify-center text-white font-bold text-sm shadow-md shrink-0">
                {user?.username?.charAt(0).toUpperCase() || "A"}
              </div>

              <div
                className={`flex-1 min-w-0 transition-opacity duration-200 ${expanded ? "opacity-100" : "opacity-0 w-0 hidden"}`}
              >
                <h4 className="font-semibold text-sm text-[#242021] dark:text-white truncate">
                  {user?.username || "Admin"}
                </h4>
                <p className="text-[10px] text-gray-500 dark:text-gray-400 font-medium uppercase tracking-wider">
                  {user?.role || "ADMIN"}
                </p>
              </div>

              {expanded && (
                <button
                  onClick={logout}
                  className="p-1.5 rounded-lg hover:bg-red-50 hover:text-red-500 text-gray-400 transition-colors shrink-0"
                  title="Se déconnecter"
                >
                  <LogOut size={16} />
                </button>
              )}
            </div>
          </div>
        </motion.aside>
      </SidebarContext.Provider>
    </>
  );
}

const SidebarItem = memo(function SidebarItem({
  icon: Icon,
  text,
  active,
  path,
}) {
  const { expanded } = useContext(SidebarContext);

  return (
    <li>
      <Link
        to={path}
        className={`
          relative flex items-center py-3 px-3.5
          font-medium text-sm rounded-xl cursor-pointer
          transition-all duration-200 group
          ${
            active
              ? "bg-gradient-to-r from-[#f5ad41]/20 to-[#f5ad41]/5 text-[#d89a35] dark:text-[#f5ad41] shadow-[0_0_20px_rgba(245,173,65,0.15)] ring-1 ring-[#f5ad41]/30"
              : "text-gray-700 dark:text-gray-300 hover:text-[#242021] dark:hover:text-white hover:bg-white/50 dark:hover:bg-white/10"
          }
          ${expanded ? "justify-start gap-3" : "justify-center"}
        `}
      >
        <Icon
          size={20}
          className={`shrink-0 transition-transform duration-200 ${active ? "scale-110" : "group-hover:scale-110"}`}
        />

        <span
          className={`transition-opacity duration-200 whitespace-nowrap ${
            expanded
              ? "opacity-100 translate-x-0 delay-75"
              : "opacity-0 -translate-x-2 absolute left-12 pointer-events-none"
          }`}
        >
          {text}
        </span>

        {/* Active Indicator Line for Expanded */}
        {active && expanded && (
          <motion.div
            layoutId="active-pill"
            className="absolute left-0 w-1 h-6 bg-[#f5ad41] rounded-r-full"
          />
        )}

        {/* Tooltip for collapsed state */}
        {!expanded && (
          <div
            className={`
              absolute left-full top-1/2 -translate-y-1/2 rounded-lg px-3 py-1.5 ml-4
              bg-[#242021] dark:bg-white text-white dark:text-[#242021] text-xs font-semibold
              invisible opacity-0 -translate-x-2 transition-all duration-200
              group-hover:visible group-hover:opacity-100 group-hover:translate-x-0
              z-50 whitespace-nowrap shadow-xl border border-white/10
            `}
          >
            {text}
            <div className="absolute left-0 top-1/2 -translate-x-1 -translate-y-1/2 w-2 h-2 bg-[#242021] dark:bg-white rotate-45" />
          </div>
        )}
      </Link>
    </li>
  );
});
