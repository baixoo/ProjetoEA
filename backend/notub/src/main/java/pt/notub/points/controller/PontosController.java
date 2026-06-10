package pt.notub.points.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.points.mapper.HistoricoPontosMapper;
import pt.notub.user.entity.Utilizador;
import pt.notub.points.dto.HistoricoPontosDTO;
import pt.notub.points.dto.PontosSaldoResponse;
import pt.notub.points.dto.UtilizarPontosRequest;
import pt.notub.points.dto.UtilizarPontosResponse;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.points.service.ServicoPontos;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/pontos", "/api/points"})
public class PontosController {

    private final ServicoPontos servicoPontos;

    public PontosController(ServicoPontos servicoPontos) {
        this.servicoPontos = servicoPontos;
    }

    @GetMapping({"/saldo", "/balance"})
    public ResponseEntity<PontosSaldoResponse> getSaldo(@AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(new PontosSaldoResponse(utilizador.getNrPontos()));
    }

    @GetMapping({"/historico", "/history"})
    public ResponseEntity<List<HistoricoPontosDTO>> getHistorico(@AuthenticatedUser Utilizador utilizador) {
        return ResponseEntity.ok(HistoricoPontosMapper.toDTOList(servicoPontos.getHistorico(utilizador.getId())));
    }

    @PostMapping({"/utilizar", "/use"})
    public ResponseEntity<?> utilizarPontos(@AuthenticatedUser Utilizador utilizador, @RequestBody UtilizarPontosRequest pedido) {
        int pontos = pedido.pontos();
        if (pontos <= 0) return ResponseEntity.badRequest().body(Map.of("erro", "Pontos deve ser maior que zero"));
        String descricao = pedido.descricao();
        servicoPontos.utilizarPontos(utilizador.getId(), pontos, descricao);
        utilizador = servicoPontos.getUtilizadorAtualizado(utilizador.getId());
        return ResponseEntity.ok(new UtilizarPontosResponse(
                utilizador.getNrPontos(), pontos + " pontos utilizados com sucesso"));
    }
}
