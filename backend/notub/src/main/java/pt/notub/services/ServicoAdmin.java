package pt.notub.services;

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
        stats.put("totalUtilizadores", utilizadorRepository.count());
        stats.put("totalBilhetesVendidos", bilheteRepository.count());
        stats.put("totalPassesAtivos", passeRepository.count());
        stats.put("veiculosAtivos", veiculoRepository.count());
        stats.put("viagensEmCurso", viagemUtilizadorRepository.findByEstado(pt.notub.models.EstadoViagem.ATIVA).size());
        return stats;
    }
}
