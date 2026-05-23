package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.notub.services.TransportNetworkService;

@RestController
@RequestMapping("/api/network")
public class TransportNetworkController {

    private final TransportNetworkService networkService;

    public TransportNetworkController(TransportNetworkService networkService) {
        this.networkService = networkService;
    }

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

    @GetMapping("/linhas")
    public ResponseEntity<?> getLinhas() {
        return ResponseEntity.ok(networkService.getAllLinhas());
    }

    @GetMapping("/linhas/{id}")
    public ResponseEntity<?> getLinhaById(@PathVariable Long id) {
        return networkService.getLinhaById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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

    @GetMapping("/linhas/{linhaId}/trajetos")
    public ResponseEntity<?> getTrajetosByLinha(@PathVariable Long linhaId) {
        return ResponseEntity.ok(networkService.getTrajetosByLinha(linhaId));
    }

    @GetMapping("/trajetos/{trajetoId}/pontos")
    public ResponseEntity<?> getPontosByTrajeto(@PathVariable Long trajetoId) {
        return ResponseEntity.ok(networkService.getPontosByTrajeto(trajetoId));
    }
}
