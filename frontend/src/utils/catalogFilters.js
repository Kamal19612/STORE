/**
 * Filtres catalogue public — même logique que {@code sucre-store/functions.php}
 * {@code getProducts($category, $availableOnly)} :
 * - toutes les fiches passent dans la grille par défaut ;
 * - une ligne n’est exclue pour « disponibilité » que si {@code $availableOnly === true}
 *   (équivalent PHP : {@code if ($availableOnly === true && !$product['disponible']) continue;}).
 *
 * L’équivalent de {@code disponible} côté API Spring est surtout {@code purchaseAllowed}
 * (colonne DISPONIBILITÉ) ; en secours on utilise {@code available} si {@code purchaseAllowed} est absent.
 */

export function normalizeCategoryLabel(value) {
  return String(value ?? "").trim().toLowerCase();
}

/**
 * @param {Array<object>} products
 * @param {string} selectedCategoryLabel ex. "Tous", "Chocolats"
 */
export function filterProductsByCategory(products, selectedCategoryLabel) {
  const sel = normalizeCategoryLabel(selectedCategoryLabel);
  if (!sel || sel === "tous") {
    return products.slice();
  }
  return products.filter((p) => {
    const pCat = normalizeCategoryLabel(p.categoryName);
    return pCat.length > 0 && pCat === sel;
  });
}

/**
 * @param {object} product élément {@code ProductResponse}
 * @returns {boolean} équivalent PHP {@code $product['disponible']}
 */
export function isDisponibleLikePhp(product) {
  if (product != null && typeof product.purchaseAllowed === "boolean") {
    return product.purchaseAllowed;
  }
  if (product != null && typeof product.available === "boolean") {
    return product.available;
  }
  return true;
}

/**
 * @param {Array<object>} products
 * @param {boolean} availableOnly même sens que {@code isset($_GET['available']) && $_GET['available'] === '1'}
 */
export function filterProductsAvailableOnly(products, availableOnly) {
  if (!availableOnly) {
    return products.slice();
  }
  return products.filter(isDisponibleLikePhp);
}

/**
 * Chaîne catégorie puis option « disponibles uniquement » (ordre PHP).
 */
export function buildCatalogGridProducts(products, selectedCategoryLabel, availableOnly) {
  let list = filterProductsByCategory(products, selectedCategoryLabel);
  list = filterProductsAvailableOnly(list, availableOnly);
  return list;
}
