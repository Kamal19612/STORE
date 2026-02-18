import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  MapPin,
  Phone,
  User,
  Home as HomeIcon,
  MessageSquare,
  Send,
  CheckCircle,
  ShoppingBag,
} from "lucide-react";
import useCartStore from "../../store/cartStore";
import api from "../../services/api";
import { toast } from "react-toastify";

const Checkout = () => {
  const { items, clearCart } = useCartStore();
  const navigate = useNavigate();

  // Calculer le total directement pour garantir la réactivité
  const total = items.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0,
  );

  const [formData, setFormData] = useState({
    customerName: "",
    customerPhone: "",
    customerAddress: "",
    customerNotes: "",
    customerLatitude: null,
    customerLongitude: null,
  });

  const [loading, setLoading] = useState(false);
  const [gpsLoading, setGpsLoading] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(null);

  useEffect(() => {
    if (items.length === 0 && !orderSuccess) {
      navigate("/");
    }
  }, [items, navigate, orderSuccess]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const getGeolocation = () => {
    setGpsLoading(true);
    if (!navigator.geolocation) {
      toast.error(
        "La géolocalisation n'est pas supportée par votre navigateur.",
      );
      setGpsLoading(false);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setFormData({
          ...formData,
          customerLatitude: position.coords.latitude,
          customerLongitude: position.coords.longitude,
        });
        toast.success("Position récupérée avec succès !");
        setGpsLoading(false);
      },
      (error) => {
        console.error("Erreur GPS:", error);
        toast.error(
          "Impossible de récupérer votre position. Vérifiez vos permissions.",
        );
        setGpsLoading(false);
      },
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    const payload = {
      ...formData,
      items: items.map((item) => ({
        productId: item.id,
        quantity: item.quantity,
      })),
    };

    try {
      const response = await api.post("/orders", payload);
      setOrderSuccess(response.data);
      // clearCart(); // Déplacé vers la fermeture de la modale de succès
      toast.success("Commande enregistrée !");
    } catch (error) {
      console.error("Erreur commande:", error);
      toast.error(
        error.response?.data?.message ||
          "Une erreur est survenue lors de la commande.",
      );
    } finally {
      setLoading(false);
    }
  };

  if (orderSuccess) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center">
        <div className="bg-white p-8 rounded-3xl shadow-xl border border-green-50">
          <div className="flex justify-center mb-6">
            <div className="bg-green-100 p-4 rounded-full">
              <CheckCircle className="h-16 w-16 text-green-500" />
            </div>
          </div>
          <h1 className="text-3xl font-black text-secondary uppercase tracking-tight">
            Merci pour votre commande !
          </h1>
          <p className="mt-4 text-gray-600 text-lg">
            Votre commande{" "}
            <span className="font-bold text-primary">
              #{orderSuccess.orderNumber}
            </span>{" "}
            a été reçue.
          </p>

          <div className="mt-8 bg-primary/10 p-6 rounded-2xl border border-primary/20">
            <p className="text-secondary font-medium mb-4">
              Pour une confirmation rapide, envoyez-nous un message sur WhatsApp
              :
            </p>
            <a
              href={orderSuccess.whatsappLink}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-3 bg-[#25D366] hover:bg-[#128C7E] text-white font-bold py-4 px-8 rounded-full transition-all transform hover:scale-105 shadow-lg"
            >
              <MessageSquare className="h-6 w-6" />
              Confirmer sur WhatsApp
            </a>
          </div>

          <button
            onClick={() => {
              clearCart();
              navigate("/");
            }}
            className="mt-8 text-secondary font-bold hover:text-primary transition-colors underline decoration-2 underline-offset-4"
          >
            Retour à l'accueil
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-black text-secondary mb-8 uppercase tracking-tight text-center">
        Finaliser ma commande
      </h1>

      <div className="w-full">
        {/* Formulaire */}
        <div className="bg-white p-6 md:p-8 rounded-3xl shadow-sm border border-gray-100">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-4">
              <h2 className="text-xl font-bold text-secondary flex items-center gap-2 mb-6">
                <span className="bg-primary text-white h-7 w-7 rounded-full flex items-center justify-center text-sm">
                  1
                </span>
                Informations Personnelles
              </h2>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1 uppercase tracking-wider">
                  Nom complet ou Pseudo
                </label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <input
                    type="text"
                    name="customerName"
                    required
                    value={formData.customerName}
                    onChange={handleInputChange}
                    placeholder="Ex: Jean Dupont"
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent transition-all outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1 uppercase tracking-wider">
                  Numéro WhatsApp
                </label>
                <div className="relative">
                  <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <input
                    type="tel"
                    name="customerPhone"
                    required
                    value={formData.customerPhone}
                    onChange={handleInputChange}
                    placeholder="Ex: 0707070707"
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent transition-all outline-none"
                  />
                </div>
              </div>
            </div>

            <div className="space-y-4 pt-6 border-t border-gray-100">
              <h2 className="text-xl font-bold text-secondary flex items-center gap-2 mb-6">
                <span className="bg-primary text-white h-7 w-7 rounded-full flex items-center justify-center text-sm">
                  2
                </span>
                Livraison
              </h2>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1 uppercase tracking-wider">
                  Adresse exacte
                </label>
                <div className="relative">
                  <HomeIcon className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                  <textarea
                    name="customerAddress"
                    required
                    rows="3"
                    value={formData.customerAddress}
                    onChange={handleInputChange}
                    placeholder="Quartier, rue, repères visuels..."
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent transition-all outline-none"
                  ></textarea>
                </div>
              </div>

              <div className="bg-gray-50 p-4 rounded-2xl border border-dashed border-gray-300">
                <p className="text-sm text-gray-600 mb-3">
                  Pour aider le livreur, partagez votre position GPS actuelle.
                </p>
                <button
                  type="button"
                  onClick={getGeolocation}
                  disabled={gpsLoading}
                  className={`flex items-center justify-center gap-2 w-full py-3 rounded-xl font-bold transition-all ${
                    formData.customerLatitude
                      ? "bg-green-100 text-green-700 border border-green-200"
                      : "bg-white text-secondary border border-gray-200 hover:border-primary hover:text-primary active:scale-95"
                  }`}
                >
                  <MapPin
                    className={`h-5 w-5 ${gpsLoading ? "animate-spin" : ""}`}
                  />
                  {formData.customerLatitude
                    ? "Position récupérée ✅"
                    : "Récupérer ma position GPS"}
                </button>
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1 uppercase tracking-wider">
                  Notes (Optionnel)
                </label>
                <div className="relative">
                  <MessageSquare className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                  <textarea
                    name="customerNotes"
                    rows="2"
                    value={formData.customerNotes}
                    onChange={handleInputChange}
                    placeholder="Précisions pour la livraison..."
                    className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary focus:border-transparent transition-all outline-none"
                  ></textarea>
                </div>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full btn-primary py-4 text-lg flex items-center justify-center gap-3 shadow-xl shadow-primary/20"
            >
              {loading ? (
                <span className="flex items-center gap-2">
                  <svg
                    className="animate-spin h-5 w-5 text-white"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                      fill="none"
                    ></circle>
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    ></path>
                  </svg>
                  Traitement...
                </span>
              ) : (
                <>
                  <Send className="h-5 w-5" />
                  Confirmer la commande
                </>
              )}
            </button>
          </form>
        </div>

        {/* Récapitulatif */}
        {/* Section Récapitulatif de la Commande (Masquée)
        <div className="space-y-6">
          <div className="bg-secondary text-white p-8 rounded-3xl shadow-xl">
            <h2 className="text-xl font-bold mb-6 uppercase tracking-tight flex items-center gap-2">
              <ShoppingBag className="text-primary" />
              Résumé de la commande
            </h2>

            <div className="space-y-4 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
              {items.map((item) => (
                <div
                  key={item.id}
                  className="flex justify-between items-center bg-secondary-light/50 p-3 rounded-xl"
                >
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 bg-white rounded-lg p-1 flex-shrink-0">
                      <img
                        src={item.mainImage}
                        alt={item.name}
                        className="h-full w-full object-contain"
                      />
                    </div>
                    <div>
                      <p className="font-bold text-sm line-clamp-1">
                        {item.name}
                      </p>
                      <p className="text-xs text-gray-400">
                        Qté: {item.quantity}
                      </p>
                    </div>
                  </div>
                  <span className="font-bold text-primary">
                    {(item.price * item.quantity).toLocaleString()} FCFA
                  </span>
                </div>
              ))}
            </div>

            <div className="mt-8 pt-8 border-t border-secondary-light space-y-4">
              <div className="flex justify-between text-gray-300">
                <span>Sous-total</span>
                <span>{total.toLocaleString()} FCFA</span>
              </div>
              <div className="flex justify-between text-gray-300">
                <span>Livraison</span>
                <span className="text-xs italic">À définir</span>
              </div>
              <div className="flex justify-between items-center text-2xl font-black pt-4 border-t border-secondary-light">
                <span className="text-primary uppercase text-sm tracking-widest">
                  Total
                </span>
                <span>{total.toLocaleString()} FCFA</span>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-gray-100 flex items-start gap-4">
            <div className="bg-primary/10 p-3 rounded-2xl text-primary">
              <MessageSquare className="h-6 w-6" />
            </div>
            <div>
              <h4 className="font-bold text-secondary">Besoin d'aide ?</h4>
              <p className="text-sm text-gray-500 mt-1">
                Notre équipe est disponible sur WhatsApp pour répondre à vos
                questions sur la livraison.
              </p>
            </div>
          </div>
        </div>
        */}
      </div>
    </div>
  );
};

export default Checkout;
