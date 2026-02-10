import { Link } from "react-router-dom";
import { ShoppingCart, Search, Menu } from "lucide-react";
import { useState } from "react";
import useCartStore from "../../store/cartStore";
import CartDrawer from "../cart/CartDrawer";

const Header = () => {
  const itemCount = useCartStore((state) => state.itemCount);
  const [isCartOpen, setIsCartOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 bg-secondary text-white shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16 md:h-20">
          {/* Menu Mobile */}
          <div className="flex md:hidden">
            <button className="p-2 rounded-md hover:bg-secondary-light transition-colors">
              <Menu className="h-6 w-6" />
            </button>
          </div>

          {/* Logo */}
          <div className="flex-shrink-0 flex items-center h-full py-2">
            <Link to="/" className="flex items-center gap-3 group">
              <div className="bg-white p-1 rounded-lg shadow-sm group-hover:shadow-md transition-shadow">
                <img
                  src="/logo-sucre.png"
                  alt="Candy Sucré Store"
                  className="h-12 md:h-14 w-auto object-contain"
                />
              </div>
              <div className="hidden lg:flex flex-col -gap-1">
                <span className="text-xl font-bold tracking-tight text-white font-brand-serif leading-tight">
                  Candy{" "}
                  <span className="text-primary tracking-widest uppercase text-xs block">
                    Sucré Store
                  </span>
                </span>
              </div>
            </Link>
          </div>

          {/* Navigation Desktop */}
          {/* Navigation Desktop supprimée : Le logo suffit pour l'accueil */}
          <nav className="hidden md:flex space-x-8">
            {/* Menu simplifié : Pas de liens de navigation explicites demandés */}
          </nav>

          {/* Actions */}
          <div className="flex items-center space-x-4">
            <button className="p-2 rounded-full hover:bg-secondary-light transition-colors text-gray-300">
              <Search className="h-5 w-5" />
            </button>
            <button
              onClick={() => setIsCartOpen(true)}
              className="relative p-2 rounded-full hover:bg-secondary-light transition-colors text-primary bg-secondary-light/50"
            >
              <ShoppingCart className="h-6 w-6" />
              {itemCount > 0 && (
                <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-white animate-bounce-subtle">
                  {itemCount}
                </span>
              )}
            </button>
          </div>
        </div>
      </div>

      <CartDrawer isOpen={isCartOpen} onClose={() => setIsCartOpen(false)} />
    </header>
  );
};

export default Header;
