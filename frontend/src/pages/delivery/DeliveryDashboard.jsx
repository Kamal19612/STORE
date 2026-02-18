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
} from "lucide-react";
import { toast } from "react-toastify";
import api from "../../services/api";

const DeliveryDashboard = () => {
  const [activeTab, setActiveTab] = useState("available"); // available | my-orders
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [validationCode, setValidationCode] = useState({}); // { orderId: "code" }

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
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [activeTab]);

  const handleClaimOrder = async (id) => {
    try {
      await api.put(`/delivery/orders/${id}/claim`);
      toast.success("Commande prise en charge !");
      fetchOrders(); // Rafraîchir
      setActiveTab("my-orders"); // Switch to my orders
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
      toast.warning("Veuillez entrer le code de confirmation");
      return;
    }

    try {
      await api.post(`/delivery/orders/${id}/complete`, { code });
      toast.success("Livraison validée avec succès !");
      fetchOrders();
    } catch (error) {
      toast.error(
        "Erreur validation : " +
          (error.response?.data?.message || "Code incorrect ?"),
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
    <div className="space-y-6 pb-20">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">Espace Livreur</h1>
        <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded-full">
          {activeTab === "available" ? "Disponibles" : "Mes courses"}
        </span>
      </div>

      {/* Tabs */}
      <div className="flex p-1 bg-gray-100 rounded-xl">
        <button
          onClick={() => setActiveTab("available")}
          className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all ${
            activeTab === "available"
              ? "bg-white text-gray-900 shadow-sm"
              : "text-gray-500 hover:text-gray-700"
          }`}
        >
          Disponibles
        </button>
        <button
          onClick={() => setActiveTab("my-orders")}
          className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all ${
            activeTab === "my-orders"
              ? "bg-white text-gray-900 shadow-sm"
              : "text-gray-500 hover:text-gray-700"
          }`}
        >
          Mes Courses
        </button>
      </div>

      {loading ? (
        <div className="p-10 text-center text-gray-500">Chargement... 🛵</div>
      ) : orders.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
          <Truck className="h-16 w-16 mb-4 text-gray-200" />
          <p className="text-lg font-medium text-gray-600">
            {activeTab === "available"
              ? "Aucune commande disponible"
              : "Aucune course en cours"}
          </p>
          <p className="text-sm">Actualisez plus tard !</p>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <div
              key={order.id}
              className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 border-l-4 border-l-primary"
            >
              {/* En-tête Carte */}
              <div className="flex justify-between items-start mb-3">
                <div>
                  <span className="text-xs font-bold text-primary bg-primary/10 px-2 py-1 rounded">
                    #{order.orderNumber}
                  </span>
                  <h3 className="font-bold text-lg text-gray-800 mt-1">
                    {order.customerName}
                  </h3>
                </div>
                <div className="text-right">
                  <span className="block text-lg font-bold text-gray-900">
                    {order.total.toLocaleString()} FCFA
                  </span>
                  <span className="text-xs text-gray-400 flex items-center gap-1 justify-end">
                    <Clock className="h-3 w-3" />
                    {new Date(order.createdAt).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                </div>
              </div>

              {/* Infos Client */}
              <div className="space-y-2 mb-4">
                <div className="flex items-start gap-2 text-sm text-gray-600">
                  <MapPin className="h-4 w-4 mt-0.5 text-gray-400 flex-shrink-0" />
                  <p className="leading-tight">{order.customerAddress}</p>
                </div>
                {/* Afficher téléphone uniquement si "Mes Courses" */}
                {activeTab === "my-orders" && order.customerPhone && (
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Phone className="h-4 w-4 text-gray-400" />
                    <p>{order.customerPhone}</p>
                  </div>
                )}
              </div>

              {/* Actions selon l'onglet */}
              {activeTab === "available" ? (
                <button
                  onClick={() => handleClaimOrder(order.id)}
                  className="w-full bg-primary hover:bg-primary/90 text-white py-3 rounded-lg font-bold shadow-sm flex items-center justify-center gap-2"
                >
                  <Package className="h-5 w-5" /> Prendre en charge
                </button>
              ) : (
                <div className="space-y-3">
                  {/* Actions Rapides */}
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      onClick={() => callCustomer(order.customerPhone)}
                      className="flex items-center justify-center gap-2 bg-gray-50 text-gray-700 py-2 rounded-lg text-sm font-medium hover:bg-gray-100"
                    >
                      <Phone className="h-4 w-4" /> Appeler
                    </button>
                    <button
                      onClick={() => openMap(order.customerAddress)}
                      className="flex items-center justify-center gap-2 bg-blue-50 text-blue-700 py-2 rounded-lg text-sm font-medium hover:bg-blue-100"
                    >
                      <Navigation className="h-4 w-4" /> GPS
                    </button>
                  </div>

                  {/* Validation du code */}
                  <div className="pt-3 border-t border-gray-100">
                    <label className="text-xs font-semibold text-gray-500 mb-1 block">
                      Code de Confirmation
                    </label>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder="CONF-XXXX"
                        className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary"
                        value={validationCode[order.id] || ""}
                        onChange={(e) =>
                          handleCodeChange(order.id, e.target.value)
                        }
                      />
                      <button
                        onClick={() => handleCompleteDelivery(order.id)}
                        className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg font-bold shadow-sm flex items-center justify-center gap-2"
                      >
                        <CheckCircle className="h-5 w-5" /> Valider
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default DeliveryDashboard;
