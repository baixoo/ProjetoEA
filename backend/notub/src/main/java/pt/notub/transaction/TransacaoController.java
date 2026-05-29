package pt.notub.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.TransacaoMapper;
import pt.notub.models.EstadoPagamento;
import pt.notub.models.Transacao;
import pt.notub.transaction.TransacaoService;
import pt.notub.transaction.dto.TransacaoDTO;

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
    public ResponseEntity<List<TransacaoDTO>> getAllTransacoes() {
        return ResponseEntity.ok(TransacaoMapper.toDTOList(transacaoService.getAllTransacoes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoDTO> getTransacaoById(@PathVariable Long id) {
        return transacaoService.getTransacaoById(id)
                .map(TransacaoMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/titulo/{tituloId}")
    public ResponseEntity<List<TransacaoDTO>> getTransacoesByTitulo(@PathVariable Long tituloId) {
        return ResponseEntity.ok(TransacaoMapper.toDTOList(transacaoService.getTransacoesByTitulo(tituloId)));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<TransacaoDTO>> getTransacoesByEstado(@PathVariable String estado) {
        try {
            EstadoPagamento ep = EstadoPagamento.valueOf(estado.toUpperCase());
            return ResponseEntity.ok(TransacaoMapper.toDTOList(transacaoService.getTransacoesByEstado(ep)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<TransacaoDTO> createTransacao(@RequestBody Map<String, Object> request) {
        Long tituloId = Long.valueOf(request.get("tituloId").toString());
        Long utilizadorId = Long.valueOf(request.get("utilizadorId").toString());
        String referenciaExterna = (String) request.get("referenciaExterna");
        Transacao transacao = transacaoService.createTransacao(tituloId, utilizadorId, referenciaExterna);
        return ResponseEntity.ok(TransacaoMapper.toDTO(transacao));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<TransacaoDTO> updateEstado(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            EstadoPagamento estado = EstadoPagamento.valueOf(request.get("estado").toUpperCase());
            Transacao transacao = transacaoService.updateEstado(id, estado);
            return ResponseEntity.ok(TransacaoMapper.toDTO(transacao));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
