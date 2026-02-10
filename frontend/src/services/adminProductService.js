import api from "./api";

const adminProductService = {
  // Récupérer tous les produits (actifs et inactifs) pour l'admin
  // Note: Le backend actuel n'a pas forcément de endpoint "getAll" admin spécifique non-paginé ou paginé qui renvoie tout.
  // Regardons AdminProductController... il n'a pas de méthode GET list !
  // Il faut utiliser le ProductController public /api/products ?
  // Mais /api/products ne renvoie que les actifs.
  //
  // Attends, je dois vérifier le backend AdminProductController.
  // Il a POST, PUT, DELETE.
  // Il n'a PAS de méthode pour lister TOUS les produits (y compris inactifs/archivés).
  //
  // VERIFICATION REQUISE :
  // ProductService.java :
  // getAllActiveProducts -> repository.findByActiveTrue
  //
  // Je dois probablement ajouter une méthode GET /api/admin/products dans AdminProductController
  // pour lister TOUS les produits (pagination).
  //
  // Pour l'instant, je vais créer ce service en supposant que je vais devoir mettre à jour le backend aussi
  // OU utiliser le endpoint public si je ne peux pas modifier le backend maintenant (mais le user veut gérer la disponibilité).
  //
  // Le user a dit : "est ce que ya une fonction permettant à l'admin...".
  // J'ai répondu "OUI" pour Create/Update.
  // Mais pour LISTER pour modifier, il faut les voir.
  //
  // Voyons ProductController.java :
  // @GetMapping("/products") -> productService.getAllActiveProducts(pageable)
  //
  // Il manque un endpoint Admin pour lister TOUT.
  // Je vais devoir l'ajouter au Backend AdminProductController.

  getAllProducts: async (page = 0, size = 10, search = "") => {
    // Ce endpoint sera à créer dans AdminProductController
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

    const response = await api.post("/admin/products", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const formData = new FormData();
    const { imageFile, ...jsonPayload } = productData;

    formData.append("product", JSON.stringify(jsonPayload));

    if (imageFile) {
      formData.append("image", imageFile);
    }

    const response = await api.put(`/admin/products/${id}`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    return response.data;
  },

  deleteProduct: async (id) => {
    await api.delete(`/admin/products/${id}`);
  },

  getProductById: async (id) => {
    // Besoin d'un endpoint pour récupérer un produit par ID (et non slug) pour l'édition,
    // ou utiliser le slug si l'admin liste affiche le slug.
    // AdminProductController n'a pas de GET /:id non plus.
    // ProductController a GET /products/:slug.
    // Utilisions le slug pour l'instant si possible, sinon on ajoutera GET /:id.
    // UPDATE: AdminProductController manque de méthodes de lecture.
    // Je vais devoir les ajouter pour faire une gestion complète.
    const response = await api.get(`/admin/products/${id}`);
    return response.data;
  },
};

export default adminProductService;
