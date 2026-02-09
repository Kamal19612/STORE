# Analyse Fonctionnelle - Migration SUCRE STORE

## 1. Vue d'ensemble

Le projet actuel est une application e-commerce "SUCRE STORE" permettant la vente de produits (principalement adultes/intimes) via un catalogue en ligne.
**Architecture actuelle** : PHP (Backend) + HTML/jQuery (Frontend) + MySQL (Commandes/Admin) + Google Sheets (Catalogue Produits).
**Cible** : Java Spring Boot (Backend) + React (Frontend) + PostgreSQL (Tout, y compris Produits).

## 2. Fonctionnalités Détectées (PHP)

### A. Module Client (Public)

| Fonctionnalité      | Description Actuelle                                               | Endpoint PHP                  |
| ------------------- | ------------------------------------------------------------------ | ----------------------------- |
| **Catalogue**       | Affichage des produits avec pagination (12/page).                  | `index.php`, `getProducts()`  |
| **Filtrage**        | Filtrer par Catégorie et Disponibilité.                            | `index.php` (params GET)      |
| **Détails Produit** | Modal avec image, description, prix, statut.                       | `index.php` (Modal JS)        |
| **Panier**          | Ajout, suppression, modification qté, vidage. Stocké en Session.   | `cart_handler.php`            |
| **Commander**       | Formulaire "Guest" (Nom, Tel, Adresse, GPS). Pas de compte client. | `process_order.php`           |
| **Géolocalisation** | Capture des coordonnées GPS via API Navigateur.                    | `index.php` (JS)              |
| **Notification**    | Génération lien WhatsApp pré-rempli après commande.                | `process_order.php`           |
| **Slider Pub**      | Carrousel d'images promotionnelles en haut de page.                | `index.php` / `slider_images` |

### B. Module Administration (Protégé)

| Fonctionnalité           | Description Actuelle                                                | Endpoint PHP             |
| ------------------------ | ------------------------------------------------------------------- | ------------------------ |
| **Authentification**     | Login administrateur (Session). Pas de "Mot de passe oublié".       | `admin/login.php`        |
| **Tableau de Bord**      | (Supposé) Résumé des ventes/commandes.                              | `admin/index.php`        |
| **Gestion Commandes**    | Liste, Détails, Changement statut, Suppression, Export Excel.       | `admin/orders.php`       |
| **Notification Client**  | Envoi de message WhatsApp manuel selon statut commande.             | `admin/orders.php` (JS)  |
| **Gestion Utilisateurs** | CRUD Administrateurs avec Rôles (Super Admin, Admin, Gestionnaire). | `admin/users.php`        |
| **Gestion Slider**       | Ajout, Modification, Ordre, Activation images.                      | `admin/slider.php`       |
| **Permissions**          | Système de droits granulaires selon le rôle.                        | `admin/config_admin.php` |

## 3. Matrice de Correspondance (Cible Spring Boot + React)

| Fonctionnalité PHP    | API Spring Boot (Proposition)         | Écran React (Proposition)              | Table DB                 |
| --------------------- | ------------------------------------- | -------------------------------------- | ------------------------ |
| **Catalogue**         | `GET /api/products` (Pageable)        | `HomePage`, `ProductList`              | `products`, `categories` |
| **Détails Produit**   | `GET /api/products/{id}`              | `ProductDetailModal` ou Page           | `products`               |
| **Panier**            | (Client-side ou Redis)                | `CartDrawer`, `CartContext`            | -                        |
| **Commande**          | `POST /api/orders`                    | `CheckoutForm`                         | `orders`, `order_items`  |
| **Login Admin**       | `POST /api/auth/login` (JWT)          | `AdminLogin`                           | `users`                  |
| **Gestion Commandes** | `GET/PUT /api/admin/orders`           | `AdminOrdersList`, `AdminOrderDetail`  | `orders`                 |
| **Gestion Produits**  | `POST/PUT/DELETE /api/admin/products` | `AdminProductList`, `AdminProductForm` | `products`               |
| **Gestion Slider**    | `CRUD /api/admin/slides`              | `AdminSliderList`                      | `slider_images`          |

