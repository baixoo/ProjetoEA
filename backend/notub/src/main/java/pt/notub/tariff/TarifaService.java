package pt.notub.tariff;

import org.springframework.stereotype.Service;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.ModalidadePasse;
import pt.notub.models.Tarifa;
import pt.notub.models.TipoUtilizador;
import pt.notub.repositories.TarifaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TarifaService {

    private final TarifaRepository tarifaRepository;

    public TarifaService(TarifaRepository tarifaRepository) {
        this.tarifaRepository = tarifaRepository;
    }

    public List<Tarifa> getAllTarifas() {
        return tarifaRepository.findAll();
    }

    public Optional<Tarifa> getTarifaById(Long id) {
        return tarifaRepository.findById(id);
    }

    public Optional<Tarifa> getTarifaByTipoUtilizadorAndModalidade(TipoUtilizador tipoUtilizador, ModalidadePasse modalidade) {
        return tarifaRepository.findByCriteria(tipoUtilizador, modalidade, 0);
    }

    public List<Tarifa> getTarifasByTipoUtilizador(TipoUtilizador tipoUtilizador) {
        return tarifaRepository.findByTipoUtilizador(tipoUtilizador);
    }

    public List<Tarifa> getTarifasByModalidade(ModalidadePasse modalidade) {
        return tarifaRepository.findByModalidade(modalidade);
    }

    public Optional<Tarifa> calcularTarifa(TipoUtilizador tipoUtilizador, ModalidadePasse modalidade, int nrZonas) {
        TipoUtilizador tipo = tipoUtilizador != null ? tipoUtilizador : TipoUtilizador.ADULTO;
        return tarifaRepository.findByCriteria(tipo, modalidade, nrZonas);
    }

    public Tarifa createTarifa(Tarifa tarifa) {
        return tarifaRepository.save(tarifa);
    }

    public Tarifa updateTarifa(Long id, Tarifa updated) {
        Tarifa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa nao encontrada"));
        tarifa.setValor(updated.getValor());
        if (updated.getTipoUtilizador() != null) tarifa.setTipoUtilizador(updated.getTipoUtilizador());
        if (updated.getModalidade() != null) tarifa.setModalidade(updated.getModalidade());
        tarifa.setNrZonas(updated.getNrZonas());
        return tarifaRepository.save(tarifa);
    }

    public void deleteTarifa(Long id) {
        tarifaRepository.deleteById(id);
    }
}
