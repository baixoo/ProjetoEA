package pt.notub.admin.service;

import org.springframework.stereotype.Service;
import pt.notub.admin.dto.AdminStatsResponse;
import pt.notub.payment.repository.TransacaoRepository;
import pt.notub.ticket.repository.BilheteRepository;
import pt.notub.ticket.repository.PasseRepository;
import pt.notub.trip.entity.EstadoViagem;
import pt.notub.trip.repository.ViagemUtilizadorRepository;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.vehicle.repository.VeiculoRepository;

@Service
public class ServicoAdmin {

    private final UtilizadorRepository utilizadorRepository;
    private final BilheteRepository bilheteRepository;
    private final PasseRepository passeRepository;
    private final TransacaoRepository transacaoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ViagemUtilizadorRepository viagemUtilizadorRepository;

    public ServicoAdmin(UtilizadorRepository utilizadorRepository, BilheteRepository bilheteRepository,
                        PasseRepository passeRepository, TransacaoRepository transacaoRepository, VeiculoRepository veiculoRepository,
                        ViagemUtilizadorRepository viagemUtilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
        this.bilheteRepository = bilheteRepository;
        this.passeRepository = passeRepository;
        this.transacaoRepository = transacaoRepository;
        this.veiculoRepository = veiculoRepository;
        this.viagemUtilizadorRepository = viagemUtilizadorRepository;
    }

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                utilizadorRepository.count(),
                bilheteRepository.count(),
                passeRepository.count(),
                veiculoRepository.count(),
                transacaoRepository.count(),
                viagemUtilizadorRepository.countByEstado(EstadoViagem.ATIVA)
        );
    }
}
