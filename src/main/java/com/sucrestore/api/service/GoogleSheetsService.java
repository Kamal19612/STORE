package com.sucrestore.api.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.sucrestore.api.config.GoogleConfig;
import com.sucrestore.api.dto.ImportSummary;
import com.sucrestore.api.dto.ProductRequest;
import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.entity.AppSetting;
import com.sucrestore.api.repository.ProductRepository;
import com.sucrestore.api.repository.AppSettingRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleSheetsService {

    @Autowired
    private GoogleConfig googleConfig;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AppSettingRepository appSettingRepository;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);

    /**
     * Initialise et retourne le service Sheets API.
     */
    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Charger les crédentials depuis le fichier JSON (supporte classpath: et chemins fichiers)
        java.io.InputStream credentialsStream;
        String path = googleConfig.getCredentialsFilePath();

        if (path.startsWith("classpath:")) {
            // Charger depuis le classpath (resources)
            String resourcePath = path.replace("classpath:", "");
            credentialsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (credentialsStream == null) {
                throw new IOException("Fichier credentials non trouvé dans le classpath: " + resourcePath);
            }
            log.info("Credentials chargés depuis le classpath: {}", resourcePath);
        } else {
            // Charger depuis le système de fichiers
            credentialsStream = new FileInputStream(path);
            log.info("Credentials chargés depuis le fichier: {}", path);
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(SCOPES);

        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(googleConfig.getApplicationName())
                .build();
    }

    /**
     * Récupère les valeurs brutes d'une plage donnée.
     */
    public List<List<Object>> getSpreadsheetValues(String spreadsheetId, String range) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return response.getValues();
    }

    /**
     * Orchestre la récupération et l'importation des produits depuis le Sheet
     * configuré.
     */
    public ImportSummary fetchProducts(String spreadsheetId) {
        long startTime = System.currentTimeMillis();
        ImportSummary summary = new ImportSummary();
        java.util.Set<String> sheetExternalIds = new java.util.HashSet<>();

        String finalSpreadsheetId = null;
        if (spreadsheetId != null && !spreadsheetId.isBlank()) {
            finalSpreadsheetId = spreadsheetId.trim();
        } else {
            // Priorité : paramètre > base de données (app_settings) > application.yml
            finalSpreadsheetId = appSettingRepository.findByKey("google_sheet_id")
                    .map(AppSetting::getValue)
                    .filter(v -> !v.isBlank())
                    .orElse(googleConfig.getSpreadsheetId());
        }

        log.info("🚀 Début synchronisation Google Sheets (ID: {})", finalSpreadsheetId);

        if (finalSpreadsheetId == null || finalSpreadsheetId.isEmpty()) {
            summary.addError(0, "ID du Google Sheet non configuré (ni dans la requête, ni en base de données, ni dans application.yml)");
            log.error("Import échoué: Spreadsheet ID manquant");
            return summary;
        }

        // Sauvegarder l'ID utilisé pour les futures synchronisations automatiques
        final String idToSave = finalSpreadsheetId;
        AppSetting sheetSetting = appSettingRepository.findByKey("google_sheet_id")
                .orElse(AppSetting.builder().key("google_sheet_id").description("ID du Google Sheet pour la synchronisation produits").build());
        sheetSetting.setValue(idToSave);
        appSettingRepository.save(sheetSetting);

        // Essayer plusieurs noms d'onglets possibles pour trouver les produits
        // Format: "NomFeuille!A:H" pour spécifier une feuille spécifique
        String[] possibleSheetNames = {"Produits!A:H", "PRODUITS!A:H", "Products!A:H", "Feuille 1!A:H", "Sheet1!A:H", "A:H"};
        String range = "Produits!A:H"; // Par défaut, on essaie "Produits"

        log.info("🔄 Démarrage de la synchronisation Google Sheets...");
        log.info("📋 Spreadsheet ID: {}, Range: {}", finalSpreadsheetId, range);

        try {
            List<List<Object>> values = getSpreadsheetValues(finalSpreadsheetId, range);

            if (values == null || values.isEmpty()) {
                log.warn("⚠️ Le sheet est vide ou inaccessible (Range: {})", range);
                summary.addError(0, "Sheet vide ou inaccessible");
                return summary;
            }

            log.info("📊 {} lignes trouvées dans le Sheet", values.size());

            int rowNum = 0;
            int processedCount = 0;
            int skippedCount = 0;
            for (List<Object> row : values) {
                rowNum++;
                if (rowNum == 1) {
                    log.info("📋 En-tête du Sheet: {}", row);
                    continue; // Skip Header
                }
                // Log des 3 premières lignes de données pour déboguer
                if (rowNum <= 4) {
                    log.info("🔍 [DEBUG] Ligne {}: {}", rowNum, row);
                }
                try {
                    String externalId = SafeGet(row, 0);
                    if (externalId != null && !externalId.isEmpty()) {
                        sheetExternalIds.add(externalId);
                    }

                    // Log toutes les 50 lignes pour suivre la progression
                    if (rowNum % 50 == 0) {
                        log.info("📊 Progression: {} lignes traitées...", rowNum);
                    }

                    boolean isNew = processRow(row, summary, rowNum);
                    processedCount++;
                    // Compter créations vs mises à jour
                    if (isNew) {
                        summary.incrementCreated();
                    } else {
                        summary.incrementUpdated();
                    }
                } catch (RuntimeException e) {
                    String errorMsg = "Erreur ligne " + rowNum + ": " + e.getMessage();
                    summary.addError(rowNum, errorMsg);
                    log.error("❌ " + errorMsg, e);
                    skippedCount++;
                }
            }

            log.info("📈 Traitement terminé: {} lignes traitées avec succès, {} ignorées/erreurs", processedCount, skippedCount);

            // Désactiver les produits qui ne sont plus dans le Sheet
            deactivateDeletedProducts(sheetExternalIds, summary);

        } catch (IOException | GeneralSecurityException e) {
            log.error("Erreur Google Sheets", e);
            summary.addError(0, "Erreur API Google Sheets: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("⏱️ Synchronisation terminée en {}ms", duration);

        return summary;
    }

    private boolean processRow(List<Object> row, ImportSummary summary, int rowNum) {
        summary.incrementTotal();

        // Mapping USER: ID, Photo, Nom, Mode d'emploi, Volume_poids, Categorie, Disponibilité, Prix
        // Index:      0   1      2    3                4             5          6              7
        String externalId = SafeGet(row, 0); // NOUVEAU: Extraction de l'ID externe
        String imageUrl = SafeGet(row, 1);
        String name = SafeGet(row, 2);
        String description = SafeGet(row, 3); // Mode d'emploi
        String volumeWeight = SafeGet(row, 4); // Volume_poids (ex: "50ml", "100g")
        String categoryName = SafeGet(row, 5);
        String availabilityStr = SafeGet(row, 6);
        String priceStr = SafeGet(row, 7);

        log.info("📝 [ROW {}] Processing: ID={}, Name={}, Category={}, Price={}, Availability={}",
            rowNum, externalId, name, categoryName, priceStr, availabilityStr);

        if (name.isEmpty()) {
            // Parfois l'ID est là mais pas le nom, on ignore
            if (SafeGet(row, 0).isEmpty()) {
                log.trace("Ligne {}: Ligne vide ignorée", rowNum);
                return false; // Ligne vide
            }
            summary.addError(rowNum, "Nom du produit obligatoire");
            log.warn("Ligne {}: Nom manquant.", rowNum);
            return false;
        }

        // Si catégorie vide, on met "Divers" par défaut ? Non, erreur pour l'instant
        if (categoryName.isEmpty()) {
            categoryName = "Divers"; // Fallback
        }

        try {
            ProductRequest request = new ProductRequest();
            request.setName(name);
            request.setCategoryName(categoryName);

            // Parsing Prix
            String cleanPrice = priceStr.replace(",", ".").replaceAll("[^0-9.]", "");
            request.setPrice(cleanPrice.isEmpty() ? BigDecimal.ZERO : new BigDecimal(cleanPrice));
            log.debug("💰 [ROW {}] Prix parsé: {} -> {}", rowNum, priceStr, request.getPrice());

            request.setDescription(description);
            request.setVolumeWeight(volumeWeight); // Volume/Poids depuis colonne E
            // Description courte générée automatiquement à partir de description si vide
            request.setShortDescription(
                    description.length() > 100 ? description.substring(0, 97) + "..." : description
            );

            // Parsing Disponibilité / Stock
            // Si c'est un nombre, c'est le stock. Si c'est du texte "En stock", on met 10, sinon 0.
            int stock = 0;
            if (availabilityStr.matches(".*\\d.*")) {
                // Contient des chiffres
                String cleanStock = availabilityStr.replaceAll("[^0-9]", "");
                stock = cleanStock.isEmpty() ? 0 : Integer.parseInt(cleanStock);
            } else {
                // Texte
                String lowerAvailability = availabilityStr.toLowerCase();
                if (lowerAvailability.contains("indisponible") || lowerAvailability.contains("rupture") || lowerAvailability.contains("non")) {
                    stock = 0;
                } else if (lowerAvailability.contains("stock") || lowerAvailability.contains("disponible") || lowerAvailability.contains("oui")) {
                    stock = 100; // Valeur arbitraire pour "En stock"
                }
            }
            request.setStock(stock);
            request.setActive(stock > 0);
            log.debug("📦 [ROW {}] Stock parsé: {} -> {} (active={})", rowNum, availabilityStr, stock, request.isActive());

            // Slug generation
            String slug = name.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
            request.setSlug(slug);

            log.info("🚀 [ROW {}] Appel importProduct pour: {} (ID={}, Stock={}, Active={})",
                rowNum, name, externalId, stock, request.isActive());

            // MODIFIÉ: Passer l'externalId au service
            ProductResponse savedProduct = productService.importProduct(request, imageUrl, externalId);
            log.info("✅ [ROW {}] Produit importé: {} (ID externe: {}, Slug: {}, Created: {})",
                rowNum, savedProduct.getName(), externalId, savedProduct.getSlug(), savedProduct.isCreated());
            summary.incrementSuccess();

            // Retourner vrai si c'est une création, faux si c'est une mise à jour
            return savedProduct.isCreated();

        } catch (RuntimeException e) {
            log.error("❌ [ROW {}] Erreur traitement: {}", rowNum, e.getMessage(), e);
            throw new RuntimeException("Erreur traitement: " + e.getMessage(), e);
        }
    }

    /**
     * Désactive tous les produits qui ne sont plus présents dans le Google
     * Sheet.
     */
    private void deactivateDeletedProducts(java.util.Set<String> sheetExternalIds, ImportSummary summary) {
        log.info("🔍 Vérification des produits supprimés du Sheet...");

        // Récupérer tous les produits actifs
        java.util.List<Product> activeProducts = productRepository.findByActiveTrue();

        int deactivatedCount = 0;
        for (Product product : activeProducts) {
            // Si le produit a un externalId ET qu'il n'est plus dans le Sheet
            if (product.getExternalId() != null
                    && !product.getExternalId().isEmpty()
                    && !sheetExternalIds.contains(product.getExternalId())) {

                product.setActive(false);
                productRepository.save(product);
                summary.incrementDeactivated();
                deactivatedCount++;
                log.info("❌ Produit désactivé (supprimé du Sheet): {} (ID externe: {})",
                        product.getName(), product.getExternalId());
            }
        }

        if (deactivatedCount == 0) {
            log.info("✅ Aucun produit à désactiver");
        } else {
            log.info("✅ {} produits désactivés", deactivatedCount);
        }
    }

    private String SafeGet(List<Object> row, int index) {
        if (index >= row.size()) {
            return "";
        }
        Object val = row.get(index);
        return val == null ? "" : val.toString().trim();
    }

    /**
     * Tâche planifiée : Importe les produits automatiquement. Le délai est
     * configurable via 'google.sheets.sync-rate' (défaut: 600000ms = 10 min).
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRateString = "${google.sheets.sync-rate:60000}")
    public void importProductsScheduled() {
        // Vérifier si la synchronisation est activée en base de données
        boolean isEnabled = appSettingRepository.findByKey("google_sheet_sync_enabled")
                .map(s -> !"false".equalsIgnoreCase(s.getValue()))
                .orElse(true);

        if (!isEnabled) {
            log.info("⏸️ Synchronisation automatique ignorée (désactivée dans les paramètres)");
            return;
        }

        log.info("🔄 Démarrage de la synchronisation automatique Google Sheets...");
        ImportSummary summary = fetchProducts(null); // Utilise l'ID par défaut

        // Logs détaillés des résultats
        log.info("📊 Résultats de la synchronisation:");
        log.info("   ✅ Créations: {}", summary.getCreatedCount());
        log.info("   🔄 Mises à jour: {}", summary.getUpdatedCount());
        log.info("   ❌ Désactivations: {}", summary.getDeactivatedCount());
        log.info("   ⚠️ Erreurs: {}", summary.getErrorCount());

        if (summary.getErrorCount() > 0) {
            log.warn("⚠️ Synchronisation terminée avec {} erreurs", summary.getErrorCount());
        } else {
            log.info("✅ Synchronisation réussie: {} produits traités", summary.getTotalProcessed());
        }
    }
}
