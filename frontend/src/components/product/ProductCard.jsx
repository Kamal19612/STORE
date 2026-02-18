import { useState } from "react";
import useCartStore from "../../store/cartStore";
import { toast } from "react-toastify";
import { Check, ShoppingBag } from "lucide-react";
import ProductDetailModal from "./ProductDetailModal";

const ProductCard = ({ product }) => {
  const addItem = useCartStore((state) => state.addItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const items = useCartStore((state) => state.items);
  const [showModal, setShowModal] = useState(false);

  // Helper to format price like PHP: 10 000 FCFA
  const formatPrice = (price) => {
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ") + " FCFA";
  };

  const isInCart = items.some((item) => item.id === product.id);

  const handleToggleCart = (e) => {
    e.stopPropagation();
    if (isInCart) {
      removeItem(product.id);
      toast.info("Produit retiré du panier", {
        icon: "🗑️",
        autoClose: 2000,
      });
    } else {
      addItem(product);
      toast.success("✓ Produit ajouté ! Vérifiez votre panier", {
        icon: "🛒",
        autoClose: 3000,
      });
    }
  };

  const handleViewDetails = (e) => {
    e.stopPropagation();
    setShowModal(true);
  };

  const handleImageClick = () => {
    setShowModal(true);
  };

  return (
    <>
      {/* 
        PHP STRUCTURE:
        <div class="product-card bg-white rounded-lg shadow-md overflow-hidden relative">
          <div class="relative h-48 bg-gray-200 cursor-pointer view-details-trigger">
            <img ... />
            <button class="detail-btn ..."><i class="fas fa-eye"></i></button>
            <div class="absolute top-2 right-2 ...">Non disponible</div>
          </div>
          <div class="p-4">
             ... info ...
          </div>
        </div>
      */}
      <div className="product-card bg-white rounded-lg shadow-md overflow-hidden relative group hover:-translate-y-1 hover:shadow-xl transition-all duration-200">
        {/* Product Image Zone */}
        <div
          className="relative h-48 bg-gray-200 cursor-pointer"
          onClick={handleImageClick}
        >
          {product.mainImage ? (
            <img
              src={product.mainImage}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <i className="fas fa-image text-6xl text-gray-400"></i>
            </div>
          )}

          {/* View Details Button - Matches PHP .detail-btn */}
          {/* 
            PHP CSS:
            .detail-btn { opacity: 1; transition: opacity 0.2s; }
            @media (min-width: 768px) { .detail-btn { opacity: 0; } .product-card:hover .detail-btn { opacity: 1; } }
            
            Tailwind equiv: opacity-100 md:opacity-0 md:group-hover:opacity-100
          */}
          <button
            onClick={handleViewDetails}
            className="absolute top-2 left-2 text-white p-2 rounded-full shadow-lg opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity duration-200 z-10 flex items-center justify-center w-8 h-8"
            style={{
              backgroundColor: "var(--primary)",
              color: "var(--secondary)",
            }}
            title="Voir les détails"
          >
            <i className="fas fa-eye text-sm"></i>
          </button>

          {/* Availability Badge */}
          {!product.available && (
            <div
              className="absolute top-2 right-2 text-white text-xs px-2 py-1 rounded"
              style={{ backgroundColor: "var(--secondary)" }}
            >
              Non disponible
            </div>
          )}
        </div>

        {/* Product Info */}
        <div className="p-4">
          <div className="mb-2">
            <span
              className="text-xs font-semibold"
              style={{ color: "var(--primary)" }}
            >
              {product.categoryName || "Catégorie"}
            </span>
          </div>

          <h3
            className="text-lg font-bold mb-2 line-clamp-2"
            style={{ color: "var(--secondary)" }}
          >
            {product.name}
          </h3>

          <p className="text-sm text-gray-600 mb-3 line-clamp-2">
            {product.description || "Aucune description disponible."}
          </p>

          <div className="flex items-center justify-between">
            <span
              className="text-2xl font-bold"
              style={{ color: "var(--primary)" }}
            >
              {formatPrice(product.price)}
            </span>

            {product.available ? (
              <button
                onClick={handleToggleCart}
                className={`px-4 py-2 rounded-lg transition text-white flex items-center justify-center ${
                  isInCart ? "bg-green-500" : ""
                }`}
                style={
                  !isInCart
                    ? {
                        backgroundColor: "var(--primary)",
                        color: "var(--secondary)",
                      }
                    : { backgroundColor: "#10b981", color: "white" }
                }
              >
                {isInCart ? (
                  <i className="fas fa-check"></i>
                ) : (
                  <i className="fas fa-cart-plus"></i>
                )}
              </button>
            ) : (
              <button
                className="bg-gray-400 text-white px-4 py-2 rounded-lg cursor-not-allowed"
                disabled
              >
                <i className="fas fa-times"></i>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Modal - Rendered outside if needed, or inline */}
      {showModal && (
        <ProductDetailModal
          product={product}
          onClose={() => setShowModal(false)}
        />
      )}
    </>
  );
};

export default ProductCard;
