package pt.notub.trip.service;

import org.springframework.stereotype.Service;
import pt.notub.driver.service.NotificacaoValidacaoService;
import pt.notub.driver.dto.NotificacaoValidacaoDTO;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.entity.Paragem;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.points.service.ServicoPontos;
import pt.notub.ticket.entity.Bilhete;
import pt.notub.ticket.entity.Passe;
import pt.notub.ticket.entity.TituloTransporte;
import pt.notub.ticket.repository.TituloTransporteRepository;
import pt.notub.trip.entity.EstadoViagem;
import pt.notub.trip.entity.ViagemUtilizador;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.repository.ViagemUtilizadorRepository;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.validation.service.GestorValidacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ViagemService {

    private final ViagemUtilizadorRepository viagemUtilizadorRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ParagemRepository paragemRepository;
    private final TituloTransporteRepository tituloTransporteRepository;
    private final ServicoPontos servicoPontos;
    private final GestorValidacao gestorValidacao;
    private final NotificacaoValidacaoService notificacaoService;

    public ViagemService(ViagemUtilizadorRepository viagemUtilizadorRepository,
                         ViagemVeiculoRepository viagemVeiculoRepository,
                         UtilizadorRepository utilizadorRepository,
                         ParagemRepository paragemRepository,
                         TituloTransporteRepository tituloTransporteRepository,
                         ServicoPontos servicoPontos,
                         GestorValidacao gestorValidacao,
                         NotificacaoValidacaoService notificacaoService) {
        this.viagemUtilizadorRepository = viagemUtilizadorRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.paragemRepository = paragemRepository;
        this.tituloTransporteRepository = tituloTransporteRepository;
        this.servicoPontos = servicoPontos;
        this.gestorValidacao = gestorValidacao;
        this.notificacaoService = notificacaoService;
    }

    public List<ViagemUtilizador> getAllViagensUtilizador() {
        return viagemUtilizadorRepository.findAll();
    }

    public List<ViagemUtilizador> getViagensByUtilizador(Long utilizadorId) {
        return viagemUtilizadorRepository.findByUtilizadorId(utilizadorId);
    }

    public Optional<ViagemUtilizador> getViagemUtilizadorById(Long id) {
        return viagemUtilizadorRepository.findById(id);
    }

    public ViagemUtilizador iniciarViagem(Long tituloId, Long paragemEntradaId, Long viagemVeiculoId) {
        TituloTransporte titulo = tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titulo nao encontrado"));
        Paragem paragemEntrada = paragemRepository.findById(paragemEntradaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        ViagemVeiculo viagemVeiculo = viagemVeiculoRepository.findById(viagemVeiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ViagemVeiculo nao encontrado"));

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

        return saved;
    }

    private static final int MAX_HOURS_FOR_POINTS = 24;

    public ViagemUtilizador terminarViagem(Long viagemId, Long paragemSaidaId) {
        ViagemUtilizador viagem = viagemUtilizadorRepository.findById(viagemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem nao encontrada"));
        Paragem paragemSaida = paragemRepository.findById(paragemSaidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));

        viagem.setParagemSaida(paragemSaida);
        viagem.setFim(LocalDateTime.now());
        viagem.setEstado(EstadoViagem.CONCLUIDA);

        ViagemUtilizador saved = viagemUtilizadorRepository.save(viagem);

        boolean concederPontos = viagem.getInicio() != null &&
                java.time.Duration.between(viagem.getInicio(), viagem.getFim()).toHours() <= MAX_HOURS_FOR_POINTS;

        if (concederPontos) {
            if (viagem.getTitulo() instanceof Bilhete bilhete && bilhete.getUtilizador() != null) {
                servicoPontos.atribuirPontosViagem(bilhete.getUtilizador().getId());
            } else if (viagem.getTitulo() instanceof Passe passe && passe.getUtilizador() != null) {
                servicoPontos.atribuirPontosViagem(passe.getUtilizador().getId());
            }
        }

        return saved;
    }

    public List<ViagemVeiculo> getAllViagensVeiculo() {
        return viagemVeiculoRepository.findAll();
    }

    public Optional<ViagemVeiculo> getViagemVeiculoById(Long id) {
        return viagemVeiculoRepository.findById(id);
    }

    public List<ViagemVeiculo> getViagensByVeiculo(Long veiculoId) {
        return viagemVeiculoRepository.findByVeiculoId(veiculoId);
    }

    public List<ViagemVeiculo> getViagensByTrajeto(Long trajetoId) {
        return viagemVeiculoRepository.findByTrajetoId(trajetoId);
    }

    public ViagemVeiculo createViagemVeiculo(ViagemVeiculo viagemVeiculo) {
        return viagemVeiculoRepository.save(viagemVeiculo);
    }

    public void deleteViagemVeiculo(Long id) {
        viagemVeiculoRepository.deleteById(id);
    }
}
