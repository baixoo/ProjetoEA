package pt.notub.validation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.validation.service.GestorValidacao;
import pt.notub.validation.dto.ValidationResultDTO;

@RestController
@RequestMapping({"/api/validacao", "/api/validation"})
public class ValidacaoController {

    private final GestorValidacao gestorValidacao;

    public ValidacaoController(GestorValidacao gestorValidacao) {
        this.gestorValidacao = gestorValidacao;
    }

    @PostMapping("/{tituloId}")
    public ResponseEntity<ValidationResultDTO> validar(@PathVariable Long tituloId) {
        boolean valido = gestorValidacao.validarTitulo(tituloId);
        return ResponseEntity.ok(new ValidationResultDTO(valido, valido ? "Titulo valido" : "Titulo invalido", false));
    }

    @PostMapping({"/{tituloId}/usar", "/{tituloId}/use"})
    public ResponseEntity<ValidationResultDTO> validarEUsar(@PathVariable Long tituloId) {
        boolean valido = gestorValidacao.validarEUsarBilhete(tituloId);
        return ResponseEntity.ok(new ValidationResultDTO(valido, valido ? "Titulo validado e consumido" : "Titulo invalido", valido));
    }
}
