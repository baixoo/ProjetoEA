package pt.notub.admin;

import org.springframework.stereotype.Service;
import pt.notub.repositories.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class ServicoAdmin {

    private final UtilizadorRepository utilizadorRepository;
    private final BilheteRepository bilheteRepository;
    private final PasseRepository passeRepository;
    private final VeiculoRepository veiculoRepository;
    private final ViagemUtilizadorRepository viagemUtilizadorRepository;

    public ServicoAdmin(UtilizadorRepository utilizadorRepository, BilheteRepository bilheteRepository,
                        PasseRepository passeRepository, VeiculoRepository veiculoRepository,
                        ViagemUtilizadorRepository viagemUtilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
        this.bilheteRepository = bilheteRepository;
        this.passeRepository = passeRepository;
        this.veiculoRepository = veiculoRepository;
        this.viagemUtilizadorRepository = viagemUtilizadorRepository;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("utilizadores", utilizadorRepository.count());
        stats.put("bilhetes", bilheteRepository.count());
        stats.put("passes", passeRepository.count());
        stats.put("veiculos", veiculoRepository.count());
        stats.put("transacoes", 0);
        stats.put("viagens", viagemUtilizadorRepository.findByEstado(pt.notub.models.EstadoViagem.ATIVA).size());
        return stats;
    }
}
