<?php
/**
 * Configuration File - Catalog Application
 * 
 * Contains all constants and configuration settings
 */

// Store information
define('STORE_INFO', [
    'name' => 'SUCRE STORE',
    'phone' => '+226 XX XX XX XX',  // À personnaliser
    'whatsapp' => '22671030518',     // Numéro WhatsApp pour les commandes
    'currency' => 'FCFA',
    'tax_rate' => 0,  // 0% tax ou à ajuster selon vos besoins
]);

// Google Sheets API configuration
define('GOOGLE_SHEETS_CONFIG', [
    'api_key' => 'AIzaSyBt9-G-xdkz5fJcLugCj2mFVPUIHRazHxA',  // Votre clé API
    'spreadsheet_id' => '1OgGElLh3Y4BmW1EE9ihAFShAFGXgxZ9rjQuQ830av1k',
    'range' => 'PRODUITS!A2:H1000',  // ✅ Nom confirmé de votre feuille
    'sheet_name' => 'PRODUITS'  // ✅ Feuille principale avec vos produits
]);

// Colonnes attendues dans le Google Sheet (index 0-based)
define('SHEET_COLUMNS', [
    'ID' => 0,              // Colonne A - ID
    'IMAGE_URL' => 1,       // Colonne B - Photo
    'NOM' => 2,             // Colonne C - Nom
    'DESCRIPTION' => 3,     // Colonne D - Mode d'emploi
    'STOCK' => 4,           // Colonne E - Volume_poids
    'CATEGORIE' => 5,       // Colonne F - Categorie
    'DISPONIBILITE' => 6,   // Colonne G - Disponibilité
    'PRIX' => 7             // Colonne H - Prix
]);

// Pagination settings
define('PRODUCTS_PER_PAGE', 12);

// Categories (sera chargé dynamiquement depuis le sheet)
// Ces catégories correspondent à ce que j'ai vu dans votre sheet
define('PRODUCT_CATEGORIES', [
    'APHRODISIAQUE',
    'SEXTOY',
    'ACCESSOIRE EROTIQUE / INTIME',
    'LINGERIES',
    'LUBRIFIANT SEXE',
    'HUILE DE MASSAGE',
    'CONDOM',
    'AGRANDISSEMENT DU PENIS'
]); 

// Database configuration
define('DB_HOST', 'localhost');
define('DB_NAME', 'u274984172_dbstore'); 
define('DB_USER', 'u274984172_userbdstore');
define('DB_PASS', '6DiC0yVhjh^U');
define('DB_CHARSET', 'utf8mb4');

// Error reporting
ini_set('display_errors', 1);
error_reporting(E_ALL);

// Set timezone
date_default_timezone_set('Africa/Ouagadougou');