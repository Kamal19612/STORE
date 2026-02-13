<?php
/**
 * Helper Functions - Catalog Application
 * 
 * Functions for fetching and processing product data
 */

/**
 * Fetch product data from Google Sheets
 * 
 * @return array Product data from Google Sheets
 */
function fetchProductData() {
    $apiKey = GOOGLE_SHEETS_CONFIG['api_key'];
    $spreadsheetId = GOOGLE_SHEETS_CONFIG['spreadsheet_id'];
    $range = GOOGLE_SHEETS_CONFIG['range'];
    
    $url = sprintf(
        'https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s?key=%s',
        $spreadsheetId,
        $range,
        $apiKey
    );
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode !== 200) {
        error_log("Google Sheets API Error: HTTP $httpCode");
        return [];
    }
    
    $data = json_decode($response, true);
    
    return $data['values'] ?? [];
}

/**
 * Parse a product row from the sheet into a structured array
 * 
 * @param array $row Raw row data from sheet
 * @param int $index Row index
 * @return array|null Structured product data or null if invalid
 */
function parseProduct($row, $index = 0) {
    // Debug: log la ligne
    error_log("Parsing row $index: " . print_r($row, true));
    
    // Vérifier que la ligne a au moins 2 colonnes (nom et catégorie minimum)
    if (count($row) < 2) {
        error_log("Row $index skipped: less than 2 columns");
        return null;
    }
    
    $columns = SHEET_COLUMNS;
    
    // Extraire les données selon les colonnes définies
    $id = isset($row[$columns['ID']]) && !empty($row[$columns['ID']]) 
        ? $row[$columns['ID']] 
        : 'prod_' . ($index + 1);
    
    $nom = isset($row[$columns['NOM']]) ? trim($row[$columns['NOM']]) : '';
    $categorie = isset($row[$columns['CATEGORIE']]) ? trim($row[$columns['CATEGORIE']]) : '';
    $description = isset($row[$columns['DESCRIPTION']]) ? trim($row[$columns['DESCRIPTION']]) : '';
    $prix = isset($row[$columns['PRIX']]) ? floatval(str_replace([' ', 'FCFA', 'F CFA'], '', $row[$columns['PRIX']])) : 0;
    $disponibilite = isset($row[$columns['DISPONIBILITE']]) 
        ? strtoupper(trim($row[$columns['DISPONIBILITE']])) 
        : 'OUI';
    $imageUrl = isset($row[$columns['IMAGE_URL']]) ? trim($row[$columns['IMAGE_URL']]) : '';
    $stock = isset($row[$columns['STOCK']]) && !empty($row[$columns['STOCK']]) 
        ? intval($row[$columns['STOCK']]) 
        : null;
    
    // Valider les champs obligatoires - juste le nom est requis
    if (empty($nom)) {
        error_log("Row $index skipped: empty name");
        return null;
    }
    
    // Si pas de prix, mettre 0 par défaut
    if ($prix <= 0) {
        error_log("Row $index: prix is 0 or invalid, setting to 0");
        $prix = 0;
    }
    
    $product = [
        'id' => $id,
        'nom' => $nom,
        'categorie' => $categorie,
        'description' => $description,
        'prix' => $prix,
        'disponible' => ($disponibilite === 'DISPONIBLE' || $disponibilite === 'OUI' || $disponibilite === '1' || $disponibilite === 'TRUE'),
        'image_url' => $imageUrl,
        'stock' => $stock
    ];
    
    error_log("Product parsed: " . print_r($product, true));
    
    return $product;
}

/**
 * Get all products with optional filtering
 * 
 * @param string|null $category Filter by category
 * @param bool|null $availableOnly Show only available products
 * @return array Array of products
 */
function getProducts($category = null, $availableOnly = null) {
    $rawData = fetchProductData();
    $products = [];
    
    foreach ($rawData as $index => $row) {
        $product = parseProduct($row, $index);
        
        if (!$product) {
            continue;
        }
        
        // Apply filters
        if ($category && $product['categorie'] !== $category) {
            continue;
        }
        
        if ($availableOnly === true && !$product['disponible']) {
            continue;
        }
        
        $products[] = $product;
    }
    
    return $products;
}

/**
 * Get unique categories from products
 * 
 * @return array List of unique categories
 */
function getCategories() {
    $rawData = fetchProductData();
    $categories = [];
    
    foreach ($rawData as $row) {
        if (isset($row[SHEET_COLUMNS['CATEGORIE']]) && !empty($row[SHEET_COLUMNS['CATEGORIE']])) {
            $cat = trim($row[SHEET_COLUMNS['CATEGORIE']]);
            if (!in_array($cat, $categories)) {
                $categories[] = $cat;
            }
        }
    }
    
    sort($categories);
    return $categories;
}

/**
 * Paginate an array of products
 * 
 * @param array $products All products
 * @param int $page Current page (1-based)
 * @param int $perPage Items per page
 * @return array Paginated data with products and pagination info
 */
function paginateProducts($products, $page = 1, $perPage = PRODUCTS_PER_PAGE) {
    $total = count($products);
    $totalPages = ceil($total / $perPage);
    
    // Ensure page is valid
    $page = max(1, min($page, $totalPages ?: 1));
    
    $offset = ($page - 1) * $perPage;
    $pagedProducts = array_slice($products, $offset, $perPage);
    
    return [
        'products' => $pagedProducts,
        'pagination' => [
            'current_page' => $page,
            'per_page' => $perPage,
            'total_items' => $total,
            'total_pages' => $totalPages,
            'has_previous' => $page > 1,
            'has_next' => $page < $totalPages
        ]
    ];
}

/**
 * Get product by ID
 * 
 * @param string $productId Product ID
 * @return array|null Product data or null if not found
 */
function getProductById($productId) {
    $products = getProducts();
    
    foreach ($products as $product) {
        if ($product['id'] === $productId) {
            return $product;
        }
    }
    
    return null;
}

/**
 * Format price for display
 * 
 * @param float $price Price value
 * @return string Formatted price
 */
function formatPrice($price) {
    return number_format($price, 0, ',', ' ') . ' ' . STORE_INFO['currency'];
}

/**
 * Sanitize output for HTML display
 * 
 * @param string $text Text to sanitize
 * @return string Sanitized text
 */
function sanitize($text) {
    return htmlspecialchars($text, ENT_QUOTES, 'UTF-8');
}