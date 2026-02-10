import useCartStore from "../../store/cartStore";
import { toast } from "react-toastify";
import { Check, ShoppingBag } from "lucide-react";

const ProductCard = ({ product }) => {
  const { addItem, removeItem, items } = useCartStore((state) => ({
    addItem: state.addItem,
    removeItem: state.removeItem,
    items: state.items,
  }));

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
    <div className="product-card bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 group">
      <div className="relative aspect-square overflow-hidden bg-gray-50">
        <img
          src={
            product.mainImage ||
            "https://via.placeholder.com/300?text=Sucre+Store"
          }
          alt={product.name}
          className="w-full h-full object-contain p-4 group-hover:scale-105 transition-transform duration-300"
        />
        {product.oldPrice && (
          <div className="absolute top-3 left-3 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded">
            PROMO
          </div>
        )}
      </div>

      <div className="p-4">
        <h3 className="text-sm font-medium text-secondary-light line-clamp-1">
          {product.categoryName}
        </h3>
        <h2 className="text-lg font-bold text-secondary mt-1 line-clamp-2 min-h-[3.5rem]">
          {product.name}
        </h2>

        <div className="mt-4 flex items-center justify-between">
          <div className="flex flex-col">
            {product.oldPrice && (
              <span className="text-sm text-gray-400 line-through">
                {product.oldPrice.toLocaleString()} FCFA
              </span>
            )}
            <span className="text-xl font-bold text-primary">
              {product.price.toLocaleString()} FCFA
            </span>
          </div>

          {product.available ? (
            <button
              onClick={handleToggleCart}
              className={`p-2 flex items-center justify-center rounded-full h-10 w-10 transition-all duration-300 shadow-md ${
                isInCart
                  ? "bg-green-500 hover:bg-green-600 text-white scale-110"
                  : "btn-primary hover:scale-105"
              }`}
              title={isInCart ? "Retirer du panier" : "Ajouter au panier"}
            >
              {isInCart ? (
                <Check className="h-6 w-6" />
              ) : (
                <ShoppingBag className="h-5 w-5" />
              )}
            </button>
          ) : (
            <span className="text-xs font-bold text-red-500 bg-red-50 px-2 py-1 rounded border border-red-100">
              Rupture
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
