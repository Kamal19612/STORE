import { useState, useEffect } from "react";
import {
  MapPin,
  Phone,
  CheckCircle,
  XCircle,
  Navigation,
  Clock,
  Package,
  Truck,
  User,
  RefreshCw,
  ChevronRight,
  Store,
} from "lucide-react";
import { toast } from "react-toastify";
import api from "../../services/api";

const DeliveryDashboard = () => {
  const [activeTab, setActiveTab] = useState("available"); // available | my-orders
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [validationCode, setValidationCode] = useState({}); // { orderId: "code" }
  const [refreshing, setRefreshing] = useState(false);
  const [settings, setSettings] = useState({});

  const fetchOrders = async () => {
    setLoading(true);
    try {
      let endpoint = "/delivery/orders";
      if (activeTab === "my-orders") {
        endpoint = "/delivery/orders/my-orders";
      }
      const response = await api.get(endpoint);
      setOrders(response.data.content || []);
    } catch (error) {
      console.error(error);
      toast.error("Erreur chargement des commandes");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };
  const fetchSettings = async () => {
    try {
      const response = await api.get("/public/settings");
      setSettings(response.data || {});
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    fetchOrders();
    fetchSettings();
  }, [activeTab]);

  const handleRefresh = () => {
    setRefreshing(true);
    fetchOrders();
    fetchSettings();
  };

  const handleClaimOrder = async (id) => {
    try {
      await api.put(`/delivery/orders/${id}/claim`);
      toast.success("Commande prise en charge !");
      setActiveTab("my-orders"); // Switch to my orders automatically
    } catch (error) {
      toast.error(
        "Erreur prise en charge : " +
          (error.response?.data?.message || "Erreur inconnue"),
      );
    }
  };

  const handleCompleteDelivery = async (id) => {
    const code = validationCode[id];
    if (!code) {
      toast.warning("Code requis");
      return;
    }

    try {
      await api.post(`/delivery/orders/${id}/complete`, { code });
      toast.success("Livraison validée ! 🎉");
      fetchOrders();
    } catch (error) {
      toast.error(
        "Erreur : " + (error.response?.data?.message || "Code incorrect"),
      );
    }
  };

  const openMap = (address) => {
    const encoded = encodeURIComponent(address);
    window.open(
      `https://www.google.com/maps/search/?api=1&query=${encoded}`,
      "_blank",
    );
  };

  const callCustomer = (phone) => {
    window.open(`tel:${phone}`, "_self");
  };

  const handleCodeChange = (id, value) => {
    setValidationCode((prev) => ({ ...prev, [id]: value }));
  };

  return (
    <div className="space-y-6 pb-24 max-w-lg mx-auto">
      {/* Header Mobile */}
      <div className="flex items-center justify-between sticky top-0 bg-gray-100/95 backdrop-blur-sm z-10 py-2 -mx-4 px-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">
            Bonjour 👋
          </h1>
          <p className="text-sm text-gray-500 font-medium">
            Prêt pour la route ?
          </p>
        </div>
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className={`p-3 rounded-full bg-white shadow-sm border border-gray-200 text-gray-600 hover:text-primary transition-all ${
            refreshing ? "animate-spin text-primary" : ""
          }`}
        >
          <RefreshCw className="h-5 w-5" />
        </button>
      </div>

      {/* Tabs - Segmented Control */}
      <div className="bg-white p-1.5 rounded-2xl shadow-sm border border-gray-200 flex relative">
        <button
          onClick={() => setActiveTab("available")}
          className={`flex-1 py-2.5 text-sm font-bold rounded-xl transition-all duration-200 flex items-center justify-center gap-2 relative z-10 ${
            activeTab === "available"
              ? "bg-gray-900 text-white shadow-md transform scale-100"
              : "text-gray-500 hover:bg-gray-50"
          }`}
        >
          <span>Disponibles</span>
          {activeTab === "available" && orders.length > 0 && (
            <span className="bg-white text-gray-900 text-[10px] px-1.5 py-0.5 rounded-full min-w-[18px]">
              {orders.length}
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveTab("my-orders")}
          className={`flex-1 py-2.5 text-sm font-bold rounded-xl transition-all duration-200 flex items-center justify-center gap-2 relative z-10 ${
            activeTab === "my-orders"
              ? "bg-gray-900 text-white shadow-md transform scale-100"
              : "text-gray-500 hover:bg-gray-50"
          }`}
        >
          <span>Mes Courses</span>
          {activeTab === "my-orders" && orders.length > 0 && (
            <span className="bg-white text-gray-900 text-[10px] px-1.5 py-0.5 rounded-full min-w-[18px]">
              {orders.length}
            </span>
          )}
        </button>
      </div>

      {/* Content */}
      <div className="space-y-4">
        {loading && !refreshing ? (
          <div className="space-y-4 animate-pulse">
            {[1, 2].map((i) => (
              <div key={i} className="h-40 bg-gray-200 rounded-2xl"></div>
            ))}
          </div>
        ) : orders.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center space-y-4 bg-white rounded-3xl border border-dashed border-gray-200 mx-4">
            <div className="w-20 h-20 bg-gray-50 rounded-full flex items-center justify-center">
              <Truck className="h-10 w-10 text-gray-300" />
            </div>
            <div>
              <p className="text-lg font-bold text-gray-900">
                {activeTab === "available"
                  ? "Aucune commande"
                  : "Vous êtes libre !"}
              </p>
              <p className="text-sm text-gray-500 mt-1">
                {activeTab === "available"
                  ? "Revenez plus tard pour de nouvelles courses."
                  : "Prenez une commande dans l'onglet 'Disponibles'."}
              </p>
            </div>
          </div>
        ) : (
          orders.map((order) => (
            <div
              key={order.id}
              className="bg-white rounded-3xl shadow-[0_2px_20px_rgba(0,0,0,0.04)] border border-gray-100 overflow-hidden"
            >
              {/* Card Header */}
              <div className="px-5 py-4 border-b border-gray-50 flex justify-between items-center bg-gray-50/50">
                <div className="flex items-center gap-2">
                  <span className="bg-white border border-gray-200 text-gray-900 text-xs font-bold px-2.5 py-1 rounded-lg shadow-sm">
                    #{order.orderNumber}
                  </span>
                  <span className="flex items-center gap-1 text-xs font-medium text-gray-500 bg-gray-100 px-2 py-1 rounded-lg">
                    <Clock className="h-3 w-3" />
                    {new Date(order.createdAt).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                </div>
                <div className="text-right">
                  <span className="block font-extrabold text-primary text-lg leading-none">
                    {order.total.toLocaleString()} F
                  </span>
                </div>
              </div>

              {/* Card Body */}
              <div className="p-5 space-y-4">
                {/* Point de retrait */}
                <div className="flex gap-3">
                  <div className="mt-1 w-10 h-10 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
                    <Store className="h-5 w-5" />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-0.5">
                      <h4 className="text-[10px] font-extrabold text-amber-600 uppercase tracking-wider">
                        BOUTIQUE
                      </h4>
                    </div>
                    <div className="flex items-center gap-2 mb-0.5">
                      <h3
                        onClick={() =>
                          openMap(
                            settings.store_location || settings.contact_address,
                          )
                        }
                        className="text-lg font-bold text-gray-900 leading-tight truncate cursor-pointer hover:text-primary transition-colors"
                      >
                        {settings.store_name || "SUCRE STORE"}
                      </h3>
                      {settings.whatsapp_number && (
                        <a
                          href={`tel:${settings.whatsapp_number}`}
                          className="text-[10px] font-bold text-amber-600 bg-amber-100/50 px-2 py-0.5 rounded-full hover:bg-amber-100 transition-colors whitespace-nowrap"
                        >
                          📞 {settings.whatsapp_number}
                        </a>
                      )}
                    </div>
                    <p className="text-sm text-gray-500 font-medium">
                      {settings.contact_address || "Adresse non configurée"}
                    </p>
                  </div>
                </div>

                {/* Visual Divider */}
                <div className="flex flex-col items-center w-10 -my-3 opacity-20">
                  <div className="w-1 h-1 rounded-full bg-gray-400 my-0.5" />
                  <div className="w-1 h-1 rounded-full bg-gray-400 my-0.5" />
                  <div className="w-1 h-1 rounded-full bg-gray-400 my-0.5" />
                </div>

                {/* Main Address (Delivery) */}
                <div className="flex gap-3">
                  <div className="mt-1 w-10 h-10 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                    <MapPin className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-gray-900 leading-tight mb-1">
                      {order.customerAddress}
                    </h3>
                    <p className="text-sm text-gray-500 font-medium">
                      Client : {order.customerName}
                    </p>
                  </div>
                </div>

                {/* Actions Section */}
                {activeTab === "available" ? (
                  <button
                    onClick={() => handleClaimOrder(order.id)}
                    className="w-full mt-2 bg-primary text-white py-3.5 rounded-2xl font-bold shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 text-base"
                  >
                    <span>Accepter la course</span>
                    <ChevronRight className="h-5 w-5 opacity-80" />
                  </button>
                ) : (
                  <div className="space-y-4 pt-2">
                    {/* Grid Actions */}
                    <div className="grid grid-cols-2 gap-3">
                      <button
                        onClick={() => callCustomer(order.customerPhone)}
                        className="flex flex-col items-center justify-center gap-1.5 bg-gray-50 hover:bg-gray-100 border border-gray-100 text-gray-800 py-3 rounded-xl transition-all active:bg-gray-200"
                      >
                        <Phone className="h-5 w-5 text-gray-600" />
                        <span className="text-xs font-bold">Appeler</span>
                      </button>
                      <button
                        onClick={() => openMap(order.customerAddress)}
                        className="flex flex-col items-center justify-center gap-1.5 bg-blue-50 hover:bg-blue-100 border border-blue-100 text-blue-700 py-3 rounded-xl transition-all active:bg-blue-200"
                      >
                        <Navigation className="h-5 w-5" />
                        <span className="text-xs font-bold">Y aller</span>
                      </button>
                    </div>

                    {/* Validation Zone */}
                    <div className="bg-gray-50 rounded-2xl p-4 border border-gray-100">
                      <label className="text-xs uppercase font-bold text-gray-400 mb-2 block tracking-wider">
                        Validation Livraison
                      </label>
                      <div className="flex gap-2">
                        <input
                          type="tel" // Numeric keyboard on mobile
                          placeholder="Code (ex: 123456)"
                          className="flex-1 bg-white border border-gray-200 rounded-xl px-4 text-center font-mono font-bold text-lg focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary placeholder:text-gray-300 placeholder:font-sans placeholder:text-sm placeholder:font-normal"
                          value={validationCode[order.id] || ""}
                          onChange={(e) =>
                            handleCodeChange(order.id, e.target.value)
                          }
                          maxLength={10}
                        />
                        <button
                          onClick={() => handleCompleteDelivery(order.id)}
                          className="bg-green-600 text-white px-5 rounded-xl font-bold shadow-md shadow-green-600/20 active:bg-green-700 transition-colors flex items-center justify-center"
                        >
                          <CheckCircle className="h-6 w-6" />
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default DeliveryDashboard;
