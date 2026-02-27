import api from "./api";

const adminUserService = {
  // Récupérer tous les utilisateurs
  getAllUsers: async () => {
    const response = await api.get("/admin/users");
    return response.data;
  },

  // Créer un utilisateur
  createUser: async (user) => {
    const response = await api.post("/admin/users", user);
    return response.data;
  },

  // Mettre à jour un utilisateur
  updateUser: async (id, user) => {
    const response = await api.put(`/admin/users/${id}`, user);
    return response.data;
  },

  // Supprimer un utilisateur
  deleteUser: async (id) => {
    await api.delete(`/admin/users/${id}`);
  },

  // Update Role Only (Legacy support or quick action)
  updateRole: async (id, role) => {
    const response = await api.put(`/admin/users/${id}/role`, { role });
    return response.data;
  },
};

export default adminUserService;
