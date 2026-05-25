import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import {
  getAdminSettings,
  updateSettings,
  resetStats,
  syncProducts,
  getTelegramWebhookInfo,
  sendTelegramTest,
} from "../../services/api";
import { Save, Settings, Trash2, CheckCircle } from "lucide-react";
import useAuthStore from "../../store/authStore";

const inputCls =
  "w-full p-2 border border-gray-300 dark:border-white/10 rounded bg-white dark:bg-[#1c191a] text-gray-900 dark:text-white focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-colors";
const labelCls = "block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1";
const smallLabelCls = "block text-xs font-bold text-gray-500 dark:text-gray-400 uppercase mb-1";
const sectionTitleCls =
  "text-lg font-semibold text-gray-800 dark:text-white border-b border-gray-200 dark:border-white/10 pb-2";

const AdminSettings = () => {
  const { user } = useAuthStore();
  const navigate = useNavigate();

  const [settings, setSettings] = useState({});
  const [loading, setLoading] = useState(true);
  const [syncLoading, setSyncLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [telegramInfoLoading, setTelegramInfoLoading] = useState(false);
  const [telegramActionLoading, setTelegramActionLoading] = useState(false);
  const [telegramStatus, setTelegramStatus] = useState("");
  const [telegramTestText, setTelegramTestText] = useState("Test Telegram OK");

  useEffect(() => {
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

  const handleSyncNow = async () => {
    setSyncLoading(true);
    try {
      await syncProducts();
      toast.success("Synchronisation terminée avec succès !");
    } catch (error) {
      console.error("Erreur synchro:", error);
      toast.error("Erreur lors de la synchronisation.");
    } finally {
      setSyncLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    // Extraction automatique de l'ID depuis une URL Google Sheets complète
    if (name === "google_sheet_id") {
      const match = value.match(/\/spreadsheets\/d\/([a-zA-Z0-9-_]+)/);
      setSettings((prev) => ({ ...prev, [name]: match ? match[1] : value }));
      return;
    }
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

  const handleTelegramWebhookInfo = async () => {
    setTelegramInfoLoading(true);
    try {
      const res = await getTelegramWebhookInfo();
      const raw = typeof res?.data === "string" ? res.data : JSON.stringify(res?.data, null, 2);
      setTelegramStatus(raw);
      toast.success("Statut webhook Telegram mis à jour.");
    } catch (error) {
      console.error("Erreur webhook info Telegram:", error);
      toast.error("Impossible de récupérer le statut du webhook Telegram.");
    } finally {
      setTelegramInfoLoading(false);
    }
  };

  const handleTelegramTest = async () => {
    setTelegramActionLoading(true);
    try {
      await sendTelegramTest(telegramTestText);
      toast.success("Message de test envoyé sur Telegram.");
    } catch (error) {
      console.error("Erreur test Telegram:", error);
      toast.error("Impossible d'envoyer le message de test Telegram.");
    } finally {
      setTelegramActionLoading(false);
    }
  };

  if (loading) return <div className="p-4 sm:p-8 text-gray-600 dark:text-gray-300">Chargement...</div>;

  return (
    <div className="p-3 sm:p-6">
      <h1 className="text-xl sm:text-2xl font-bold text-gray-800 dark:text-white mb-5 sm:mb-6 flex items-center gap-2">
        <Settings className="w-7 h-7 sm:w-8 sm:h-8 text-primary" />
        Paramètres de l'application
      </h1>

      <div className="bg-white dark:bg-[#242021] rounded-lg shadow-md p-4 sm:p-6 max-w-4xl">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid md:grid-cols-2 gap-4 sm:gap-6">

            {/* Section Contact */}
            <div className="space-y-4">
              <h2 className={sectionTitleCls}>Coordonnées & Informations</h2>

              <div>
                <label className={labelCls}>Nom de la boutique</label>
                <input type="text" name="store_name" value={settings.store_name || ""} onChange={handleChange} placeholder="SUCRE STORE" className={`${inputCls} font-bold`} />
              </div>

              <div>
                <label className={labelCls}>Numéro WhatsApp (Commandes)</label>
                <input type="text" name="whatsapp_number" value={settings.whatsapp_number || ""} onChange={handleChange} placeholder="226XXXXXXXX" className={`${inputCls} font-mono`} />
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Format international sans + (ex: 22670123456)</p>
              </div>

              <div>
                <label className={labelCls}>Indicatif par défaut (Checkout)</label>
                <input
                  type="text"
                  name="customer_whatsapp_dial_code"
                  value={settings.customer_whatsapp_dial_code || ""}
                  onChange={handleChange}
                  placeholder="+226"
                  className={`${inputCls} font-mono`}
                />
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  Pré-rempli dans le champ &quot;Numéro WhatsApp&quot; du checkout (ex: +226, +225).
                </p>
              </div>

              <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800/30 p-3 rounded-lg">
                <label className="block text-sm font-bold text-blue-800 dark:text-blue-300 mb-1 flex items-center gap-1">
                  Telegram Bot Token
                  <span className="inline-flex items-center justify-center w-4 h-4 rounded-full bg-blue-200 text-blue-700 text-[10px] font-bold" title="Token du bot Telegram">i</span>
                </label>
                <input
                  type="password"
                  name="telegram_bot_token"
                  value={settings.telegram_bot_token || ""}
                  onChange={handleChange}
                  placeholder="Ex: 123456789:AAE..."
                  className={`${inputCls} font-mono border-blue-300 dark:border-blue-700`}
                  autoComplete="off"
                />
                <div className="text-xs text-blue-600 dark:text-blue-400 mt-2 space-y-1">
                  <p>Token du bot Telegram (BotFather) utilisé pour envoyer les notifications.</p>
                  <p className="font-semibold">Comment obtenir le token ?</p>
                  <ol className="list-decimal ml-4 pl-1">
                    <li>Ouvrez Telegram</li>
                    <li>Cherchez le bot <strong>@BotFather</strong></li>
                    <li>Envoyez <code>/newbot</code> puis suivez les étapes</li>
                    <li>Copiez le token (format <code>123456:ABC...</code>) et collez-le ici.</li>
                  </ol>
                </div>
              </div>

              <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800/30 p-3 rounded-lg">
                <label className="block text-sm font-bold text-blue-800 dark:text-blue-300 mb-1 flex items-center gap-1">
                  Telegram Chat ID
                  <span className="inline-flex items-center justify-center w-4 h-4 rounded-full bg-blue-200 text-blue-700 text-[10px] font-bold" title="Identifiant du bot Telegram">i</span>
                </label>
                <input type="text" name="telegram_chat_id" value={settings.telegram_chat_id || ""} onChange={handleChange} placeholder="Ex: 5654423490" className={`${inputCls} font-mono border-blue-300 dark:border-blue-700`} />
                <div className="text-xs text-blue-600 dark:text-blue-400 mt-2 space-y-1">
                  <p>Où recevez-vous les alertes de commandes via Telegram.</p>
                  <p className="font-semibold">Comment trouver votre ID ?</p>
                  <ol className="list-decimal ml-4 pl-1">
                    <li>Ouvrez Telegram</li>
                    <li>Cherchez le bot <strong>@userinfobot</strong></li>
                    <li>Envoyez lui le message <code>/start</code></li>
                    <li>Copiez l'ID (suite de chiffres) et collez-le ici.</li>
                  </ol>
                </div>
              </div>

              <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800/30 p-3 rounded-lg flex flex-col gap-2">
                <div className="text-xs text-blue-700 dark:text-blue-300">
                  <p className="font-semibold">Webhook Telegram</p>
                  <p>
                    Le webhook est <strong>toujours actif</strong> (enregistrement automatique côté serveur).
                  </p>
                </div>
                <div className="flex flex-col sm:flex-row gap-2">
                  <button
                    type="button"
                    disabled={telegramInfoLoading}
                    onClick={handleTelegramWebhookInfo}
                    className="flex-1 flex items-center justify-center gap-2 bg-white/80 hover:bg-white text-blue-700 border border-blue-200 px-4 py-2 rounded-lg font-bold transition-colors disabled:opacity-50 text-sm dark:bg-blue-950/20 dark:hover:bg-blue-950/30 dark:text-blue-200 dark:border-blue-800/30"
                  >
                    <CheckCircle className={`w-4 h-4 ${telegramInfoLoading ? "animate-pulse" : ""}`} />
                    {telegramInfoLoading ? "Lecture..." : "Voir le statut"}
                  </button>
                </div>

                <div className="flex flex-col sm:flex-row gap-2 pt-1">
                  <button
                    type="button"
                    disabled={telegramActionLoading}
                    onClick={handleTelegramTest}
                    className="flex-1 flex items-center justify-center gap-2 bg-white/80 hover:bg-white text-green-700 border border-green-200 px-4 py-2 rounded-lg font-bold transition-colors disabled:opacity-50 text-sm dark:bg-green-950/10 dark:hover:bg-green-950/20 dark:text-green-200 dark:border-green-900/30"
                  >
                    Envoyer test
                  </button>
                </div>

                <input
                  type="text"
                  value={telegramTestText}
                  onChange={(e) => setTelegramTestText(e.target.value)}
                  placeholder="Texte du message de test"
                  className={`${inputCls} font-mono border-blue-300 dark:border-blue-700`}
                />

                {telegramStatus ? (
                  <pre className="text-[11px] leading-snug whitespace-pre-wrap break-words bg-white/60 dark:bg-black/20 border border-blue-200 dark:border-blue-800/30 rounded p-2 text-blue-900 dark:text-blue-100 max-h-56 overflow-auto">
                    {telegramStatus}
                  </pre>
                ) : null}
              </div>

              <div>
                <label className={labelCls}>Numéro de téléphone</label>
                <input type="text" name="contact_phone" value={settings.contact_phone || ""} onChange={handleChange} placeholder="+225 07..." className={inputCls} />
              </div>

              <div>
                <label className={labelCls}>Email de contact</label>
                <input type="email" name="contact_email" value={settings.contact_email || ""} onChange={handleChange} placeholder="contact@example.com" className={inputCls} />
              </div>

              <div>
                <label className={labelCls}>Adresse physique</label>
                <textarea name="contact_address" value={settings.contact_address || ""} onChange={handleChange} rows="3" className={inputCls} />
              </div>

              <div>
                <label className={labelCls}>Localisation Google Maps (Lien ou Coordonnées)</label>
                <input type="text" name="store_location" value={settings.store_location || ""} onChange={handleChange} placeholder="ex: 12.371, -1.519 ou lien maps" className={`${inputCls} font-mono`} />
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Utilisé pour le bouton "Y aller" des livreurs.</p>
              </div>
            </div>

            {/* Section Réseaux Sociaux & Tarification */}
            <div className="space-y-4">
              <h2 className={sectionTitleCls}>Réseaux Sociaux & Divers</h2>

              <div>
                <label className={labelCls}>Lien Facebook</label>
                <input type="text" name="social_facebook" value={settings.social_facebook || ""} onChange={handleChange} placeholder="https://facebook.com/..." className={inputCls} />
              </div>

              <div>
                <label className={labelCls}>Lien Instagram</label>
                <input type="text" name="social_instagram" value={settings.social_instagram || ""} onChange={handleChange} placeholder="https://instagram.com/..." className={inputCls} />
              </div>

              <div>
                <label className={labelCls}>Texte Copyright (Pied de page)</label>
                <input type="text" name="footer_copyright" value={settings.footer_copyright || ""} onChange={handleChange} placeholder="Tous droits réservés." className={inputCls} />
              </div>

              {/* Tarification Livraison */}
              <div className="pt-4 space-y-4">
                <h2 className={sectionTitleCls}>Tarification Livraison</h2>

                <div className="space-y-4 bg-gray-50 dark:bg-[#1c191a] p-4 rounded-lg border border-gray-200 dark:border-white/10">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                    <div>
                      <label className={smallLabelCls}>Zone 1 max (km)</label>
                      <input type="number" name="dist_tier_1_limit" value={settings.dist_tier_1_limit || "5"} onChange={handleChange} className={inputCls} />
                    </div>
                    <div>
                      <label className={smallLabelCls}>Prix Zone 1 (FCFA)</label>
                      <input type="number" name="dist_tier_1_price" value={settings.dist_tier_1_price || "1000"} onChange={handleChange} className={`${inputCls} font-bold text-primary`} />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                    <div>
                      <label className={smallLabelCls}>Zone 2 max (km)</label>
                      <input type="number" name="dist_tier_2_limit" value={settings.dist_tier_2_limit || "10"} onChange={handleChange} className={inputCls} />
                    </div>
                    <div>
                      <label className={smallLabelCls}>Prix Zone 2 (FCFA)</label>
                      <input type="number" name="dist_tier_2_price" value={settings.dist_tier_2_price || "2000"} onChange={handleChange} className={`${inputCls} font-bold text-primary`} />
                    </div>
                  </div>

                  <div>
                    <label className={smallLabelCls}>Prix Hors Zone / Zone 3 (FCFA)</label>
                    <input type="number" name="dist_tier_3_price" value={settings.dist_tier_3_price || "3500"} onChange={handleChange} className={`${inputCls} font-bold text-primary`} />
                  </div>

                  <div>
                    <label className={smallLabelCls}>Achat min. livraison gratuite (Optionnel)</label>
                    <input type="number" name="min_order_free_delivery" value={settings.min_order_free_delivery || ""} onChange={handleChange} placeholder="Ex: 50000" className={inputCls} />
                  </div>

                  <div className="pt-2 grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 border-t border-gray-200 dark:border-white/10">
                    <div>
                      <label className={smallLabelCls}>Supplément Express (FCFA)</label>
                      <input type="number" name="express_surcharge" value={settings.express_surcharge || "1000"} onChange={handleChange} className={`${inputCls} font-bold text-orange-600`} />
                    </div>
                    <div>
                      <label className={smallLabelCls}>Supplément Programmé (FCFA)</label>
                      <input type="number" name="scheduled_surcharge" value={settings.scheduled_surcharge || "500"} onChange={handleChange} className={`${inputCls} font-bold text-blue-600`} />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>


          <div className="pt-4 border-t border-gray-200 dark:border-white/10 flex flex-col sm:flex-row sm:justify-end gap-3">
            <button
              type="submit"
              disabled={saving}
              className="flex items-center justify-center gap-2 bg-primary hover:bg-primary-dark text-white px-6 py-2.5 rounded-lg font-bold transition-colors disabled:opacity-50"
            >
              <Save className="w-5 h-5" />
              {saving ? "Sauvegarde..." : "Enregistrer les modifications"}
            </button>
          </div>

          {/* Zone de Danger */}
          <div className="mt-4 sm:mt-6 bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/40 rounded-lg p-4 sm:p-6">
            <h2 className="text-lg sm:text-xl font-bold text-red-700 dark:text-red-400 mb-2 flex items-center gap-2">
              <Trash2 className="w-5 h-5 sm:w-6 sm:h-6" />
              Zone de Danger
            </h2>
            <p className="text-red-600 dark:text-red-400/80 mb-4 text-sm">
              Les actions ci-dessous sont irréversibles. Soyez prudent.
            </p>

            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-white dark:bg-[#242021] p-4 rounded-lg border border-red-100 dark:border-red-900/20">
              <div>
                <h3 className="font-semibold text-gray-800 dark:text-white">Réinitialiser les statistiques</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Supprime <strong>toutes les commandes</strong> et remet les compteurs à zéro.
                </p>
              </div>
              <button
                type="button"
                onClick={async () => {
                  if (window.confirm("Êtes-vous SÛR de vouloir tout supprimer ?\nCette action est IRRÉVERSIBLE.")) {
                    if (window.confirm("Dernière chance : Cela supprimera TOUT l'historique des commandes. Confirmer ?")) {
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
                className="shrink-0 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg font-bold transition-colors text-sm"
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
