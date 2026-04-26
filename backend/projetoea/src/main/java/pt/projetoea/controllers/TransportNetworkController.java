package pt.projetoea.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.projetoea.services.TransportNetworkService;

@RestController
@RequestMapping("/api/network")
public class TransportNetworkController {

    private final TransportNetworkService networkService;

    public TransportNetworkController(TransportNetworkService networkService) {
        this.networkService = networkService;
    }

    // ---- Paragens ----

    @GetMapping("/paragens")
    public ResponseEntity<?> getParagens() {
        return ResponseEntity.ok(networkService.getAllParagens());
    }

    @GetMapping("/paragens/{id}")
    public ResponseEntity<?> getParagemById(@PathVariable Long id) {
        return networkService.getParagemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Carreiras ----

    @GetMapping("/carreiras")
    public ResponseEntity<?> getCarreiras() {
        return ResponseEntity.ok(networkService.getAllCarreiras());
    }

    @GetMapping("/carreiras/{id}")
    public ResponseEntity<?> getCarreiraById(@PathVariable Long id) {
        return networkService.getCarreiraById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- Trajetos ----

    @GetMapping("/trajetos")
    public ResponseEntity<?> getTrajetos() {
        return ResponseEntity.ok(networkService.getAllTrajetos());
    }

    @GetMapping("/trajetos/{id}")
    public ResponseEntity<?> getTrajetoById(@PathVariable Long id) {
        return networkService.getTrajetoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/carreiras/{carreiraId}/trajetos")
    public ResponseEntity<?> getTrajetosByCarreira(@PathVariable Long carreiraId) {
        return ResponseEntity.ok(networkService.getTrajetosByCarreira(carreiraId));
    }

    // ---- Sequências de Paragem ----

    @GetMapping("/trajetos/{trajetoId}/sequencias")
    public ResponseEntity<?> getSequenciasByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(networkService.getSequenciasByTrajeto(trajetoId));
    }
}
