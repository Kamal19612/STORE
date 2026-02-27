# Analyse de la Structure de Données (PostgreSQL)

## 1. Diagramme Logique des Données (Texte)

### Entités Principales

- **USERS** : Administrateurs et gestionnaires de l'application.
- **CATEGORIES** : Types de produits (ex: Aphrodisiaques, Lingerie).
- **PRODUCTS** : Catalogue des articles vendus.
- **ORDERS** : Commandes passées par les clients (Guest).
- **ORDER_ITEMS** : Lignes contenues dans chaque commande (Produit + Quantité).
- **SLIDER_IMAGES** : Images promotionnelles affichées en page d'accueil.

### Relations (Cardinalités)

1.  **CATEGORIES 1:N PRODUCTS**
    - Une catégorie contient **plusieurs** produits.
    - Un produit appartient à **une seule** catégorie.
    - _Constraint_ : Suppression d'une catégorie bloquée si elle contient des produits (`ON DELETE RESTRICT`).

2.  **ORDERS 1:N ORDER_ITEMS**
    - Une commande contient **plusieurs** lignes d'articles.
    - Une ligne d'article appartient à **une seule** commande.
    - _Constraint_ : Suppression d'une commande entraîne la suppression de ses lignes (`ON DELETE CASCADE`).

3.  **PRODUCTS 1:N ORDER_ITEMS**
    - Un produit peut se retrouver dans **plusieurs** lignes de commande (commandes différentes).
    - Une ligne de commande référence **un** produit.
    - _Constraint_ : Si un produit est supprimé du catalogue, la ligne de commande conserve les infos (Nom, Prix) mais le lien devient NULL (`ON DELETE SET NULL`), afin de garder l'historique des ventes intact.

## 2. Contraintes et Règles Métier

### Intégrité des Données

- **Unicité** :
  - `users.username` et `users.email` doivent être uniques.
  - `products.slug` et `categories.slug` pour les URLs SEO.
  - `orders.order_number` pour identifier les commandes (ex: ORD-2023-X).
- **Valeurs par défaut** :
  - Prix par défaut : 0.00.
  - Statut Commande : `PENDING`.
  - Rôle Utilisateur : `MANAGER`.

### Règles Implicites

- **Gestion des Stocks** : La table `products` a un champ `stock_quantity`. Une commande validée devrait décrémenter ce stock (logique métier à implémenter dans le Service Java).
- **Historique Prix** : La table `order_items` stocke `product_price` au moment de l'achat. Si le prix du produit change dans `products`, l'historique de la commande reste correct.
- **Utilisateurs** : Pas de table `customers` car le système fonctionne en mode "Invité". Les infos client sont dupliquées dans chaque `orders`.

## 3. Script SQL

Le script SQL complet est fourni dans le fichier joint `database_schema.sql`.

## 4. Spécificités PostgreSQL (Implémentation)

Pour garantir la performance et la compatibilité avec Spring Boot/PostgreSQL :

- **Identifiants (`id`)** : Utilisation du type `BIGSERIAL` (équivalent à `AUTO_INCREMENT` mais géré par des séquences). Spring Boot doit utiliser `GenerationType.IDENTITY`.
- **Booléens** : Utilisation du type natif `BOOLEAN` (vs `TINYINT(1)` en MySQL). Les valeurs sont `TRUE`/`FALSE`.
- **Dates** : Utilisation de `TIMESTAMP WITHOUT TIME ZONE`. C'est l'application (Spring Boot) qui gère la Timezone (UTC recommandé en base).
- **Recherche Textuelle** : Pour la recherche produit, il est recommandé d'ajouter ultérieurement un index GIN sur `to_tsvector('french', name || ' ' || description)` pour une recherche "Full Text" performante.
