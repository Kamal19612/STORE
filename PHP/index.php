<?php
/**
 * Product Catalog Application
 * Main index file
 */

session_start();

require_once 'config.php';
require_once 'functions.php';

// Initialize cart if not exists
if (!isset($_SESSION['cart'])) {
    $_SESSION['cart'] = [];
}

// Get filter parameters
$selectedCategory = $_GET['category'] ?? '';
$availableOnly = isset($_GET['available']) && $_GET['available'] === '1';
$currentPage = isset($_GET['page']) ? intval($_GET['page']) : 1;

// Fetch products with filters
$allProducts = getProducts(
    !empty($selectedCategory) ? $selectedCategory : null,
    $availableOnly ? true : null
);

// Paginate results
$paginatedData = paginateProducts($allProducts, $currentPage);
$products = $paginatedData['products'];
$pagination = $paginatedData['pagination'];

// Get all categories for filter
$categories = getCategories();

// Build query string for pagination links
$queryParams = [];
if (!empty($selectedCategory)) {
    $queryParams['category'] = $selectedCategory;
}
if ($availableOnly) {
    $queryParams['available'] = '1';
}
$queryString = !empty($queryParams) ? '&' . http_build_query($queryParams) : '';
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= STORE_INFO['name'] ?> - Catalogue</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary: #f5ad41;
            --primary-dark: #d89a35;
            --secondary: #242021;
            --secondary-light: #3a3638;
        }
        
        .product-card {
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .product-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 20px rgba(242, 33, 33, 0.15);
        }
        .cart-badge {
            animation: bounce 0.3s ease-in-out;
        }
        @keyframes bounce {
            0%, 100% { transform: scale(1); }
            50% { transform: scale(1.2); }
        }
        .no-scrollbar::-webkit-scrollbar {
            display: none;
        }
        .no-scrollbar {
            -ms-overflow-style: none;
            scrollbar-width: none;
        }
        
        .detail-btn {
            opacity: 1;
            transition: opacity 0.2s;
        }
        
        @media (min-width: 768px) {
            .detail-btn {
                opacity: 0;
            }
            
            .product-card:hover .detail-btn {
                opacity: 1;
            }
        }
        
        /* Mobile Categories Bar */
        .mobile-categories-bar {
            display: none;
        }
        
        @media (max-width: 1023px) {
            /* Masquer le sidebar sur mobile */
            .desktop-sidebar {
                display: none;
            }
            
            /* Afficher la barre mobile */
            .mobile-categories-bar {
                display: flex;
                align-items: center;
              
                margin-bottom: 20px;
                overflow: hidden;
            }
            
            .category-btn-label {
                display: flex;
                align-items: center;
                background-color: var(--primary);
                color: var(--secondary);
                padding: 10px 16px;
                border-radius: 28px;
                font-weight: bold;
                white-space: nowrap;
                margin-right: 12px;
                flex-shrink: 0;
                font-size:13px;
            }
            
            .category-btn-label i {
                margin-right: 8px;
                font-size: 14px;
            }
            
            .categories-scroll {
                display: flex;
                overflow-x: auto;
                gap: 8px;
                flex: 1;
                -webkit-overflow-scrolling: touch;
                scrollbar-width: none;
            }
            
            .categories-scroll::-webkit-scrollbar {
                display: none;
            }
            
            .category-item {
                padding: 10px 8px;
                white-space: nowrap;
                font-weight: 600;
                transition: all 0.3s;
                cursor: pointer;
                color: var(--secondary);
                text-decoration: none;
                display: inline-block;
                flex-shrink: 0;
                font-size: 12px;
            }
            
            .category-item:hover {
              
                                color: var(--primary);

            }
            
            .category-item.active {
                color: var(--primary);
                border-color: var(--primary);
            }
        }
        
        /****************** SLIDER PUB STYLE *****************/
                .slider-container {
                    position: relative;
                    width: 100%;
                }
                
                .slider-wrapper {
                    position: relative;
                    width: 100%;
                }
                
                .slide {
                    display: none;
                    width: 100%;
                }
                
                .slide.active {
                    display: block;
                    animation: fadeIn 0.5s;
                }
                
                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                
                .slider-prev, .slider-next {
                    cursor: pointer;
                }
                
                .slider-dot {
                    cursor: pointer;
                    transition: background-color 0.3s;
                }
                
                @media (max-width: 768px) {
                    .slider-prev, .slider-next {
                        padding: 10px;
                    }
                }
                
                
                /* Par défaut : afficher web, masquer mobile */
                .slider-section-web {
                    display: block;
                }
                
                .slider-section-mobile {
                    display: none;
                }
                
                /* Sur mobile (écrans < 768px) : masquer web, afficher mobile */
                @media (max-width: 767px) {
                    .slider-section-web {
                        display: none;
                    }
                    
                    .slider-section-mobile {
                        display: block;
                    }
                }
                
                .slide.active img {
    border-radius: 15px;
}

        /* Toast Notification */
        .toast-notification {
            position: fixed;
            top: 100px;
            right: 20px;
            background-color: #10b981;
            color: white;
            padding: 16px 24px;
            border-radius: 8px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
            z-index: 9999;
            animation: slideIn 0.3s ease-out;
            min-width: 300px;
        }
        
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
        
        @keyframes slideOut {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(400px);
                opacity: 0;
            }
        }
        
        .toast-notification.hiding {
            animation: slideOut 0.3s ease-in;
        }
        
        /* Cart Button Blink Animation */
        @keyframes blink {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.4; }
        }
        
        .cart-blink {
            animation: blink 0.5s ease-in-out 3;
        }
    </style>
