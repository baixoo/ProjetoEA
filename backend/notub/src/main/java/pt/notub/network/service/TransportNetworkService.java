package pt.notub.network.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.entity.Linha;
import pt.notub.network.entity.Paragem;
import pt.notub.network.entity.PontosDePassagem;
import pt.notub.network.entity.Trajeto;
import pt.notub.network.repository.LinhaRepository;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.network.repository.PontosDePassagemRepository;
import pt.notub.network.repository.TrajetoRepository;

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

    public TransportNetworkService(ParagemRepository paragemRepository,
                                   LinhaRepository linhaRepository,
                                   TrajetoRepository trajetoRepository,
                                   PontosDePassagemRepository pontosDePassagemRepository) {
        this.paragemRepository = paragemRepository;
        this.linhaRepository = linhaRepository;
        this.trajetoRepository = trajetoRepository;
        this.pontosDePassagemRepository = pontosDePassagemRepository;
    }

    public List<Paragem> getAllParagens() {
        return paragemRepository.findAll();
    }

    public Optional<Paragem> getParagemById(Long id) {
        return paragemRepository.findById(id);
    }

    public List<Linha> getAllLinhas() {
        return linhaRepository.findAll();
    }

    public Optional<Linha> getLinhaById(Long id) {
        return linhaRepository.findById(id);
    }

    public List<Trajeto> getAllTrajetos() {
        return trajetoRepository.findAll();
    }

    public Optional<Trajeto> getTrajetoById(Long id) {
        return trajetoRepository.findById(id);
    }

    public List<Trajeto> getTrajetosByLinha(Long linhaId) {
        return trajetoRepository.findByLinhaId(linhaId);
    }

    public List<PontosDePassagem> getPontosByTrajeto(Long trajetoId) {
        return pontosDePassagemRepository.findByTrajetoIdOrderByOrdemAsc(trajetoId);
    }

    public Map<Long, List<Trajeto>> getTrajetosByLinhaMap() {
        return trajetoRepository.findByLinhaIsNotNull().stream()
                .collect(Collectors.groupingBy(t -> t.getLinha().getId()));
    }

    public List<Trajeto> getTrajetosForLinha(Long linhaId) {
        return trajetoRepository.findByLinhaId(linhaId);
    }

    public Linha createLinha(Linha linha) {
        return linhaRepository.save(linha);
    }

    public Linha updateLinha(Long id, Linha updated) {
        Linha linha = linhaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada"));
        if (updated.getNome() != null) {
            linha.setNome(updated.getNome());
        }
        if (updated.getIdentificadorServico() != null) {
            linha.setIdentificadorServico(updated.getIdentificadorServico());
        }
        return linhaRepository.save(linha);
    }

    public void deleteLinha(Long id) {
        linhaRepository.deleteById(id);
    }

    public Trajeto createTrajeto(Trajeto trajeto) {
        return trajetoRepository.save(trajeto);
    }

    public Trajeto updateTrajeto(Long id, Trajeto updated) {
        Trajeto trajeto = trajetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
        if (updated.getDirecao() != null) {
            trajeto.setDirecao(updated.getDirecao());
        }
        if (updated.getLinha() != null) {
            trajeto.setLinha(updated.getLinha());
        }
        return trajetoRepository.save(trajeto);
    }

    public void deleteTrajeto(Long id) {
        trajetoRepository.deleteById(id);
    }

    public Paragem createParagem(Paragem paragem) {
        return paragemRepository.save(paragem);
    }

    public Paragem updateParagem(Long id, Paragem updated) {
        Paragem paragem = paragemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        if (updated.getNome() != null) {
            paragem.setNome(updated.getNome());
        }
        if (updated.getLocalizacao() != null) {
            paragem.setLocalizacao(updated.getLocalizacao());
        }
        return paragemRepository.save(paragem);
    }

    public void deleteParagem(Long id) {
        paragemRepository.deleteById(id);
    }

    public PontosDePassagem addPonto(Long trajetoId, PontosDePassagem ponto) {
        Trajeto trajeto = trajetoRepository.findById(trajetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trajeto nao encontrado"));
        if (trajeto.getPontosDePassagem() == null) {
            trajeto.setPontosDePassagem(new ArrayList<>());
        }
        trajeto.getPontosDePassagem().add(ponto);
        trajetoRepository.save(trajeto);
        return ponto;
    }

    public void deletePonto(Long id) {
        pontosDePassagemRepository.deleteById(id);
    }
}
