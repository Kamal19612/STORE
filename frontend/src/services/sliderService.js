import api from "./api";

const sliderService = {
  // Récupérer toutes les images
  getAll: async () => {
    const response = await api.get("/admin/slider");
    return response.data;
  },

  // Créer une image (FormData)
  create: async (formData) => {
    const response = await api.post("/admin/slider", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  // Mettre à jour (FormData)
  update: async (id, formData) => {
    const response = await api.put(`/admin/slider/${id}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  // Supprimer
  delete: async (id) => {
    await api.delete(`/admin/slider/${id}`);
  },
};

export default sliderService;
