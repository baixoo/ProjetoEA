package pt.notub.driver.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.ConflitoException;
import pt.notub.driver.dto.DriverTrajetoDTO;
import pt.notub.driver.dto.StartViagemRequest;
import pt.notub.driver.dto.ViagemScheduleDTO;
import pt.notub.driver.dto.AvancarViagemResponseDTO;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.entity.Horario;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.network.repository.HorarioRepository;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.entity.Viagem;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.mapper.ViagemMapper;
import pt.notub.trip.repository.ViagemVeiculoRepository;
import pt.notub.trip.repository.ViagemRepository;
import pt.notub.vehicle.dto.VeiculoDTO;
import pt.notub.vehicle.entity.Veiculo;
import pt.notub.vehicle.mapper.VeiculoMapper;
import pt.notub.vehicle.repository.VeiculoRepository;

@Service
public class DriverService {

    private final VeiculoRepository veiculoRepository;
    private final ViagemVeiculoRepository viagemVeiculoRepository;
    private final TrajetoRepository trajetoRepository;
    private final ViagemRepository viagemRepository;
    private final HorarioRepository horarioRepository;

    public DriverService(VeiculoRepository veiculoRepository,
                         ViagemVeiculoRepository viagemVeiculoRepository,
                         TrajetoRepository trajetoRepository,
                         ViagemRepository viagemRepository,
                         HorarioRepository horarioRepository) {
        this.veiculoRepository = veiculoRepository;
        this.viagemVeiculoRepository = viagemVeiculoRepository;
        this.trajetoRepository = trajetoRepository;
        this.viagemRepository = viagemRepository;
        this.horarioRepository = horarioRepository;
    }

    public List<VeiculoDTO> getVeiculosComLinha() {
        return VeiculoMapper.toDTOList(veiculoRepository.findByLinhaIsNotNull());
    }

    public VeiculoDTO getVeiculoById(Long id) {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
        return VeiculoMapper.toDTO(veiculo);
    }

    public List<ViagemVeiculoDTO> getViagensAtivas(Long veiculoId) {
        return ViagemMapper.toVeiculoDTOList(viagemVeiculoRepository.findActiveByVeiculoId(veiculoId));
    }

    public List<DriverTrajetoDTO> getTrajetos(Long linhaId) {
        List<Trajeto> trajetos = linhaId != null
                ? trajetoRepository.findByLinhaId(linhaId)
                : trajetoRepository.findAll();

        return trajetos.stream().map(this::toDriverTrajetoDTO).toList();
    }

    public List<ViagemScheduleDTO> getHorariosDisponiveis(Long trajetoId) {
        LocalTime now = LocalTime.now();
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        List<String> serviceIds;
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            serviceIds = List.of("SAB", "ELECSAB");
        } else if (dayOfWeek == DayOfWeek.SUNDAY) {
            serviceIds = List.of("DOM", "ELECDOM");
        } else {
            serviceIds = List.of("UTEIS", "ELECUTEIS");
        }

        List<Viagem> all = viagemRepository.findByTrajetoAndServices(trajetoId, serviceIds);

        LocalTime start = now.minusHours(2);
        LocalTime end = now.plusMinutes(15);

