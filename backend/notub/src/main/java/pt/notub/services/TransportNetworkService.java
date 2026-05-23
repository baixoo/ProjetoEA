package pt.notub.services;

import org.springframework.stereotype.Service;
import pt.notub.models.Linha;
import pt.notub.models.Paragem;
import pt.notub.models.PontosDePassagem;
import pt.notub.models.Trajeto;
import pt.notub.repositories.LinhaRepository;
import pt.notub.repositories.ParagemRepository;
import pt.notub.repositories.PontosDePassagemRepository;
import pt.notub.repositories.TrajetoRepository;

import java.util.List;
import java.util.Optional;

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
}
