import { useState, useEffect } from "react";
import {
  Trash2,
  Upload,
  Plus,
  Save,
  GripVertical,
  CheckCircle,
  XCircle,
} from "lucide-react";
import { toast } from "react-toastify";
import sliderService from "../../../services/sliderService";

const AdminSlider = () => {
  const [slides, setSlides] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);

  // State pour le nouveau slide
  const [newSlide, setNewSlide] = useState({
    title: "",
    imageUrl: "",
    displayOrder: 10,
    active: true,
    imageFile: null,
  });
  const [previewUrl, setPreviewUrl] = useState("");

  const fetchSlides = async () => {
    try {
      const data = await sliderService.getAll();
      setSlides(data);
    } catch (error) {
      toast.error("Erreur chargement carrousel");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSlides();
  }, []);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setNewSlide({ ...newSlide, imageFile: file });
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      const formData = new FormData();
      formData.append(
        "slider",
        JSON.stringify({
          title: newSlide.title,
          imageUrl: newSlide.imageUrl, // URL manuelle
          displayOrder: newSlide.displayOrder,
          active: newSlide.active,
        }),
      );
      if (newSlide.imageFile) {
        formData.append("image", newSlide.imageFile);
      }

      await sliderService.create(formData);
      toast.success("Image ajoutée !");
      setIsAdding(false);
      setNewSlide({
        title: "",
        imageUrl: "",
        displayOrder: 10,
        active: true,
        imageFile: null,
      });
      setPreviewUrl("");
      fetchSlides();
    } catch (error) {
      toast.error("Erreur ajout image");
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm("Supprimer cette image ?")) {
      try {
        await sliderService.delete(id);
        toast.success("Image supprimée");
        fetchSlides();
      } catch (error) {
        toast.error("Erreur suppression");
      }
    }
  };

  const toggleActive = async (slide) => {
    try {
      const formData = new FormData();
      formData.append(
        "slider",
        JSON.stringify({
          ...slide,
          active: !slide.active,
        }),
      );
      await sliderService.update(slide.id, formData);
      fetchSlides();
    } catch (error) {
      toast.error("Erreur mise à jour statut");
    }
  };

  /// NOTE: Reordering Drag&Drop is complex to implement perfectly in one go.
  /// For now, we allow editing the "Order" number directly.

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Gestion Carrousel
          </h1>
          <p className="text-gray-500">Images de la page d'accueil</p>
        </div>
        <button
          onClick={() => setIsAdding(!isAdding)}
          className="bg-primary text-white px-4 py-2 rounded-lg hover:bg-primary/90 flex items-center gap-2"
        >
          {isAdding ? (
            "Annuler"
          ) : (
            <>
              <Plus className="h-5 w-5" /> Ajouter une image
            </>
          )}
        </button>
      </div>

      {isAdding && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 mb-8 animate-fade-in">
          <h3 className="font-bold text-lg mb-4">Nouvelle Diapositive</h3>
          <form onSubmit={handleCreate} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Titre (Optionnel)
                </label>
                <input
                  type="text"
                  value={newSlide.title}
                  onChange={(e) =>
                    setNewSlide({ ...newSlide, title: e.target.value })
                  }
                  className="w-full p-2 border border-gray-300 rounded-lg"
                  placeholder="Ex: Nouvelle Collection"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Ordre d'affichage
                </label>
                <input
                  type="number"
                  value={newSlide.displayOrder}
                  onChange={(e) =>
                    setNewSlide({
                      ...newSlide,
                      displayOrder: parseInt(e.target.value),
                    })
                  }
                  className="w-full p-2 border border-gray-300 rounded-lg"
                />
              </div>
            </div>

            {/* Image Upload / URL */}
            <div className="space-y-3">
              <label className="block text-sm font-medium text-gray-700">
                Image
              </label>

              <div className="flex flex-col md:flex-row gap-4 items-start">
                <div className="flex-1 w-full">
                  <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:bg-gray-50 transition-colors cursor-pointer relative">
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleFileChange}
                      className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    />
                    <Upload className="h-8 w-8 text-gray-400 mx-auto mb-2" />
                    <span className="text-sm text-gray-500">
                      Cliquez pour uploader un fichier
                    </span>
                  </div>
                </div>

                <div className="flex items-center text-gray-400 font-medium">
                  OU
                </div>

                <div className="flex-1 w-full">
                  <input
                    type="text"
                    value={newSlide.imageUrl}
                    onChange={(e) => {
                      setNewSlide({ ...newSlide, imageUrl: e.target.value });
                      setPreviewUrl(e.target.value); // Preview URL directly
                    }}
                    className="w-full p-3 border border-gray-300 rounded-lg"
                    placeholder="Coller une URL d'image externe..."
                  />
                </div>
              </div>

              {previewUrl && (
                <div className="mt-4 relative w-full h-48 bg-gray-100 rounded-lg overflow-hidden border border-gray-200">
                  <img
                    src={previewUrl}
                    alt="Preview"
                    className="w-full h-full object-cover"
                  />
                </div>
              )}
            </div>

            <div className="flex justify-end pt-4">
              <button
                type="submit"
                className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700 flex items-center gap-2 font-medium"
              >
                <Save className="h-5 w-5" /> Enregistrer
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Liste des slides */}
      <div className="space-y-4">
        {loading ? (
          <p className="text-center text-gray-500">Chargement...</p>
        ) : slides.length === 0 ? (
          <p className="text-center text-gray-500 p-10 bg-gray-50 rounded-lg">
            Aucune image dans le carrousel.
          </p>
        ) : (
          slides.map((slide) => (
            <div
              key={slide.id}
              className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-col md:flex-row items-center gap-6"
            >
              {/* Image Preview */}
              <div className="h-24 w-40 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0 border border-gray-200">
                <img
                  src={slide.imageUrl}
                  alt={slide.title}
                  className="w-full h-full object-cover"
                />
              </div>

              {/* Infos */}
              <div className="flex-1">
                <h4 className="font-bold text-gray-800">
                  {slide.title || "Sans titre"}
                </h4>
                <p className="text-sm text-gray-500 truncate max-w-md">
                  {slide.imageUrl}
                </p>
                <div className="mt-2 flex items-center gap-4 text-sm">
                  <span className="text-gray-600 bg-gray-100 px-2 py-1 rounded">
                    Ordre: {slide.displayOrder}
                  </span>
                  <button
                    onClick={() => toggleActive(slide)}
                    className={`flex items-center gap-1 font-medium ${slide.active ? "text-green-600" : "text-gray-400"}`}
                  >
                    {slide.active ? (
                      <CheckCircle className="h-4 w-4" />
                    ) : (
                      <XCircle className="h-4 w-4" />
                    )}
                    {slide.active ? "Actif" : "Inactif"}
                  </button>
                </div>
              </div>

              {/* Actions */}
              <button
                onClick={() => handleDelete(slide.id)}
                className="p-2 text-gray-400 hover:text-red-500 transition-colors"
                title="Supprimer"
              >
                <Trash2 className="h-5 w-5" />
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default AdminSlider;