## 4. Améliorations Proposées pour la Migration

### Technique

1.  **Suppression Google Sheets** : Migration des données produits vers PostgreSQL pour performance et intégrité (Relations FK).
2.  **Sécurité** : Passage de Session PHP à **JWT (JSON Web Token)** pour l'API Stateless.
3.  **Frontend** : SPA (Single Page Application) avec React pour une expérience plus fluide (plus de rechargement de page pour le filtrage).

### Fonctionnel

1.  **Gestion des Stocks** : Actuellement géré manuellement ou via Sheet. Implémenter une décrémentation automatique lors de la commande.
2.  **Statuts de Commande** : Mieux définir le workflow (En attente -> En cours -> Livrée -> Annulée).
3.  **Dashboard Analytics** : Ajouter des graphiques de ventes avec Recharts coté Admin.
4.  **Historique Client** : (Optionnel) Permettre de voir l'historique des commandes si on lie par numéro de téléphone.

5.  Spécifications Détaillées (Règles Métier)

Pour garantir une iso-fonctionnalité lors de la migration, voici les règles métier strictes extraites du code PHP (`process_order.php`, `config_admin.php`).

### A. Modèle de Message WhatsApp (Format Exact)

Le backend ou le frontend doit générer le lien WhatsApp avec ce message pré-formaté :

```text
*NOUVELLE COMMANDE SUCRE STORE*

Commande: #{order_id}
Date: {dd/mm/yyyy hh:mm}

*CLIENT*
Nom: {customer_name}
Tel: {customer_phone}
Adresse: {customer_address}
📍 Position GPS: https://www.google.com/maps?q={lat},{long}

*ARTICLES*
- {qty}x {product_name} ({price} FCFA)
- {qty}x {product_name} ({price} FCFA)

*TOTAL: {total_amount} FCFA*

Notes: {customer_notes} (si présent)
```

### B. Matrice des Rôles & Permissions

Le système cible doit implémenter ces rôles (Spring Security) correspondant à `admin/config_admin.php` :

| Permission / Rôle       | `SUPER_ADMIN` | `ADMIN` | `GESTIONNAIRE` |
| ----------------------- | ------------- | ------- | -------------- |
| Voir Dashboard          | ✅            | ✅      | ✅             |
| Voir Commandes          | ✅            | ✅      | ✅             |
| **Modifier** Commandes  | ✅            | ✅      | ✅             |
| **Supprimer** Commandes | ✅            | ❌      | ❌             |
| Voir Utilisateurs       | ✅            | ✅      | ❌             |
| **Gérer** Utilisateurs  | ✅            | ❌      | ❌             |
| Voir Slider             | ✅            | ✅      | ❌             |
| **Gérer** Slider        | ✅            | ✅      | ❌             |

### C. Validation de Commande (Guest Checkout)

Champs obligatoires à valider coté API (`POST /api/orders`) :

1.  `customer_name` : Non vide.
2.  `customer_phone` : Non vide.
3.  `customer_address` : Non vide.
4.  `cart_items` : Au moins 1 article.

- _Note_ : GPS (`lat`, `long`) et Notes (`notes`) sont optionnels.

### D. Statuts de Commande

Codes couleur et icones à reprendre dans le Back-office :

- **PENDING** (`En attente`) : Couleur `warning` (Jaune), Icone `clock`.
- **CONFIRMED** (`En cours`) : Couleur `info` (Bleu), Icone `truck`.
- **DELIVERED** (`Livrée`) : Couleur `success` (Vert), Icone `check-circle`.
- **CANCELLED** (`Annulée`) : Couleur `danger` (Rouge), Icone `times-circle`.
