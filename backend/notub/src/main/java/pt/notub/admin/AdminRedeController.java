package pt.notub.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.*;
import pt.notub.repositories.*;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping("/api/admin/network")
public class AdminRedeController {

    private final LinhaRepository linhaRepository;
    private final TrajetoRepository trajetoRepository;
    private final ParagemRepository paragemRepository;
    private final PontosDePassagemRepository pontosDePassagemRepository;

    public AdminRedeController(LinhaRepository linhaRepository, TrajetoRepository trajetoRepository,
                               ParagemRepository paragemRepository, PontosDePassagemRepository pontosDePassagemRepository) {
        this.linhaRepository = linhaRepository;
        this.trajetoRepository = trajetoRepository;
        this.paragemRepository = paragemRepository;
        this.pontosDePassagemRepository = pontosDePassagemRepository;
    }

    @PostMapping("/linhas")
    public ResponseEntity<Linha> createLinha(@RequestBody Linha linha) {
        return ResponseEntity.ok(linhaRepository.save(linha));
    }

    @PutMapping("/linhas/{id}")
    public ResponseEntity<?> updateLinha(@PathVariable Long id, @RequestBody Linha updated) {
        Linha linha = linhaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada"));
        if (updated.getNome() != null) linha.setNome(updated.getNome());
        if (updated.getIdentificadorServico() != null) linha.setIdentificadorServico(updated.getIdentificadorServico());
        return ResponseEntity.ok(linhaRepository.save(linha));
    }

    @DeleteMapping("/linhas/{id}")
    public ResponseEntity<?> deleteLinha(@PathVariable Long id) {
        linhaRepository.deleteById(id);
        return ResponseEntity.ok("Linha eliminada");
    }

    @PostMapping("/trajetos")
    public ResponseEntity<Trajeto> createTrajeto(@RequestBody Trajeto trajeto) {
        return ResponseEntity.ok(trajetoRepository.save(trajeto));
    }

    @PutMapping("/trajetos/{id}")
    public ResponseEntity<?> updateTrajeto(@PathVariable Long id, @RequestBody Trajeto updated) {
        Trajeto trajeto = trajetoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
        if (updated.getDirecao() != null) trajeto.setDirecao(updated.getDirecao());
        if (updated.getLinha() != null) trajeto.setLinha(updated.getLinha());
        return ResponseEntity.ok(trajetoRepository.save(trajeto));
    }

    @DeleteMapping("/trajetos/{id}")
    public ResponseEntity<?> deleteTrajeto(@PathVariable Long id) {
        trajetoRepository.deleteById(id);
        return ResponseEntity.ok("Trajeto eliminado");
    }

    @PostMapping("/paragens")
    public ResponseEntity<Paragem> createParagem(@RequestBody Paragem paragem) {
        return ResponseEntity.ok(paragemRepository.save(paragem));
    }

    @PutMapping("/paragens/{id}")
    public ResponseEntity<?> updateParagem(@PathVariable Long id, @RequestBody Paragem updated) {
        Paragem paragem = paragemRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        if (updated.getNome() != null) paragem.setNome(updated.getNome());
        if (updated.getLocalizacao() != null) paragem.setLocalizacao(updated.getLocalizacao());
        return ResponseEntity.ok(paragemRepository.save(paragem));
    }

    @DeleteMapping("/paragens/{id}")
    public ResponseEntity<?> deleteParagem(@PathVariable Long id) {
        paragemRepository.deleteById(id);
        return ResponseEntity.ok("Paragem eliminada");
    }

    @PostMapping("/trajetos/{trajetoId}/pontos")
    public ResponseEntity<PontosDePassagem> addPonto(@PathVariable Long trajetoId, @RequestBody PontosDePassagem ponto) {
        Trajeto trajeto = trajetoRepository.findById(trajetoId).orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
        if (trajeto.getPontosDePassagem() == null) {
            trajeto.setPontosDePassagem(new java.util.ArrayList<>());
        }
        trajeto.getPontosDePassagem().add(ponto);
        trajetoRepository.save(trajeto);
        return ResponseEntity.ok(ponto);
    }

    @DeleteMapping("/pontos/{id}")
    public ResponseEntity<?> deletePonto(@PathVariable Long id) {
        pontosDePassagemRepository.deleteById(id);
        return ResponseEntity.ok("Ponto de passagem eliminado");
    }
}
