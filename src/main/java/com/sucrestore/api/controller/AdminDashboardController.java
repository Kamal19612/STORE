package com.sucrestore.api.controller;

import com.sucrestore.api.dto.DashboardStatsResponse;
import com.sucrestore.api.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour les endpoints du dashboard admin
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * GET /api/admin/dashboard/stats Récupère les statistiques pour le
     * dashboard admin
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        DashboardStatsResponse stats = dashboardService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}
