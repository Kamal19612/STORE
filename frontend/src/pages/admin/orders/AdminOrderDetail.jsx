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
  MessageSquare,
  History,
  Truck,
  Clock,
  CheckCircle,
} from "lucide-react";
import { toast } from "react-toastify";
import adminOrderService from "../../../services/adminOrderService";

const AdminOrderDetail = () => {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [whatsappPhone, setWhatsappPhone] = useState("");
  const [deliveryTime, setDeliveryTime] = useState(null);

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const [orderData, historyData] = await Promise.all([
          adminOrderService.getOrderById(id),
          adminOrderService.getOrderHistory(id),
        ]);
        setOrder(orderData);
        setHistory(historyData);
        setWhatsappPhone(orderData.customerPhone || "");

        // Find delivery time from history if status is DELIVERED
        if (orderData.status === "DELIVERED") {
          const deliveredEvent = historyData.find(
            (h) => h.status === "DELIVERED",
          );
          if (deliveredEvent) {
            setDeliveryTime(deliveredEvent.createdAt);
          } else {
            // Fallback to updatedAt if history doesn't have it (legacy)
            setDeliveryTime(orderData.updatedAt);
          }
        }
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
      // Refresh history to see the new change immediately
      const historyData = await adminOrderService.getOrderHistory(id);
      setHistory(historyData);

      toast.success(`Statut mis à jour : ${newStatus}`);
    } catch (error) {
      toast.error("Erreur lors de la mise à jour");
    } finally {
      setUpdating(false);
    }
  };

  const handleWhatsAppNotify = async () => {
    try {
      const link = await adminOrderService.getWhatsAppNotificationLink(
        id,
        whatsappPhone,
      );
      window.open(link, "_blank");
    } catch (error) {
      toast.error("Erreur génération lien WhatsApp");
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

  const getStatusLabel = (status) => {
    const labels = {
      PENDING: "En attente",
      CONFIRMED: "Confirmée",
      SHIPPED: "En livraison",
      DELIVERED: "Livrée",
      CANCELLED: "Annulée",
    };
    return labels[status] || status;
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleString("fr-FR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  return (
    <div className="w-full p-4 md:p-6 bg-gray-50/50 min-h-screen">
      <div className="max-w-[1600px] mx-auto space-y-6">
        {/* Header - Full Width */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <Link
                to="/admin/orders"
                className="p-2.5 bg-gray-50 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors"
              >
                <ArrowLeft className="h-5 w-5 text-gray-600" />
              </Link>
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl font-bold text-gray-800">
                    Commande #{order.orderNumber}
                  </h1>
                  <span
                    className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide border ${getStatusColor(order.status)}`}
                  >
                    {getStatusLabel(order.status)}
                  </span>
                </div>
                <div className="flex items-center gap-6 mt-2 text-sm text-gray-500 font-medium">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-gray-400" />
                    {formatDate(order.createdAt)}
                  </div>
                  {order.confirmationCode && (
                    <div className="flex items-center gap-2 px-2 py-0.5 bg-gray-100 rounded border border-gray-200">
                      <span className="text-gray-500 text-xs uppercase">
                        Code:
                      </span>
                      <span className="font-mono text-gray-900">
                        {order.confirmationCode}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Delivery Time Badge */}
            {order.status === "DELIVERED" && deliveryTime && (
              <div className="bg-green-50 border border-green-200 rounded-lg px-4 py-2 flex items-center gap-3">
                <div className="bg-green-100 p-2 rounded-full">
                  <CheckCircle className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-[10px] text-green-600 font-bold uppercase tracking-wider">
                    Livrée le
                  </p>
                  <p className="text-sm font-bold text-green-800">
                    {formatDate(deliveryTime)}
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Main Layout - Sticky Sidebar */}
        <div className="flex flex-col lg:flex-row gap-6 items-start">
          {/* Left Column: Order Items & Notes (Takes remaining space) */}
          <div className="flex-1 w-full space-y-6">
            {/* Items Card */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-gray-200 bg-gray-50/50 flex justify-between items-center">
                <h3 className="font-bold text-gray-800 flex items-center gap-2">
                  <Package className="h-5 w-5 text-primary" />
                  Contenu de la commande
                </h3>
                <span className="bg-white border border-gray-200 text-gray-700 text-xs font-bold px-3 py-1 rounded-full shadow-sm">
                  {order.items.length} article(s)
                </span>
              </div>

              <div className="divide-y divide-gray-100">
                {order.items.map((item) => (
                  <div
                    key={item.id}
                    className="p-5 flex flex-col sm:flex-row sm:items-center gap-4 hover:bg-gray-50 transition-colors"
                  >
                    <div className="h-16 w-16 bg-white rounded-lg border border-gray-200 p-2 shrink-0 flex items-center justify-center">
                      <img
                        src={item.product?.mainImage || "/placeholder.png"}
                        alt={item.product?.name}
                        className="max-h-full max-w-full object-contain"
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h4 className="font-semibold text-gray-900 text-base truncate">
                        {item.product?.name}
                      </h4>
                      <p className="text-sm text-gray-500 mt-0.5">
                        PU: {item.unitPrice.toLocaleString()} FCFA
                      </p>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="flex items-center justify-end gap-2 mb-1">
                        <span className="text-xs font-bold text-gray-500 bg-gray-100 px-2 py-1 rounded">
                          x{item.quantity}
                        </span>
                      </div>
                      <p className="font-bold text-gray-900 text-lg">
                        {item.totalPrice.toLocaleString()}{" "}
                        <span className="text-xs font-normal text-gray-500">
                          FCFA
                        </span>
                      </p>
                    </div>
                  </div>
                ))}
              </div>

              <div className="bg-gray-50 px-6 py-5 border-t border-gray-200">
                <div className="flex justify-between items-center">
                  <span className="text-gray-600 font-medium">
                    Total Global
                  </span>
                  <span className="text-2xl font-extrabold text-primary tracking-tight">
                    {order.total.toLocaleString()} FCFA
                  </span>
                </div>
              </div>
            </div>

            {/* Note Client */}
            {order.customerNotes && (
              <div className="bg-amber-50 rounded-xl border border-amber-200 p-6 relative overflow-hidden">
                <div className="absolute top-0 right-0 p-4 opacity-10">
                  <MessageSquare className="w-24 h-24 text-amber-900" />
                </div>
                <h4 className="flex items-center gap-2 font-bold text-amber-900 mb-3 relative z-10">
                  <MessageSquare className="h-5 w-5" />
                  Message du client
                </h4>
                <p className="text-amber-900/80 italic text-base leading-relaxed relative z-10 bg-white/50 p-4 rounded-lg border border-amber-100">
                  "{order.customerNotes}"
                </p>
              </div>
            )}
          </div>

          {/* Right Column: Static Sidebar (Does not follow scroll) */}
          {/* w-[360px] fixed width, standard static positioning */}
          <div className="w-full lg:w-[360px] shrink-0 space-y-6">
            {/* 1. Actions Rapides */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
              <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-gray-400" /> Actions
              </h3>
              <div className="space-y-2">
                {["PENDING", "CONFIRMED", "CANCELLED"]
                  .filter((s) => s !== order.status)
                  .map((status) => (
                    <button
                      key={status}
                      onClick={() => handleStatusChange(status)}
                      disabled={updating}
                      className={`w-full px-4 py-3 text-sm font-semibold rounded-lg border transition-all flex items-center justify-between group ${
                        status === "CANCELLED"
                          ? "border-red-100 bg-white text-red-600 hover:bg-red-50 hover:border-red-200"
                          : "border-gray-100 bg-white text-gray-700 hover:bg-gray-50 hover:border-gray-300 hover:text-gray-900"
                      }`}
                    >
                      <span>Passer en {getStatusLabel(status)}</span>
                      <ArrowLeft className="w-4 h-4 rotate-180 opacity-0 group-hover:opacity-100 transition-all transform group-hover:translate-x-1" />
                    </button>
                  ))}
              </div>
            </div>

            {/* 2. Client Info */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-gray-800 flex items-center gap-2">
                  <User className="w-5 h-5 text-blue-500" />
                  Client
                </h3>
                <a
                  href={`tel:${order.customerPhone}`}
                  className="p-2 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors"
                >
                  <Phone className="w-4 h-4" />
                </a>
              </div>

              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center text-gray-500 font-bold text-lg">
                    {order.customerName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p className="font-bold text-gray-900">
                      {order.customerName}
                    </p>
                    <p className="text-sm text-gray-500">
                      {order.customerPhone}
                    </p>
                  </div>
                </div>

                <div className="p-3 bg-gray-50 rounded-lg border border-gray-100">
                  <div className="flex items-start gap-2">
                    <MapPin className="w-4 h-4 text-gray-400 mt-0.5 shrink-0" />
                    <p className="text-sm text-gray-600 font-medium leading-tight">
                      {order.customerAddress}
                    </p>
                  </div>
                  {order.customerLatitude && (
                    <a
                      href={`https://www.google.com/maps?q=${order.customerLatitude},${order.customerLongitude}`}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-3 w-full flex items-center justify-center gap-2 py-2 bg-white border border-gray-200 rounded text-xs font-bold text-gray-600 hover:bg-gray-50 hover:text-gray-900 transition-colors"
                    >
                      <ExternalLink className="w-3 h-3" />
                      Voir sur la carte
                    </a>
                  )}
                </div>

                {/* WhatsApp Quick Action */}
                <div>
                  <div className="relative">
                    <input
                      type="text"
                      value={whatsappPhone}
                      onChange={(e) => setWhatsappPhone(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-green-500/20 focus:border-green-500"
                      placeholder="Numéro WhatsApp"
                    />
                    <MessageSquare className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
                  </div>
                  <button
                    onClick={handleWhatsAppNotify}
                    disabled={order.status !== "CONFIRMED"}
                    className={`mt-2 w-full py-2 rounded-lg text-xs font-bold uppercase tracking-wide transition-colors ${
                      order.status === "CONFIRMED"
                        ? "bg-[#25D366] text-white hover:brightness-110 shadow-sm shadow-green-500/30"
                        : "bg-gray-100 text-gray-400 cursor-not-allowed"
                    }`}
                  >
                    Envoyer notif WhatsApp
                  </button>
                </div>
              </div>
            </div>

            {/* 3. Delivery Info */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
              <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2">
                <Truck className="w-5 h-5 text-purple-500" />
                Livraison
              </h3>

              {order.deliveryAgent ? (
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
                      {order.deliveryAgent.username.charAt(0)}
                    </div>
                    <div>
                      <p className="font-bold text-gray-900 text-sm">
                        {order.deliveryAgent.username}
                      </p>
                      <p className="text-xs text-purple-600 bg-purple-50 px-1.5 py-0.5 rounded inline-block">
                        Agent #{order.deliveryAgent.id}
                      </p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <a
                      href={`mailto:${order.deliveryAgent.email}`}
                      className="flex flex-col items-center justify-center p-2 bg-gray-50 rounded hover:bg-gray-100 transition-colors border border-transparent hover:border-gray-200"
                    >
                      <User className="w-4 h-4 text-gray-400 mb-1" />
                      <span className="text-[10px] text-gray-500 font-medium">
                        Email
                      </span>
                    </a>
                    {order.deliveryAgent.phone ? (
                      <a
                        href={`tel:${order.deliveryAgent.phone}`}
                        className="flex flex-col items-center justify-center p-2 bg-purple-50 rounded hover:bg-purple-100 transition-colors border border-purple-100"
                      >
                        <Phone className="w-4 h-4 text-purple-500 mb-1" />
                        <span className="text-[10px] text-purple-700 font-bold">
                          {order.deliveryAgent.phone}
                        </span>
                      </a>
                    ) : (
                      <div className="flex flex-col items-center justify-center p-2 bg-gray-50 rounded border border-dashed border-gray-200 opacity-50">
                        <Phone className="w-4 h-4 text-gray-400 mb-1" />
                        <span className="text-[10px] text-gray-400">N/A</span>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="text-center py-6 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                  <Truck className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                  <p className="text-sm text-gray-500 font-medium">
                    Non assigné
                  </p>
                </div>
              )}
            </div>

            {/* 4. History (Compact) */}
            {history.length > 0 && (
              <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
                <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2">
                  <History className="w-5 h-5 text-gray-500" />
                  Historique
                </h3>
                <div className="relative pl-2 border-l-2 border-gray-100 space-y-6">
                  {history.slice(0, 5).map((event, idx) => (
                    <div key={idx} className="relative pl-4">
                      <div
                        className={`absolute -left-[5px] top-1.5 w-2 h-2 rounded-full border border-white shadow-sm ${
                          event.status === "DELIVERED"
                            ? "bg-green-500"
                            : event.status === "CANCELLED"
                              ? "bg-red-500"
                              : idx === 0
                                ? "bg-blue-500"
                                : "bg-gray-300"
                        }`}
                      ></div>
                      <div>
                        <p className="text-xs font-bold text-gray-800 uppercase tracking-wide">
                          {getStatusLabel(event.status)}
                        </p>
                        <p className="text-[10px] text-gray-500 font-medium mt-0.5">
                          {formatDate(event.createdAt)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminOrderDetail;
