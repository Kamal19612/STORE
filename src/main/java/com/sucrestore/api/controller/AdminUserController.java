package com.sucrestore.api.controller;

import com.sucrestore.api.entity.User;
import com.sucrestore.api.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour l'administration des utilisateurs. Nécessite le rôle
 * ADMIN ou SUPER_ADMIN.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminUserController {

    @Autowired
    private UserDetailsServiceImpl userService;

    /**
     * GET /api/admin/users : Liste de tous les utilisateurs.
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * PUT /api/admin/users/{id}/role : Changer le rôle. Body: { "role": "ADMIN"
     * }
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String role = payload.get("role");
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }
}
