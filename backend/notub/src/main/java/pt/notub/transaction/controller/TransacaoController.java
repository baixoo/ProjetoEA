package pt.notub.transaction.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        return ResponseEntity.ok(transacaoService.getAllTransacoes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoDTO> getTransacaoById(@PathVariable Long id) {
        return ResponseEntity.ok(transacaoService.getTransacaoById(id));
    }

    @GetMapping({"/titulo/{tituloId}", "/title/{tituloId}"})
    public ResponseEntity<List<TransacaoDTO>> getTransacoesByTitulo(@PathVariable Long tituloId) {
        return ResponseEntity.ok(transacaoService.getTransacoesByTitulo(tituloId));
    }

    @GetMapping({"/estado/{estado}", "/status/{estado}"})
    public ResponseEntity<List<TransacaoDTO>> getTransacoesByEstado(@PathVariable String estado) {
        return ResponseEntity.ok(transacaoService.getTransacoesByEstado(estado));
    }

    @PostMapping
    public ResponseEntity<TransacaoDTO> createTransacao(@RequestBody CreateTransacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transacaoService.createTransacao(request));
    }

    @PutMapping({"/{id}/estado", "/{id}/status"})
    public ResponseEntity<TransacaoDTO> updateEstado(@PathVariable Long id, @RequestBody UpdateEstadoPagamentoRequest request) {
        return ResponseEntity.ok(transacaoService.updateEstado(id, request));
    }
}
