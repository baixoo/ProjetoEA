package pt.notub.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.services.GestorValidacao;
import java.util.Map;

@RestController
@RequestMapping("/api/validacao")
public class ValidacaoController {

    private final GestorValidacao gestorValidacao;

    public ValidacaoController(GestorValidacao gestorValidacao) {
        this.gestorValidacao = gestorValidacao;
    }

    @PostMapping("/{tituloId}")
    public ResponseEntity<?> validar(@PathVariable Long tituloId) {
        boolean valido = gestorValidacao.validarTitulo(tituloId);
        return ResponseEntity.ok(Map.of("valido", valido, "mensagem", valido ? "Titulo valido" : "Titulo invalido"));
    }

    @PostMapping("/{tituloId}/usar")
    public ResponseEntity<?> validarEUsar(@PathVariable Long tituloId) {
        boolean valido = gestorValidacao.validarEUsarBilhete(tituloId);
        return ResponseEntity.ok(Map.of("valido", valido, "consumido", valido, "mensagem", valido ? "Titulo validado e consumido" : "Titulo invalido"));
    }
}
