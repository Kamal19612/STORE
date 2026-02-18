import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, Search, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import adminOrderService from "../../../services/adminOrderService";

const AdminOrderList = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const navigate = useNavigate();

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const data = await adminOrderService.getAllOrders(page);

      // Extraction sécurisée des données
      let ordersArray = [];
      let pages = 1;

      if (Array.isArray(data)) {
        // Réponse directe en tableau
        ordersArray = data;
      } else if (data && typeof data === "object") {
        // Réponse paginée Spring Boot
        ordersArray = data.content || [];
        pages = data.totalPages || 1;
      }

      setOrders(ordersArray);
      setTotalPages(pages);
    } catch (error) {
      console.error("Erreur chargement commandes:", error);
      toast.error(
        `Impossible de charger les commandes: ${error.response?.data?.message || error.message}`,
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const handleDelete = async (id) => {
    if (
      window.confirm(
        "Êtes-vous sûr de vouloir supprimer cette commande définitivement ?",
      )
    ) {
      try {
        await adminOrderService.deleteOrder(id);
        toast.success("Commande supprimée");
        fetchOrders();
      } catch (error) {
        toast.error("Erreur suppression (Permission insuffisante ?)");
      }
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      case "CONFIRMED":
        return "bg-blue-100 text-blue-800";
      case "SHIPPED":
        return "bg-purple-100 text-purple-800";
      case "DELIVERED":
        return "bg-green-100 text-green-800";
      case "CANCELLED":
        return "bg-red-100 text-red-800";
      default:
        return "bg-gray-100 text-gray-800";
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
    return new Date(dateString).toLocaleString("fr-FR");
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Gestion des Commandes
          </h1>
          <p className="text-gray-500">Suivi des achats clients</p>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  N° Commande
                </th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Date
                </th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Client
                </th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Code
                </th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Total
                </th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                  Statut
                </th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr>
                  <td
                    colSpan="6"
                    className="px-6 py-10 text-center text-gray-500"
                  >
                    Chargement...
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td
                    colSpan="6"
                    className="px-6 py-10 text-center text-gray-500"
                  >
                    Aucune commande trouvée.
                  </td>
                </tr>
              ) : (
                orders.map((order) => (
                  <tr
                    key={order.id}
                    className="hover:bg-gray-50 transition-colors"
                  >
                    <td className="px-6 py-4 font-medium text-gray-900">
                      #{order.orderNumber}
                    </td>
                    <td className="px-6 py-4 text-gray-600 text-sm">
                      {formatDate(order.createdAt)}
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm font-medium text-gray-900">
                        {order.customerName}
                      </div>
                      <div className="text-xs text-gray-500">
                        {order.customerPhone}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {order.confirmationCode ? (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-mono font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                          {order.confirmationCode}
                        </span>
                      ) : (
                        <span className="text-xs text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4 font-bold text-primary">
                      {order.total.toLocaleString()} FCFA
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(order.status)}`}
                      >
                        {getStatusLabel(order.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/admin/orders/${order.id}`}
                          className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          title="Voir Détails"
                        >
                          <Eye className="h-5 w-5" />
                        </Link>
                        {/* Only Super Admin normally, handled by backend error if tried */}
                        <button
                          onClick={() => handleDelete(order.id)}
                          className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Supprimer"
                        >
                          <Trash2 className="h-5 w-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination (Simplifiée) */}
        <div className="bg-gray-50 px-6 py-4 border-t border-gray-200 flex items-center justify-between">
          <span className="text-sm text-gray-500">
            Page {page + 1} / {totalPages || 1}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-white disabled:opacity-50"
            >
              Précédent
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= totalPages - 1}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-white disabled:opacity-50"
            >
              Suivant
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminOrderList;
