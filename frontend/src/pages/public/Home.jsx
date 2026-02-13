import { useState, useEffect } from "react";
import productService from "../../services/productService";
import ProductCard from "../../components/product/ProductCard";
import Slider from "../../components/public/Slider";
import CategoryBar from "../../components/product/CategoryBar";

const Home = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState("Tous");
  const [showAvailableOnly, setShowAvailableOnly] = useState(false);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const data = await productService.getAllProducts(0, 100); // Fetch more for client-side filtering
        setProducts(data.content || []);
      } catch (err) {
        console.error("Erreur lors du chargement des produits:", err);
        setError(
          "Impossible de charger les produits. Vérifiez que le backend est lancé.",
        );
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  // Filtrage Client-Side pour correspondre au design PHP immédiatement
  const filteredProducts = products.filter((product) => {
    const matchCategory =
      selectedCategory === "Tous" ||
      (product.categoryName &&
        product.categoryName.toLowerCase() === selectedCategory.toLowerCase());

    const matchAvailability = !showAvailableOnly || product.available;

    return matchCategory && matchAvailability;
  });

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
        <p className="mt-4 text-gray-500 font-medium">
          Sucre Store charge vos gourmandises...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-20 px-4">
        <p className="text-red-500 font-bold text-xl">{error}</p>
        <button
          onClick={() => window.location.reload()}
          className="mt-4 btn-primary"
        >
          Réessayer
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6 pb-10">
      {/* Carrousel */}
      <Slider />

      {/* Mobile Category Bar */}
      <div className="mb-6 lg:hidden">
        <CategoryBar
          selectedCategory={selectedCategory}
          onSelectCategory={setSelectedCategory}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Desktop Sidebar Filters */}
        <aside className="hidden lg:block lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6 sticky top-24">
            <h2 className="text-xl font-bold mb-4 text-secondary">
              <span className="mr-2">🔍</span>Filtres
            </h2>

            {/* Category Filter */}
            <div className="mb-6">
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Catégorie
              </label>
              <div className="space-y-2">
                <button
                  onClick={() => setSelectedCategory("Tous")}
                  className={`block w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                    selectedCategory === "Tous"
                      ? "bg-primary text-secondary font-bold"
                      : "hover:bg-gray-100 text-gray-600"
                  }`}
                >
                  Tous les produits
                </button>
                {/* Extract unique categories from products for the sidebar list */}
                {[
                  ...new Set(
                    products.map((p) => p.categoryName).filter(Boolean),
                  ),
                ].map((cat) => (
                  <button
                    key={cat}
                    onClick={() => setSelectedCategory(cat)}
                    className={`block w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                      selectedCategory === cat
                        ? "bg-primary text-secondary font-bold"
                        : "hover:bg-gray-100 text-gray-600"
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            {/* Availability Filter */}
            <div className="mb-6 pt-6 border-t border-gray-100">
              <label className="flex items-center space-x-3 cursor-pointer group">
                <input
                  type="checkbox"
                  checked={showAvailableOnly}
                  onChange={(e) => setShowAvailableOnly(e.target.checked)}
                  className="w-5 h-5 text-primary border-gray-300 rounded focus:ring-primary focus:ring-offset-0"
                />
                <span className="text-sm text-gray-700 group-hover:text-primary transition-colors">
                  Disponibles uniquement
                </span>
              </label>
            </div>

            {/* Results Count */}
            <div className="mt-6 pt-6 border-t border-gray-200">
              <p className="text-sm text-gray-600">
                <strong>{filteredProducts.length}</strong> produit(s) trouvé(s)
              </p>
            </div>
          </div>
        </aside>

        {/* Product Grid */}
        <div className="lg:col-span-3">
          {/* Header Mobile Only (Title) */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <div>
              <h1 className="text-3xl font-extrabold text-secondary tracking-tight font-brand-serif">
                {selectedCategory === "Tous"
                  ? "Nos Produits"
                  : selectedCategory}
              </h1>
              <p className="text-gray-500 mt-1">
                {selectedCategory === "Tous"
                  ? "Découvrez notre sélection sucrée"
                  : `Découvrez nos produits ${selectedCategory}`}
              </p>
            </div>
          </div>

          {filteredProducts.length === 0 ? (
            <div className="bg-white rounded-lg shadow-md p-12 text-center">
              <div className="text-6xl mb-4">📦</div>
              <h3 className="text-xl font-semibold text-gray-700 mb-2">
                Aucun produit trouvé
              </h3>
              <p className="text-gray-500">Essayez de modifier vos filtres</p>
              <button
                onClick={() => {
                  setSelectedCategory("Tous");
                  setShowAvailableOnly(false);
                }}
                className="mt-4 text-primary hover:text-primary-dark font-medium underline"
              >
                Réinitialiser les filtres
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Home;
