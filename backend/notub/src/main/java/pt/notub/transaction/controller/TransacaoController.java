package pt.notub.transaction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.transaction.mapper.TransacaoMapper;
import pt.notub.payment.entity.EstadoPagamento;
import pt.notub.payment.entity.Transacao;
import pt.notub.transaction.service.TransacaoService;
import pt.notub.transaction.dto.CreateTransacaoRequest;
import pt.notub.transaction.dto.TransacaoDTO;
import pt.notub.transaction.dto.UpdateEstadoPagamentoRequest;

import java.util.List;

@RestController
@RequestMapping({"/api/transacoes", "/api/transactions"})
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

    @GetMapping({"/titulo/{tituloId}", "/title/{tituloId}"})
    public ResponseEntity<List<TransacaoDTO>> getTransacoesByTitulo(@PathVariable Long tituloId) {
        return ResponseEntity.ok(TransacaoMapper.toDTOList(transacaoService.getTransacoesByTitulo(tituloId)));
    }

    @GetMapping({"/estado/{estado}", "/status/{estado}"})
    public ResponseEntity<List<TransacaoDTO>> getTransacoesByEstado(@PathVariable String estado) {
        try {
            EstadoPagamento ep = EstadoPagamento.valueOf(estado.toUpperCase());
            return ResponseEntity.ok(TransacaoMapper.toDTOList(transacaoService.getTransacoesByEstado(ep)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<TransacaoDTO> createTransacao(@RequestBody CreateTransacaoRequest request) {
        Transacao transacao = transacaoService.createTransacao(
                request.tituloId(), request.utilizadorId(), request.referenciaExterna());
        return ResponseEntity.ok(TransacaoMapper.toDTO(transacao));
    }

    @PutMapping({"/{id}/estado", "/{id}/status"})
    public ResponseEntity<TransacaoDTO> updateEstado(@PathVariable Long id, @RequestBody UpdateEstadoPagamentoRequest request) {
        try {
            EstadoPagamento estado = EstadoPagamento.valueOf(request.estado().toUpperCase());
            Transacao transacao = transacaoService.updateEstado(id, estado);
            return ResponseEntity.ok(TransacaoMapper.toDTO(transacao));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
