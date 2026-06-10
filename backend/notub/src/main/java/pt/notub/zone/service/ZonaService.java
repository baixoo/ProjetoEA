package pt.notub.zone.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.entity.Paragem;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.zone.entity.Zona;
import pt.notub.zone.repository.ZonaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ZonaService {

    private final ZonaRepository zonaRepository;
    private final ParagemRepository paragemRepository;

    public ZonaService(ZonaRepository zonaRepository, ParagemRepository paragemRepository) {
        this.zonaRepository = zonaRepository;
        this.paragemRepository = paragemRepository;
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        if (updated.getNome() != null) zona.setNome(updated.getNome());
        if (updated.getNum() != 0) zona.setNum(updated.getNum());
        return zonaRepository.save(zona);
    }

    public void deleteZona(Long id) {
        zonaRepository.deleteById(id);
    }

    public Zona addParagem(Long zonaId, Long paragemId) {
        Zona zona = zonaRepository.findById(zonaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        Paragem paragem = paragemRepository.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        paragem.setZona(zona);
        paragemRepository.save(paragem);
        return zona;
    }

    public void removeParagem(Long paragemId) {
        Paragem paragem = paragemRepository.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        paragem.setZona(null);
        paragemRepository.save(paragem);
    }
}
