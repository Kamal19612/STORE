package com.sucrestore.api.service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.sucrestore.api.config.GoogleConfig;
import com.sucrestore.api.dto.ImportSummary;
import com.sucrestore.api.dto.ProductRequest;
import com.sucrestore.api.dto.ProductResponse;
import com.sucrestore.api.entity.Product;
import com.sucrestore.api.entity.AppSetting;
import com.sucrestore.api.entity.Store;
import com.sucrestore.api.repository.ProductRepository;
import com.sucrestore.api.repository.AppSettingRepository;
import com.sucrestore.api.repository.StoreRepository;
import com.sucrestore.api.tenant.StoreContext;
import com.sucrestore.api.tenant.StoreResolverService;

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

    @Autowired
    private StoreRepository storeRepository;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // NOTE: utilisé aussi pour l'export (écriture) des commandes.
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private static final Pattern SPREADSHEET_ID_IN_URL = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)");
    private static final Pattern SPREADSHEET_ID_PLAIN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    /** « non » comme mot entier (évite faux positifs type « mignon », « pignon »). */
    private static final Pattern AVAILABILITY_NEGATIVE_NON = Pattern.compile("(?i)\\bnon\\b");

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

    private RestTemplate createSheetsImportRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(googleConfig.getHttpConnectTimeoutMs());
        f.setReadTimeout(googleConfig.getHttpReadTimeoutMs());
        return new RestTemplate(f);
    }

    /**
     * Extrait l’ID du classeur depuis une URL de partage ou valide un ID nu.
     */
    static String normalizeSpreadsheetId(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        var m = SPREADSHEET_ID_IN_URL.matcher(t);
        if (m.find()) {
            return m.group(1);
        }
        if (SPREADSHEET_ID_PLAIN.matcher(t).matches()) {
            return t;
        }
        return null;
    }

    private String importMode() {
        String m = googleConfig.getProductImportMode();
        return m == null || m.isBlank() ? "auto" : m.trim().toLowerCase(Locale.ROOT);
    }

    private Long resolveSheetGid(Long requestGid) {
        if (requestGid != null) {
            return requestGid;
        }
        return appSettingRepository.findByKey("google_sheet_gid")
            .map(AppSetting::getValue)
            .filter(v -> !v.isBlank())
            .map(v -> {
                try {
                    return Long.parseLong(v.trim());
                } catch (NumberFormatException e) {
                    log.warn("app_settings.google_sheet_gid invalide (nombre attendu): {}", v);
                    return null;
                }
            })
            .orElse(googleConfig.getCsvExportSheetGid());
    }

    /**
     * Télécharge le Sheet en CSV via l’URL d’export publique (aucun jeton OAuth).
     * Fonctionne si le fichier est accessible sans compte Google (ex. « Toute personne disposant du lien » en lecteur).
     */
    private List<List<Object>> fetchValuesViaPublicCsvExport(String spreadsheetId, Long sheetGid)
            throws IOException, CsvException {
        StringBuilder url = new StringBuilder("https://docs.google.com/spreadsheets/d/")
            .append(spreadsheetId)
            .append("/export?format=csv");
        if (sheetGid != null) {
            url.append("&gid=").append(sheetGid);
        }
        String urlStr = url.toString();
        log.info("Téléchargement export CSV Google (sans API): {}", urlStr);

        RestTemplate rt = createSheetsImportRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "SucreStore-ProductImport/1.0");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response;
        try {
            response = rt.exchange(urlStr, HttpMethod.GET, entity, byte[].class);
        } catch (HttpStatusCodeException e) {
            throw new IOException("HTTP " + e.getStatusCode().value() + " sur l’export CSV: " + e.getStatusText(), e);
        } catch (RestClientException e) {
            throw new IOException("Erreur réseau lors du téléchargement CSV: " + e.getMessage(), e);
        }

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new IOException("Réponse CSV vide (HTTP " + response.getStatusCode().value() + ")");
        }

        int probe = Math.min(512, body.length);
        String head = new String(body, 0, probe, StandardCharsets.UTF_8).trim();
        if (head.startsWith("<!DOCTYPE") || head.startsWith("<html") || head.startsWith("<HTML")) {
            throw new IOException(
                "La réponse n’est pas du CSV (page HTML). Le fichier n’est probablement pas en « Toute personne disposant du lien » "
                    + "ou l’export est bloqué. Partagez en lecture publique par lien, ou donnez accès au compte de service (fichier JSON) "
                    + "et utilisez product-import-mode=api.");
        }

        return parseCsvBytesToGrid(body);
    }

    private static List<List<Object>> parseCsvBytesToGrid(byte[] body) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            List<List<Object>> grid = new ArrayList<>(rows.size());
            for (String[] row : rows) {
                List<Object> line = new ArrayList<>(row.length);
                for (String cell : row) {
                    line.add(cell);
                }
                grid.add(line);
            }
            return grid;
        }
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
     * Efface une plage (ou une feuille via "NomFeuille!A:Z").
     */
    public void clearValues(String spreadsheetId, String range) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        service.spreadsheets().values()
            .clear(spreadsheetId, range, new ClearValuesRequest())
            .execute();
    }

    /**
     * Écrit des valeurs dans une plage.
     */
    public UpdateValuesResponse updateValues(String spreadsheetId, String range, List<List<Object>> values)
            throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        ValueRange body = new ValueRange().setValues(values);
        return service.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute();
    }

    /**
     * Orchestre la récupération et l'importation des produits depuis le Sheet
     * configuré (même comportement qu’avant : pas de {@code sheetGid} explicite).
     */
    public ImportSummary fetchProducts(String spreadsheetId) {
        return fetchProducts(spreadsheetId, null);
    }

    /**
     * @param sheetGid identifiant d’onglet ({@code gid} dans l’URL du Sheet) pour l’export CSV ; optionnel (1er onglet).
     */
    public ImportSummary fetchProducts(String spreadsheetId, Long sheetGid) {
        long startTime = System.currentTimeMillis();
        ImportSummary summary = new ImportSummary();
        java.util.Set<String> sheetExternalIds = new java.util.HashSet<>();

        String rawId = null;
        if (spreadsheetId != null && !spreadsheetId.isBlank()) {
            rawId = spreadsheetId.trim();
        } else {
            rawId = appSettingRepository.findByKey("google_sheet_id")
                    .map(AppSetting::getValue)
                    .filter(v -> !v.isBlank())
                    .orElse(googleConfig.getSpreadsheetId());
        }

        String finalSpreadsheetId = normalizeSpreadsheetId(rawId);
        log.info("Début synchronisation Google Sheets (mode={}, ID normalisé={})",
            importMode(), finalSpreadsheetId);

        if (finalSpreadsheetId == null || finalSpreadsheetId.isEmpty()) {
            summary.addError(0,
                "ID du Google Sheet invalide ou non configuré. Utilisez l’ID du classeur ou une URL "
                    + "https://docs.google.com/spreadsheets/d/…/edit (ni dans la requête, ni en base, ni dans application.yml).");
            log.error("Import échoué: Spreadsheet ID manquant ou non reconnu (entrée: {})", rawId);
            return summary;
        }

        final String idToSave = finalSpreadsheetId;
        AppSetting sheetSetting = appSettingRepository.findByKey("google_sheet_id")
                .orElse(AppSetting.builder().key("google_sheet_id").description("ID du Google Sheet pour la synchronisation produits").build());
        sheetSetting.setValue(idToSave);
        appSettingRepository.save(sheetSetting);

        Long resolvedGid = resolveSheetGid(sheetGid);
        String mode = importMode();

        String[] possibleRanges = {
            "Produits!A:H",
            "PRODUITS!A:H",
            "Products!A:H",
            "Feuille 1!A:H",
            "Feuille1!A:H",
            "Sheet1!A:H",
            "A:H"
        };
        String range = possibleRanges[0];

        log.info("Synchronisation produits: spreadsheetId={}, sheetGid={}, importMode={}", finalSpreadsheetId, resolvedGid, mode);

        try {
            List<List<Object>> values = null;
            String sourceLabel = null;
            Exception csvFailure = null;
            Exception apiFailure = null;

            boolean tryCsv = "auto".equals(mode) || "csv".equals(mode);
            boolean tryApi = "auto".equals(mode) || "api".equals(mode);

            if (tryCsv) {
                try {
                    values = fetchValuesViaPublicCsvExport(finalSpreadsheetId, resolvedGid);
                    sourceLabel = "export CSV public (gid=" + resolvedGid + ")";
                } catch (IOException | CsvException e) {
                    csvFailure = e;
                    log.warn("Export CSV public indisponible ou invalide: {}", e.getMessage());
                }
            }

            if ((values == null || values.isEmpty()) && tryApi) {
                log.info("Lecture via API Google Sheets (compte de service)…");
                for (String candidateRange : possibleRanges) {
                    try {
                        log.info("Tentative API avec range: {}", candidateRange);
                        List<List<Object>> candidateValues = getSpreadsheetValues(finalSpreadsheetId, candidateRange);
                        if (candidateValues != null && !candidateValues.isEmpty()) {
                            values = candidateValues;
                            range = candidateRange;
                            sourceLabel = "API Google Sheets, range=" + candidateRange;
                            break;
                        }
                    } catch (IOException | GeneralSecurityException e) {
                        apiFailure = e;
                        log.warn("Échec lecture API range {}: {}", candidateRange, e.getMessage());
                    }
                }
            }

            if (values == null || values.isEmpty()) {
                StringBuilder hint = new StringBuilder();
                if ("csv".equals(mode) && csvFailure != null) {
                    hint.append("CSV: ").append(csvFailure.getMessage());
                } else if ("api".equals(mode) && apiFailure != null) {
                    hint.append("API: ").append(apiFailure.getMessage())
                        .append(" — Vérifiez credentials.json et que le compte de service a accès au fichier.");
                } else {
                    if (csvFailure != null) {
                        hint.append("CSV: ").append(csvFailure.getMessage()).append(". ");
                    }
                    if (apiFailure != null) {
                        hint.append("API: ").append(apiFailure.getMessage());
                    }
                    if (hint.length() == 0) {
                        hint.append("Aucune donnée (sheet vide sur les plages testées ou export CSV vide).");
                    } else {
                        hint.append(" — Pour CSV sans compte Google : partage « Toute personne disposant du lien » (lecteur). "
                            + "Pour l’API : partager le Sheet avec l’e-mail du compte de service du fichier JSON.");
                    }
                }
                summary.addError(0, "Impossible de lire le Sheet. " + hint);
                log.error("Import produits annulé. {}", hint);
                return summary;
            }

            consumeProductRows(values, sourceLabel != null ? sourceLabel : range, summary, sheetExternalIds);
            deactivateDeletedProducts(sheetExternalIds, summary);

        } catch (Exception e) {
            log.error("Erreur Google Sheets (inattendue)", e);
            summary.addError(0, "Erreur Google Sheets: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Synchronisation terminée en {}ms", duration);

        return summary;
    }

    /**
     * Résultat du parsing de la colonne « Disponibilité » (aligné PHP + séparation rupture / fiche inactive).
     */
    private record SheetDisponibiliteParse(int stock, boolean purchaseAllowed, boolean catalogActive) {}

    /**
     * Colonne « Disponibilité » du sheet :
     * <ul>
     *   <li>Valeurs PHP « vrai » : DISPONIBLE, OUI, 1, TRUE (et équivalents) → vente autorisée ; stock par défaut 100 ou 1 si cellule "1".</li>
     *   <li>Rupture / épuisé / hors stock → stock 0 mais {@code purchaseAllowed true} (réassort possible).</li>
     *   <li>INACTIF / masqué / désactivé → fiche inactive + pas de vente.</li>
     *   <li>NON, FALSE, indisponible, « non » mot entier → pas de vente.</li>
     *   <li>Chiffres → stock numérique ; quantité 0 → pas de vente.</li>
     *   <li>Cellule vide → stock 100, vente autorisée (comportement historique).</li>
     * </ul>
     */
    private static SheetDisponibiliteParse parseSheetDisponibiliteColumn(String availabilityStr) {
        String raw = availabilityStr == null ? "" : availabilityStr.trim();
        if (raw.isEmpty()) {
            return new SheetDisponibiliteParse(100, true, true);
        }
        String lower = raw.toLowerCase(Locale.ROOT);

        if (lower.contains("inactif")
            || lower.contains("désactivé")
            || lower.contains("desactive")
            || lower.contains("masqué")
            || lower.contains("masque")) {
            return new SheetDisponibiliteParse(0, false, false);
        }

        if (lower.contains("rupture")
            || lower.contains("épuisé")
            || lower.contains("epuise")
            || lower.contains("hors stock")
            || lower.contains("hors-stock")
            || lower.contains("plus de stock")
            || lower.contains("stock épuisé")
            || lower.contains("stock epuise")) {
            return new SheetDisponibiliteParse(0, true, true);
        }

        if (lower.contains("indisponible")
            || AVAILABILITY_NEGATIVE_NON.matcher(lower).find()
            || lower.equals("false")
            || lower.equals("faux")
            || lower.equals("no")
            || lower.equals("0")
            || raw.matches("0+")) {
            return new SheetDisponibiliteParse(0, false, true);
        }

        if (lower.contains("disponible")
            || lower.equals("oui")
            || lower.equals("ok")
            || lower.equals("yes")
            || lower.equals("true")
            || lower.equals("1")
            || lower.contains("en stock")) {
            int stock = "1".equals(lower) ? 1 : 100;
            return new SheetDisponibiliteParse(stock, true, true);
        }

        if (raw.matches("\\d+")) {
            try {
                int n = Integer.parseInt(raw);
                return new SheetDisponibiliteParse(Math.max(0, n), n > 0, true);
            } catch (NumberFormatException e) {
                return new SheetDisponibiliteParse(0, false, true);
            }
        }

        if (lower.matches(".*\\d.*")) {
            String cleanStock = raw.replaceAll("[^0-9]", "");
            if (cleanStock.isEmpty()) {
                return new SheetDisponibiliteParse(0, false, true);
            }
            try {
                int n = Integer.parseInt(cleanStock);
                return new SheetDisponibiliteParse(Math.max(0, n), n > 0, true);
            } catch (NumberFormatException e) {
                return new SheetDisponibiliteParse(0, false, true);
            }
        }

        return new SheetDisponibiliteParse(100, true, true);
    }

    private void consumeProductRows(List<List<Object>> values, String sourceLabel, ImportSummary summary,
            java.util.Set<String> sheetExternalIds) {
        log.info("{} lignes lues ({})", values.size(), sourceLabel);

        int rowNum = 0;
        int processedCount = 0;
        int skippedCount = 0;
        for (List<Object> row : values) {
            rowNum++;
            if (rowNum == 1) {
                log.info("En-tête: {}", row);
                continue;
            }
            if (rowNum <= 4) {
                log.info("[DEBUG] Ligne {}: {}", rowNum, row);
            }
            try {
                String externalId = normalizeExternalIdKey(SafeGet(row, 0));
                if (!externalId.isEmpty()) {
                    sheetExternalIds.add(externalId);
                }
                if (rowNum % 50 == 0) {
                    log.info("Progression: {} lignes…", rowNum);
                }
                boolean isNew = processRow(row, summary, rowNum);
                processedCount++;
                if (isNew) {
                    summary.incrementCreated();
                } else {
                    summary.incrementUpdated();
                }
            } catch (RuntimeException e) {
                String errorMsg = "Erreur ligne " + rowNum + ": " + e.getMessage();
                summary.addError(rowNum, errorMsg);
                log.error(errorMsg, e);
                skippedCount++;
            }
        }
        log.info("Traitement terminé: {} lignes données, {} erreurs ligne", processedCount, skippedCount);
    }

    private boolean processRow(List<Object> row, ImportSummary summary, int rowNum) {
        summary.incrementTotal();

        // Mapping USER: ID, Photo, Nom, Mode d'emploi, Volume_poids, Categorie, Disponibilité, Prix
        // Index:      0   1      2    3                4             5          6              7
        String externalId = normalizeExternalIdKey(SafeGet(row, 0));
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

            SheetDisponibiliteParse disp = parseSheetDisponibiliteColumn(availabilityStr);
            request.setStock(disp.stock());
            request.setPurchaseAllowed(disp.purchaseAllowed());
            request.setActive(disp.catalogActive());
            log.debug("📦 [ROW {}] Disponibilité: raw={} → stock={}, purchaseAllowed={}, active={}",
                rowNum, availabilityStr, disp.stock(), disp.purchaseAllowed(), disp.catalogActive());

            // Slug : préfixer par l’ID externe si présent (évite deux lignes Sheet → même slug → une seule ligne en base).
            String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
            String slug;
            if (externalId != null && !externalId.isBlank()) {
                String idPart = externalId.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
                slug = idPart + "-" + baseSlug;
            } else {
                slug = baseSlug;
            }
            if (slug.length() > 200) {
                slug = slug.substring(0, 200);
            }
            request.setSlug(slug);

            log.info("🚀 [ROW {}] Appel importProduct pour: {} (ID={}, Stock={}, disponibleAPI={})",
                rowNum, name, externalId, disp.stock(), disp.purchaseAllowed() && disp.stock() > 0 && disp.catalogActive());

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
        Long storeId = StoreContext.getStoreIdOrNull();
        if (storeId == null) {
            log.warn("Désactivation des produits absents du Sheet ignorée : aucun magasin (StoreContext) — "
                + "évite d’affecter plusieurs boutiques (ex. tâche planifiée sans tenant).");
            return;
        }

        if (sheetExternalIds == null || sheetExternalIds.isEmpty()) {
            log.info("Désactivation des absents ignorée : aucun ID externe (colonne A) non vide sur le Sheet — "
                + "sinon des produits encore listés pourraient être archivés par erreur.");
            return;
        }

        log.info("Vérification des produits supprimés du Sheet (storeId={})…", storeId);

        java.util.List<Product> activeProducts = productRepository.findByActiveTrueAndStoreId(storeId);

        int deactivatedCount = 0;
        for (Product product : activeProducts) {
            String dbKey = normalizeExternalIdKey(product.getExternalId());
            if (!dbKey.isEmpty() && !sheetExternalIds.contains(dbKey)) {

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
     * Google Sheets renvoie souvent la colonne A comme nombre (Double) → "1.0" alors qu’en base on a "1".
     * Sans normalisation, {@code sheetExternalIds} ne matche pas → produits désactivés à tort après import.
     */
    static String normalizeExternalIdKey(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        try {
            if (t.matches("^-?\\d+(\\.0+)?$")) {
                return String.valueOf((long) Double.parseDouble(t));
            }
        } catch (NumberFormatException ignored) {
            // garder t
        }
        return t;
    }

    /**
     * Tâche planifiée : importe les produits automatiquement.
     * Intervalle : {@code google.sheets.sync-rate} (défaut 60000 ms = 1 minute dans application.yml).
     */
    @org.springframework.scheduling.annotation.Scheduled(
        fixedDelayString = "${google.sheets.sync-rate:60000}",
        initialDelayString = "${google.sheets.sync-initial-delay:15000}")
    public void importProductsScheduled() {
        // Vérifier si la synchronisation est activée en base de données
        boolean isEnabled = appSettingRepository.findByKey("google_sheet_sync_enabled")
                .map(s -> !"false".equalsIgnoreCase(s.getValue()))
                .orElse(true);

        if (!isEnabled) {
            log.info("⏸️ Synchronisation automatique ignorée (désactivée dans les paramètres)");
            return;
        }

        Store defaultStore = storeRepository.findByCode(StoreResolverService.DEFAULT_STORE_CODE).orElse(null);
        if (defaultStore == null) {
            log.error(
                "Synchronisation Google Sheets annulée : magasin par défaut introuvable (code '{}').",
                StoreResolverService.DEFAULT_STORE_CODE);
            return;
        }

        log.info("Démarrage synchronisation automatique Google Sheets (magasin={}, id={})…",
            defaultStore.getCode(), defaultStore.getId());
        try {
            StoreContext.set(defaultStore);
            ImportSummary summary = fetchProducts(null);

            log.info("Résultats synchronisation Sheets : créations={}, mises à jour={}, désactivations={}, erreurs={}",
                summary.getCreatedCount(), summary.getUpdatedCount(), summary.getDeactivatedCount(), summary.getErrorCount());

            if (summary.getErrorCount() > 0) {
                log.warn("Synchronisation terminée avec {} erreur(s). Détail : {}", summary.getErrorCount(), summary.getErrorMessages());
            } else {
                log.info("Synchronisation Sheets OK — {} ligne(s) produit traitée(s).", summary.getTotalProcessed());
            }
        } catch (RuntimeException e) {
            log.error("Échec synchronisation automatique Google Sheets", e);
        } finally {
            StoreContext.clear();
        }
    }
}
