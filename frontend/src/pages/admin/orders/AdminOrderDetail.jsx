import { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import {
  ArrowLeft,
  MapPin,
  Phone,
  User,
  Calendar,
  DollarSign,
  Package,
  ExternalLink,
} from "lucide-react";
import { toast } from "react-toastify";
import adminOrderService from "../../../services/adminOrderService";

const AdminOrderDetail = () => {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const data = await adminOrderService.getOrderById(id);
        setOrder(data);
      } catch (error) {
        console.error(error);
        toast.error("Erreur chargement commande");
      } finally {
        setLoading(false);
      }
    };
    fetchOrder();
  }, [id]);

  const handleStatusChange = async (newStatus) => {
    if (!window.confirm(`Changer le statut en ${newStatus} ?`)) return;

    setUpdating(true);
    try {
      const updatedOrder = await adminOrderService.updateOrderStatus(
        id,
        newStatus,
      );
      setOrder(updatedOrder);
      toast.success(`Statut mis à jour : ${newStatus}`);
    } catch (error) {
      toast.error("Erreur lors de la mise à jour");
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return <div className="p-10 text-center">Chargement...</div>;
  if (!order)
    return (
      <div className="p-10 text-center text-red-500">Commande introuvable</div>
    );

  const getStatusColor = (status) => {
    switch (status) {
      case "PENDING":
        return "bg-yellow-100 text-yellow-800 border-yellow-200";
      case "CONFIRMED":
        return "bg-blue-100 text-blue-800 border-blue-200";
      case "SHIPPED":
        return "bg-purple-100 text-purple-800 border-purple-200";
      case "DELIVERED":
        return "bg-green-100 text-green-800 border-green-200";
      case "CANCELLED":
        return "bg-red-100 text-red-800 border-red-200";
      default:
        return "bg-gray-100 text-gray-800 border-gray-200";
    }
  };

  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="flex items-center gap-4 mb-8">
        <Link
          to="/admin/orders"
          className="p-2 bg-white rounded-full border border-gray-200 hover:bg-gray-50 transition-colors"
        >
          <ArrowLeft className="h-5 w-5 text-gray-500" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Détail Commande #{order.orderNumber}
          </h1>
          <p className="text-gray-500">
            Passée le {new Date(order.createdAt).toLocaleString("fr-FR")}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Colonne Gauche : Détails */}
        <div className="lg:col-span-2 space-y-6">
          {/* Liste des articles */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-100 bg-gray-50">
              <h3 className="font-bold text-gray-700 flex items-center gap-2">
                <Package className="h-5 w-5 text-primary" /> Articles (
                {order.items.length})
              </h3>
            </div>
            <div className="divide-y divide-gray-100">
              {order.items.map((item) => (
                <div
                  key={item.id}
                  className="p-6 flex items-center justify-between"
                >
                  <div className="flex items-center gap-4">
                    <div className="h-16 w-16 bg-gray-50 rounded-lg border border-gray-200 p-1">
                      <img
                        src={item.product?.mainImage || "/placeholder.png"}
                        alt={item.product?.name}
                        className="h-full w-full object-contain"
                      />
                    </div>
                    <div>
                      <h4 className="font-bold text-gray-800">
                        {item.product?.name}
                      </h4>
                      <p className="text-sm text-gray-500">
                        Qté: {item.quantity}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-bold text-gray-900">
                      {item.totalPrice.toLocaleString()} FCFA
                    </div>
                    <div className="text-xs text-gray-400">
                      {item.unitPrice.toLocaleString()} / unité
                    </div>
                  </div>
                </div>
              ))}
            </div>
            <div className="bg-gray-50 px-6 py-4 border-t border-gray-100 flex justify-between items-center">
              <span className="font-bold text-gray-700">Total Commande</span>
              <span className="font-extrabold text-xl text-primary">
                {order.total.toLocaleString()} FCFA
              </span>
            </div>
          </div>

          {/* Notes Client */}
          {order.customerNotes && (
            <div className="bg-yellow-50 rounded-xl p-6 border border-yellow-100 text-yellow-800">
              <h4 className="font-bold mb-2">Note du client :</h4>
              <p className="italic">"{order.customerNotes}"</p>
            </div>
          )}
        </div>

        {/* Colonne Droite : Infos & Actions */}
        <div className="space-y-6">
          {/* Carte État & Actions */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="font-bold text-gray-700 mb-4">Statut</h3>
            <div
              className={`p-3 rounded-lg border text-center font-bold mb-6 ${getStatusColor(order.status)}`}
            >
              {order.status}
            </div>

            <label className="block text-sm font-medium text-gray-700 mb-2">
              Modifier le statut :
            </label>
            <div className="grid grid-cols-1 gap-2">
              {["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"]
                .filter((s) => s !== order.status)
                .map((status) => (
                  <button
                    key={status}
                    onClick={() => handleStatusChange(status)}
                    disabled={updating}
                    className="px-4 py-2 text-sm border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left font-medium text-gray-600"
                  >
                    Passer à {status}
                  </button>
                ))}
            </div>
          </div>

          {/* Carte Client */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 space-y-4">
            <h3 className="font-bold text-gray-700 border-b border-gray-100 pb-2">
              Client
            </h3>

            <div className="flex items-start gap-3">
              <User className="h-5 w-5 text-gray-400 mt-0.5" />
              <div>
                <p className="font-medium text-gray-900">
                  {order.customerName}
                </p>
                <p className="text-sm text-gray-500">Client invité</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Phone className="h-5 w-5 text-gray-400 mt-0.5" />
              <div>
                <p className="font-medium text-gray-900">
                  {order.customerPhone}
                </p>
                <a
                  href={`tel:${order.customerPhone}`}
                  className="text-xs text-blue-600 hover:underline"
                >
                  Appeler
                </a>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <MapPin className="h-5 w-5 text-gray-400 mt-0.5" />
              <div>
                <p className="font-medium text-gray-900">
                  Adresse de livraison
                </p>
                <p className="text-sm text-gray-600 mt-1">
                  {order.customerAddress}
                </p>
                {order.customerLatitude && order.customerLongitude && (
                  <a
                    href={`https://www.google.com/maps?q=${order.customerLatitude},${order.customerLongitude}`}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1 text-xs text-blue-600 hover:underline mt-2"
                  >
                    <ExternalLink className="h-3 w-3" /> Voir sur Maps
                  </a>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminOrderDetail;
