package pt.projetoea.services;

import org.springframework.stereotype.Service;
import pt.projetoea.models.Zona;
import pt.projetoea.repositories.ZonaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ZonaService {

    private final ZonaRepository zonaRepository;

    public ZonaService(ZonaRepository zonaRepository) {
        this.zonaRepository = zonaRepository;
    }

    public List<Zona> getAllZonas() {
        return zonaRepository.findAll();
    }

    public Optional<Zona> getZonaById(Long id) {
        return zonaRepository.findById(id);
    }

    public Optional<Zona> getZonaByNome(String nome) {
        return zonaRepository.findByNome(nome);
    }

    public Zona createZona(Zona zona) {
        return zonaRepository.save(zona);
    }

    public Zona updateZona(Long id, Zona updated) {
        Zona zona = zonaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zona not found"));
        if (updated.getNome() != null) zona.setNome(updated.getNome());
        if (updated.getParagens() != null) zona.setParagens(updated.getParagens());
        return zonaRepository.save(zona);
    }

    public void deleteZona(Long id) {
        zonaRepository.deleteById(id);
    }
}
