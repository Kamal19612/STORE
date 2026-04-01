package com.sucrestore.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sucrestore.api.dto.ImportSummary;
import com.sucrestore.api.repository.AppSettingRepository;
import com.sucrestore.api.service.GoogleSheetsService;
import com.sucrestore.api.service.ProductService;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class AdminImportController {

    @Autowired
    private ProductService productService;

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @org.springframework.beans.factory.annotation.Value("${google.sheets.spreadsheet-id:}")
    private String defaultSpreadsheetId;

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSummary> importProducts(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productService.processCsvImport(file));
    }

    @PostMapping({"/import-google-sheets", "/google-sheets-sync"})
    public ResponseEntity<ImportSummary> importFromGoogleSheets(
            @RequestParam(value = "spreadsheetId", required = false) String spreadsheetId) {
        return ResponseEntity.ok(googleSheetsService.fetchProducts(spreadsheetId));
    }

    /**
     * GET /api/admin/products/sheet-config : Retourne l'ID du sheet sauvegardé.
     */
    @GetMapping("/sheet-config")
    public ResponseEntity<java.util.Map<String, String>> getSheetConfig() {
        String savedId = appSettingRepository.findByKey("google_sheet_id")
                .map(s -> s.getValue())
                .filter(v -> !v.isBlank())
                .orElse(defaultSpreadsheetId); // Fallback sur le YML si non défini en DB

        return ResponseEntity.ok(java.util.Map.of("spreadsheetId", savedId != null ? savedId : ""));
    }
}
