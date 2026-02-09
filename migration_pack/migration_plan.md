# Plan de Migration - Vers Spring Boot + React + PostgreSQL

## 1. Stratégie Globale

La migration se fera en **Big Bang** (basculement complet en une fois) car l'application actuelle est relativement petite et le changement de structure de données (Google Sheets -> PostgreSQL) rend la cohabitation difficile.
Cependant, l'Admin PHP pourra rester accessible en lecture seule au début si nécessaire.

## 2. Étapes de Migration (Chronologique)

### Phase 1 : Initialisation (Semaine 1)

1.  **Backend** : Initialiser projet Spring Boot (start.spring.io).
    - Dépendances : Web, JPA, Security, PostgreSQL Driver, Lombok, JWT.
    - Configuration GitHub/GitLab CI.
2.  **Frontend** : Initialiser projet Vite + React.
    - Installation Tailwind CSS.
3.  **Base de Données** :
    - Créer nouvelle BD `dbstore_v2` sur PostgreSQL.
    - Exécuter le script `database_schema.sql`.

### Phase 2 : Migration des Données (Semaine 1-2)

_Critique : Passage de Google Sheets à PostgreSQL._

1.  **Extraction** : Exporter le Google Sheet `PRODUITS` en CSV.
2.  **Transformation** : Nettoyer les données (formater prix, vérifier doublons).
3.  **Chargement** : Importer dans la table `categories` (via script ou manuel) puis `products` (Outil `COPY` de Postgres ou script SQL).
4.  **Reprise de l'existant** :
    - Importer la table `users` de l'ancien MySQL vers PostgreSQL (attention au hachage mot de passe).
    - Importer `slider_images`.
    - Archiver `orders` (Optionnel : importer l'historique).

### Phase 3 : Développement Backend (Semaine 2-3)

**Complexité : Moyenne**

1.  Implémenter `User` et `AuthController` (JWT).
2.  Implémenter `Product` et `Category` (API Publique + Admin CRUD).
3.  Implémenter `Order` (Création Guest + Admin Gestion).
    - _Challenge_ : Refaire la logique de notification WhatsApp (Backend ou Frontend ? -> Frontend via lien `wa.me`, comme avant, c'est plus simple. Backend si on veut automatiser avec Twilio plus tard).
    - _Amélioration_ : Gestion stocks temps réel.

### Phase 4 : Développement Frontend (Semaine 3-4)

**Complexité : Élevée (Design et UX)**

1.  **Intégration Design** : Recréer le Header, Footer, Cards Produits avec Tailwind.
2.  **Logique Client** :
    - Store Zustand pour le Panier (Persistance localStorage).
    - Appels API Axios.
3.  **Pages Admin** : Dashboard, Tableaux de gestion (React Table).

### Phase 5 : Recette et Bascule (Semaine 5)

1.  **Tests Bout-en-Bout** : Vérifier le flux complet (Panier -> Commande -> Admin -> Statut).
2.  **Déploiement** :
    - Backend : Cloud (AWS RDS/Heroku Postgres/Supabase) avec Docker.
    - Frontend : Vercel ou Netlify (ou Serveur Web Nginx sur le même VPS).
3.  **DNS** : Basculer le domaine vers la nouvelle IP.

## 3. Estimation de Complexité & Risques

| Module                 | Complexité | Risques                           | Solutions                                                   |
| ---------------------- | ---------- | --------------------------------- | ----------------------------------------------------------- |
| **Auth & Users**       | Faible     | Perte accès Admin                 | Créer un super-admin par défaut via script au lancement.    |
| **Produits/Catalogue** | Moyenne    | Données Google Sheet incohérentes | Script de validation strict lors de l'import.               |
| **Panier & Commande**  | Élevée     | Perte de commandes, Bug Stock     | Tests unitaires sur le Service Commande (`@Transactional`). |
| **Frontend/UX**        | Élevée     | Design non fidèle                 | Utiliser les mêmes codes couleurs et assets qu'en PHP.      |
| **Déploiement**        | Moyenne    | CORS, HTTPS                       | Configurer correctement Nginx/Apache et Spring Security.    |

- Accès complet au Google Sheet (Lecture).
- Accès à la BD MySQL actuelle (Dump).
- Instance PostgreSQL locale ou distante installée.

## 5. Protocole de Migration des Données (Détail)

### A. Mapping Google Sheets -> PostgreSQL (`products`)

Voici la correspondance exacte des colonnes basée sur `config.php` et `functions.php`.

**Source**: Export CSV du Sheet `PRODUITS`.

| Colonne CSV (Header) | Champ PostgreSQL  | Traitement / Transformation                                                                          |
| -------------------- | ----------------- | ---------------------------------------------------------------------------------------------------- |
| `A` (ID)             | `slug` (ou `sku`) | Utiliser comme identifiant unique.                                                                   |
| `B` (Nom)            | `name`            | Trim. Obligatoire.                                                                                   |
| `C` (Catégorie)      | `category_id`     | ⚠️ **Action Requise** : Créer d'abord la catégorie dans la table `categories` et récupérer son `id`. |
| `D` (Description)    | `description`     | Trim.                                                                                                |
| `E` (Prix)           | `price`           | Enlever "FCFA", espaces. Convertir en Decimal. Si vide -> 0.                                         |
| `F` (Disponibilité)  | `available`       | `TRUE` si valeur IN ('DISPONIBLE', 'OUI', '1', 'TRUE').                                              |
| `G` (Image URL)      | `image_url`       | Trim.                                                                                                |
| `H` (Stock)          | `stock_quantity`  | Convertir en INT. Si vide -> 0.                                                                      |

### B. Imports des Images

1.  **Produits** : Les URLs dans le Google Sheet sont des liens externes (`https://...`).
    - _Option 1_ : Les laisser telles quelles (Hotlinking).
    - _Option 2 (Recommandée)_ : Script Python pour télécharger toutes les images, les redimensionner (WebP), les uploader sur S3/Cloudinary, et mettre à jour la BDD.
2.  **Slider** : Les images sont actuellement stockées sur le serveur (`upload/slider/`).
    - Récupérer le dossier `upload/` via FTP.
    - Placer dans le dossier `public/assets/slider` du projet React ou sur S3.
