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
    const { imageFile, mainImageFile, secondaryImageFiles, ...jsonPayload } = productData;

    formData.append("product", JSON.stringify(jsonPayload));

    // Compat: ancien champ "image" + nouveau "mainImage"
    const effectiveMain = mainImageFile || imageFile;
    if (effectiveMain) formData.append("mainImage", effectiveMain);

    if (Array.isArray(secondaryImageFiles)) {
      secondaryImageFiles.forEach((file) => {
        if (file) formData.append("secondaryImages", file);
      });
    }

    const response = await api.post("/admin/products", formData);
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const formData = new FormData();
    const { imageFile, mainImageFile, secondaryImageFiles, ...jsonPayload } = productData;

    formData.append("product", JSON.stringify(jsonPayload));

    const effectiveMain = mainImageFile || imageFile;
    if (effectiveMain) formData.append("mainImage", effectiveMain);

    if (Array.isArray(secondaryImageFiles)) {
      secondaryImageFiles.forEach((file) => {
        if (file) formData.append("secondaryImages", file);
      });
    }

    const response = await api.put(`/admin/products/${id}`, formData);
    return response.data;
  },

  deleteProduct: async (id) => {
    await api.delete(`/admin/products/${id}`);
  },

  deleteAllProducts: async () => {
    const response = await api.delete("/admin/products");
    return response.data;
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

  importFromGoogleSheets: async (spreadsheetId, sheetGid) => {
    const params = new URLSearchParams();
    if (spreadsheetId) params.set("spreadsheetId", spreadsheetId);
    if (sheetGid != null && sheetGid !== "") params.set("sheetGid", String(sheetGid));
    const q = params.toString();
    const url = `/admin/products/import-google-sheets${q ? `?${q}` : ""}`;
    const response = await api.post(url);
    return response.data;
  },

  syncGoogleSheet: async () => {
    // Utilise l'endpoint synchronisé (sans ID, prend celui en DB)
    const response = await api.post("/admin/products/google-sheets-sync");
    return response.data;
  },

  getSheetConfig: async () => {
    const response = await api.get("/admin/products/sheet-config");
    return response.data;
  },
};

export default adminProductService;
