package pt.notub.points.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.points.dto.HistoricoPontosDTO;
import pt.notub.points.dto.PontosSaldoResponse;
import pt.notub.points.dto.UtilizarPontosRequest;
import pt.notub.points.dto.UtilizarPontosResponse;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.points.service.ServicoPontos;

import java.util.List;

@RestController
@RequestMapping({"/api/pontos", "/api/points"})
public class PontosController {

    private final ServicoPontos servicoPontos;

    public PontosController(ServicoPontos servicoPontos) {
        this.servicoPontos = servicoPontos;
    }

    @GetMapping({"/saldo", "/balance"})
    public ResponseEntity<PontosSaldoResponse> getSaldo(@AuthenticatedUser AuthenticatedUserContext utilizador) {
        return ResponseEntity.ok(servicoPontos.getSaldo(utilizador.id()));
    }

    @GetMapping({"/historico", "/history"})
    public ResponseEntity<List<HistoricoPontosDTO>> getHistorico(@AuthenticatedUser AuthenticatedUserContext utilizador) {
        return ResponseEntity.ok(servicoPontos.getHistorico(utilizador.id()));
    }

    @PostMapping({"/utilizar", "/use"})
    public ResponseEntity<UtilizarPontosResponse> utilizarPontos(@AuthenticatedUser AuthenticatedUserContext utilizador,
                                                                 @RequestBody UtilizarPontosRequest pedido) {
        return ResponseEntity.ok(servicoPontos.utilizarPontos(utilizador.id(), pedido.pontos(), pedido.descricao()));
    }
}
