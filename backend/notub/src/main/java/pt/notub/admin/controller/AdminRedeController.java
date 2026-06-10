package pt.notub.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.network.entity.Linha;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.service.TransportNetworkService;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping("/api/admin/network")
public class AdminRedeController {

    private final TransportNetworkService transportNetworkService;

    public AdminRedeController(TransportNetworkService transportNetworkService) {
        this.transportNetworkService = transportNetworkService;
    }

    @PostMapping({"/linhas", "/lines"})
    public ResponseEntity<Linha> createLinha(@RequestBody Linha linha) {
        return ResponseEntity.ok(transportNetworkService.createLinha(linha));
    }

    @PutMapping({"/linhas/{id}", "/lines/{id}"})
    public ResponseEntity<?> updateLinha(@PathVariable Long id, @RequestBody Linha updated) {
        return ResponseEntity.ok(transportNetworkService.updateLinha(id, updated));
    }

    @DeleteMapping({"/linhas/{id}", "/lines/{id}"})
    public ResponseEntity<?> deleteLinha(@PathVariable Long id) {
        transportNetworkService.deleteLinha(id);
        return ResponseEntity.ok("Linha eliminada");
    }

    @PostMapping({"/trajetos", "/routes"})
    public ResponseEntity<Trajeto> createTrajeto(@RequestBody Trajeto trajeto) {
        return ResponseEntity.ok(transportNetworkService.createTrajeto(trajeto));
    }

    @PutMapping({"/trajetos/{id}", "/routes/{id}"})
    public ResponseEntity<?> updateTrajeto(@PathVariable Long id, @RequestBody Trajeto updated) {
        return ResponseEntity.ok(transportNetworkService.updateTrajeto(id, updated));
    }

    @DeleteMapping({"/trajetos/{id}", "/routes/{id}"})
    public ResponseEntity<?> deleteTrajeto(@PathVariable Long id) {
        transportNetworkService.deleteTrajeto(id);
        return ResponseEntity.ok("Trajeto eliminado");
    }

    @PostMapping({"/paragens", "/stops"})
    public ResponseEntity<Paragem> createParagem(@RequestBody Paragem paragem) {
        return ResponseEntity.ok(transportNetworkService.createParagem(paragem));
    }

    @PutMapping({"/paragens/{id}", "/stops/{id}"})
    public ResponseEntity<?> updateParagem(@PathVariable Long id, @RequestBody Paragem updated) {
        return ResponseEntity.ok(transportNetworkService.updateParagem(id, updated));
    }

    @DeleteMapping({"/paragens/{id}", "/stops/{id}"})
    public ResponseEntity<?> deleteParagem(@PathVariable Long id) {
        transportNetworkService.deleteParagem(id);
        return ResponseEntity.ok("Paragem eliminada");
    }

    @PostMapping({"/trajetos/{trajetoId}/pontos", "/routes/{trajetoId}/points"})
    public ResponseEntity<PontosDePassagem> addPonto(@PathVariable Long trajetoId, @RequestBody PontosDePassagem ponto) {
        return ResponseEntity.ok(transportNetworkService.addPonto(trajetoId, ponto));
    }

    @DeleteMapping({"/pontos/{id}", "/points/{id}"})
    public ResponseEntity<?> deletePonto(@PathVariable Long id) {
        transportNetworkService.deletePonto(id);
        return ResponseEntity.ok("Ponto de passagem eliminado");
    }
}
