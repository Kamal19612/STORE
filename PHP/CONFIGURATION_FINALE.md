# ⚡ CONFIGURATION FINALE - SUCRE STORE

## ✅ STRUCTURE DE VOTRE GOOGLE SHEET CONFIRMÉE

Votre Google Sheet a **3 feuilles** :

1. **PRODUITS** (gid=0) → 🎯 **C'EST CELLE-CI QU'ON UTILISE**
2. **CATEGORIES** (gid=258301320) → Utilisée en référence dans PRODUITS
3. **LISTE DEROULANTE** (gid=609628463) → Utilisée en référence dans PRODUITS

---

## 🔧 CONFIGURATION config.php

### ✅ NOM DE LA FEUILLE - DÉJÀ CONFIGURÉ !

```php
'range' => 'PRODUITS!A2:H1000',
'sheet_name' => 'PRODUITS'
```

**✅ C'EST BON ! Pas besoin de modifier cette section.**

---

### ⚠️ À MODIFIER : BASE DE DONNÉES (Lignes 50-53)

```php
define('DB_NAME', 'votre_base_de_donnees');  // ⚠️ MODIFIER
define('DB_USER', 'votre_utilisateur');      // ⚠️ MODIFIER  
define('DB_PASS', 'votre_mot_de_passe');     // ⚠️ MODIFIER
```

**Exemple :**
```php
define('DB_NAME', 'sucre_store');
define('DB_USER', 'root');
define('DB_PASS', 'monmotdepasse');
```

---

### ⚠️ À MODIFIER : WHATSAPP (Ligne 11)

```php
'whatsapp' => '22660713131',  // ⚠️ MODIFIER avec votre numéro
```

**Format correct :**
- ✅ `'22670123456'` (code pays + numéro, pas d'espace)
- ❌ `'+226 70 12 34 56'` (INCORRECT)

---

## 📊 STRUCTURE ATTENDUE DANS VOTRE FEUILLE "PRODUITS"

Votre feuille **PRODUITS** doit avoir ces colonnes (ligne 1) :

| A | B | C | D | E | F | G | H |
|---|---|---|---|---|---|---|---|
| **ID** | **NOM** | **CATEGORIE** | **DESCRIPTION** | **PRIX** | **DISPONIBILITE** | **IMAGE** | **STOCK** |

**Exemple de données (ligne 2+) :**
```
PROD001 | Vibromasseur Rose | SEXTOY | Description... | 15000 | OUI | https://... | 10
PROD002 | Lingerie Sexy | LINGERIES | Description... | 12000 | OUI | https://... | 5
```

**Points critiques :**
- ✅ Colonne **E (PRIX)** = Nombres purs : `15000` (pas "15 000" ni "15000 FCFA")
- ✅ Colonne **F (DISPO)** = `OUI` ou `NON` (en majuscules)
- ✅ Colonne **C (CATEGORIE)** = Référence la feuille CATEGORIES (c'est OK, votre formule s'en occupe)

---

## 🚀 DÉPLOIEMENT EN 3 ÉTAPES

### 1️⃣ Modifier config.php
- ✅ Nom feuille : PRODUITS (déjà fait)
- ⚠️ Base de données : à modifier
- ⚠️ WhatsApp : à modifier

### 2️⃣ Upload fichiers
```
/votre-site/
├── index.php
├── config.php (modifié)
├── functions.php
├── cart_handler.php
├── process_order.php
├── db_connection.php
├── order_helpers.php
├── test_sheets.php
└── .htaccess
```

### 3️⃣ Tester
```
1. http://votre-site.com/test_sheets.php  → Vérifier connexion
2. http://votre-site.com/index.php        → Voir le catalogue
```

---

## ✅ RÉSULTAT ATTENDU

### test_sheets.php devrait afficher :
```
✅ Connexion réussie ! X ligne(s) récupérée(s)

Total Produits: X
Disponibles: X  
Catégories: X
```

### index.php devrait afficher :
- ✅ Grille de produits avec images
- ✅ Filtres par catégorie (venant de votre feuille CATEGORIES)
- ✅ Pagination
- ✅ Panier fonctionnel

---

## 🎯 C'EST PRÊT !

Votre configuration est **quasi-complète**. Il ne reste que :
1. Credentials MySQL
2. Numéro WhatsApp

**Temps estimé : 2 minutes** ⏱️

Bon déploiement ! 🚀
