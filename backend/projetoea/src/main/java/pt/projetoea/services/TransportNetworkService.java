package pt.projetoea.services;

import org.springframework.stereotype.Service;
import pt.projetoea.models.Carreira;
import pt.projetoea.models.Paragem;
import pt.projetoea.models.SequenciaParagem;
import pt.projetoea.models.Trajeto;
import pt.projetoea.repositories.CarreiraRepository;
import pt.projetoea.repositories.ParagemRepository;
import pt.projetoea.repositories.SequenciaParagemRepository;
import pt.projetoea.repositories.TrajetoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TransportNetworkService {

    private final ParagemRepository paragemRepository;
    private final CarreiraRepository carreiraRepository;
    private final TrajetoRepository trajetoRepository;
    private final SequenciaParagemRepository sequenciaParagemRepository;

    public TransportNetworkService(ParagemRepository paragemRepository,
                                   CarreiraRepository carreiraRepository,
                                   TrajetoRepository trajetoRepository,
                                   SequenciaParagemRepository sequenciaParagemRepository) {
        this.paragemRepository = paragemRepository;
        this.carreiraRepository = carreiraRepository;
        this.trajetoRepository = trajetoRepository;
        this.sequenciaParagemRepository = sequenciaParagemRepository;
    }

    // ---- Paragens ----

    public List<Paragem> getAllParagens() {
        return paragemRepository.findAll();
    }

    public Optional<Paragem> getParagemById(Long id) {
        return paragemRepository.findById(id);
    }

    // ---- Carreiras ----

    public List<Carreira> getAllCarreiras() {
        return carreiraRepository.findAll();
    }

    public Optional<Carreira> getCarreiraById(Long id) {
        return carreiraRepository.findById(id);
    }

    // ---- Trajetos ----

    public List<Trajeto> getAllTrajetos() {
        return trajetoRepository.findAll();
    }

    public Optional<Trajeto> getTrajetoById(Long id) {
        return trajetoRepository.findById(id);
    }

    public List<Trajeto> getTrajetosByCarreira(Long carreiraId) {
        return trajetoRepository.findByCarreiraId(carreiraId);
    }

    // ---- SequenciaParagem ----

    public List<SequenciaParagem> getSequenciasByTrajeto(Long trajetoId) {
        return sequenciaParagemRepository.findByTrajetoIdOrderByOrdemAsc(trajetoId);
    }
}
