import { useState, useEffect } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Save,
  Upload,
  Link as LinkIcon,
  Image as ImageIcon,
} from "lucide-react";
import { toast } from "react-toastify";
import adminProductService from "../../../services/adminProductService";
import productService from "../../../services/productService";

const AdminProductForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditMode = !!id;

  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState([]);

  // État du formulaire
  const [formData, setFormData] = useState({
    name: "",
    slug: "",
    categoryId: "",
    categoryName: "", // Ajout pour l'input libre
    price: "",
    oldPrice: "",
    stock: 0,
    shortDescription: "",
    description: "",
    imageUrl: "", // Pour l'URL directe (ex: Google Drive/Sheet)
    active: true,
  });

  const [imageFile, setImageFile] = useState(null);
  const [previewApiImage, setPreviewApiImage] = useState(null);

  useEffect(() => {
    // Charger les catégories pour le select
    const loadCategories = async () => {
      try {
        const data = await productService.getCategories();
        setCategories(data);
      } catch (error) {
        toast.error("Erreur chargement catégories");
      }
    };

    loadCategories();
  }, []);

  // Charger le produit une fois les catégories disponibles
  useEffect(() => {
    if (!isEditMode || categories.length === 0) return;

    adminProductService
      .getProductById(id)
      .then((product) => {
        setFormData({
          name: product.name,
          slug: product.slug,
          categoryId: product.categoryId || "",
          categoryName: product.categoryName || "",
          price: product.price,
          oldPrice: product.oldPrice || "",
          stock: product.stock ?? 0,
          shortDescription: product.shortDescription || "",
          description: product.description || "",
          imageUrl: product.mainImage || "",
          active: product.active,
        });
        setPreviewApiImage(product.mainImage);
      })
      .catch((e) => {
        console.error(e);
        toast.error("Erreur chargement produit");
      });
  }, [categories, isEditMode, id]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    // Conversion automatique des liens Google Drive
    if (name === "imageUrl") {
      let cleanUrl = value;
      // Regex pour capturer l'ID d'un lien Google Drive standard
      const driveRegex = /https:\/\/drive\.google\.com\/file\/d\/([^/]+)/;
      const match = value.match(driveRegex);

      if (match && match[1]) {
        // Convertir en lien visualisable direct
        cleanUrl = `https://drive.google.com/uc?export=view&id=${match[1]}`;
      }

      setFormData((prev) => ({ ...prev, [name]: cleanUrl }));
    } else {
      setFormData((prev) => ({
        ...prev,
        [name]: type === "checkbox" ? checked : value,
      }));
    }

    // Génération automatique du slug depuis le nom
    if (name === "name" && !isEditMode) {
      const slug = value
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/(^-|-$)+/g, "");
      setFormData((prev) => ({ ...prev, slug }));
    }
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setImageFile(e.target.files[0]);
      // Reset URL si fichier choisi
      setFormData((prev) => ({ ...prev, imageUrl: "" }));
    }
  };

  const calculateStock = (val) => {
    // Helper si besoin
    return val;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const payload = {
        ...formData,
        stock: parseInt(formData.stock),
        price: parseFloat(formData.price),
        oldPrice: formData.oldPrice ? parseFloat(formData.oldPrice) : null,
        categoryName: formData.categoryName,
        categoryId: null, // On laisse le backend résoudre via le nom
        imageFile, // Sera extrait par le service
      };

      if (isEditMode) {
        await adminProductService.updateProduct(id, payload);
        toast.success("Produit modifié avec succès");
      } else {
        await adminProductService.createProduct(payload);
        toast.success("Produit créé avec succès");
      }
      navigate("/admin/products");
    } catch (error) {
      console.error(error);
      toast.error("Erreur lors de l'enregistrement");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-3 sm:p-6">
      <div className="flex items-center gap-3 sm:gap-4 mb-5 sm:mb-8">
        <Link
          to="/admin/products"
          className="p-2 bg-white dark:bg-[#242021] rounded-full border border-gray-200 dark:border-white/10 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors"
        >
          <ArrowLeft className="h-5 w-5 text-gray-500 dark:text-gray-400" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-gray-800 dark:text-white">
            {isEditMode ? "Modifier le Produit" : "Nouveau Produit"}
          </h1>
          <p className="text-gray-500 dark:text-gray-400">
            Remplissez les informations ci-dessous
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5 sm:space-y-8">
        <div className="bg-white dark:bg-[#242021] p-4 sm:p-8 rounded-2xl shadow-sm border border-gray-100 dark:border-white/10 space-y-6 transition-colors">
          {/* Informations de base */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
                Nom du produit
              </label>
              <input
                type="text"
                name="name"
                required
                value={formData.name}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white dark:bg-[#1c191a] dark:text-white shadow-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
                Slug (URL)
              </label>
              <input
                type="text"
                name="slug"
                required
                value={formData.slug}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl bg-gray-50 dark:bg-[#1c191a]/50 text-gray-500 dark:text-gray-400 focus:bg-white dark:focus:bg-[#1c191a] focus:ring-2 focus:ring-primary outline-none shadow-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
                Catégorie (Sélectionner ou Créer)
              </label>
              <input
                list="categories-list"
                name="categoryName"
                required
                value={formData.categoryName}
                onChange={handleChange}
                placeholder="Ex: APHRODISIAQUE, SEXTOY..."
                className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white dark:bg-[#1c191a] dark:text-white shadow-sm"
                autoComplete="off"
              />
              <datalist id="categories-list">
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.name} />
                ))}
              </datalist>
            </div>
            <div>
              <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
                Stock
              </label>
              <input
                type="number"
                name="stock"
                required
                min="0"
                value={formData.stock}
                onChange={handleChange}
                className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white dark:bg-[#1c191a] dark:text-white shadow-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
                Prix (FCFA)
              </label>
              <div className="relative">
                <input
                  type="number"
                  name="price"
                  required
                  min="0"
                  step="0.01"
                  value={formData.price}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white dark:bg-[#1c191a] dark:text-white shadow-sm"
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 text-sm font-bold">
                  FCFA
                </span>
              </div>
            </div>
            <div>
              <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
                Ancien Prix (Optionnel)
              </label>
              <div className="relative">
                <input
                  type="number"
                  name="oldPrice"
                  min="0"
                  step="0.01"
                  value={formData.oldPrice}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white dark:bg-[#1c191a] dark:text-white shadow-sm"
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 text-sm font-bold">
                  FCFA
                </span>
              </div>
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
              Description courte
            </label>
            <textarea
              name="shortDescription"
              rows="2"
              value={formData.shortDescription}
              onChange={handleChange}
              className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent outline-none bg-white dark:bg-[#1c191a] dark:text-white shadow-sm"
            />
          </div>

          {/* Image */}
          <div>
            <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-4">
              Image du produit
            </label>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Option 1 : Lien externe */}
              <div className="space-y-3">
                <p className="text-sm font-semibold text-gray-600 dark:text-gray-400 flex items-center gap-2">
                  <LinkIcon className="h-4 w-4" /> Via Lien (Recommandé si
                  Google Sheet)
                </p>
                <input
                  type="url"
                  name="imageUrl"
                  placeholder="https://exemple.com/image.jpg"
                  value={formData.imageUrl}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary outline-none bg-white dark:bg-[#1c191a] dark:text-white shadow-sm"
                />
              </div>

              {/* Option 2 : Upload */}
              <div className="space-y-3">
                <p className="text-sm font-semibold text-gray-600 dark:text-gray-400 flex items-center gap-2">
                  <Upload className="h-4 w-4" /> Ou Télécharger un fichier
                </p>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  className="block w-full text-sm text-gray-500 dark:text-gray-400
                              file:mr-4 file:py-2 file:px-4
                              file:rounded-full file:border-0
                              file:text-sm file:font-semibold
                              file:bg-primary/10 file:text-primary
                              hover:file:bg-primary/20
                              dark:file:bg-white/10 dark:file:text-primary
                            "
                />
              </div>
            </div>

            {/* Prévisualisation */}
            {(formData.imageUrl || imageFile || previewApiImage) && (
              <div className="mt-6">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                  Prévisualisation :
                </p>
                <div className="h-32 w-32 rounded-lg border border-gray-200 dark:border-white/10 overflow-hidden bg-gray-50 dark:bg-[#1c191a] flex items-center justify-center">
                  <img
                    src={
                      imageFile
                        ? URL.createObjectURL(imageFile)
                        : formData.imageUrl || previewApiImage
                    }
                    alt="Preview"
                    className="h-full w-full object-contain"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Checkbox Actif */}
          <div className="flex items-center gap-3 pt-4 border-t border-gray-100 dark:border-white/10">
            <input
              type="checkbox"
              id="active"
              name="active"
              checked={formData.active}
              onChange={handleChange}
              className="w-5 h-5 text-primary border-gray-300 dark:border-gray-600 rounded focus:ring-primary bg-white dark:bg-[#1c191a]"
            />
            <label
              htmlFor="active"
              className="text-sm font-bold text-gray-700 dark:text-gray-300 select-none cursor-pointer"
            >
              Produit Actif (Visible sur la boutique) ?
            </label>
          </div>
        </div>

        {/* Boutons actions */}
        <div className="flex flex-col sm:flex-row sm:justify-end gap-3 sm:gap-4">
          <button
            type="button"
            onClick={() => navigate("/admin/products")}
            className="px-6 py-3 rounded-xl border border-gray-300 dark:border-white/10 text-gray-700 dark:text-gray-300 font-bold hover:bg-gray-50 dark:hover:bg-white/5 transition-colors"
          >
            Annuler
          </button>
          <button
            type="submit"
            disabled={loading}
            className="px-8 py-3 rounded-xl bg-primary text-white font-bold hover:bg-primary-dark shadow-lg shadow-primary/25 transition-all flex items-center justify-center gap-2"
          >
            {loading && (
              <div className="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full" />
            )}
            <Save className="h-5 w-5" />
            {isEditMode ? "Enregistrer les modifications" : "Créer le produit"}
          </button>
        </div>
      </form>
    </div>
  );
};

export default AdminProductForm;
