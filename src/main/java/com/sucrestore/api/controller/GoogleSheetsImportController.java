package com.sucrestore.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sucrestore.api.dto.ImportSummary;
import com.sucrestore.api.service.GoogleSheetsService;

/**
 * Contrôleur REST pour l'importation de produits depuis Google Sheets.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class GoogleSheetsImportController {

    @Autowired
    private GoogleSheetsService googleSheetsService;

    /**
     * POST /api/admin/products/import : Importe les produits depuis Google
     * Sheets.
     *
     * @param spreadsheetId (optionnel) ID du Google Sheet à importer. Si non
     * fourni, utilise l'ID configuré dans application.yml
     * @return ImportSummary avec le nombre de produits importés, erreurs, etc.
     */
    @PostMapping("/google-sheets-sync")
    public ResponseEntity<ImportSummary> importProducts(
            @RequestParam(value = "spreadsheetId", required = false) String spreadsheetId) {

        ImportSummary summary = googleSheetsService.fetchProducts(spreadsheetId);

        return ResponseEntity.ok(summary);
    }
}
