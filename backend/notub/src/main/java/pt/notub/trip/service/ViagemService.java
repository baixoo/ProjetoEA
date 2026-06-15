package pt.notub.trip.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pt.notub.common.exception.ConflitoException;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;
import pt.notub.driver.dto.NotificacaoValidacaoDTO;
import pt.notub.driver.service.NotificacaoValidacaoService;
import pt.notub.network.entity.Horario;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.points.service.ServicoPontos;
import pt.notub.ticket.entity.Bilhete;
import pt.notub.ticket.entity.Passe;
import pt.notub.ticket.entity.TipoTituloTransporte;
import pt.notub.ticket.entity.TituloTransporte;
import pt.notub.ticket.repository.TituloTransporteRepository;
import pt.notub.trip.dto.CreateViagemVeiculoRequest;
import pt.notub.trip.dto.ParagemAtualDTO;
import pt.notub.trip.dto.ViagemDTO;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.dto.ZonaMinMaxDTO;
import pt.notub.trip.entity.EstadoViagem;
import pt.notub.trip.entity.Monitorizacao;
import pt.notub.trip.entity.ViagemUtilizador;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.mapper.ViagemMapper;
import pt.notub.trip.repository.ViagemUtilizadorRepository;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.trip.repository.MonitorizacaoRepository;
import pt.notub.network.repository.PontosDePassagemRepository; 

// Service
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;
import pt.notub.validation.service.GestorValidacao;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.repository.VeiculoRepository;

import pt.notub.trip.observerPattern.VehicleTripSubject;
import pt.notub.trip.observerPattern.VehicleTripObserver;

import jakarta.transaction.Transactional;

import pt.notub.common.security.AuthenticatedUser;
import pt.notub.common.security.AuthenticatedUserContext;

@Service
public class ViagemService implements VehicleTripSubject {

    private static final int MAX_HOURS_FOR_POINTS = 24;

    private final ViagemUtilizadorRepository viagemUtilizadorRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;
    private final ParagemRepository paragemRepository;
    private final HorarioRepository horarioRepository;
    private final TituloTransporteRepository tituloTransporteRepository;
    private final VeiculoRepository veiculoRepository;
    private final TrajetoRepository trajetoRepository;
    private final MonitorizacaoRepository monitorizacaoRepository;
    private final MonitorizacaoService monitorizacaoService;
    private final UtilizadorRepository utilizadorRepository;
    private final ServicoPontos servicoPontos;
    private final GestorValidacao gestorValidacao;
    private final NotificacaoValidacaoService notificacaoService;
    private final PontosDePassagemRepository pontosDePassagemRepository;


