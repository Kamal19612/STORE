# Architecture Frontend - React

## 1. Technologies & Stack

- **Framework** : React 18+
- **Build Tool** : Vite (Rapide & Léger)
- **Langage** : TypeScript (Recommandé) ou JavaScript.
- **Style** : Tailwind CSS (Pour reproduire le design actuel facilement).
- **Routing** : React Router DOM 6.
- **HTTP Client** : Axios.
- **État Global** : Zustand (Simple & Performant) ou Context API (Panier).
- **Icônes** : Lucide React ou Heroicons.
- **UI Components** : Headless UI (Dropdowns, Modals) ou Shadcn/ui (Basé sur Radix).

## 2. Structure du Projet

```
src
├── assets              # Images, polices, fichiers statiques
├── components
│   ├── common          # Boutons, Inputs, Modals (Réutilisables)
│   ├── layout          # Header, Footer, AdminSidebar
│   ├── product         # ProductCard, ProductGrid
│   └── cart            # CartDrawer, CartItem
├── context             # AuthContext (si nécessaire)
├── hooks               # Custom Hooks (useCart, useAuth)
├── layouts             # Mises en page (PublicLayout, AdminLayout)
├── pages
│   ├── public          # Home, ProductDetail, Checkout, Success
│   └── admin           # Dashboard, Orders, Products, Login
├── services            # Appels API (api.js, authService.js)
├── store               # Zustand Store (cartStore.js)
├── utils               # Formatteurs (Prix), Constants
└── App.jsx             # Routes
```

## 3. Design & UX (Reproduction Fidèle)

L'objectif est de garder l'identité visuelle actuelle tout en modernisant l'UX.

- **Couleurs** :
  - Primaire : `#f5ad41` (Orange/Or actuel)
  - Secondaire : `#242021` (Noir/Gris foncé)
  - Background : `#f9fafb` (Gris très clair)
- **Polices** : Garder la police actuelle ou passer à _Inter_ / _Roboto_.
- **Composants Clés** :
  - **Navbar** : Logo à gauche, Panier à droite (Badge animé).
  - **Slider** : Swiper.js pour le carrousel promo.
  - **Grille Produits** : Cartes avec effet hover, bouton "Ajouter", image centrée.
  - **Modal Détails** : S'ouvre au clic sur l'image (comme l'actuel) ou page dédiée (meilleur pour SEO). _Recommendation : Page dédiée `/product/:slug` avec navigation fluide._
  - **Panier (Drawer)** : S'ouvre sur le côté droit (Slide-over).
  - **Checkout** : Formulaire multi-étapes ou simple page (Guest Checkout).

## 4. Routes (React Router)

### Publique

- `/` : Accueil (Slider + Liste Produits + Filtres).
- `/product/:slug` : Détails Produit.
- `/cart` : Page Panier (optionnelle si Drawer).
- `/checkout` : Finalisation Commande.
- `/order-tracking` : Suivi de commande.

### Admin (Protégé /admin)

- `/admin/login` : Connexion.
- `/admin/dashboard` : Stats.
- `/admin/orders` : Liste Commandes.
- `/admin/orders/:id` : Détail Commande.
- `/admin/products` : Gestion Catalogue (CRUD).
- `/admin/slider` : Gestion Slider.
- `/admin/users` : Gestion Utilisateurs.

## 5. Gestion de l'État (State Management)

### Panier (CartStore - Zustand)

- `items` : Tableau d'objets `{ id, name, price, quantity, image }`.
- `addItem(product)` : Ajoute ou incrémente.
- `removeItem(id)` : Supprime.
- `updateQuantity(id, qty)` : Modifie.
- `clearCart()` : Vide après commande.
- `total` : Calculé dynamiquement.

### Authentification (AuthContext)

- `user` : User connecté (Admin).
- `token` : JWT stocké en `localStorage`.
- `login(credentials)` : Appel API + Stockage.
- `logout()` : Suppression token + Redirection.

## 6. Intégration API (Axios)

Configuration d'une instance Axios avec Intercepteur pour injecter le token.

```javascript
// services/api.js
import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
```

## 7. Spécifications de Design Détaillées (Pixel Perfect)

Pour garantir une reproduction fidèle du design PHP actuel, voici les valeurs exactes extraites de `index.php` et `style.css`.

### 🎨 Palette de Couleurs

Ces variables CSS doivent être définies dans `index.css` (Tailwind config).

| Variable CSS        | Valeur Hex          | Usage                                |
| ------------------- | ------------------- | ------------------------------------ |
| `--primary`         | `#f5ad41`           | Boutons, Badges, Prix, Header Mobile |
| `--primary-dark`    | `#d89a35`           | Hover des éléments primaires         |
| `--secondary`       | `#242021`           | Header, Textes importants, Footer    |
| `--secondary-light` | `#3a3638`           | Variantes du noir                    |
| `Background`        | `#f9fafb` (gray-50) | Fond de page                         |

### 🎭 Animations et Effets

À intégrer dans `tailwind.config.js` (`theme.extend.keyframes`).

```css
/* Carte Produit Hover */
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(242, 33, 33, 0.15); /* Ombre rouge subtile spécifique */
}

/* Badge Panier (Bounce) */
@keyframes bounce {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

/* Toast Notification (Slide In) */
@keyframes slideIn {
  from {
    transform: translateX(400px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* Bouton Panier (Blink) */
@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}
```

### 📱 Comportements Responsive (Mobile vs Desktop)

- **Breakpoint** : `1023px` (lg).
- **Sidebar Filtres** :
  - **Desktop (> 1023px)** : Affichée à gauche (sticky).
  - **Mobile** : Masquée.
- **Barre Catégories Mobile** :
  - **Desktop** : Masquée.
  - **Mobile** : Affichée horizontalement (`overflow-x: auto`), scrollable, avec les boutons "pill".

### 📝 Formulaire de Commande (Checkout)

Le formulaire doit contenir _exactement_ ces champs pour correspondre au backend :

1.  **Nom ou pseudo** (`customer_name`) - _Requis_
2.  **Numéro de téléphone** (`customer_phone`) - _Requis_
3.  **Adresse de livraison** (`customer_address`) - _Requis_ - _TextArea_
4.  **Position GPS** (Bouton "Récupérer ma position") :
    - Doit utiliser `navigator.geolocation.getCurrentPosition`.
    - Remplit les champs cachés `customer_latitude` et `customer_longitude`.
    - Affiche un message de succès vert si trouvé.
5.  **Notes supplémentaires** (`customer_notes`) - _Optionnel_
