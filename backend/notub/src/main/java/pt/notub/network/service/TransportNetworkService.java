package pt.notub.network.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.dto.LinhaDTO;
import pt.notub.network.dto.LinhaRequest;
import pt.notub.network.dto.ParagemDTO;
import pt.notub.network.dto.ParagemRequest;
import pt.notub.network.dto.PontoPassagemDTO;
import pt.notub.network.dto.PontoPassagemRequest;
import pt.notub.network.dto.TrajetoDTO;
import pt.notub.network.dto.TrajetoRequest;
import pt.notub.network.entity.Linha;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.mapper.LinhaMapper;
import pt.notub.network.mapper.ParagemMapper;
import pt.notub.network.mapper.PontoPassagemMapper;
import pt.notub.network.mapper.TrajetoMapper;
import pt.notub.network.repository.LinhaRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;
import pt.notub.vehicle.entity.Point;
import pt.notub.zone.entity.Zona;
import pt.notub.zone.repository.ZonaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransportNetworkService {

    private final ParagemRepository paragemRepository;
    private final LinhaRepository linhaRepository;
    private final TrajetoRepository trajetoRepository;
    private final PontosDePassagemRepository pontosDePassagemRepository;
    private final ZonaRepository zonaRepository;

    public TransportNetworkService(ParagemRepository paragemRepository,
                                   LinhaRepository linhaRepository,
                                   TrajetoRepository trajetoRepository,
                                   PontosDePassagemRepository pontosDePassagemRepository,
                                   ZonaRepository zonaRepository) {
        this.paragemRepository = paragemRepository;
        this.linhaRepository = linhaRepository;
        this.trajetoRepository = trajetoRepository;
        this.pontosDePassagemRepository = pontosDePassagemRepository;
        this.zonaRepository = zonaRepository;
    }

    public List<ParagemDTO> getAllParagens() {
        return ParagemMapper.toDTOList(paragemRepository.findAll());
    }

    public ParagemDTO getParagemById(Long id) {
        return ParagemMapper.toDTO(findParagem(id));
    }

    public List<LinhaDTO> getAllLinhas() {
        return LinhaMapper.toDTOList(linhaRepository.findAll(), getTrajetosByLinhaMap());
    }

    public LinhaDTO getLinhaById(Long id) {
        Linha linha = findLinha(id);
        return LinhaMapper.toDTO(linha, findTrajetosForLinha(id));
    }

    public List<TrajetoDTO> getAllTrajetos() {
        return TrajetoMapper.toDTOList(trajetoRepository.findAll());
    }

    public TrajetoDTO getTrajetoById(Long id) {
        return TrajetoMapper.toDTO(findTrajeto(id));
    }

    public List<TrajetoDTO> getTrajetosByLinha(Long linhaId) {
        findLinha(linhaId);
        return TrajetoMapper.toDTOList(trajetoRepository.findByLinhaId(linhaId));
    }

    public List<PontoPassagemDTO> getPontosByTrajeto(Long trajetoId) {
        findTrajeto(trajetoId);
        return PontoPassagemMapper.toDTOList(pontosDePassagemRepository.findByTrajetoIdOrderByOrdemAsc(trajetoId));
    }

    public Map<Long, List<Trajeto>> getTrajetosByLinhaMap() {
        return trajetoRepository.findByLinhaIsNotNull().stream()
                .collect(Collectors.groupingBy(t -> t.getLinha().getId()));
    }

    public LinhaDTO createLinha(LinhaRequest pedido) {
        Linha linha = new Linha();
        if (pedido.nome() == null || pedido.nome().isBlank()) {
            throw new PedidoInvalidoException("Nome da linha invalido");
        }
        linha.setNome(pedido.nome());
        linha.setIdentificadorServico(pedido.identificadorServico());
        Linha saved = linhaRepository.save(linha);
        return LinhaMapper.toDTO(saved, findTrajetosForLinha(saved.getId()));
    }

    public LinhaDTO updateLinha(Long id, LinhaRequest updated) {
        Linha linha = findLinha(id);
        if (updated.nome() != null) {
            linha.setNome(updated.nome());
        }
        if (updated.identificadorServico() != null) {
            linha.setIdentificadorServico(updated.identificadorServico());
        }
        return LinhaMapper.toDTO(linhaRepository.save(linha), findTrajetosForLinha(id));
    }

    public void deleteLinha(Long id) {
        linhaRepository.deleteById(id);
    }

    public TrajetoDTO createTrajeto(TrajetoRequest pedido) {
        Trajeto trajeto = new Trajeto();
        applyTrajetoRequest(trajeto, pedido);
        return TrajetoMapper.toDTO(trajetoRepository.save(trajeto));
    }

    public TrajetoDTO updateTrajeto(Long id, TrajetoRequest updated) {
        Trajeto trajeto = findTrajeto(id);
        applyTrajetoRequest(trajeto, updated);
        return TrajetoMapper.toDTO(trajetoRepository.save(trajeto));
    }

    public void deleteTrajeto(Long id) {
        trajetoRepository.deleteById(id);
    }

    public ParagemDTO createParagem(ParagemRequest pedido) {
        Paragem paragem = new Paragem();
        applyParagemRequest(paragem, pedido);
        return ParagemMapper.toDTO(paragemRepository.save(paragem));
    }

    public ParagemDTO updateParagem(Long id, ParagemRequest updated) {
        Paragem paragem = findParagem(id);
        applyParagemRequest(paragem, updated);
        return ParagemMapper.toDTO(paragemRepository.save(paragem));
    }

    public void deleteParagem(Long id) {
        paragemRepository.deleteById(id);
    }

    public PontoPassagemDTO addPonto(Long trajetoId, PontoPassagemRequest pedido) {
        Trajeto trajeto = findTrajeto(trajetoId);
        PontosDePassagem ponto = new PontosDePassagem();
        ponto.setOrdem(requireValue(pedido.ordem(), "Ordem invalida"));
        if (pedido.horaChegada() != null) {
            ponto.setHoraChegada(pedido.horaChegada());
        }
        if (pedido.tempoDesdeInicio() != null) {
            ponto.setTempoDesdeInicio(pedido.tempoDesdeInicio());
        }
        if (pedido.paragemId() != null) {
            ponto.setParagem(findParagem(pedido.paragemId()));
        } else {
            throw new PedidoInvalidoException("Paragem invalida");
        }
        if (trajeto.getPontosDePassagem() == null) {
            trajeto.setPontosDePassagem(new ArrayList<>());
        }
        trajeto.getPontosDePassagem().add(ponto);
        trajetoRepository.save(trajeto);
        return PontoPassagemMapper.toDTO(ponto);
    }

    public void deletePonto(Long id) {
        pontosDePassagemRepository.deleteById(id);
    }

    private Linha findLinha(Long id) {
        return linhaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada"));
    }

    private Trajeto findTrajeto(Long id) {
        return trajetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
    }

    private Paragem findParagem(Long id) {
        return paragemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
    }

    private List<Trajeto> findTrajetosForLinha(Long linhaId) {
        return trajetoRepository.findByLinhaId(linhaId);
    }

    private void applyTrajetoRequest(Trajeto trajeto, TrajetoRequest pedido) {
        if (pedido.direcao() != null) {
            try {
                trajeto.setDirecao(pt.notub.network.entity.Direcao.valueOf(pedido.direcao().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new PedidoInvalidoException("Direcao invalida");
            }
        }
        if (pedido.linhaId() != null) {
            trajeto.setLinha(findLinha(pedido.linhaId()));
        }
    }

    private void applyParagemRequest(Paragem paragem, ParagemRequest pedido) {
        if (pedido.nome() != null) {
            paragem.setNome(pedido.nome());
        }
        if (pedido.localizacao() != null) {
            paragem.setLocalizacao(pedido.localizacao());
        }
        if (pedido.zonaId() != null) {
            Zona zona = zonaRepository.findById(pedido.zonaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
            paragem.setZona(zona);
        }
    }

    private int requireValue(Integer value, String message) {
        if (value == null) {
            throw new PedidoInvalidoException(message);
        }
        return value;
    }
}
