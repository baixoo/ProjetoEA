package pt.notub.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.notub.admin.dto.AdminStatsResponse;
import pt.notub.admin.service.ServicoAdmin;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final ServicoAdmin servicoAdmin;

    public AdminStatsController(ServicoAdmin servicoAdmin) {
        this.servicoAdmin = servicoAdmin;
    }

    @GetMapping
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(servicoAdmin.getStats());
    }
}
