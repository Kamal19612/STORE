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
          <div className="flex-shrink-0 flex items-center h-full py-4">
            <Link to="/" className="flex items-center gap-3 group">
              <img
                src="https://sucrestore.web-genious.com/images/logo-sucre.png"
                alt="Candy Sucré Store"
                className="h-12 w-auto object-contain"
              />
            </Link>
          </div>

          {/* Navigation Desktop */}
          <nav className="hidden md:flex space-x-8"></nav>

          {/* Actions */}
          <div className="flex items-center space-x-4">
            <button
              id="cart-toggle"
              onClick={() => setIsCartOpen(true)}
              className="relative px-4 py-2 rounded-lg transition-transform hover:scale-105 active:scale-95 bg-primary text-secondary"
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

      <CartDrawer isOpen={isCartOpen} onClose={() => setIsCartOpen(false)} />
    </header>
  );
};

export default Header;