    public ViagemService(ViagemUtilizadorRepository viagemUtilizadorRepository,
                         ViagemVeiculoRepository viagemVeiculoRepository,
                         ParagemRepository paragemRepository,
                         HorarioRepository horarioRepository,
                         TituloTransporteRepository tituloTransporteRepository,
                         VeiculoRepository veiculoRepository,
                         TrajetoRepository trajetoRepository,
                         MonitorizacaoRepository monitorizacaoRepository,
                         MonitorizacaoService monitorizacaoService,
                         UtilizadorRepository utilizadorRepository,
                         ServicoPontos servicoPontos,
                         GestorValidacao gestorValidacao,
                         NotificacaoValidacaoService notificacaoService,
                         PontosDePassagemRepository pontosDePassagemRepository) {
        this.viagemUtilizadorRepository = viagemUtilizadorRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
        this.paragemRepository = paragemRepository;
        this.horarioRepository = horarioRepository;
        this.tituloTransporteRepository = tituloTransporteRepository;
        this.veiculoRepository = veiculoRepository;
        this.trajetoRepository = trajetoRepository;
        this.monitorizacaoRepository = monitorizacaoRepository;
        this.monitorizacaoService = monitorizacaoService;
        this.utilizadorRepository = utilizadorRepository;
        this.servicoPontos = servicoPontos;
        this.gestorValidacao = gestorValidacao;
        this.notificacaoService = notificacaoService;
        this.pontosDePassagemRepository = pontosDePassagemRepository;
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

    private List<TituloTransporte> getValidTitulos(Long utilizadorId, String tipoTitulo, Long quantidade, Integer paragemZona) {

        TipoTituloTransporte tipoEnum = TipoTituloTransporte.valueOf(tipoTitulo.toUpperCase());
        List<TituloTransporte> titulos = tituloTransporteRepository.findByTipoAndUtilizadorId(tipoEnum, utilizadorId);

        if (titulos == null || titulos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum título de transporte encontrado para o utilizador e tipo fornecidos");
        }

        if (paragemZona != null) {
            titulos = titulos.stream()
                    .filter(t -> t.getZona() != null && t.getZona().getNum() >= paragemZona)
                    .filter(t -> {
                        if (t instanceof Bilhete) {
                            return !((Bilhete) t).isUsado();
                        }
                        return true; 
                    })
                    .sorted(Comparator.comparingInt(t -> t.getZona().getNum()))
                    .toList();
        }

        if (titulos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum título de transporte válido encontrado para os critérios fornecidos");
        }

        if (titulos.size() < quantidade) {
            throw new RecursoNaoEncontradoException(
                String.format("Títulos válidos insuficientes. Pedidos: %d, Disponíveis: %d", quantidade, titulos.size())
            );
        }

        return titulos.stream().limit(quantidade).toList();
    }

    public ViagemDTO iniciarViagem(Long utilizadorId, String tipoTitulo, Long quantidade, Long paragemEntradaId, Long viagemVeiculoId) {
        Paragem paragemEntrada = findParagem(paragemEntradaId);
        Integer paragemZona = paragemEntrada.getZona() != null ? paragemEntrada.getZona().getNum() : null;
        
        List<TituloTransporte> titulosAValidar = getValidTitulos(utilizadorId, tipoTitulo, quantidade, paragemZona);
        ViagemVeiculo viagemVeiculo = findViagemVeiculo(viagemVeiculoId);
        
        boolean todosValidos = true;

        for (TituloTransporte titulo : titulosAValidar) {
            boolean valido = gestorValidacao.validarTitulo(titulo.getId());
            
            if (!valido) {
                todosValidos = false;
                throw new RuntimeException("Falha ao validar o título com ID: " + titulo.getId());
            }
            
            if (titulo instanceof Bilhete) {
                ((Bilhete) titulo).setUsado(true);
                tituloTransporteRepository.save(titulo);
            }
        }

        TituloTransporte tituloPrincipal = titulosAValidar.get(0);

        ViagemUtilizador viagem = new ViagemUtilizador();
        viagem.setTitulo(tituloPrincipal); 
        viagem.setParagemEntrada(paragemEntrada);
        viagem.setViagemVeiculo(viagemVeiculo);
        viagem.setInicio(LocalDateTime.now());
        viagem.setEstado(EstadoViagem.ATIVA);
        
        if (tituloPrincipal.getUtilizador() != null) {
            viagem.setUtilizador(tituloPrincipal.getUtilizador());
        }

        ViagemUtilizador saved = viagemUtilizadorRepository.save(viagem);

        Veiculo veiculo = viagemVeiculo.getVeiculo();
        if (veiculo != null) {
            veiculo.setLotacaoAtual(veiculo.getLotacaoAtual() + quantidade.intValue());
            veiculoRepository.save(veiculo);
        }

        if (tituloPrincipal.getUtilizador() != null) {
            Long userId = tituloPrincipal.getUtilizador().getId();
            this.addSubscription(viagemVeiculoId, userId);
        }

        String nomePassageiro = "Desconhecido";
        if (tituloPrincipal.getUtilizador() != null) {
            nomePassageiro = tituloPrincipal.getUtilizador().getPrimeiroNome() + " " + tituloPrincipal.getUtilizador().getUltimoNome();
        }

        if (quantidade > 1) {
            nomePassageiro += " (Grupo de " + quantidade + ")";
        }

        String tituloTipo = "";
        if (tituloPrincipal instanceof Bilhete) {
            tituloTipo = "Bilhete";
        } else if (tituloPrincipal instanceof Passe passe) {
            tituloTipo = "Passe " + passe.getModalidade().name(); 
        }

        Long veiculoId = viagemVeiculo.getVeiculo() != null ? viagemVeiculo.getVeiculo().getId() : null;
        NotificacaoValidacaoDTO notificacao = new NotificacaoValidacaoDTO(
                todosValidos, nomePassageiro, tituloTipo, veiculoId, LocalDateTime.now());
        notificacaoService.notificarMotorista(notificacao);

        return ViagemMapper.toDTO(saved);
    }

    public ViagemDTO terminarViagem(Long viagemId, Long pontoPassagemSaidaId) {
        ViagemUtilizador viagem = findViagemUtilizador(viagemId);
        PontosDePassagem pontoPassagemSaida = pontosDePassagemRepository.findById(pontoPassagemSaidaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto de passagem de saída não encontrado"));


        Paragem paragemSaida = pontoPassagemSaida.getParagem();
        if (paragemSaida == null) {
            throw new RecursoNaoEncontradoException("Paragem de saída não encontrada para o ponto de passagem especificado");
        }

        viagem.setParagemSaida(paragemSaida);
        viagem.setFim(LocalDateTime.now());
        viagem.setEstado(EstadoViagem.CONCLUIDA);

        ViagemUtilizador saved = viagemUtilizadorRepository.save(viagem);

        ViagemVeiculo viagemVeiculo = viagem.getViagemVeiculo();
        if (viagemVeiculo != null && viagemVeiculo.getVeiculo() != null) {
            Veiculo veiculo = viagemVeiculo.getVeiculo();
            int novaLotacao = Math.max(0, veiculo.getLotacaoAtual() - 1);
            veiculo.setLotacaoAtual(novaLotacao);
            veiculoRepository.save(veiculo);
        }

        
        boolean concederPontos = viagem.getInicio() != null
        && java.time.Duration.between(viagem.getInicio(), viagem.getFim()).toHours() <= MAX_HOURS_FOR_POINTS;
        
        if (concederPontos && viagem.getTitulo().getUtilizador() != null) {
            servicoPontos.atribuirPontosViagem(viagem.getTitulo().getUtilizador().getId());
        }

        // Ao terminar a viagem, remove a subscrição do utilizador para essa viagem automaticamente
        if (viagemVeiculo != null && viagem.getTitulo() != null && viagem.getTitulo().getUtilizador() != null) {
            Long userId = viagem.getTitulo().getUtilizador().getId();
            Long viagemVeiculoId = viagemVeiculo.getId();
            
            this.removeSubscription(viagemVeiculoId, userId);
        }

        return ViagemMapper.toDTO(saved);
    }

    public List<ViagemVeiculoDTO> getAllViagensVeiculo() {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findActiveViagens());
    }

    public ViagemVeiculoDTO getViagemVeiculoById(Long id) {
        return ViagemMapper.toVeiculoDTO(findViagemVeiculo(id));
    }

    public List<ViagemVeiculoDTO> getViagensByVeiculo(Long veiculoId) {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findActiveByVeiculoId(veiculoId));
    }

