import { X, Plus, Minus, Trash2, ShoppingBag } from "lucide-react";
import useCartStore from "../../store/cartStore";
import { useNavigate } from "react-router-dom";

const CartDrawer = ({ isOpen, onClose }) => {
  const { items, removeItem, updateQuantity, total, itemCount } =
    useCartStore();
  const navigate = useNavigate();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] overflow-hidden">
      {/* Overlay */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      <div className="absolute inset-y-0 right-0 max-w-full flex">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col animate-slide-in">
          {/* Header */}
          <div className="px-4 py-6 bg-secondary text-white flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShoppingBag className="h-6 w-6 text-primary" />
              <h2 className="text-xl font-bold uppercase tracking-tight">
                Mon Panier ({itemCount})
              </h2>
            </div>
            <button
              onClick={onClose}
              className="p-2 rounded-full hover:bg-secondary-light transition-colors"
            >
              <X className="h-6 w-6" />
            </button>
          </div>

          {/* Items List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {items.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-8">
                <div className="bg-gray-100 p-6 rounded-full mb-4">
                  <ShoppingBag className="h-12 w-12 text-gray-400" />
                </div>
                <h3 className="text-lg font-bold text-secondary">
                  Votre panier est vide
                </h3>
                <p className="text-gray-500 mt-2">
                  Découvrez nos délices et ajoutez vos préférés ici !
                </p>
                <button onClick={onClose} className="mt-6 btn-primary w-full">
                  Continuer mes achats
                </button>
              </div>
            ) : (
              items.map((item) => (
                <div
                  key={item.id}
                  className="flex gap-4 p-3 bg-gray-50 rounded-xl border border-gray-100 group"
                >
                  <div className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-lg bg-white p-2 border border-gray-100">
                    <img
                      src={item.mainImage || "https://via.placeholder.com/100"}
                      alt={item.name}
                      className="h-full w-full object-contain"
                    />
                  </div>

                  <div className="flex-1 flex flex-col">
                    <div className="flex justify-between items-start">
                      <h4 className="font-bold text-secondary text-sm line-clamp-2">
                        {item.name}
                      </h4>
                      <button
                        onClick={() => removeItem(item.id)}
                        className="text-gray-400 hover:text-red-500 p-1"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>

                    <div className="mt-auto flex justify-between items-center">
                      <div className="flex items-center gap-3 bg-white rounded-full border border-gray-200 px-2 py-1">
                        <button
                          onClick={() =>
                            updateQuantity(item.id, item.quantity - 1)
                          }
                          className="p-1 rounded-full hover:bg-gray-100 text-secondary"
                        >
                          <Minus className="h-3 w-3" />
                        </button>
                        <span className="font-bold text-sm min-w-[20px] text-center">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() =>
                            updateQuantity(item.id, item.quantity + 1)
                          }
                          className="p-1 rounded-full hover:bg-gray-100 text-secondary"
                        >
                          <Plus className="h-3 w-3" />
                        </button>
                      </div>
                      <span className="font-bold text-primary">
                        {(item.price * item.quantity).toLocaleString()} FCFA
                      </span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          {items.length > 0 && (
            <div className="border-t border-gray-100 p-6 space-y-4 bg-gray-50">
              <div className="flex justify-between items-center text-lg">
                <span className="font-medium text-gray-500 uppercase tracking-wider text-sm">
                  Sous-total
                </span>
                <span className="font-black text-2xl text-secondary">
                  {total.toLocaleString()} FCFA
                </span>
              </div>
              <p className="text-xs text-gray-400 italic">
                Frais de livraison calculés lors de la commande.
              </p>

              <button
                onClick={() => {
                  onClose();
                  navigate("/checkout");
                }}
                className="btn-primary w-full py-4 text-lg shadow-lg shadow-primary/20 flex items-center justify-center gap-2"
              >
                Commander maintenant
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CartDrawer;
