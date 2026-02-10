import api from "./api";

const adminOrderService = {
  // Récupérer toutes les commandes (paginé)
  getAllOrders: async (page = 0, size = 10) => {
    // Le backend AdminOrderController utilise Pageable
    const response = await api.get(
      `/admin/orders?page=${page}&size=${size}&sort=createdAt,desc`,
    );
    return response.data;
  },

  // Récupérer une commande par ID
  getOrderById: async (id) => {
    const response = await api.get(`/admin/orders/${id}`);
    return response.data;
  },

  // Mettre à jour le statut
  // Backend attend: PUT /api/admin/orders/{id}/status avec Body { "status": "VALUE" }
  updateOrderStatus: async (id, status) => {
    const response = await api.put(`/admin/orders/${id}/status`, { status });
    return response.data;
  },

  // Supprimer une commande
  deleteOrder: async (id) => {
    await api.delete(`/admin/orders/${id}`);
  },
};

export default adminOrderService;
