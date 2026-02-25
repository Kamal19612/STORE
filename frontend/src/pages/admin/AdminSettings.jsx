import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import {
  getAdminSettings,
  updateSettings,
  resetStats,
} from "../../services/api";
import { Save, Settings, Trash2 } from "lucide-react";
import useAuthStore from "../../store/authStore";

const AdminSettings = () => {
  const { user } = useAuthStore();
  const navigate = useNavigate();

  const [settings, setSettings] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    // Vérification stricte du rôle SUPER_ADMIN
    if (user?.role !== "SUPER_ADMIN") {
      toast.error("Accès non autorisé.");
      navigate("/admin/dashboard");
      return;
    }
    fetchSettings();
  }, [user, navigate]);

  const fetchSettings = async () => {
    try {
      const response = await getAdminSettings();
      // Convert list of objects {key, value} to object {key: value}
      const settingsMap = {};
      response.data.forEach((s) => {
        settingsMap[s.key] = s.value;
      });
      setSettings(settingsMap);
    } catch (error) {
      console.error("Erreur chargement paramètres:", error);
      toast.error("Impossible de charger les paramètres.");
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setSettings((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await updateSettings(settings);
      toast.success("Paramètres mis à jour avec succès !");
    } catch (error) {
      console.error("Erreur sauvegarde:", error);
      toast.error("Erreur lors de la sauvegarde.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-8">Chargement...</div>;

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-800 mb-6 flex items-center gap-2">
        <Settings className="w-8 h-8 text-primary" />
        Paramètres de l'application
      </h1>

      <div className="bg-white rounded-lg shadow-md p-6 max-w-4xl">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid md:grid-cols-2 gap-6">
            {/* Section Contact */}
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-secondary border-b pb-2">
                Coordonnées & Informations
              </h2>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Nom de la boutique
                </label>
                <input
                  type="text"
                  name="store_name"
                  value={settings.store_name || ""}
                  onChange={handleChange}
                  placeholder="SUCRE STORE"
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent font-bold"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Numéro WhatsApp (Commandes)
                </label>
                <input
                  type="text"
                  name="whatsapp_number"
                  value={settings.whatsapp_number || ""}
                  onChange={handleChange}
                  placeholder="226XXXXXXXX"
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent font-mono"
                />
                <p className="text-xs text-gray-500 mt-1">
                  Format international sans + (ex: 22670123456)
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Numéro de téléphone
                </label>
                <input
                  type="text"
                  name="contact_phone"
                  value={settings.contact_phone || ""}
                  onChange={handleChange}
                  placeholder="+225 07..."
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email de contact
                </label>
                <input
                  type="email"
                  name="contact_email"
                  value={settings.contact_email || ""}
                  onChange={handleChange}
                  placeholder="contact@example.com"
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Adresse physique
                </label>
                <textarea
                  name="contact_address"
                  value={settings.contact_address || ""}
                  onChange={handleChange}
                  rows="3"
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                ></textarea>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Localisation Google Maps (Lien ou Coordonnées)
                </label>
                <input
                  type="text"
                  name="store_location"
                  value={settings.store_location || ""}
                  onChange={handleChange}
                  placeholder="ex: 12.371, -1.519 ou lien maps"
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent font-mono"
                />
                <p className="text-xs text-gray-500 mt-1">
                  Ce champ est utilisé pour le bouton "Y aller" des livreurs.
                </p>
              </div>
            </div>

            {/* Section Réseaux Sociaux & Divers */}
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-secondary border-b pb-2">
                Réseaux Sociaux & Divers
              </h2>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Lien Facebook
                </label>
                <input
                  type="text"
                  name="social_facebook"
                  value={settings.social_facebook || ""}
                  onChange={handleChange}
                  placeholder="https://facebook.com/..."
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Lien Instagram
                </label>
                <input
                  type="text"
                  name="social_instagram"
                  value={settings.social_instagram || ""}
                  onChange={handleChange}
                  placeholder="https://instagram.com/..."
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Texte Copyright (Pied de page)
                </label>
                <input
                  type="text"
                  name="footer_copyright"
                  value={settings.footer_copyright || ""}
                  onChange={handleChange}
                  placeholder="Tous droits réservés."
                  className="w-full p-2 border rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                />
              </div>
            </div>
          </div>

          <div className="pt-4 border-t flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="flex items-center gap-2 bg-primary hover:bg-primary-dark text-white px-6 py-2 rounded-lg font-bold transition-colors disabled:opacity-50"
            >
              <Save className="w-5 h-5" />
              {saving ? "Sauvegarde..." : "Enregistrer les modifications"}
            </button>
          </div>

          {/* Zone de Danger */}
          <div className="mt-8 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/40 rounded-lg p-6 max-w-4xl">
            <h2 className="text-xl font-bold text-red-700 dark:text-red-400 mb-2 flex items-center gap-2">
              <Trash2 className="w-6 h-6" />
              Zone de Danger
            </h2>
            <p className="text-red-600 dark:text-red-400/80 mb-4 text-sm">
              Les actions ci-dessous sont irréversibles. Soyez prudent.
            </p>

            <div className="flex items-center justify-between bg-white dark:bg-secondary p-4 rounded-lg border border-red-100 dark:border-red-900/20">
              <div>
                <h3 className="font-semibold text-gray-800 dark:text-white">
                  Réinitialiser les statistiques
                </h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Supprime <strong>toutes les commandes</strong> et remet les
                  compteurs à zéro.
                </p>
              </div>
              <button
                onClick={async () => {
                  if (
                    window.confirm(
                      "Êtes-vous SÛR de vouloir tout supprimer ?\nCette action est IRRÉVERSIBLE.",
                    )
                  ) {
                    if (
                      window.confirm(
                        "Dernière chance : Cela supprimera TOUT l'historique des commandes. Confirmer ?",
                      )
                    ) {
                      try {
                        await resetStats();
                        toast.success("Statistiques réinitialisées.");
                      } catch (e) {
                        console.error(e);
                        toast.error("Erreur réinitialisation.");
                      }
                    }
                  }
                }}
                className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg font-bold transition-colors text-sm"
              >
                Réinitialiser Tout
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AdminSettings;
