package pt.notub.tariff;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.TarifaMapper;
import pt.notub.models.ModalidadePasse;
import pt.notub.models.TipoUtilizador;
import pt.notub.tariff.dto.TarifaDTO;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    @GetMapping
    public ResponseEntity<List<TarifaDTO>> getAllTarifas() {
        return ResponseEntity.ok(TarifaMapper.toDTOList(tarifaService.getAllTarifas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarifaDTO> getTarifaById(@PathVariable Long id) {
        return tarifaService.getTarifaById(id)
                .map(TarifaMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tipo-utilizador/{tipoUtilizador}")
    public ResponseEntity<List<TarifaDTO>> getTarifasByTipoUtilizador(@PathVariable String tipoUtilizador) {
        try {
            TipoUtilizador tipo = TipoUtilizador.valueOf(tipoUtilizador.toUpperCase());
            return ResponseEntity.ok(TarifaMapper.toDTOList(tarifaService.getTarifasByTipoUtilizador(tipo)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/modalidade/{modalidade}")
    public ResponseEntity<List<TarifaDTO>> getTarifasByModalidade(@PathVariable String modalidade) {
        try {
            ModalidadePasse mod = ModalidadePasse.valueOf(modalidade.toUpperCase());
            return ResponseEntity.ok(TarifaMapper.toDTOList(tarifaService.getTarifasByModalidade(mod)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/tipo-utilizador/{tipoUtilizador}/modalidade/{modalidade}")
    public ResponseEntity<TarifaDTO> getTarifaByTipoUtilizadorAndModalidade(@PathVariable String tipoUtilizador, @PathVariable String modalidade) {
        try {
            TipoUtilizador tipo = TipoUtilizador.valueOf(tipoUtilizador.toUpperCase());
            ModalidadePasse mod = ModalidadePasse.valueOf(modalidade.toUpperCase());
            return tarifaService.getTarifaByTipoUtilizadorAndModalidade(tipo, mod)
                    .map(TarifaMapper::toDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/calculadora")
    public ResponseEntity<TarifaDTO> calcularTarifa(
            @RequestParam(required = false) String tipoProduto,
            @RequestParam(required = false) String tipoUtilizador,
            @RequestParam(required = false) String modalidade,
            @RequestParam int nrZonas) {
        TipoUtilizador tipo = null;
        if (tipoUtilizador != null) {
            try {
                tipo = TipoUtilizador.valueOf(tipoUtilizador.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        ModalidadePasse mod = null;
        if (modalidade != null) {
            try {
                mod = ModalidadePasse.valueOf(modalidade.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (tipoProduto != null && tipoProduto.equalsIgnoreCase("PASSE") && mod == null) {
            return ResponseEntity.badRequest().build();
        }
        if (tipoProduto != null && !tipoProduto.equalsIgnoreCase("BILHETE")
                && !tipoProduto.equalsIgnoreCase("PASSE")) {
            return ResponseEntity.badRequest().build();
        }
        return tarifaService.calcularTarifa(tipo, mod, nrZonas)
                .map(TarifaMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
