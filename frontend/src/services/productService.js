import api from "./api";

const productService = {
  getAllProducts: async (page = 0, size = 10) => {
    const response = await api.get(`/products?page=${page}&size=${size}`);
    return response.data;
  },

  getProductBySlug: async (slug) => {
    const response = await api.get(`/products/${slug}`);
    return response.data;
  },

  getCategories: async () => {
    const response = await api.get("/categories");
    return response.data;
  },
};

export default productService;
