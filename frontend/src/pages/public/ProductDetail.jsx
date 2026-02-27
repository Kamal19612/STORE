import { useParams, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { ArrowLeft, ShoppingCart, Check } from "lucide-react";
import { toast } from "react-toastify";
import productService from "../../services/productService";
import useCartStore from "../../store/cartStore";

const ProductDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const addItem = useCartStore((state) => state.addItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const items = useCartStore((state) => state.items);

  const isInCart = items.some((item) => item.id === parseInt(id));

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const data = await productService.getProductById(id);
        setProduct(data);
      } catch (err) {
        console.error("Erreur chargement produit:", err);
        setError("Produit introuvable");
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [id]);

  const handleToggleCart = () => {
    if (isInCart) {
      removeItem(product.id);
      toast.info("Produit retiré du panier");
    } else {
      addItem(product);
      toast.success("Produit ajouté au panier ! 🛒");
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 text-center">
        <div className="text-6xl mb-4">😕</div>
        <h2 className="text-2xl font-bold text-gray-700 mb-4">
          Produit introuvable
        </h2>
        <button
          onClick={() => navigate("/")}
          className="btn-primary inline-flex items-center gap-2"
        >
          <ArrowLeft className="h-5 w-5" />
          Retour à la boutique
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Breadcrumb */}
      <button
        onClick={() => navigate("/")}
        className="inline-flex items-center gap-2 text-primary hover:text-primary-dark mb-6 transition-colors"
      >
        <ArrowLeft className="h-5 w-5" />
        <span className="font-medium">Retour aux produits</span>
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12">
        {/* Image Section */}
        <div className="bg-white rounded-lg shadow-lg overflow-hidden">
          <div className="aspect-square w-full bg-gray-100 flex items-center justify-center">
            <img
              src={
                product.mainImage ||
                "https://via.placeholder.com/600?text=Sucre+Store"
              }
              alt={product.name}
              className="w-full h-full object-cover"
            />
          </div>
        </div>

        {/* Product Info Section */}
        <div className="flex flex-col">
          {/* Category Badge */}
          <div className="mb-4">
            <span className="inline-block bg-primary text-secondary px-4 py-1 rounded-full text-sm font-bold uppercase">
              {product.categoryName || "Produit"}
            </span>
          </div>

          {/* Product Name */}
          <h1 className="text-3xl lg:text-4xl font-extrabold text-secondary mb-4 font-brand-serif">
            {product.name}
          </h1>

          {/* Price */}
          <div className="mb-6">
            <div className="flex items-baseline gap-3">
              <span className="text-4xl font-bold text-primary">
                {product.price.toLocaleString()} FCFA
              </span>
              {product.oldPrice && (
                <span className="text-xl text-gray-400 line-through">
                  {product.oldPrice.toLocaleString()} FCFA
                </span>
              )}
            </div>
          </div>

          {/* Availability */}
          <div className="mb-6">
            {product.available ? (
              <div className="inline-flex items-center gap-2 px-4 py-2 bg-green-50 text-green-700 rounded-lg">
                <Check className="h-5 w-5" />
                <span className="font-semibold">En stock</span>
              </div>
            ) : (
              <div className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-600 rounded-lg">
                <span className="font-semibold">Non disponible</span>
              </div>
            )}
          </div>

          {/* Description */}
          <div className="mb-8 flex-grow">
            <h2 className="text-xl font-bold text-secondary mb-3">
              Description
            </h2>
            <p className="text-gray-700 leading-relaxed whitespace-pre-line">
              {product.description || "Aucune description disponible"}
            </p>
            {product.shortDescription && (
              <p className="text-gray-600 mt-3 italic">
                {product.shortDescription}
              </p>
            )}
          </div>

          {/* Add to Cart Button */}
          <div className="border-t pt-6">
            {product.available ? (
              <button
                onClick={handleToggleCart}
                className={`w-full py-4 rounded-lg font-bold text-lg transition-all duration-200 shadow-lg flex items-center justify-center gap-3 ${
                  isInCart
                    ? "bg-green-500 hover:bg-green-600 text-white"
                    : "bg-primary hover:bg-primary-dark text-secondary"
                }`}
              >
                {isInCart ? (
                  <>
                    <Check className="h-6 w-6" />
                    <span>Dans le panier</span>
                  </>
                ) : (
                  <>
                    <ShoppingCart className="h-6 w-6" />
                    <span>Ajouter au panier</span>
                  </>
                )}
              </button>
            ) : (
              <button
                disabled
                className="w-full py-4 rounded-lg font-bold text-lg bg-gray-300 text-gray-500 cursor-not-allowed"
              >
                Produit indisponible
              </button>
            )}
          </div>

          {/* Additional Info */}
          {product.stock > 0 && (
            <div className="mt-4 text-sm text-gray-500">
              Stock restant : {product.stock} unité(s)
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProductDetail;
