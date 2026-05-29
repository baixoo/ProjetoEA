package pt.notub.user;

import org.springframework.stereotype.Service;
import pt.notub.models.TipoUtilizador;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;

    public UtilizadorService(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    public List<Utilizador> getAllUtilizadores() {
        return utilizadorRepository.findAll();
    }

    public Optional<Utilizador> getUtilizadorById(Long id) {
        return utilizadorRepository.findById(id);
    }

    public Optional<Utilizador> getUtilizadorByEmail(String email) {
        return utilizadorRepository.findByEmail(email);
    }

    public Utilizador updateUtilizador(String email, Utilizador updated) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));

        if (updated.getPrimeiroNome() != null) utilizador.setPrimeiroNome(updated.getPrimeiroNome());
        if (updated.getUltimoNome() != null) utilizador.setUltimoNome(updated.getUltimoNome());
        if (updated.getDataNascimento() != null) utilizador.setDataNascimento(updated.getDataNascimento());
        if (updated.getTipoUtilizador() != null) utilizador.setTipoUtilizador(updated.getTipoUtilizador());

        String novoNif = updated.getNif();
        if (novoNif != null && novoNif.isBlank()) {
            novoNif = null;
        }
        if (novoNif != null) {
            String nifAtual = utilizador.getNif();
            if (!novoNif.equals(nifAtual) && utilizadorRepository.existsByNif(novoNif)) {
                throw new IllegalArgumentException("NIF ja em uso!");
            }
            utilizador.setNif(novoNif);
        } else if (updated.getNif() != null) {
            utilizador.setNif(null);
        }

        return utilizadorRepository.save(utilizador);
    }

    public Utilizador updateTipoUtilizador(Long id, TipoUtilizador tipoUtilizador) {
        Utilizador utilizador = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilizador nao encontrado"));
        utilizador.setTipoUtilizador(tipoUtilizador);
        return utilizadorRepository.save(utilizador);
    }

    public void deleteUtilizador(Long id) {
        utilizadorRepository.deleteById(id);
    }
}
