package pt.notub.validation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.validation.dto.GeoVerificationRequest;
import pt.notub.validation.dto.GeoVerificationResponse;
import pt.notub.validation.service.GeoVerificationService;

@RestController
@RequestMapping({"/api/geo", "/api/geolocation"})
public class GeoVerificationController {

    private final GeoVerificationService geoVerificationService;

    public GeoVerificationController(GeoVerificationService geoVerificationService) {
        this.geoVerificationService = geoVerificationService;
    }

    @PostMapping({"/verificar", "/verify"})
    public ResponseEntity<GeoVerificationResponse> verificarProximidade(@RequestBody GeoVerificationRequest request) {
        boolean proximo = geoVerificationService.isProximo(request.paragemId(), request.latitude(), request.longitude());
        return ResponseEntity.ok(new GeoVerificationResponse(proximo));
    }
}
