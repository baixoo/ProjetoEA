package pt.notub.points;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.HistoricoPontosMapper;
import pt.notub.models.Utilizador;
import pt.notub.points.dto.HistoricoPontosDTO;
import pt.notub.security.AuthenticatedUser;
import pt.notub.points.ServicoPontos;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pontos")
public class PontosController {

    private final ServicoPontos servicoPontos;

    public PontosController(ServicoPontos servicoPontos) {
        this.servicoPontos = servicoPontos;
    }

    @GetMapping("/saldo")
    public ResponseEntity<?> getSaldo(@AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(Map.of("nrPontos", utilizador.getNrPontos()));
    }

    @GetMapping("/historico")
    public ResponseEntity<List<HistoricoPontosDTO>> getHistorico(@AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(HistoricoPontosMapper.toDTOList(servicoPontos.getHistorico(utilizador.getId())));
    }

    @PostMapping("/utilizar")
    public ResponseEntity<?> utilizarPontos(@AuthenticatedUser Utilizador utilizador, @RequestBody Map<String, Object> pedido) {
        int pontos = ((Number) pedido.get("pontos")).intValue();
        if (pontos <= 0) return ResponseEntity.badRequest().body(Map.of("erro", "Pontos deve ser maior que zero"));
        String descricao = (String) pedido.get("descricao");
        servicoPontos.utilizarPontos(utilizador.getId(), pontos, descricao);
        utilizador = servicoPontos.getUtilizadorAtualizado(utilizador.getId());
        return ResponseEntity.ok(Map.of("nrPontos", utilizador.getNrPontos(), "mensagem", pontos + " pontos utilizados com sucesso"));
    }
}
