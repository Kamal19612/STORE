import { toast } from "react-toastify";
import useCartStore from "../../store/cartStore";

const ProductDetailModal = ({ product, onClose }) => {
  const addItem = useCartStore((state) => state.addItem);
  const items = useCartStore((state) => state.items);

  if (!product) return null;

  // Format price helper
  const formatPrice = (price) => {
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ") + " FCFA";
  };

  const isInCart = items.some((item) => item.id === product.id);

  const handleAddToCart = () => {
    if (!isInCart) {
      addItem(product);
      toast.success("✓ Produit ajouté ! Vérifiez votre panier");
    }
    onClose();
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      id="details-modal"
      className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4 backdrop-blur-sm"
      onClick={handleOverlayClick}
    >
      <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <div className="flex justify-between items-start mb-4">
            <h2
              className="text-2xl font-bold"
              style={{ color: "var(--secondary)" }}
            >
              {product.name}
            </h2>
            <button
              onClick={onClose}
              className="text-gray-500 hover:text-gray-700"
            >
              <i className="fas fa-times text-2xl"></i>
            </button>
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            <div>
              {product.mainImage ? (
                <img
                  src={product.mainImage}
                  alt={product.name}
                  className="w-full h-64 object-cover rounded-lg"
                />
              ) : (
                <div className="w-full h-64 bg-gray-200 rounded-lg flex items-center justify-center">
                  <i className="fas fa-image text-6xl text-gray-400"></i>
                </div>
              )}
              <div className="mt-4">
                <span
                  className="text-xs font-semibold px-3 py-1 rounded-full"
                  style={{
                    backgroundColor: "var(--primary)",
                    color: "var(--secondary)",
                  }}
                >
                  {product.categoryName || "Catégorie"}
                </span>
              </div>
            </div>

            <div>
              <div className="mb-4">
                <h3
                  className="font-bold mb-2"
                  style={{ color: "var(--secondary)" }}
                >
                  Mode d'emploi
                </h3>
                <p className="text-gray-700">
                  {product.description || "Non spécifié"}
                </p>
              </div>

              <div className="mb-4">
                <h3
                  className="font-bold mb-2"
                  style={{ color: "var(--secondary)" }}
                >
                  Volume / Poids
                </h3>
                <p className="text-gray-700">
                  {product.volumeWeight || product.stock || "Non spécifié"}
                </p>
              </div>

              <div className="mb-6">
                <h3
                  className="font-bold mb-2"
                  style={{ color: "var(--secondary)" }}
                >
                  Prix
                </h3>
                <p
                  className="text-3xl font-bold"
                  style={{ color: "var(--primary)" }}
                >
                  {formatPrice(product.price)}
                </p>
              </div>

              {product.available ? (
                <button
                  onClick={handleAddToCart}
                  className="w-full text-white py-3 rounded-lg font-bold transition"
                  style={{
                    backgroundColor: "var(--primary)",
                    color: "var(--secondary)",
                  }}
                >
                  <i className="fas fa-cart-plus mr-2"></i>Ajouter au panier
                </button>
              ) : (
                <button
                  className="w-full bg-gray-400 text-white py-3 rounded-lg font-bold cursor-not-allowed"
                  disabled
                >
                  <i className="fas fa-times mr-2"></i>Non disponible
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetailModal;