</head>
<body class="bg-gray-50">
    <?php
    // Récupérer les slides actifs
    try {
        require_once 'db_connection.php';
        $pdo = getDbConnection();
        $stmt = $pdo->query("SELECT * FROM slider_images WHERE active = 1 ORDER BY order_position ASC LIMIT 10");
        $slides = $stmt->fetchAll(PDO::FETCH_ASSOC);
    } catch (Exception $e) {
        $slides = [];
    }
    ?>
  
    
    <!-- Header -->
    <header style="background-color: var(--secondary);" class="shadow-md sticky top-0 z-50">
        <div class="container mx-auto px-4 py-4">
            <div class="flex items-center justify-between">
                <div class="flex items-center space-x-4">
                    <img src="https://sucrestore.web-genious.com/images/logo-sucre.png" alt="<?= STORE_INFO['name'] ?>" class="h-12 object-contain">
                </div>
                
                <!-- Cart Button -->
                <button id="cart-toggle" class="relative text-white px-4 py-2 rounded-lg transition" style="background-color: var(--primary); color: var(--secondary);">
                    <i class="fas fa-shopping-cart"></i>
                    <span id="cart-count" class="absolute -top-2 -right-2 text-xs rounded-full w-6 h-6 flex items-center justify-center cart-badge" style="background-color: var(--secondary); color: white;">
                        0
                    </span>
                </button>
            </div>
        </div>
    </header>

  

  
    <div class="container mx-auto px-4 py-6">
        
            <?php if (!empty($slides)): ?>
            <!-- Slider Pub Web -->
            <div class="slider-container relative overflow-hidden slider-section-web" style="">
                <div class="slider-wrapper">
                    <?php foreach ($slides as $index => $slide): ?>
                    <div class="slide <?= $index === 0 ? 'active' : '' ?>" data-slide="<?= $index ?>">
                        <?php if (!empty($slide['link'])): ?>
                        <a href="<?= htmlspecialchars($slide['link']) ?>" target="_blank">
                            <img src="<?= htmlspecialchars($slide['image_url']) ?>" 
                                 alt="<?= htmlspecialchars($slide['title']) ?>"
                                 class="w-full h-auto object-cover"
                                 style="max-height: 400px;">
                        </a>
                        <?php else: ?>
                        <img src="<?= htmlspecialchars($slide['image_url']) ?>" 
                             alt="<?= htmlspecialchars($slide['title']) ?>"
                             class="w-full h-auto object-cover"
                             style="max-height: 400px;">
                        <?php endif; ?>
                    </div>
                    <?php endforeach; ?>
                </div>
                
                <?php if (count($slides) > 1): ?>
                <!-- Navigation arrows -->
                <button class="slider-prev absolute left-4 top-1/2 transform -translate-y-1/2 bg-black bg-opacity-50 text-white p-3 rounded-full hover:bg-opacity-75 transition z-10">
                    <i class="fas fa-chevron-left"></i>
                </button>
                <button class="slider-next absolute right-4 top-1/2 transform -translate-y-1/2 bg-black bg-opacity-50 text-white p-3 rounded-full hover:bg-opacity-75 transition z-10">
                    <i class="fas fa-chevron-right"></i>
                </button>
                
                <!-- Dots -->
                <div class="slider-dots absolute bottom-4 left-1/2 transform -translate-x-1/2 flex space-x-2 z-10">
                    <?php foreach ($slides as $index => $slide): ?>
                    <button class="slider-dot w-3 h-3 rounded-full <?= $index === 0 ? 'bg-white' : 'bg-white bg-opacity-50' ?>" 
                            data-slide="<?= $index ?>"></button>
                    <?php endforeach; ?>
                </div>
                <?php endif; ?>
            </div>
    
 
    
    <script>
        // Slider functionality
        <?php if (count($slides) > 1): ?>
        let currentSlide = 0;
        const totalSlides = <?= count($slides) ?>;
        
        function showSlide(index) {
            const slides = document.querySelectorAll('.slide');
            const dots = document.querySelectorAll('.slider-dot');
            
            slides.forEach(s => s.classList.remove('active'));
            dots.forEach(d => d.classList.remove('bg-white'));
            dots.forEach(d => d.classList.add('bg-white', 'bg-opacity-50'));
            
            currentSlide = (index + totalSlides) % totalSlides;
            slides[currentSlide].classList.add('active');
            dots[currentSlide].classList.remove('bg-opacity-50');
            dots[currentSlide].classList.add('bg-white');
        }
        
        document.querySelector('.slider-next')?.addEventListener('click', () => {
            showSlide(currentSlide + 1);
        });
        
        document.querySelector('.slider-prev')?.addEventListener('click', () => {
            showSlide(currentSlide - 1);
        });
        
        document.querySelectorAll('.slider-dot').forEach((dot, index) => {
            dot.addEventListener('click', () => {
                showSlide(index);
            });
        });
        
        // Auto-slide every 5 seconds
        setInterval(() => {
            showSlide(currentSlide + 1);
        }, 5000);
        <?php endif; ?>
    </script>
    <?php endif; ?><br/>
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
            
            <!-- Slider Pub Mobile -->
            <div class="slider-container relative overflow-hidden slider-section-mobile" style="">
                <div class="slider-wrapper">
                    <?php foreach ($slides as $index => $slide): ?>
                    <div class="slide <?= $index === 0 ? 'active' : '' ?>" data-slide="<?= $index ?>">
                        <?php if (!empty($slide['link'])): ?>
                        <a href="<?= htmlspecialchars($slide['link']) ?>" target="_blank">
                            <img src="<?= htmlspecialchars($slide['image_url']) ?>" 
                                 alt="<?= htmlspecialchars($slide['title']) ?>"
                                 class="w-full h-auto object-cover"
                                 style="max-height: 400px;">
                        </a>
                        <?php else: ?>
                        <img src="<?= htmlspecialchars($slide['image_url']) ?>" 
                             alt="<?= htmlspecialchars($slide['title']) ?>"
                             class="w-full h-auto object-cover"
                             style="max-height: 400px;">
                        <?php endif; ?>
                    </div>
                    <?php endforeach; ?>
                </div>
                
                <?php if (count($slides) > 1): ?>
                <!-- Navigation arrows -->
                <button class="slider-prev absolute left-4 top-1/2 transform -translate-y-1/2 bg-black bg-opacity-50 text-white p-3 rounded-full hover:bg-opacity-75 transition z-10">
                    <i class="fas fa-chevron-left"></i>
                </button>
                <button class="slider-next absolute right-4 top-1/2 transform -translate-y-1/2 bg-black bg-opacity-50 text-white p-3 rounded-full hover:bg-opacity-75 transition z-10">
                    <i class="fas fa-chevron-right"></i>
                </button>
                
                <!-- Dots -->
                <div class="slider-dots absolute bottom-4 left-1/2 transform -translate-x-1/2 flex space-x-2 z-10">
                    <?php foreach ($slides as $index => $slide): ?>
                    <button class="slider-dot w-3 h-3 rounded-full <?= $index === 0 ? 'bg-white' : 'bg-white bg-opacity-50' ?>" 
                            data-slide="<?= $index ?>"></button>
                    <?php endforeach; ?>
                </div>
                <?php endif; ?>
            </div>
            
            
            <!-- Sidebar Filters -->
            <aside class="lg:col-span-1 desktop-sidebar">
                <div class="bg-white rounded-lg shadow-md p-6 sticky top-24">
                    <h2 class="text-xl font-bold mb-4 text-gray-800">
                        <i class="fas fa-filter mr-2"></i>Filtres
                    </h2>
                    
                    <form id="filter-form" method="GET" action="">
                        <!-- Category Filter -->
                        <div class="mb-6">
                            <label class="block text-sm font-semibold text-gray-700 mb-2">
                                Catégorie
                            </label>
                            <select name="category" class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-pink-500">
                                <option value="">Toutes les catégories</option>
                                <?php foreach ($categories as $cat): ?>
                                <option value="<?= sanitize($cat) ?>" <?= $selectedCategory === $cat ? 'selected' : '' ?>>
                                    <?= sanitize($cat) ?>
                                </option>
                                <?php endforeach; ?>
                            </select>
                        </div>
                        
                        <!-- Availability Filter -->
                        <div class="mb-6">
                            <label class="flex items-center space-x-2 cursor-pointer">
                                <input type="checkbox" name="available" value="1" 
                                    <?= $availableOnly ? 'checked' : '' ?>
                                    class="w-4 h-4 text-pink-600 border-gray-300 rounded focus:ring-pink-500">
                                <span class="text-sm text-gray-700">Disponibles uniquement</span>
                            </label>
                        </div>
                        
                        <button type="submit" class="w-full text-white py-2 rounded-lg transition" style="background-color: var(--primary); color: var(--secondary);">
                            Appliquer les filtres
                        </button>
                        
                        <?php if (!empty($selectedCategory) || $availableOnly): ?>
                        <a href="index.php" class="block text-center mt-2 text-sm text-gray-600 hover:text-gray-800">
                            Réinitialiser les filtres
                        </a>
                        <?php endif; ?>
                    </form>
                    
                    <!-- Results Count -->
                    <div class="mt-6 pt-6 border-t border-gray-200">
                        <p class="text-sm text-gray-600">
                            <strong><?= $pagination['total_items'] ?></strong> produit(s) trouvé(s)
                        </p>
                    </div>
                </div>
            </aside>

            <!-- Mobile Categories Bar -->
            <div class="mobile-categories-bar col-span-full">
                <div class="category-btn-label">
                    <i class="fas fa-bars"></i>
                    <span>Catégorie</span>
                </div>
                <div class="categories-scroll">
                    <a href="index.php" class="category-item <?= empty($selectedCategory) ? 'active' : '' ?>">
                        TOUT
                    </a>
                    <?php foreach ($categories as $cat): ?>
                    <a href="?category=<?= urlencode($cat) ?>" 
                       class="category-item <?= $selectedCategory === $cat ? 'active' : '' ?>">
                        <?= strtoupper(sanitize($cat)) ?>
                    </a>
                    <?php endforeach; ?>
                </div>
            </div>

            <!-- Main Content -->
            <main class="lg:col-span-3">
                <?php if (empty($products)): ?>
                <!-- No Products -->
                <div class="bg-white rounded-lg shadow-md p-12 text-center">
                    <i class="fas fa-box-open text-6xl text-gray-300 mb-4"></i>
                    <h3 class="text-xl font-semibold text-gray-700 mb-2">Aucun produit trouvé</h3>
                    <p class="text-gray-500">Essayez de modifier vos filtres</p>
                </div>
                <?php else: ?>
                <!-- Products Grid -->
                <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                    <?php foreach ($products as $product): ?>
                    <div class="product-card bg-white rounded-lg shadow-md overflow-hidden relative">
                        <!-- Product Image -->
                        <div class="relative h-48 bg-gray-200 cursor-pointer view-details-trigger" 
                             data-product='<?= json_encode($product, JSON_HEX_APOS | JSON_HEX_QUOT) ?>'>
                            <?php if (!empty($product['image_url'])): ?>
                            <img src="<?= sanitize($product['image_url']) ?>" 
                                 alt="<?= sanitize($product['nom']) ?>"
                                 class="w-full h-full object-cover">
                            <?php else: ?>
                            <div class="w-full h-full flex items-center justify-center">
                                <i class="fas fa-image text-6xl text-gray-400"></i>
                            </div>
                            <?php endif; ?>
                            
                            <!-- Bouton détails -->
                            <button class="detail-btn absolute top-2 left-2 text-white p-2 rounded-full shadow-lg view-details"
                                    style="background-color: var(--primary); color: var(--secondary);"
                                    data-product='<?= json_encode($product, JSON_HEX_APOS | JSON_HEX_QUOT) ?>'>
                                <i class="fas fa-eye"></i>
                            </button>
                            
                            <!-- Availability Badge -->
                            <?php if (!$product['disponible']): ?>
                            <div class="absolute top-2 right-2 text-white text-xs px-2 py-1 rounded" style="background-color: var(--secondary);">
                                Non disponible
                            </div>
                            <?php endif; ?>
                        </div>
                        
                        <!-- Product Info -->
                        <div class="p-4">
                            <div class="mb-2">
                                <span class="text-xs font-semibold" style="color: var(--primary);">
                                    <?= sanitize($product['categorie']) ?>
                                </span>
                            </div>
                            
                            <h3 class="text-lg font-bold mb-2 line-clamp-2" style="color: var(--secondary);">
                                <?= sanitize($product['nom']) ?>
                            </h3>
                            
                            <p class="text-sm text-gray-600 mb-3 line-clamp-2">
                                <?= sanitize($product['description']) ?>
                            </p>
                            
                            <div class="flex items-center justify-between">
                                <span class="text-2xl font-bold" style="color: var(--primary);">
                                    <?= formatPrice($product['prix']) ?>
                                </span>
                                
                                <?php if ($product['disponible']): ?>
                                <button class="add-to-cart text-white px-4 py-2 rounded-lg transition"
                                        style="background-color: var(--primary); color: var(--secondary);"
                                        data-product-id="<?= sanitize($product['id']) ?>"
                                        data-product-name="<?= sanitize($product['nom']) ?>">
                                    <i class="fas fa-cart-plus"></i>
                                </button>
                                <?php else: ?>
                                <button class="bg-gray-400 text-white px-4 py-2 rounded-lg cursor-not-allowed" disabled>
                                    <i class="fas fa-times"></i>
                                </button>
                                <?php endif; ?>
                            </div>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>

                <!-- Pagination -->
                <?php if ($pagination['total_pages'] > 1): ?>
                <div class="mt-8 flex justify-center">
                    <nav class="flex items-center space-x-2">
                        <?php if ($pagination['has_previous']): ?>
                        <a href="?page=<?= $pagination['current_page'] - 1 ?><?= $queryString ?>" 
                           class="px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
                            <i class="fas fa-chevron-left"></i>
                        </a>
                        <?php endif; ?>
                        
                        <?php for ($i = 1; $i <= $pagination['total_pages']; $i++): ?>
                            <?php if ($i == $pagination['current_page']): ?>
                            <span class="px-4 py-2 text-white rounded-lg font-semibold" style="background-color: var(--primary); color: var(--secondary);">
                                <?= $i ?>
                            </span>
                            <?php elseif ($i == 1 || $i == $pagination['total_pages'] || abs($i - $pagination['current_page']) <= 2): ?>
                            <a href="?page=<?= $i ?><?= $queryString ?>" 
                               class="px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
                                <?= $i ?>
                            </a>
                            <?php elseif (abs($i - $pagination['current_page']) == 3): ?>
                            <span class="px-2">...</span>
                            <?php endif; ?>
                        <?php endfor; ?>
                        
                        <?php if ($pagination['has_next']): ?>
                        <a href="?page=<?= $pagination['current_page'] + 1 ?><?= $queryString ?>" 
                           class="px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50">
                            <i class="fas fa-chevron-right"></i>
                        </a>
                        <?php endif; ?>
                    </nav>
                </div>
                <?php endif; ?>
                <?php endif; ?>
            </main>
        </div>
    </div>

    <!-- Cart Sidebar -->
    <div id="cart-sidebar" class="fixed top-0 right-0 h-full w-full md:w-96 bg-white shadow-2xl transform translate-x-full transition-transform duration-300 z-50">
        <div class="h-full flex flex-col">
            <!-- Cart Header -->
            <div class="text-white p-4 flex items-center justify-between" style="background-color: var(--primary); color: var(--secondary);">
                <h2 class="text-xl font-bold">
                    <i class="fas fa-shopping-cart mr-2"></i>Mon Panier
                </h2>
                <button id="close-cart" class="hover:opacity-80" style="color: var(--secondary);">
                    <i class="fas fa-times text-2xl"></i>
                </button>
            </div>
            
            <!-- Cart Items -->
            <div id="cart-items" class="flex-1 overflow-y-auto p-4">
                <div class="text-center text-gray-500 py-12">
                    <i class="fas fa-shopping-basket text-6xl mb-4"></i>
                    <p>Votre panier est vide</p>
                </div>
            </div>
            
            <!-- Cart Footer -->
            <div id="cart-footer" class="border-t border-gray-200 p-4 hidden">
                <div class="mb-4">
                    <div class="flex justify-between mb-2">
                        <span class="text-gray-600">Sous-total:</span>
                        <span id="cart-subtotal" class="font-semibold">0 FCFA</span>
                    </div>
                    <div class="flex justify-between text-lg font-bold">
                        <span>Total:</span>
                        <span id="cart-total" style="color: var(--primary);">0 FCFA</span>
                    </div>
                </div>
                
                <button id="checkout-btn" class="w-full text-white py-3 rounded-lg font-semibold transition mb-2" style="background-color: var(--primary); color: var(--secondary);">
                    Passer la commande
                </button>
                
                <button id="clear-cart-btn" class="w-full bg-gray-200 text-gray-700 py-2 rounded-lg hover:bg-gray-300 transition">
                    Vider le panier
                </button>
            </div>
        </div>
    </div>

    <!-- Overlay -->
    <div id="cart-overlay" class="fixed inset-0 bg-black bg-opacity-50 hidden z-40"></div>

    <!-- Checkout Modal -->
    <div id="checkout-modal" class="fixed inset-0 bg-black bg-opacity-50 hidden z-50 flex items-center justify-center p-4">
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
            <div class="text-white p-4 rounded-t-lg" style="background-color: var(--primary); color: var(--secondary);">
                <h3 class="text-xl font-bold">Finaliser la commande</h3>
            </div>
            
            <form id="checkout-form" class="p-6">
                <div class="mb-4">
                    <label class="block text-sm font-semibold text-gray-700 mb-2">
                        Nom ou pseudo <span class="text-red-500">*</span>
                    </label>
                    <input type="text" name="customer_name" id="customer_name" required
                           class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-pink-500">
                </div>
                
                <div class="mb-4">
                    <label class="block text-sm font-semibold text-gray-700 mb-2">
                        Numéro de téléphone <span class="text-red-500">*</span>
                    </label>
                    <input type="tel" name="customer_phone" id="customer_phone" required
                           class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-pink-500"
                           placeholder="Ex: 70 XX XX XX">
                </div>
                
                <div class="mb-4">
                    <label class="block text-sm font-semibold text-gray-700 mb-2">
                        Adresse de livraison <span class="text-red-500">*</span>
                    </label>
                    <textarea name="customer_address" id="customer_address" required rows="3"
                              class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-pink-500"></textarea>
                    
                    <!-- Bouton Géolocalisation -->
                    <button type="button" id="get-location-btn" class="mt-2 w-full bg-blue-500 text-white py-2 px-4 rounded-lg hover:bg-blue-600 transition flex items-center justify-center">
                        <i class="fas fa-map-marker-alt mr-2"></i>
                        <span id="location-btn-text">Récupérer ma position GPS</span>
                    </button>
                    
                    <!-- Champs cachés pour les coordonnées GPS -->
                    <input type="hidden" name="customer_latitude" id="customer_latitude">
                    <input type="hidden" name="customer_longitude" id="customer_longitude">
                    
                    <!-- Affichage des coordonnées -->
                    <div id="location-display" class="mt-2 p-2 bg-green-50 border border-green-200 rounded-lg hidden">
                        <p class="text-xs text-green-700">
                            <i class="fas fa-check-circle mr-1"></i>
                            <strong>Position GPS enregistrée</strong><br>
                            <span id="location-coords" class="text-gray-600"></span>
                        </p>
                    </div>
                </div>
                
                <div class="mb-6">
                    <label class="block text-sm font-semibold text-gray-700 mb-2">
                        Notes supplémentaires (optionnel)
                    </label>
                    <textarea name="customer_notes" id="customer_notes" rows="2"
                              class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-pink-500"></textarea>
                </div>
                
                <div class="flex space-x-3">
                    <button type="button" id="cancel-checkout" class="flex-1 bg-gray-200 text-gray-700 py-2 rounded-lg hover:bg-gray-300 transition">
                        Annuler
                    </button>
                    <button type="submit" class="flex-1 text-white py-2 rounded-lg transition" style="background-color: var(--primary); color: var(--secondary);">
                        Confirmer
                    </button>
                </div>
            </form>
        </div>
    </div>

    <!-- Product Details Modal -->
    <div id="details-modal" class="hidden fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
        <div class="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div class="p-6">
                <div class="flex justify-between items-start mb-4">
                    <h2 class="text-2xl font-bold" style="color: var(--secondary);" id="detail-name"></h2>
                    <button id="close-details" class="text-gray-500 hover:text-gray-700">
                        <i class="fas fa-times text-2xl"></i>
                    </button>
                </div>
                
                <div class="grid md:grid-cols-2 gap-6">
                    <div>
                        <img id="detail-image" src="" alt="" class="w-full h-64 object-cover rounded-lg">
                        <div class="mt-4">
                            <span class="text-xs font-semibold px-3 py-1 rounded-full" style="background-color: var(--primary); color: var(--secondary);" id="detail-category"></span>
                        </div>
                    </div>
                    
                    <div>
                        <div class="mb-4">
                            <h3 class="font-bold mb-2" style="color: var(--secondary);">Mode d'emploi</h3>
                            <p class="text-gray-700" id="detail-description"></p>
                        </div>
                        
                        <div class="mb-4">
                            <h3 class="font-bold mb-2" style="color: var(--secondary);">Volume / Poids</h3>
                            <p class="text-gray-700" id="detail-stock"></p>
                        </div>
                        
                        <div class="mb-6">
                            <h3 class="font-bold mb-2" style="color: var(--secondary);">Prix</h3>
                            <p class="text-3xl font-bold" style="color: var(--primary);" id="detail-price"></p>
                        </div>
                        
                        <button id="add-from-details" class="w-full text-white py-3 rounded-lg font-bold transition" style="background-color: var(--primary); color: var(--secondary);">
                            <i class="fas fa-cart-plus mr-2"></i>Ajouter au panier
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Success Modal -->
    <div id="success-modal" class="fixed inset-0 bg-black bg-opacity-50 hidden z-50 flex items-center justify-center p-4">
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full">
            <div class="p-6 text-center">
                <div class="text-green-500 mb-4">
                    <i class="fas fa-check-circle text-6xl"></i>
                </div>
                <h3 class="text-2xl font-bold text-gray-800 mb-2">Dernière étape !</h3>
                <p class="text-gray-600 mb-4">Votre commande <span id="order-number" class="font-semibold"></span> sera confirmée.</p>
                
                <div id="order-summary" class="bg-gray-50 p-4 rounded-lg mb-4 text-left text-sm"></div>
                
                <div class="flex flex-col space-y-2">
                    <a id="whatsapp-btn" href="#" target="_blank" 
                       class="bg-green-500 text-white py-3 rounded-lg font-semibold hover:bg-green-600 transition flex items-center justify-center">
                        <i class="fab fa-whatsapp text-xl mr-2"></i>
                        Confirmer votre commande
                    </a>
                    <button id="close-success" class="bg-gray-200 text-gray-700 py-2 rounded-lg hover:bg-gray-300 transition">
                        Fermer
                    </button>
                </div>
            </div>
        </div>
    </div>

    <script>
    $(document).ready(function() {
        const currency = '<?= STORE_INFO['currency'] ?>';
        let cart = {};
        
        // Load cart on page load
        loadCart();
        
        // Toggle cart sidebar
        $('#cart-toggle, #cart-overlay').on('click', function() {
            toggleCart();
        });
        
        $('#close-cart').on('click', function() {
            closeCart();
        });
        
        // Add to cart with toggle behavior
        $('.add-to-cart').on('click', function() {
            const $btn = $(this);
            const productId = $btn.data('product-id');
            const productName = $btn.data('product-name');
            const isInCart = $btn.hasClass('in-cart');
            
            if (isInCart) {
                // Remove from cart (button is green, make it yellow)
                removeFromCart(productId);
                $btn.removeClass('in-cart bg-green-500')
                    .css('background-color', 'var(--primary)')
                    .html('<i class="fas fa-cart-plus"></i>');
                showToast('Produit retiré du panier', 'warning');
            } else {
                // Add to cart (button is yellow, make it green)
                addToCart(productId, 1);
                $btn.addClass('in-cart bg-green-500')
                    .css('background-color', '#10b981')
                    .html('<i class="fas fa-check"></i>');
                showToast('✓ Produit ajouté ! Vérifiez votre panier', 'success');
                
                // Blink cart button
                $('#cart-toggle').addClass('cart-blink');
                setTimeout(() => {
                    $('#cart-toggle').removeClass('cart-blink');
                }, 1500);
            }
        });
        
        // Clear cart
        $('#clear-cart-btn').on('click', function() {
            if (confirm('Voulez-vous vraiment vider votre panier ?')) {
                clearCart();
            }
        });
        
        // Checkout
        $('#checkout-btn').on('click', function() {
            $('#checkout-modal').removeClass('hidden');
        });
        
        $('#cancel-checkout').on('click', function() {
            $('#checkout-modal').addClass('hidden');
        });
        
        // Submit checkout form
        $('#checkout-form').on('submit', function(e) {
            e.preventDefault();
            submitOrder();
        });
        
        // Close success modal
        $('#close-success').on('click', function() {
            $('#success-modal').addClass('hidden');
            location.reload();
        });
        
        // Géolocalisation GPS
        $('#get-location-btn').on('click', function() {
            const $btn = $(this);
            const $btnText = $('#location-btn-text');
            const $display = $('#location-display');
            const $coords = $('#location-coords');
            
            // Vérifier si la géolocalisation est supportée
            if (!navigator.geolocation) {
                alert('La géolocalisation n\'est pas supportée par votre navigateur.');
                return;
            }
            
            // Changer le bouton en mode chargement
            $btn.prop('disabled', true);
            $btnText.html('<i class="fas fa-spinner fa-spin mr-2"></i>Récupération en cours...');
            
            // Obtenir la position
            navigator.geolocation.getCurrentPosition(
                function(position) {
                    // Succès
                    const lat = position.coords.latitude;
                    const lon = position.coords.longitude;
                    const accuracy = Math.round(position.coords.accuracy);
                    
                    // Sauvegarder les coordonnées
                    $('#customer_latitude').val(lat);
                    $('#customer_longitude').val(lon);
                    
                    // Afficher les coordonnées
                    $coords.html(`Latitude: ${lat.toFixed(6)}, Longitude: ${lon.toFixed(6)}<br>Précision: ±${accuracy}m`);
                    $display.removeClass('hidden');
                    
                    // Changer le bouton en succès
                    $btn.removeClass('bg-blue-500 hover:bg-blue-600').addClass('bg-green-500');
                    $btnText.html('<i class="fas fa-check mr-2"></i>Position enregistrée');
                    
                    // Optionnel: Ajouter un lien Google Maps dans l'adresse
                    const mapsLink = `https://www.google.com/maps?q=${lat},${lon}`;
                    const currentAddress = $('#customer_address').val();
                    if (!currentAddress.includes('maps')) {
                        $('#customer_address').val(currentAddress + `\n\nLien Google Maps: ${mapsLink}`);
                    }
                },
                function(error) {
                    // Erreur
                    let errorMessage = 'Impossible de récupérer votre position.';
                    
                    switch(error.code) {
                        case error.PERMISSION_DENIED:
                            errorMessage = 'Vous devez autoriser l\'accès à votre position.';
                            break;
                        case error.POSITION_UNAVAILABLE:
                            errorMessage = 'Position non disponible.';
                            break;
                        case error.TIMEOUT:
                            errorMessage = 'Délai d\'attente dépassé.';
                            break;
                    }
                    
                    alert(errorMessage);
                    
                    // Réinitialiser le bouton
                    $btn.prop('disabled', false);
                    $btnText.html('<i class="fas fa-map-marker-alt mr-2"></i>Récupérer ma position GPS');
                },
                {
                    enableHighAccuracy: true,
                    timeout: 10000,
                    maximumAge: 0
                }
            );
        });
        
        /**
         * Show toast notification
         */
        function showToast(message, type = 'success') {
            const bgColor = type === 'success' ? '#10b981' : '#f59e0b';
            const $toast = $(`
                <div class="toast-notification" style="background-color: ${bgColor};">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <i class="fas fa-${type === 'success' ? 'check-circle' : 'info-circle'}" style="font-size: 20px;"></i>
                        <div>
                            <div style="font-weight: bold; margin-bottom: 2px;">${message}</div>
                            <div style="font-size: 12px; opacity: 0.9;">Cliquez sur le panier pour voir</div>
                        </div>
                    </div>
                </div>
            `);
            
            $('body').append($toast);
            
            // Auto-hide after 3 seconds
            setTimeout(() => {
                $toast.addClass('hiding');
                setTimeout(() => $toast.remove(), 300);
            }, 3000);
        }
        
        /**
         * Toggle cart sidebar
         */
        function toggleCart() {
            const isOpen = !$('#cart-sidebar').hasClass('translate-x-full');
            if (isOpen) {
                closeCart();
            } else {
                openCart();
            }
        }
        
        function openCart() {
            $('#cart-sidebar').removeClass('translate-x-full');
            $('#cart-overlay').removeClass('hidden');
        }
        
        function closeCart() {
            $('#cart-sidebar').addClass('translate-x-full');
            $('#cart-overlay').addClass('hidden');
        }
        
        /**
         * Load cart from server
         */
        function loadCart() {
            $.ajax({
                url: 'cart_handler.php',
                type: 'POST',
                data: { action: 'get_cart' },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        cart = response.cart;
                        updateCartDisplay(response.summary);
                    }
                }
            });
        }
        
        /**
         * Add product to cart
         */
        function addToCart(productId, quantity) {
            $.ajax({
                url: 'cart_handler.php',
                type: 'POST',
                data: {
                    action: 'add',
                    product_id: productId,
                    quantity: quantity
                },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        cart = response.cart;
                        updateCartDisplay(response.summary);
                        // Removed openCart() - sidebar no longer auto-opens
                    } else {
                        alert(response.message);
                    }
                }
            });
        }
        
        /**
         * Remove from cart
         */
        function removeFromCart(productId) {
            $.ajax({
                url: 'cart_handler.php',
                type: 'POST',
                data: {
                    action: 'remove',
                    product_id: productId,
                    quantity: 1
                },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        cart = response.cart;
                        updateCartDisplay(response.summary);
                    }
                }
            });
        }
        
        /**
         * Update quantity
         */
        function updateQuantity(productId, quantity) {
            $.ajax({
                url: 'cart_handler.php',
                type: 'POST',
                data: {
                    action: 'update_quantity',
                    product_id: productId,
                    quantity: quantity
                },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        cart = response.cart;
                        updateCartDisplay(response.summary);
                    }
                }
            });
        }
        
        /**
         * Clear cart
         */
        function clearCart() {
            $.ajax({
                url: 'cart_handler.php',
                type: 'POST',
                data: { action: 'clear' },
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        cart = {};
                        updateCartDisplay(response.summary);
                    }
                }
            });
        }
        
        /**
         * Update cart display
         */
        function updateCartDisplay(summary) {
            // Update cart count
            $('#cart-count').text(summary.count).addClass('cart-badge');
            setTimeout(() => $('#cart-count').removeClass('cart-badge'), 300);
            
            // Update cart items
            const $cartItems = $('#cart-items');
            if (summary.count === 0) {
                $cartItems.html(`
                    <div class="text-center text-gray-500 py-12">
                        <i class="fas fa-shopping-basket text-6xl mb-4"></i>
                        <p>Votre panier est vide</p>
                    </div>
                `);
                $('#cart-footer').addClass('hidden');
            } else {
                let itemsHtml = '<div class="space-y-3">';
                summary.items.forEach(item => {
                    itemsHtml += `
                        <div class="flex items-center space-x-3 bg-gray-50 p-3 rounded-lg">
                            <div class="flex-1">
                                <h4 class="font-semibold text-sm">${escapeHtml(item.nom)}</h4>
                                <p class="text-xs text-gray-600">${formatPrice(item.prix)}</p>
                            </div>
                            <div class="flex items-center space-x-2">
                                <button class="cart-decrease bg-gray-200 w-8 h-8 rounded flex items-center justify-center hover:bg-gray-300"
                                        data-product-id="${item.id}">
                                    <i class="fas fa-minus text-xs"></i>
                                </button>
                                <span class="w-8 text-center font-semibold">${item.quantity}</span>
                                <button class="cart-increase text-white w-8 h-8 rounded flex items-center justify-center transition"
                                        style="background-color: var(--primary); color: var(--secondary);"
                                        data-product-id="${item.id}">
                                    <i class="fas fa-plus text-xs"></i>
                                </button>
                            </div>
                        </div>
                    `;
                });
                itemsHtml += '</div>';
                $cartItems.html(itemsHtml);
                
                // Update totals
                $('#cart-subtotal').text(formatPrice(summary.subtotal));
                $('#cart-total').text(formatPrice(summary.total));
                $('#cart-footer').removeClass('hidden');
                
                // Bind cart item buttons
                $('.cart-increase').on('click', function() {
                    const productId = $(this).data('product-id');
                    addToCart(productId, 1);
                });
                
                $('.cart-decrease').on('click', function() {
                    const productId = $(this).data('product-id');
                    removeFromCart(productId);
                });
            }
        }
        
        /**
         * Submit order
         */
        function submitOrder() {
            const formData = $('#checkout-form').serialize();
            
            $.ajax({
                url: 'process_order.php',
                type: 'POST',
                data: formData + '&action=checkout',
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        // Hide checkout modal
                        $('#checkout-modal').addClass('hidden');
                        
                        // Show success modal
                        $('#order-number').text(response.order_id);
                        
                        // Build order summary
                        let summaryHtml = '';
                        response.ticket.items.forEach(item => {
                            summaryHtml += `<p>${item.quantity}x ${escapeHtml(item.name)} - ${formatPrice(item.price * item.quantity)}</p>`;
                        });
                        summaryHtml += `<p class="font-bold mt-2">Total: ${formatPrice(response.ticket.total)}</p>`;
                        $('#order-summary').html(summaryHtml);
                        
                        // Set WhatsApp link
                        if (response.whatsapp_link) {
                            $('#whatsapp-btn').attr('href', response.whatsapp_link);
                        }
                        
                        $('#success-modal').removeClass('hidden');
                        
                        // Clear cart
                        clearCart();
                        closeCart();
                    } else {
                        alert('Erreur: ' + response.message);
                    }
                },
                error: function() {
                    alert('Erreur lors de l\'envoi de la commande');
                }
            });
        }
        
        /**
         * Format price
         */
        function formatPrice(price) {
            return new Intl.NumberFormat('fr-FR').format(price) + ' ' + currency;
        }
        
        /**
         * Escape HTML
         */
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
        
        // Product Details Modal
        let currentDetailProduct = null;
        
        // Ouvrir détails en cliquant sur l'image
        $(document).on('click', '.view-details-trigger', function(e) {
            e.stopPropagation();
            const product = $(this).data('product');
            openProductDetails(product);
        });
        
        // Ouvrir détails en cliquant sur le bouton oeil
        $(document).on('click', '.view-details', function(e) {
            e.stopPropagation();
            const product = $(this).data('product');
            openProductDetails(product);
        });
        
        function openProductDetails(product) {
            currentDetailProduct = product;
            
            $('#detail-name').text(product.nom);
            $('#detail-category').text(product.categorie);
            $('#detail-description').text(product.description || 'Non spécifié');
            $('#detail-stock').text(product.stock || 'Non spécifié');
            $('#detail-price').text(formatPrice(product.prix));
            
            if (product.image_url) {
                $('#detail-image').attr('src', product.image_url).show();
            } else {
                $('#detail-image').hide();
            }
            
            $('#details-modal').removeClass('hidden');
        }
        
        $('#close-details').click(function() {
            $('#details-modal').addClass('hidden');
        });
        
        $('#details-modal').click(function(e) {
            if (e.target === this) {
                $('#details-modal').addClass('hidden');
            }
        });
        
        $('#add-from-details').click(function() {
            if (currentDetailProduct) {
                addToCart(currentDetailProduct.id, currentDetailProduct.nom, currentDetailProduct.description, currentDetailProduct.prix, currentDetailProduct.image_url);
                $('#details-modal').addClass('hidden');
            }
        });
    });
    </script>
</body>
</html>