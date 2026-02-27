import { useState } from "react";
import {
  Upload,
  Link as LinkIcon,
  FileText,
  X,
  Check,
  AlertCircle,
} from "lucide-react";
import { toast } from "react-toastify";
import adminProductService from "../../services/adminProductService";

const ImportProductModal = ({ isOpen, onClose, onSuccess }) => {
  const [activeTab, setActiveTab] = useState("file"); // 'file' or 'url'
  const [file, setFile] = useState(null);
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState(null);

  if (!isOpen) return null;

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleUrlChange = (e) => {
    setUrl(e.target.value);
  };

  const handleImport = async () => {
    setLoading(true);
    setSummary(null);
    try {
      let fileToUpload = file;

      if (activeTab === "url") {
        // Extraction de l'ID si c'est une URL complète
        let spreadsheetId = url;
        const match = url.match(/\/spreadsheets\/d\/([a-zA-Z0-9-_]+)/);
        if (match && match[1]) {
          spreadsheetId = match[1];
        }

        const result =
          await adminProductService.importFromGoogleSheets(spreadsheetId);
        setSummary(result);

        if (result.successCount > 0) {
          toast.success(
            `${result.successCount} produits importés via API Google Sheets !`,
          );
          if (onSuccess) onSuccess();
        } else if (result.failureCount > 0) {
          toast.warn(
            "Aucun produit importé. Vérifiez l'ID et les permissions.",
          );
        }
        setLoading(false);
        return; // Fin du traitement pour URL
      }

      if (!file) {
        toast.error("Veuillez sélectionner un fichier ou une URL.");
        setLoading(false);
        return;
      }

      const result = await adminProductService.importProducts(file);
      setSummary(result);

      if (result.successCount > 0) {
        toast.success(`${result.successCount} produits importés avec succès !`);
        if (onSuccess) onSuccess();
      } else if (result.failureCount > 0) {
        toast.warn("Aucun produit importé. Vérifiez le format.");
      }
    } catch (error) {
      console.error(error);
      toast.error("Erreur lors de l'importation.");
    } finally {
      setLoading(false);
    }
  };

  const reset = () => {
    setFile(null);
    setUrl("");
    setSummary(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white dark:bg-[#242021] rounded-2xl shadow-xl w-full max-w-lg overflow-hidden transition-colors border border-gray-100 dark:border-white/10">
        <div className="p-6 border-b border-gray-100 dark:border-white/10 flex justify-between items-center">
          <h2 className="text-xl font-bold text-secondary dark:text-white">
            Importer des Produits
          </h2>
          <button
            onClick={reset}
            className="p-2 hover:bg-gray-100 dark:hover:bg-white/10 rounded-full dark:text-gray-400"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="p-6">
          {!summary ? (
            <>
              {/* Tabs */}
              <div className="flex gap-4 mb-6 border-b border-gray-100 dark:border-white/10">
                <button
                  className={`pb-2 px-4 font-medium transition-colors border-b-2 ${activeTab === "file" ? "border-primary text-primary" : "border-transparent text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"}`}
                  onClick={() => setActiveTab("file")}
                >
                  Fichier CSV
                </button>
                <button
                  className={`pb-2 px-4 font-medium transition-colors border-b-2 ${activeTab === "url" ? "border-primary text-primary" : "border-transparent text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"}`}
                  onClick={() => setActiveTab("url")}
                >
                  Google Sheet (Public)
                </button>
              </div>

              {/* Content */}
              <div className="min-h-[150px]">
                {activeTab === "file" ? (
                  <label
                    htmlFor="csv-file-input"
                    className={`border-2 border-dashed border-gray-200 dark:border-white/20 rounded-xl p-8 flex flex-col items-center justify-center text-center transition-colors ${!file ? "hover:bg-gray-50 dark:hover:bg-white/5 cursor-pointer" : ""}`}
                  >
                    <Upload className="h-10 w-10 text-gray-300 dark:text-gray-600 mb-4" />
                    {file ? (
                      <div className="flex flex-col items-center gap-3">
                        <div className="flex items-center gap-2 text-green-600 dark:text-green-400 font-medium">
                          <FileText className="h-5 w-5" />
                          {file.name}
                        </div>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.preventDefault();
                            setFile(null);
                          }}
                          className="text-sm text-red-500 hover:text-red-700 underline"
                        >
                          Changer de fichier
                        </button>
                      </div>
                    ) : (
                      <>
                        <p className="text-gray-600 dark:text-gray-300 font-medium">
                          Glissez votre CSV ici ou cliquez
                        </p>
                        <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">
                          Format: Nom, Catégorie, Prix, Image, Description
                        </p>
                      </>
                    )}
                    <input
                      id="csv-file-input"
                      type="file"
                      accept=".csv"
                      onChange={handleFileChange}
                      className="hidden"
                      disabled={file !== null}
                    />
                  </label>
                ) : (
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-2">
                        Lien Google Sheet (Public)
                      </label>
                      <div className="relative">
                        <LinkIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                        <input
                          type="url"
                          value={url}
                          onChange={handleUrlChange}
                          placeholder="https://docs.google.com/spreadsheets/d/..."
                          className="w-full pl-10 pr-4 py-3 border border-gray-200 dark:border-white/20 rounded-xl focus:ring-2 focus:ring-primary outline-none bg-white dark:bg-[#1c191a] dark:text-white"
                        />
                      </div>
                      <p className="text-xs text-gray-400 dark:text-gray-500 mt-2">
                        Assurez-vous que le lien est accessible ("Tous les
                        utilisateurs disposant du lien").
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="space-y-4">
              <div className="bg-green-50 dark:bg-green-900/20 p-4 rounded-xl border border-green-100 dark:border-green-900/30 flex items-center gap-4">
                <div className="bg-green-100 dark:bg-green-900/40 p-2 rounded-full text-green-600 dark:text-green-400">
                  <Check className="h-6 w-6" />
                </div>
                <div>
                  <p className="font-bold text-green-800 dark:text-green-300">
                    Importation terminée
                  </p>
                  <p className="text-green-600 dark:text-green-400 text-sm">
                    {summary.successCount} produits créés / mis à jour.
                  </p>
                </div>
              </div>

              {summary.failureCount > 0 && (
                <div className="bg-red-50 dark:bg-red-900/20 p-4 rounded-xl border border-red-100 dark:border-red-900/30">
                  <div className="flex items-center gap-2 mb-2 text-red-800 dark:text-red-300 font-bold">
                    <AlertCircle className="h-5 w-5" />
                    {summary.failureCount} Erreurs
                  </div>
                  <div className="max-h-32 overflow-y-auto text-xs text-red-600 dark:text-red-400 space-y-1 bg-white dark:bg-[#1c191a] p-2 rounded border border-red-100 dark:border-red-900/30">
                    {summary.errorMessages.map((msg, i) => (
                      <div key={i}>{msg}</div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="p-6 border-t border-gray-100 dark:border-white/10 bg-gray-50 dark:bg-[#242021] flex justify-end gap-3 rounded-b-2xl">
          {!summary ? (
            <>
              <button
                onClick={reset}
                className="px-4 py-2 text-gray-600 dark:text-gray-300 font-bold hover:bg-gray-200 dark:hover:bg-white/10 rounded-lg transition-colors"
              >
                Annuler
              </button>
              <button
                onClick={handleImport}
                disabled={loading || (!file && !url)}
                className="px-6 py-2 bg-primary text-white font-bold rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                {loading && (
                  <div className="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
                )}
                Lancer l'import
              </button>
            </>
          ) : (
            <button
              onClick={reset}
              className="px-6 py-2 bg-gray-800 dark:bg-gray-700 text-white font-bold rounded-lg hover:bg-gray-900 dark:hover:bg-gray-600 transition-colors"
            >
              Fermer
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default ImportProductModal;
