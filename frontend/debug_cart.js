// DEBUG SCRIPT - À exécuter dans la console du navigateur

console.log("=== DEBUG CART STORE ===");

// 1. Vérifier le localStorage
const cartData = localStorage.getItem("sucre-store-cart");
console.log("Raw localStorage data:", cartData);

if (cartData) {
  const parsed = JSON.parse(cartData);
  console.log("Parsed cart data:", parsed);
  console.log("Items in cart:", parsed.state?.items);

  // 2. Vérifier la structure de chaque item
  if (parsed.state?.items) {
    parsed.state.items.forEach((item, index) => {
      console.log(`Item ${index}:`, {
        id: item.id,
        name: item.name,
        price: item.price,
        quantity: item.quantity,
        priceType: typeof item.price,
        hasPrice: item.hasOwnProperty("price"),
      });
    });
  }

  // 3. Calculer manuellement le total
  const manualTotal = parsed.state?.items?.reduce((sum, item) => {
    console.log(
      `Adding: ${sum} + (${item.price} * ${item.quantity}) = ${sum + (item.price || 0) * item.quantity}`,
    );
    return sum + (item.price || 0) * item.quantity;
  }, 0);
  console.log("Manual total calculation:", manualTotal);
}

// 4. Vérifier le getter total via Zustand
console.log("=== END DEBUG ===");
