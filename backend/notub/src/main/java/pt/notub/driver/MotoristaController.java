package pt.notub.driver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.notub.common.mapper.VeiculoMapper;
import pt.notub.common.mapper.ViagemMapper;
import pt.notub.repositories.VeiculoRepository;
import pt.notub.repositories.ViagemVeiculoRepository;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.vehicle.dto.VeiculoDTO;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
public class MotoristaController {

    private final VeiculoRepository veiculoRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;

    public MotoristaController(VeiculoRepository veiculoRepository, ViagemVeiculoRepository viagemVeiculoRepository) {
        this.veiculoRepository = veiculoRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
    }

    @GetMapping("/veiculos")
    public ResponseEntity<List<VeiculoDTO>> getVeiculos() {
        return ResponseEntity.ok(VeiculoMapper.toDTOList(veiculoRepository.findAll()));
    }

    @GetMapping("/veiculos/{veiculoId}/viagens")
    public ResponseEntity<List<ViagemVeiculoDTO>> getViagensAtivas(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findByVeiculoId(veiculoId)));
    }
}
