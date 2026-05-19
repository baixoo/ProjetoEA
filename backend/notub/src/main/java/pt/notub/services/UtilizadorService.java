package pt.notub.services;

import org.springframework.stereotype.Service;
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updated.getPrimeiroNome() != null) utilizador.setPrimeiroNome(updated.getPrimeiroNome());
        if (updated.getUltimoNome() != null) utilizador.setUltimoNome(updated.getUltimoNome());
        if (updated.getNif() != null) utilizador.setNif(updated.getNif());
        if (updated.getDataNascimento() != null) utilizador.setDataNascimento(updated.getDataNascimento());
        if (updated.getPerfil() != null) utilizador.setPerfil(updated.getPerfil());

        return utilizadorRepository.save(utilizador);
    }

    public void deleteUtilizador(Long id) {
        utilizadorRepository.deleteById(id);
    }
}
