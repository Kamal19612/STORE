import api from "./api";

/**
 * Charge tout le catalogue public (toutes les pages Spring), y compris archivés / rupture
 * (tri id DESC : une seule page ne suffit pas si le catalogue dépasse la taille demandée).
 */
async function fetchFullProductCatalog(pageSize = 100) {
  let page = 0;
  const all = [];
  const maxPages = 200;
  while (page < maxPages) {
    const response = await api.get(`/products?page=${page}&size=${pageSize}`);
    const chunk = response.data?.content ?? [];
    if (chunk.length === 0) break;
    all.push(...chunk);
    // Ne pas s’appuyer sur totalElements (souvent absent / incohérent derrière un proxy) :
    // avec tri id DESC, les fiches inactives ou anciennes peuvent être sur les pages suivantes.
    if (chunk.length < pageSize) break;
    page += 1;
  }
  return { content: all, totalElements: all.length };
}

const productService = {
  getAllProducts: async (page = 0, size = 10) => {
    const response = await api.get(`/products?page=${page}&size=${size}`);
    return response.data;
  },

  getFullProductCatalog: fetchFullProductCatalog,

  // Compat: certains écrans utilisent "id" mais l'API attend un slug
  getProductById: async (idOrSlug) => {
    return productService.getProductBySlug(idOrSlug);
  },

  getProductBySlug: async (slug) => {
    const response = await api.get(`/products/${slug}`);
    return response.data;
  },

  getTopProducts: async (limit = 10) => {
    const response = await api.get(`/products/top?limit=${limit}`);
    return response.data;
  },

  getCategories: async () => {
    const response = await api.get("/categories");
    return response.data;
  },
};

export default productService;
