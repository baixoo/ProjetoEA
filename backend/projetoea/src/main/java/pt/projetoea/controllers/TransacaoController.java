package pt.projetoea.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.projetoea.models.EstadoPagamento;
import pt.projetoea.models.Transacao;
import pt.projetoea.services.TransacaoService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @GetMapping
    public ResponseEntity<List<Transacao>> getAllTransacoes() {
        return ResponseEntity.ok(transacaoService.getAllTransacoes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransacaoById(@PathVariable Long id) {
        return transacaoService.getTransacaoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/titulo/{tituloId}")
    public ResponseEntity<List<Transacao>> getTransacoesByTitulo(@PathVariable Long tituloId) {
        return ResponseEntity.ok(transacaoService.getTransacoesByTitulo(tituloId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> getTransacoesByEstado(@PathVariable String estado) {
        try {
            EstadoPagamento ep = EstadoPagamento.valueOf(estado.toUpperCase());
            return ResponseEntity.ok(transacaoService.getTransacoesByEstado(ep));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Estado inválido");
        }
    }

    @PostMapping
    public ResponseEntity<?> createTransacao(@RequestBody Map<String, Object> request) {
        Long tituloId = Long.valueOf(request.get("tituloId").toString());
        String referenciaExterna = (String) request.get("referenciaExterna");
        Transacao transacao = transacaoService.createTransacao(tituloId, referenciaExterna);
        return ResponseEntity.ok(transacao);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> updateEstado(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            EstadoPagamento estado = EstadoPagamento.valueOf(request.get("estado").toUpperCase());
            Transacao transacao = transacaoService.updateEstado(id, estado);
            return ResponseEntity.ok(transacao);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Estado inválido");
        }
    }
}
