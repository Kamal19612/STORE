import useCartStore from "../../store/cartStore";
import { toast } from "react-toastify";
import { Check, ShoppingBag, Eye } from "lucide-react";

const ProductCard = ({ product }) => {
  const addItem = useCartStore((state) => state.addItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const items = useCartStore((state) => state.items);

  const isInCart = items.some((item) => item.id === product.id);

  const handleToggleCart = (e) => {
    e.stopPropagation();
    if (isInCart) {
      removeItem(product.id);
    } else {
      if (items.length === 0) {
        toast.info(
          <div className="flex flex-col gap-1">
            <span className="font-bold">Excellent choix ! 🍬</span>
            <span className="text-sm">
              N'oubliez pas de vérifier votre panier.
            </span>
          </div>,
          {
            icon: "🛒",
            position: "top-right",
            autoClose: 4000,
          },
        );
      }
      addItem(product);
    }
  };

  return (
    <div className="product-card bg-white rounded-lg overflow-hidden shadow-md border border-gray-100 group hover:-translate-y-1 hover:shadow-xl transition-all duration-200">
      <div className="relative h-48 overflow-hidden bg-gray-200 flex items-center justify-center cursor-pointer">
        <img
          src={
            product.mainImage ||
            "https://via.placeholder.com/300?text=Sucre+Store"
          }
          alt={product.name}
          className="h-full w-full object-cover transition-transform duration-300"
        />

        {/* View Details Button - Appears on Hover */}
        <button
          className="absolute top-2 left-2 p-2 rounded-full shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 bg-primary text-secondary hover:scale-110"
          title="Voir les détails"
        >
          <Eye className="h-5 w-5" />
        </button>

        {/* Promo Badge */}
        {product.oldPrice && (
          <div className="absolute top-2 right-2 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded">
            PROMO
          </div>
        )}

        {/* Not Available Badge (Matches PHP) */}
        {!product.available && (
          <div className="absolute top-2 right-2 bg-secondary text-white text-xs px-2 py-1 rounded">
            Non disponible
          </div>
        )}
      </div>

      <div className="p-4">
        <div className="mb-2">
          <span className="text-xs font-semibold text-primary uppercase">
            {product.categoryName || "Catégorie"}
          </span>
        </div>

        <h2 className="text-lg font-bold text-secondary mb-2 line-clamp-2 min-h-[3.5rem]">
          {product.name}
        </h2>

        <p className="text-sm text-gray-600 mb-3">
          {/* Description mocked for now if not available in prop */}
          {product.description || "Découvrez ce délice..."}
        </p>

        <div className="flex items-center justify-between mt-auto">
          <span className="text-2xl font-bold text-primary">
            {product.price.toLocaleString()} FCFA
          </span>

          {product.available ? (
            <button
              onClick={handleToggleCart}
              className={`px-4 py-2 rounded-lg transition-all duration-200 shadow-sm flex items-center justify-center ${
                isInCart
                  ? "bg-green-500 hover:bg-green-600 text-white"
                  : "bg-primary hover:bg-primary-dark text-secondary"
              }`}
              title={isInCart ? "Retirer du panier" : "Ajouter au panier"}
            >
              {isInCart ? (
                <Check className="h-5 w-5" />
              ) : (
                <ShoppingBag className="h-5 w-5" />
              )}
            </button>
          ) : (
            <button
              className="bg-gray-400 text-white px-4 py-2 rounded-lg cursor-not-allowed"
              disabled
            >
              <div className="h-5 w-5 relative">
                <div className="absolute inset-0 transform rotate-45 bg-white h-0.5 w-full top-1/2 -mt-px"></div>
                <div className="absolute inset-0 transform -rotate-45 bg-white h-0.5 w-full top-1/2 -mt-px"></div>
              </div>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
