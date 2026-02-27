import api from "./api";

const adminProductService = {
  getAllProducts: async (page = 0, size = 10, search = "") => {
    const params = new URLSearchParams({
      page,
      size,
      search,
    });
    const response = await api.get(`/admin/products?${params.toString()}`);
    return response.data;
  },

  createProduct: async (productData) => {
    // Gestion du multipart pour l'image si c'est un fichier
    const formData = new FormData();

    // Séparation des données JSON et du fichier
    const { imageFile, ...jsonPayload } = productData;

    formData.append("product", JSON.stringify(jsonPayload));

    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await api.post("/admin/products", formData);
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const formData = new FormData();
    const { imageFile, ...jsonPayload } = productData;

    formData.append("product", JSON.stringify(jsonPayload));

    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await api.put(`/admin/products/${id}`, formData);
    return response.data;
  },

  deleteProduct: async (id) => {
    await api.delete(`/admin/products/${id}`);
  },

  getProductById: async (id) => {
    const response = await api.get(`/admin/products/${id}`);
    return response.data;
  },

  importProducts: async (file) => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await api.post("/admin/products/import", formData);
    return response.data;
  },

  importFromGoogleSheets: async (spreadsheetId) => {
    let url = "/admin/products/import-google-sheets";
    if (spreadsheetId) {
      url += `?spreadsheetId=${spreadsheetId}`;
    }
    const response = await api.post(url);
    return response.data;
  },
};

export default adminProductService;
