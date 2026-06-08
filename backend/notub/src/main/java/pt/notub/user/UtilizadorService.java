package pt.notub.user;

import org.springframework.stereotype.Service;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.TipoUtilizador;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;
import pt.notub.validation.NifValidator;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Service
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;

    public UtilizadorService(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    public static TipoUtilizador calcularTipoUtilizador(LocalDate dataNascimento) {
        if (dataNascimento == null) return TipoUtilizador.ADULTO;
        int age = Period.between(dataNascimento, LocalDate.now()).getYears();
        if (age < 12) return TipoUtilizador.CRIANCA;
        if (age <= 23) return TipoUtilizador.ESTUDANTE;
        if (age >= 65) return TipoUtilizador.SENIOR;
        return TipoUtilizador.ADULTO;
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));

        if (updated.getPrimeiroNome() != null) {
            if (!updated.getPrimeiroNome().matches("^[a-zA-ZÀ-ÿ]+$")) {
                throw new IllegalArgumentException("O primeiro nome deve conter apenas letras");
            }
            utilizador.setPrimeiroNome(updated.getPrimeiroNome());
        }
        if (updated.getUltimoNome() != null) {
            if (!updated.getUltimoNome().matches("^[a-zA-ZÀ-ÿ\\s]*$")) {
                throw new IllegalArgumentException("O ultimo nome deve conter apenas letras e espacos");
            }
            utilizador.setUltimoNome(updated.getUltimoNome());
        }
        if (updated.getDataNascimento() != null) {
            if (updated.getDataNascimento().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("A data de nascimento nao pode ser futura");
            }
            if (updated.getDataNascimento().isBefore(LocalDate.of(1900, 1, 1))) {
                throw new IllegalArgumentException("A data de nascimento deve ser posterior a 1900-01-01");
            }
            utilizador.setDataNascimento(updated.getDataNascimento());
            utilizador.setTipoUtilizador(calcularTipoUtilizador(updated.getDataNascimento()));
        }

        String novoNif = updated.getNif();
        if (novoNif != null && novoNif.isBlank()) {
            novoNif = null;
        }
        if (novoNif != null) {
            if (!NifValidator.isValid(novoNif)) {
                throw new IllegalArgumentException("NIF invalido");
            }
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        utilizador.setTipoUtilizador(tipoUtilizador);
        return utilizadorRepository.save(utilizador);
    }

    public void deleteUtilizador(Long id) {
        utilizadorRepository.deleteById(id);
    }
}