    public List<ViagemVeiculoDTO> getViagensByTrajeto(Long trajetoId) {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findByTrajetoId(trajetoId));
    }

    public ViagemVeiculoDTO createViagemVeiculo(CreateViagemVeiculoRequest request) {
        ViagemVeiculo viagemVeiculo = new ViagemVeiculo();
        // viagemVeiculo.setTripId(request.tripId());
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

    public ParagemAtualDTO getParagemAtual(Long viagemVeiculoId) {
        ViagemVeiculo viagem = findViagemVeiculo(viagemVeiculoId);
        if (viagem == null) {
            throw new RecursoNaoEncontradoException("ViagemVeiculo nao encontrada");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime startTime = viagem.getStartTime();
        LocalDateTime finishTime = viagem.getFinishTime();

        System.out.println("-> StartTime: " + startTime + " | FinishTime: " + finishTime + " | Agora: " + agora);

        if (startTime == null || (finishTime != null && (agora.isBefore(startTime) || agora.isAfter(finishTime)))) {
            throw new ConflitoException("Viagem nao esta a decorrer neste momento");
        }

        PontosDePassagem pontoAtual = viagem.getPontoAtual();
        
        if (pontoAtual == null) {
            throw new RecursoNaoEncontradoException("Ponto de passagem atual nao definido para este veiculo");
        }

        Paragem paragem = pontoAtual.getParagem();
        
        if (paragem == null || paragem.getId() == null) {
            throw new RecursoNaoEncontradoException("Paragem atual nao encontrada para o ponto de passagem");
        }

        return new ParagemAtualDTO(paragem.getId());
    }

    public ZonaMinMaxDTO getZonaMinMax(Long viagemVeiculoId, Long paragemId) {
        ViagemVeiculo viagem = findViagemVeiculo(viagemVeiculoId);
        List<PontosDePassagem> pontos = getPontosComParagem(viagem);

        if (pontos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Pontos de passagem nao encontrados");
        }

        int indiceParagemAtual = findIndiceParagem(pontos, paragemId);

        if (indiceParagemAtual == -1) {
            throw new PedidoInvalidoException("A paragem especificada nao pertence ao trajeto desta viagem.");
        }

        IntSummaryStatistics zonas = pontos.subList(indiceParagemAtual, pontos.size()).stream()
                .filter(p -> p.getParagem().getZona() != null)
                .mapToInt(p -> p.getParagem().getZona().getNum())
                .summaryStatistics();

        if (zonas.getCount() == 0) {
            throw new RecursoNaoEncontradoException("Zonas nao encontradas");
        }

        System.out.println("-> Zonas calculadas para ViagemVeiculo ID " + viagemVeiculoId + " a partir da Paragem ID " + paragemId + ": Min = " + zonas.getMin() + ", Max = " + zonas.getMax());
        return new ZonaMinMaxDTO(zonas.getMin(), zonas.getMax());
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

    private List<PontosDePassagem> getPontosComParagem(ViagemVeiculo viagem) {
        if (viagem.getTrajeto() == null || viagem.getTrajeto().getPontosDePassagem() == null) {
            return List.of();
        }

        return viagem.getTrajeto().getPontosDePassagem().stream()
                .filter(p -> p.getParagem() != null)
                .sorted(Comparator.comparingInt(PontosDePassagem::getOrdem))
                .toList();
    }

    private int findIndiceParagem(List<PontosDePassagem> pontos, Long paragemId) {
        for (int i = 0; i < pontos.size(); i++) {
            if (pontos.get(i).getParagem().getId().equals(paragemId)) {
                return i;
            }
        }
        return -1;
    }

    private TituloTransporte findTitulo(Long tituloId) {
        return tituloTransporteRepository.findById(tituloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titulo nao encontrado"));
    }

    private Paragem findParagem(Long paragemId) {
        return paragemRepository.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
    }

    // ======== Implementação do padrão Observer ========

    @Transactional
    @Override
    public void addSubscription(Long viagemVeiculoId, Long userId) {
        Utilizador utilizador = utilizadorRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        
        ViagemVeiculo viagemVeiculo = viagemVeiculoRepository.findById(viagemVeiculoId)
                .orElseThrow(() -> new RuntimeException("Viagem não encontrada"));

        Monitorizacao monitorizacao = new Monitorizacao();
        monitorizacao.setUtilizador(utilizador);
        monitorizacao.setViagemVeiculo(viagemVeiculo);

        monitorizacaoRepository.save(monitorizacao);
    }

    @Transactional
    @Override
    public void removeSubscription(Long viagemVeiculoId, Long userId) {
        System.out.println("Removing subscription for ViagemVeiculo ID " + viagemVeiculoId + " and User ID " + userId);
        Monitorizacao monitorizacao = monitorizacaoRepository
                .findByUtilizadorIdAndViagemVeiculoId(userId, viagemVeiculoId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        monitorizacaoRepository.delete(monitorizacao);
    }

    public void notifySubscribers(Long viagemId, Long novoPontoPassagemId) {
        monitorizacaoService.onLocationUpdate(viagemId, novoPontoPassagemId);
    }

    public void notifySubscribers(Long viagemId) {
        monitorizacaoService.onTripFinished(viagemId);
    }

}
