import useCartStore from "../../store/cartStore";

const ProductCard = ({ product }) => {
  const addItem = useCartStore((state) => state.addItem);

  const handleAddToCart = (e) => {
    e.stopPropagation();
    addItem(product);
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

          <button
            onClick={handleAddToCart}
            className="btn-primary p-2 flex items-center justify-center rounded-full h-10 w-10"
            title="Ajouter au panier"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
