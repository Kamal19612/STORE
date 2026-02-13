package com.sucrestore.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sucrestore.api.dto.ImportSummary;
import com.sucrestore.api.service.ProductImportService;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class AdminImportController {

    @Autowired
    private ProductImportService productImportService;

    @Autowired
    private com.sucrestore.api.service.GoogleSheetsService googleSheetsService;

    @PostMapping("/import")
    public ResponseEntity<ImportSummary> importProducts(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productImportService.importProducts(file));
    }

    @PostMapping("/import-google-sheets")
    public ResponseEntity<ImportSummary> importFromGoogleSheets(@RequestParam(value = "spreadsheetId", required = false) String spreadsheetId) {
        return ResponseEntity.ok(googleSheetsService.fetchProducts(spreadsheetId));
    }
}
