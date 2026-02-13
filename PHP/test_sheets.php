<?php
/**
 * Test Google Sheets Connection
 * 
 * Ce fichier permet de tester la connexion à Google Sheets
 * et de visualiser les données récupérées
 */

require_once 'config.php';
require_once 'functions.php';

?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Test Google Sheets - <?= STORE_INFO['name'] ?></title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 1200px;
            margin: 20px auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .container {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #e91e63;
            border-bottom: 3px solid #e91e63;
            padding-bottom: 10px;
        }
        h2 {
            color: #333;
            margin-top: 30px;
        }
        .success {
            background: #d4edda;
            border: 1px solid #c3e6cb;
            color: #155724;
            padding: 15px;
            border-radius: 4px;
            margin: 15px 0;
        }
        .error {
            background: #f8d7da;
            border: 1px solid #f5c6cb;
            color: #721c24;
            padding: 15px;
            border-radius: 4px;
            margin: 15px 0;
        }
        .info {
            background: #d1ecf1;
            border: 1px solid #bee5eb;
            color: #0c5460;
            padding: 15px;
            border-radius: 4px;
            margin: 15px 0;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        th {
            background: #e91e63;
            color: white;
        }
        tr:nth-child(even) {
            background: #f9f9f9;
        }
        .badge {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
        }
        .badge-success {
            background: #28a745;
            color: white;
        }
        .badge-danger {
            background: #dc3545;
            color: white;
        }
        pre {
            background: #f4f4f4;
            border: 1px solid #ddd;
            padding: 15px;
            border-radius: 4px;
            overflow-x: auto;
        }
        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin: 20px 0;
        }
        .stat-card {
            background: linear-gradient(135deg, #e91e63, #f06292);
            color: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }
        .stat-card h3 {
            margin: 0 0 10px 0;
            font-size: 14px;
            opacity: 0.9;
        }
        .stat-card .number {
            font-size: 32px;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔍 Test de connexion Google Sheets</h1>
        
        <?php
        echo '<div class="info">';
        echo '<strong>Configuration actuelle :</strong><br>';
        echo 'Spreadsheet ID: ' . GOOGLE_SHEETS_CONFIG['spreadsheet_id'] . '<br>';
        echo 'Range: ' . GOOGLE_SHEETS_CONFIG['range'] . '<br>';
        echo 'Sheet Name: ' . GOOGLE_SHEETS_CONFIG['sheet_name'];
        echo '</div>';
        
        try {
            echo '<h2>📊 Récupération des données...</h2>';
            
            // Fetch raw data
            $rawData = fetchProductData();
            
            if (empty($rawData)) {
                echo '<div class="error">';
                echo '<strong>❌ Erreur :</strong> Aucune donnée récupérée.<br><br>';
                echo '<strong>Vérifications à faire :</strong><br>';
                echo '1. Le Google Sheet est-il partagé publiquement ou avec votre clé API ?<br>';
                echo '2. La clé API est-elle correcte ?<br>';
                echo '3. Le nom de la feuille (sheet_name) est-il correct ?<br>';
                echo '4. L\'API Google Sheets est-elle activée dans votre projet Google Cloud ?';
                echo '</div>';
            } else {
                echo '<div class="success">';
                echo '✅ Connexion réussie ! ' . count($rawData) . ' ligne(s) récupérée(s) depuis Google Sheets.';
                echo '</div>';
                
                // Parse products
                $products = getProducts();
                $categories = getCategories();
                
                // Calculate stats
                $availableProducts = array_filter($products, fn($p) => $p['disponible']);
                $totalValue = array_sum(array_map(fn($p) => $p['prix'], $products));
                
                echo '<div class="stats">';
                echo '<div class="stat-card">';
                echo '<h3>Total Produits</h3>';
                echo '<div class="number">' . count($products) . '</div>';
                echo '</div>';
                
                echo '<div class="stat-card">';
                echo '<h3>Disponibles</h3>';
                echo '<div class="number">' . count($availableProducts) . '</div>';
                echo '</div>';
                
                echo '<div class="stat-card">';
                echo '<h3>Catégories</h3>';
                echo '<div class="number">' . count($categories) . '</div>';
                echo '</div>';
                
                echo '<div class="stat-card">';
                echo '<h3>Valeur Stock</h3>';
                echo '<div class="number">' . number_format($totalValue, 0) . '</div>';
                echo '</div>';
                echo '</div>';
                
                // Display categories
                echo '<h2>📁 Catégories trouvées</h2>';
                if (empty($categories)) {
                    echo '<div class="error">Aucune catégorie trouvée.</div>';
                } else {
                    echo '<div class="info">';
                    echo implode(' • ', array_map('htmlspecialchars', $categories));
                    echo '</div>';
                }
                
                // Display products table
                echo '<h2>🛍️ Produits chargés</h2>';
                if (empty($products)) {
                    echo '<div class="error">Aucun produit valide trouvé.</div>';
                } else {
                    echo '<table>';
                    echo '<thead>';
                    echo '<tr>';
                    echo '<th>ID</th>';
                    echo '<th>Nom</th>';
                    echo '<th>Catégorie</th>';
                    echo '<th>Prix</th>';
                    echo '<th>Disponible</th>';
                    echo '<th>Image</th>';
                    echo '</tr>';
                    echo '</thead>';
                    echo '<tbody>';
                    
                    foreach ($products as $product) {
                        echo '<tr>';
                        echo '<td>' . htmlspecialchars($product['id']) . '</td>';
                        echo '<td><strong>' . htmlspecialchars($product['nom']) . '</strong><br>';
                        echo '<small>' . htmlspecialchars(substr($product['description'], 0, 50)) . '...</small></td>';
                        echo '<td>' . htmlspecialchars($product['categorie']) . '</td>';
                        echo '<td><strong>' . formatPrice($product['prix']) . '</strong></td>';
                        echo '<td>';
                        if ($product['disponible']) {
                            echo '<span class="badge badge-success">OUI</span>';
                        } else {
                            echo '<span class="badge badge-danger">NON</span>';
                        }
                        echo '</td>';
                        echo '<td>';
                        if (!empty($product['image_url'])) {
                            echo '✅ Oui';
                        } else {
                            echo '❌ Non';
                        }
                        echo '</td>';
                        echo '</tr>';
                    }
                    
                    echo '</tbody>';
                    echo '</table>';
                }
                
                // Show raw data sample
                echo '<h2>📋 Données brutes (5 premières lignes)</h2>';
                echo '<pre>';
                print_r(array_slice($rawData, 0, 5));
                echo '</pre>';
            }
            
        } catch (Exception $e) {
            echo '<div class="error">';
            echo '<strong>❌ Erreur :</strong> ' . htmlspecialchars($e->getMessage());
            echo '</div>';
        }
        ?>
        
        <h2>🔧 Instructions de configuration</h2>
        <div class="info">
            <p><strong>Si vous voyez cette erreur, voici les étapes à suivre :</strong></p>
            <ol>
                <li><strong>Rendre le Google Sheet public :</strong>
                    <ul>
                        <li>Ouvrir le Google Sheet</li>
                        <li>Cliquer sur "Partager" en haut à droite</li>
                        <li>Changer "Accès limité" en "Tous les utilisateurs avec le lien"</li>
                        <li>Définir les permissions sur "Lecteur"</li>
                        <li>Copier le lien</li>
                    </ul>
                </li>
                <li><strong>OU créer une clé API Google :</strong>
                    <ul>
                        <li>Aller sur <a href="https://console.cloud.google.com/" target="_blank">Google Cloud Console</a></li>
                        <li>Créer un nouveau projet (ou sélectionner un existant)</li>
                        <li>Activer l'API "Google Sheets API"</li>
                        <li>Créer des identifiants → Clé API</li>
                        <li>Copier la clé et la mettre dans config.php</li>
                    </ul>
                </li>
                <li><strong>Vérifier le nom de la feuille :</strong>
                    <ul>
                        <li>Le nom doit correspondre exactement à l'onglet dans votre Google Sheet</li>
                        <li>Par défaut: "PRODUITS", "Feuille1", "Sheet1", etc.</li>
                    </ul>
                </li>
            </ol>
        </div>
        
        <div style="margin-top: 30px; padding-top: 20px; border-top: 2px solid #ddd; text-align: center; color: #666;">
            <p>🚀 <strong>Test Google Sheets</strong> - Application créée par WEB GENIOUS</p>
        </div>
    </div>
</body>
</html>