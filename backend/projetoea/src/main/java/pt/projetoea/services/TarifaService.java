package pt.projetoea.services;

import org.springframework.stereotype.Service;
import pt.projetoea.models.ModalidadePasse;
import pt.projetoea.models.Tarifa;
import pt.projetoea.models.TipoPerfil;
import pt.projetoea.repositories.TarifaRepository;

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

    public Optional<Tarifa> getTarifaByPerfilAndModalidade(TipoPerfil perfil, ModalidadePasse modalidade) {
        return tarifaRepository.findByPerfilAndModalidade(perfil, modalidade);
    }

    public List<Tarifa> getTarifasByPerfil(TipoPerfil perfil) {
        return tarifaRepository.findByPerfil(perfil);
    }

    public List<Tarifa> getTarifasByModalidade(ModalidadePasse modalidade) {
        return tarifaRepository.findByModalidade(modalidade);
    }

    public Tarifa createTarifa(Tarifa tarifa) {
        return tarifaRepository.save(tarifa);
    }

    public Tarifa updateTarifa(Long id, Tarifa updated) {
        Tarifa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarifa not found"));
        tarifa.setValor(updated.getValor());
        if (updated.getPerfil() != null) tarifa.setPerfil(updated.getPerfil());
        if (updated.getModalidade() != null) tarifa.setModalidade(updated.getModalidade());
        return tarifaRepository.save(tarifa);
    }

    public void deleteTarifa(Long id) {
        tarifaRepository.deleteById(id);
    }
}
