import { useState, useEffect } from "react";
import {
  MapPin,
  Phone,
  CheckCircle,
  XCircle,
  Navigation,
  Clock,
} from "lucide-react";
import { toast } from "react-toastify";
import api from "../../services/api"; // Utilisation directe de l'instance axios pour l'instant

const DeliveryDashboard = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchOrders = async () => {
    try {
      // Appel au endpoint créé dans DeliveryController
      const response = await api.get("/delivery/orders");
      setOrders(response.data.content || []);
    } catch (error) {
      console.error(error);
      toast.error("Erreur chargement des livraisons");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const updateStatus = async (id, status) => {
    if (!window.confirm(`Confirmer le statut : ${status} ?`)) return;

    try {
      await api.put(`/delivery/orders/${id}/status`, { status });
      toast.success("Statut mis à jour !");
      fetchOrders(); // Rafraîchir la liste
    } catch (error) {
      toast.error("Erreur mise à jour");
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

  if (loading) {
    return (
      <div className="p-10 text-center text-gray-500">
        Chargement des courses... 🛵
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-gray-400">
        <CheckCircle className="h-16 w-16 mb-4 text-green-100" />
        <p className="text-lg font-medium text-gray-600">
          Aucune livraison en attente
        </p>
        <p className="text-sm">Bonne pause ! ☕</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="font-bold text-gray-700 mb-2">
        À Livrer ({orders.length})
      </h2>

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
            {order.customerPhone && (
              <div className="flex items-center gap-2 text-sm text-gray-600">
                <Phone className="h-4 w-4 text-gray-400" />
                <p>{order.customerPhone}</p>
              </div>
            )}
          </div>

          {/* Actions Rapides */}
          <div className="grid grid-cols-2 gap-3 mb-4">
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

          {/* Actions Statut */}
          <div className="pt-3 border-t border-gray-100 flex gap-3">
            <button
              onClick={() => updateStatus(order.id, "DELIVERED")}
              className="flex-1 bg-green-500 hover:bg-green-600 text-white py-3 rounded-lg font-bold shadow-sm flex items-center justify-center gap-2"
            >
              <CheckCircle className="h-5 w-5" /> Livré
            </button>
            <button
              onClick={() => updateStatus(order.id, "CANCELLED")}
              className="px-4 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg"
              title="Annuler / Échoué"
            >
              <XCircle className="h-6 w-6" />
            </button>
          </div>
        </div>
      ))}
    </div>
  );
};

export default DeliveryDashboard;
