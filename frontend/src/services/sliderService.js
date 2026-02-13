import api from "./api";

const sliderService = {
  // Public: Récupérer les sliders actifs
  getPublicAll: async () => {
    const response = await api.get("/sliders");
    return response.data;
  },

  // Admin: Récupérer tous les sliders
  getAll: async () => {
    const response = await api.get("/admin/sliders");
    return response.data;
  },

  // Admin: Créer un slider
  create: async (formData) => {
    const response = await api.post("/admin/sliders", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data;
  },

  // Admin: Supprimer un slider
  delete: async (id) => {
    await api.delete(`/admin/sliders/${id}`);
  },

  // Admin: Toggle Active
  toggleActive: async (id) => {
    const response = await api.put(`/admin/sliders/${id}/toggle`);
    return response.data;
  },

  // Admin: Update (si besoin, pas implémenté en backend pour l'instant sauf toggle)
  update: async (id, formData) => {
    // Note: Le backend n'a pas de PUT update global pour l'instant, juste toggle.
    // Si AdminSlider appelle update pour toggle, on redirige vers toggle.
    // Si c'est pour autre chose, ça plantera.
    // AdminSlider actuel appelle update pour le toggle.
    return sliderService.toggleActive(id);
  },
};

export default sliderService;