        return all.stream()
                .filter(v -> {
                    LocalTime time = v.getHoraPartida();
                    if (time == null) return false;
                    if (start.isBefore(end)) {
                        return !time.isBefore(start) && !time.isAfter(end);
                    } else {
                        return !time.isBefore(start) || !time.isAfter(end);
                    }
                })
                .sorted(Comparator.comparing(Viagem::getHoraPartida))
                .map(v -> new ViagemScheduleDTO(v.getId(), v.getHoraPartida(), v.getGtfsTripId(), v.getServiceId()))
                .toList();
    }

    public ViagemVeiculoDTO startViagem(StartViagemRequest request) {
        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado"));
        Trajeto trajeto = trajetoRepository.findById(request.trajetoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
        Viagem viagem = viagemRepository.findById(request.viagemId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem nao encontrada"));

        // Validate vehicle line matches trajeto line, and scheduled run matches trajeto
        if (veiculo.getLinha() == null || trajeto.getLinha() == null ||
                !veiculo.getLinha().getId().equals(trajeto.getLinha().getId())) {
            throw new PedidoInvalidoException("A linha do veículo não coincide com a linha do trajeto.");
        }
        if (viagem.getTrajeto() == null || !viagem.getTrajeto().getId().equals(trajeto.getId())) {
            throw new PedidoInvalidoException("A viagem agendada não coincide com o trajeto.");
        }

        // Get first stop of trajeto
        if (trajeto.getPontosDePassagem() == null || trajeto.getPontosDePassagem().isEmpty()) {
            throw new PedidoInvalidoException("O trajeto não tem pontos de passagem.");
        }
        PontosDePassagem firstPonto = trajeto.getPontosDePassagem().stream()
                .min(Comparator.comparingInt(PontosDePassagem::getOrdem))
                .orElseThrow(() -> new PedidoInvalidoException("O trajeto não tem pontos de passagem."));

        // Ensure there is no active run for this vehicle
        List<ViagemVeiculo> active = viagemVeiculoRepository.findActiveByVeiculoId(request.veiculoId());
        if (!active.isEmpty()) {
            throw new ConflitoException("O veículo já tem uma viagem ativa.");
        }

        ViagemVeiculo viagemVeiculo = new ViagemVeiculo();
        viagemVeiculo.setVeiculo(veiculo);
        viagemVeiculo.setTrajeto(trajeto);
        viagemVeiculo.setViagemPlaneada(viagem);
        viagemVeiculo.setPontoAtual(firstPonto);
        viagemVeiculo.setStartTime(LocalDateTime.now());

        // Calculate initial delay: click time vs viagem.horaPartida
        LocalTime clickTime = LocalTime.now();
        long diffMinutos = ChronoUnit.MINUTES.between(viagem.getHoraPartida(), clickTime);
        int delay = (int) Math.max(0, diffMinutos);
        veiculo.setTempoAtraso(delay);
        veiculoRepository.save(veiculo);

        return ViagemMapper.toVeiculoDTO(viagemVeiculoRepository.save(viagemVeiculo));
    }

    public AvancarViagemResponseDTO avancarViagem(Long id) {
        ViagemVeiculo viagemVeiculo = viagemVeiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem de veículo não encontrada"));
        
        if (viagemVeiculo.getStartTime() == null || viagemVeiculo.getFinishTime() != null) {
            throw new ConflitoException("A viagem não está ativa");
        }

        Trajeto trajeto = viagemVeiculo.getTrajeto();
        if (trajeto == null || trajeto.getPontosDePassagem() == null || trajeto.getPontosDePassagem().isEmpty()) {
            throw new PedidoInvalidoException("Trajeto sem pontos de passagem");
        }

        List<PontosDePassagem> sortedPontos = trajeto.getPontosDePassagem().stream()
                .sorted(Comparator.comparingInt(PontosDePassagem::getOrdem))
                .toList();

        PontosDePassagem pontoAtual = viagemVeiculo.getPontoAtual();
        int currentIndex = -1;
        if (pontoAtual != null) {
            for (int i = 0; i < sortedPontos.size(); i++) {
                if (sortedPontos.get(i).getId().equals(pontoAtual.getId())) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int nextIndex = currentIndex + 1;
        if (nextIndex >= sortedPontos.size()) {
            // Already at final stop, cannot advance
            boolean isFinal = true;
            int currentDelay = viagemVeiculo.getVeiculo() != null && viagemVeiculo.getVeiculo().getTempoAtraso() != null 
                    ? viagemVeiculo.getVeiculo().getTempoAtraso() : 0;
            return new AvancarViagemResponseDTO(
                    pontoAtual != null ? pontoAtual.getId() : null,
                    pontoAtual != null && pontoAtual.getParagem() != null ? pontoAtual.getParagem().getNome() : "",
                    null,
                    "",
                    isFinal,
                    currentDelay
            );
        }

        PontosDePassagem pontoSeguinte = sortedPontos.get(nextIndex);
        viagemVeiculo.setPontoAtual(pontoSeguinte);

        // Check if next stop is the last one in the list
        boolean isFinal = (nextIndex == sortedPontos.size() - 1);

        // Calculate delay for this next stop
        int delay = 0;
        Viagem viagemPlaneada = viagemVeiculo.getViagemPlaneada();
        if (viagemPlaneada != null && viagemPlaneada.getGtfsTripId() != null) {
            Optional<Horario> horarioOpt = horarioRepository.findByPontoPassagemIdAndGtfsTripId(pontoSeguinte.getId(), viagemPlaneada.getGtfsTripId());
            if (horarioOpt.isPresent()) {
                LocalTime clickTime = LocalTime.now();
                long diffMinutos = ChronoUnit.MINUTES.between(horarioOpt.get().getHora(), clickTime);
                delay = (int) Math.max(0, diffMinutos);
            }
        }

        Veiculo veiculo = viagemVeiculo.getVeiculo();
        if (veiculo != null) {
            veiculo.setTempoAtraso(delay);
            veiculoRepository.save(veiculo);
        }

        viagemVeiculoRepository.save(viagemVeiculo);

        PontosDePassagem pontoDepois = (nextIndex + 1 < sortedPontos.size()) ? sortedPontos.get(nextIndex + 1) : null;

        return new AvancarViagemResponseDTO(
                pontoSeguinte.getId(),
                pontoSeguinte.getParagem() != null ? pontoSeguinte.getParagem().getNome() : "",
                pontoDepois != null ? pontoDepois.getId() : null,
                pontoDepois != null && pontoDepois.getParagem() != null ? pontoDepois.getParagem().getNome() : "",
                isFinal,
                delay
        );
    }

    public void endViagem(Long id) {
        ViagemVeiculo viagem = viagemVeiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Viagem nao encontrada"));
        viagem.setFinishTime(LocalDateTime.now());
        viagemVeiculoRepository.save(viagem);

        Veiculo veiculo = viagem.getVeiculo();
        if (veiculo != null) {
            veiculo.setTempoAtraso(0);
            veiculo.setLotacaoAtual(0);
            veiculoRepository.save(veiculo);
        }
    }

    private DriverTrajetoDTO toDriverTrajetoDTO(Trajeto trajeto) {
        String linhaNome = trajeto.getLinha() != null ? trajeto.getLinha().getNome() : "Sem linha";
        String direcao = trajeto.getDirecao() != null ? trajeto.getDirecao().name() : "?";
        String primeiraParagem = "";
        String ultimaParagem = "";

        if (trajeto.getPontosDePassagem() != null && !trajeto.getPontosDePassagem().isEmpty()) {
            List<PontosDePassagem> sorted = trajeto.getPontosDePassagem().stream()
                    .sorted(Comparator.comparingInt(PontosDePassagem::getOrdem))
                    .toList();
            if (sorted.getFirst().getParagem() != null) {
                primeiraParagem = sorted.getFirst().getParagem().getNome();
            }
            if (sorted.getLast().getParagem() != null) {
                ultimaParagem = sorted.getLast().getParagem().getNome();
            }
        }

        return new DriverTrajetoDTO(trajeto.getId(), linhaNome, direcao, primeiraParagem, ultimaParagem);
    }
}
