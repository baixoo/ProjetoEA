package pt.notub.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.network.dto.LinhaDTO;
import pt.notub.network.dto.LinhaRequest;
import pt.notub.network.dto.ParagemDTO;
import pt.notub.network.dto.ParagemRequest;
import pt.notub.network.dto.PontoPassagemDTO;
import pt.notub.network.dto.PontoPassagemRequest;
import pt.notub.network.dto.TrajetoDTO;
import pt.notub.network.dto.TrajetoRequest;
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
    public ResponseEntity<LinhaDTO> createLinha(@RequestBody LinhaRequest linha) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportNetworkService.createLinha(linha));
    }

    @PutMapping({"/linhas/{id}", "/lines/{id}"})
    public ResponseEntity<LinhaDTO> updateLinha(@PathVariable Long id, @RequestBody LinhaRequest updated) {
        return ResponseEntity.ok(transportNetworkService.updateLinha(id, updated));
    }

    @DeleteMapping({"/linhas/{id}", "/lines/{id}"})
    public ResponseEntity<Void> deleteLinha(@PathVariable Long id) {
        transportNetworkService.deleteLinha(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/trajetos", "/routes"})
    public ResponseEntity<TrajetoDTO> createTrajeto(@RequestBody TrajetoRequest trajeto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportNetworkService.createTrajeto(trajeto));
    }

    @PutMapping({"/trajetos/{id}", "/routes/{id}"})
    public ResponseEntity<TrajetoDTO> updateTrajeto(@PathVariable Long id, @RequestBody TrajetoRequest updated) {
        return ResponseEntity.ok(transportNetworkService.updateTrajeto(id, updated));
    }

    @DeleteMapping({"/trajetos/{id}", "/routes/{id}"})
    public ResponseEntity<Void> deleteTrajeto(@PathVariable Long id) {
        transportNetworkService.deleteTrajeto(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/paragens", "/stops"})
    public ResponseEntity<ParagemDTO> createParagem(@RequestBody ParagemRequest paragem) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportNetworkService.createParagem(paragem));
    }

    @PutMapping({"/paragens/{id}", "/stops/{id}"})
    public ResponseEntity<ParagemDTO> updateParagem(@PathVariable Long id, @RequestBody ParagemRequest updated) {
        return ResponseEntity.ok(transportNetworkService.updateParagem(id, updated));
    }

    @DeleteMapping({"/paragens/{id}", "/stops/{id}"})
    public ResponseEntity<Void> deleteParagem(@PathVariable Long id) {
        transportNetworkService.deleteParagem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/trajetos/{trajetoId}/pontos", "/routes/{trajetoId}/points"})
    public ResponseEntity<PontoPassagemDTO> addPonto(@PathVariable Long trajetoId, @RequestBody PontoPassagemRequest ponto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transportNetworkService.addPonto(trajetoId, ponto));
    }

    @DeleteMapping({"/pontos/{id}", "/points/{id}"})
    public ResponseEntity<Void> deletePonto(@PathVariable Long id) {
        transportNetworkService.deletePonto(id);
        return ResponseEntity.noContent().build();
    }
}
