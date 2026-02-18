import { Link } from "react-router-dom";
import { ShoppingCart, Menu, X } from "lucide-react";
import { useState, useEffect } from "react";
import useCartStore from "../../store/cartStore";
import CartDrawer from "../cart/CartDrawer";

const Header = () => {
  const items = useCartStore((state) => state.items);
  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Désactiver le scroll du body quand le menu mobile est ouvert
  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }

    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isMobileMenuOpen]);

  return (
    <header className="sticky top-0 z-50 bg-secondary text-white shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16 md:h-20">
          {/* Menu Mobile Button */}
          <div className="flex md:hidden">
            <button
              onClick={() => setIsMobileMenuOpen(true)}
              className="p-2 rounded-md hover:bg-secondary-light transition-colors"
              aria-label="Ouvrir le menu"
            >
              <Menu className="h-6 w-6" />
            </button>
          </div>

          {/* Logo */}
          <div className="flex-shrink-0 flex items-center h-full py-4">
            <Link to="/" className="flex items-center gap-3 group">
              <img
                src="/logo-sucre.png"
                alt="Candy Sucré Store"
                className="h-12 w-auto object-contain"
              />
            </Link>
          </div>

          {/* Navigation Desktop - Vide pour l'instant */}
          <nav className="hidden md:flex space-x-8"></nav>

          {/* Actions */}
          <div className="flex items-center space-x-4">
            <button
              id="cart-toggle"
              onClick={() => setIsCartOpen(true)}
              className={`relative px-4 py-2 rounded-lg transition-transform hover:scale-105 active:scale-95 bg-primary text-secondary ${itemCount > 0 ? "animate-pulse-fast border-2 border-white shadow-lg" : ""}`}
            >
              <ShoppingCart className="h-6 w-6" />
              <span
                id="cart-count"
                className={`absolute -top-2 -right-2 flex h-6 w-6 items-center justify-center rounded-full bg-secondary text-white text-xs font-bold shadow-sm ${itemCount > 0 ? "animate-bounce-scale" : ""}`}
              >
                {itemCount}
              </span>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Slide-in */}
      {isMobileMenuOpen && (
        <>
          {/* Overlay sombre */}
          <div
            className="fixed inset-0 bg-black/50 z-40 md:hidden"
            onClick={() => setIsMobileMenuOpen(false)}
          />

          {/* Menu Slide-in depuis la gauche */}
          <div className="fixed inset-y-0 left-0 w-64 bg-secondary z-50 shadow-xl transform transition-transform duration-300 ease-in-out md:hidden">
            {/* Header du menu */}
            <div className="flex items-center justify-between p-4 border-b border-secondary-light">
              <span className="text-lg font-bold text-white">Menu</span>
              <button
                onClick={() => setIsMobileMenuOpen(false)}
                className="p-2 rounded-md hover:bg-secondary-light transition-colors"
                aria-label="Fermer le menu"
              >
                <X className="h-6 w-6 text-white" />
              </button>
            </div>

            {/* Navigation Links */}
            <nav className="flex flex-col p-4 space-y-2">
              <Link
                to="/"
                onClick={() => setIsMobileMenuOpen(false)}
                className="px-4 py-3 text-white hover:bg-secondary-light rounded-lg transition-colors flex items-center gap-3"
              >
                <span className="text-xl">🏠</span>
                <span>Accueil</span>
              </Link>
              <Link
                to="/products"
                onClick={() => setIsMobileMenuOpen(false)}
                className="px-4 py-3 text-white hover:bg-secondary-light rounded-lg transition-colors flex items-center gap-3"
              >
                <span className="text-xl">🍬</span>
                <span>Produits</span>
              </Link>
              <Link
                to="/track-order"
                onClick={() => setIsMobileMenuOpen(false)}
                className="px-4 py-3 text-white hover:bg-secondary-light rounded-lg transition-colors flex items-center gap-3"
              >
                <span className="text-xl">📦</span>
                <span>Suivre ma commande</span>
              </Link>
            </nav>
          </div>
        </>
      )}

      <CartDrawer isOpen={isCartOpen} onClose={() => setIsCartOpen(false)} />
    </header>
  );
};

export default Header;
