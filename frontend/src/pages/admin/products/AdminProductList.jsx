import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Plus,
  Search,
  Edit,
  Trash2,
  CheckCircle,
  XCircle,
  Upload,
} from "lucide-react";
import { toast } from "react-toastify";
import adminProductService from "../../../services/adminProductService";
import ImportProductModal from "../../../components/admin/ImportProductModal";

const AdminProductList = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState("");
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);

  const navigate = useNavigate();

  const fetchProducts = async () => {
    setLoading(true);
    try {
      // Backend pagination commence à 0
      const data = await adminProductService.getAllProducts(page, 10, search);
      setProducts(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (error) {
      console.error("Erreur chargement produits:", error);
      toast.error("Impossible de charger les produits");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [page, search]);

  const handleDelete = async (id) => {
    if (window.confirm("Êtes-vous sûr de vouloir supprimer ce produit ?")) {
      try {
        await adminProductService.deleteProduct(id);
        toast.success("Produit supprimé (archivé) avec succès");
        fetchProducts();
      } catch (error) {
        toast.error("Erreur lors de la suppression");
      }
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0); // Reset page on search
    fetchProducts();
  };

  return (
    <div className="p-2 sm:p-4">
      <div className="flex flex-col gap-3 mb-4 sm:mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h1 className="text-lg sm:text-2xl font-bold text-gray-800 dark:text-white">
              Gestion des Produits
            </h1>
            <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-400">Gérez votre catalogue</p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setIsImportModalOpen(true)}
              className="flex items-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs sm:text-sm py-2 px-3 rounded-lg"
            >
              <Upload className="h-4 w-4" />
              <span className="hidden sm:inline">Importer</span>
            </button>
            <Link
              to="/admin/products/new"
              className="flex items-center gap-2 bg-primary hover:bg-primary-dark text-secondary text-xs sm:text-sm py-2 px-3 rounded-lg font-bold"
            >
              <Plus className="h-4 w-4" />
              <span>Nouveau</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Search Bar */}
      <div className="bg-white dark:bg-[#242021] p-3 rounded-lg border border-gray-100 dark:border-white/10 mb-4">
        <form onSubmit={handleSearch} className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher..."
            className="w-full pl-9 pr-3 py-2 border border-gray-200 dark:border-gray-700 dark:bg-[#1c191a] dark:text-white rounded-lg text-sm"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </form>
      </div>

      {/* Mobile Card View */}
      <div className="lg:hidden space-y-3">
        {loading ? (
          [...Array(3)].map((_, i) => (
            <div key={i} className="bg-white dark:bg-[#242021] rounded-lg p-4 animate-pulse">
              <div className="flex gap-3">
                <div className="h-16 w-16 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
                <div className="flex-1">
                  <div className="h-4 w-32 bg-gray-200 dark:bg-gray-700 rounded mb-2"></div>
                  <div className="h-3 w-20 bg-gray-200 dark:bg-gray-700 rounded"></div>
                </div>
              </div>
            </div>
          ))
        ) : products.length === 0 ? (
          <div className="text-center py-8 text-gray-500">Aucun produit trouvé.</div>
        ) : (
          products.map((product) => (
            <div key={product.id} className="bg-white dark:bg-[#242021] rounded-lg shadow-sm border border-gray-100 dark:border-white/10 p-3">
              <div className="flex gap-3">
                <div className="h-16 w-16 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden bg-white dark:bg-[#1c191a] shrink-0">
                  <img src={product.mainImage || "/placeholder.png"} alt={product.name} className="h-full w-full object-contain" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-gray-900 dark:text-white text-sm truncate">{product.name}</div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">{product.categoryName}</div>
                  <div className="flex justify-between items-center mt-2">
                    <span className="font-bold text-primary dark:text-primary-400">{product.price.toLocaleString()} FCA</span>
                    <div className="flex gap-1">
                      <button onClick={() => navigate(`/admin/products/edit/${product.id}`)} className="p-1.5 text-blue-500 hover:bg-blue-50 rounded">
                        <Edit className="h-4 w-4" />
                      </button>
                      <button onClick={() => handleDelete(product.id)} className="p-1.5 text-red-500 hover:bg-red-50 rounded">
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Desktop Table View */}
      <div className="hidden lg:block bg-white dark:bg-[#242021] rounded-xl shadow-sm border border-gray-100 dark:border-white/10 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead className="bg-gray-50 dark:bg-[#1c191a] border-b border-gray-200 dark:border-white/10">
              <tr>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Image</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Nom</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase hidden md:table-cell">Catégorie</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Prix</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase hidden sm:table-cell">Stock</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase hidden sm:table-cell">Statut</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-500 uppercase text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 dark:divide-white/5">
              {products.map((product) => (
                <tr key={product.id} className="hover:bg-gray-50 dark:hover:bg-white/5">
                  <td className="px-4 py-3">
                    <div className="h-10 w-10 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden bg-white p-1">
                      <img src={product.mainImage || "/placeholder.png"} alt={product.name} className="h-full w-full object-contain" />
                    </div>
                  </td>
                  <td className="px-4 py-3 font-medium text-gray-900 dark:text-white text-sm">{product.name}</td>
                  <td className="px-4 py-3 text-gray-600 dark:text-gray-300 hidden md:table-cell text-sm">{product.categoryName}</td>
                  <td className="px-4 py-3 font-bold text-primary dark:text-primary-400 text-sm">{product.price.toLocaleString()} FCA</td>
                  <td className="px-4 py-3 text-sm hidden sm:table-cell">
                    {product.stock > 0 ? <span className="text-green-600 font-bold">{product.stock}</span> : <span className="text-red-500 font-bold">Rupture</span>}
                  </td>
                  <td className="px-4 py-3 text-sm hidden sm:table-cell">
                    {product.available ? <span className="text-green-600 flex items-center gap-1"><CheckCircle className="h-3 w-3" /> Actif</span> : <span className="text-gray-400 flex items-center gap-1"><XCircle className="h-3 w-3" /> Inactif</span>}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex justify-end gap-2">
                      <button onClick={() => navigate(`/admin/products/edit/${product.id}`)} className="p-2 text-gray-500 hover:text-blue-600 rounded-lg"><Edit className="h-4 w-4" /></button>
                      <button onClick={() => handleDelete(product.id)} className="p-2 text-gray-500 hover:text-red-600 rounded-lg"><Trash2 className="h-4 w-4" /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {products.length > 0 && (
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mt-4 bg-white dark:bg-[#242021] p-3 rounded-lg border border-gray-100 dark:border-white/10">
          <span className="text-xs sm:text-sm text-gray-500">Page {page + 1} sur {totalPages > 0 ? totalPages : 1}</span>
          <div className="flex gap-2 self-end sm:self-auto">
            <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} className="px-3 py-1.5 border rounded-lg text-xs hover:bg-gray-50 disabled:opacity-50">Précédent</button>
            <button onClick={() => setPage((p) => p + 1)} disabled={page >= totalPages - 1} className="px-3 py-1.5 border rounded-lg text-xs hover:bg-gray-50 disabled:opacity-50">Suivant</button>
          </div>
        </div>
      )}

      <ImportProductModal isOpen={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} onSuccess={() => { fetchProducts(); }} />
    </div>
  );
};

export default AdminProductList;
