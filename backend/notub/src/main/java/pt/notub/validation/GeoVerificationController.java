package pt.notub.validation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.validation.GeoVerificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/geo")
public class GeoVerificationController {

    private final GeoVerificationService geoVerificationService;

    public GeoVerificationController(GeoVerificationService geoVerificationService) {
        this.geoVerificationService = geoVerificationService;
    }

    @PostMapping("/verificar")
    public ResponseEntity<?> verificarProximidade(@RequestBody Map<String, Object> request) {
        Long paragemId = Long.valueOf(request.get("paragemId").toString());
        double lat = Double.parseDouble(request.get("latitude").toString());
        double lng = Double.parseDouble(request.get("longitude").toString());

        boolean proximo = geoVerificationService.isProximo(paragemId, lat, lng);
        return ResponseEntity.ok(Map.of("proximo", proximo));
    }
}
