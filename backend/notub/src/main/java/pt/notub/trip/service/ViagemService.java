package pt.notub.trip.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.driver.dto.NotificacaoValidacaoDTO;
import pt.notub.driver.service.NotificacaoValidacaoService;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.points.service.ServicoPontos;
import pt.notub.ticket.entity.Bilhete;
import pt.notub.ticket.entity.Passe;
import pt.notub.ticket.entity.TituloTransporte;
import pt.notub.ticket.repository.TituloTransporteRepository;
import pt.notub.trip.dto.CreateViagemVeiculoRequest;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.entity.EstadoViagem;
import pt.notub.trip.entity.ViagemUtilizador;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.mapper.ViagemMapper;
import pt.notub.trip.repository.ViagemUtilizadorRepository;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.validation.service.GestorValidacao;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.repository.VeiculoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViagemService {

    private static final int MAX_HOURS_FOR_POINTS = 24;

    private final ViagemUtilizadorRepository viagemUtilizadorRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;
    private final ParagemRepository paragemRepository;
    private final TituloTransporteRepository tituloTransporteRepository;
    private final VeiculoRepository veiculoRepository;
    private final TrajetoRepository trajetoRepository;
    private final ServicoPontos servicoPontos;
    private final GestorValidacao gestorValidacao;
    private final NotificacaoValidacaoService notificacaoService;

    public ViagemService(ViagemUtilizadorRepository viagemUtilizadorRepository,
                         ViagemVeiculoRepository viagemVeiculoRepository,
                         ParagemRepository paragemRepository,
                         TituloTransporteRepository tituloTransporteRepository,
                         VeiculoRepository veiculoRepository,
                         TrajetoRepository trajetoRepository,
                         ServicoPontos servicoPontos,
                         GestorValidacao gestorValidacao,
                         NotificacaoValidacaoService notificacaoService) {
        this.viagemUtilizadorRepository = viagemUtilizadorRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
        this.paragemRepository = paragemRepository;
        this.tituloTransporteRepository = tituloTransporteRepository;
        this.veiculoRepository = veiculoRepository;
        this.trajetoRepository = trajetoRepository;
        this.servicoPontos = servicoPontos;
        this.gestorValidacao = gestorValidacao;
        this.notificacaoService = notificacaoService;
    }

    public List<ViagemDTO> getAllViagensUtilizador() {
        return ViagemMapper.toDTOList(viagemUtilizadorRepository.findAll());
    }

    public List<ViagemDTO> getViagensByUtilizador(Long utilizadorId) {
        return ViagemMapper.toDTOList(viagemUtilizadorRepository.findByUtilizadorId(utilizadorId));
    }

    public ViagemDTO getViagemUtilizadorById(Long id) {
        return ViagemMapper.toDTO(findViagemUtilizador(id));
    }

    public ViagemDTO iniciarViagem(Long tituloId, Long paragemEntradaId, Long viagemVeiculoId) {
        TituloTransporte titulo = findTitulo(tituloId);
        Paragem paragemEntrada = findParagem(paragemEntradaId);
        ViagemVeiculo viagemVeiculo = findViagemVeiculo(viagemVeiculoId);

        boolean valido = gestorValidacao.validarTitulo(tituloId);

        if (titulo instanceof Bilhete bilhete && valido) {
            bilhete.setUsado(true);
            tituloTransporteRepository.save(bilhete);
        }

        ViagemUtilizador viagem = new ViagemUtilizador();
        viagem.setTitulo(titulo);
        viagem.setParagemEntrada(paragemEntrada);
        viagem.setViagemVeiculo(viagemVeiculo);
        viagem.setInicio(LocalDateTime.now());
        viagem.setEstado(EstadoViagem.ATIVA);

        ViagemUtilizador saved = viagemUtilizadorRepository.save(viagem);

        String nomePassageiro = "";
        String tituloTipo = "";
        if (titulo instanceof Bilhete bilhete) {
            nomePassageiro = bilhete.getUtilizador() != null
                    ? bilhete.getUtilizador().getPrimeiroNome() + " " + bilhete.getUtilizador().getUltimoNome()
                    : "Desconhecido";
            tituloTipo = "Bilhete";
        } else if (titulo instanceof Passe passe) {
            nomePassageiro = passe.getUtilizador() != null
                    ? passe.getUtilizador().getPrimeiroNome() + " " + passe.getUtilizador().getUltimoNome()
                    : "Desconhecido";
            tituloTipo = "Passe " + passe.getModalidade().name();
        }

        Long veiculoId = viagemVeiculo.getVeiculo() != null ? viagemVeiculo.getVeiculo().getId() : null;
        NotificacaoValidacaoDTO notificacao = new NotificacaoValidacaoDTO(
                valido, nomePassageiro, tituloTipo, veiculoId, LocalDateTime.now());
        notificacaoService.notificarMotorista(notificacao);

        return ViagemMapper.toDTO(saved);
    }

    public ViagemDTO terminarViagem(Long viagemId, Long paragemSaidaId) {
        ViagemUtilizador viagem = findViagemUtilizador(viagemId);
        Paragem paragemSaida = findParagem(paragemSaidaId);

        viagem.setParagemSaida(paragemSaida);
        viagem.setFim(LocalDateTime.now());
        viagem.setEstado(EstadoViagem.CONCLUIDA);

        ViagemUtilizador saved = viagemUtilizadorRepository.save(viagem);

        boolean concederPontos = viagem.getInicio() != null
                && java.time.Duration.between(viagem.getInicio(), viagem.getFim()).toHours() <= MAX_HOURS_FOR_POINTS;

        if (concederPontos) {
            if (viagem.getTitulo() instanceof Bilhete bilhete && bilhete.getUtilizador() != null) {
                servicoPontos.atribuirPontosViagem(bilhete.getUtilizador().getId());
            } else if (viagem.getTitulo() instanceof Passe passe && passe.getUtilizador() != null) {
                servicoPontos.atribuirPontosViagem(passe.getUtilizador().getId());
            }
        }

        return ViagemMapper.toDTO(saved);
    }

    public List<ViagemVeiculoDTO> getAllViagensVeiculo() {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findAll());
    }

    public ViagemVeiculoDTO getViagemVeiculoById(Long id) {
        return ViagemMapper.toVeiculoDTO(findViagemVeiculo(id));
    }

    public List<ViagemVeiculoDTO> getViagensByVeiculo(Long veiculoId) {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findByVeiculoId(veiculoId));
    }

    public List<ViagemVeiculoDTO> getViagensByTrajeto(Long trajetoId) {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findByTrajetoId(trajetoId));
    }

    public ViagemVeiculoDTO createViagemVeiculo(CreateViagemVeiculoRequest request) {
        ViagemVeiculo viagemVeiculo = new ViagemVeiculo();
        viagemVeiculo.setTripId(request.tripId());
        if (request.veiculoId() != null) {
            Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
            viagemVeiculo.setVeiculo(veiculo);
        }
        if (request.trajetoId() != null) {
            Trajeto trajeto = trajetoRepository.findById(request.trajetoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
            viagemVeiculo.setTrajeto(trajeto);
        }
        return ViagemMapper.toVeiculoDTO(viagemVeiculoRepository.save(viagemVeiculo));
    }

    public void deleteViagemVeiculo(Long id) {
        viagemVeiculoRepository.deleteById(id);
    }

    private ViagemUtilizador findViagemUtilizador(Long id) {
        return viagemUtilizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem nao encontrada"));
    }

    private ViagemVeiculo findViagemVeiculo(Long id) {
        return viagemVeiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ViagemVeiculo nao encontrado"));
    }

    private TituloTransporte findTitulo(Long tituloId) {
        return tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titulo nao encontrado"));
    }

    private Paragem findParagem(Long paragemId) {
        return paragemRepository.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
    }
}
