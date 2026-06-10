package pt.notub.user.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.ConflitoException;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.common.util.NifValidator;
import pt.notub.user.dto.UpdateUserProfileRequest;
import pt.notub.user.dto.UserDTO;
import pt.notub.user.entity.TipoPapel;
import pt.notub.user.entity.TipoUtilizador;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.mapper.UserMapper;
import pt.notub.user.repository.UtilizadorRepository;

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

    public List<UserDTO> getAllUtilizadores() {
        return UserMapper.toDTOList(utilizadorRepository.findAll());
    }

    public UserDTO getUtilizadorById(Long id) {
        Utilizador utilizador = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        return UserMapper.toDTO(utilizador);
    }

    public Optional<Utilizador> getUtilizadorByEmail(String email) {
        return utilizadorRepository.findByEmail(email);
    }

    public UserDTO getUtilizadorByEmailDto(String email) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        return UserMapper.toDTO(utilizador);
    }

    public UserDTO updateUtilizador(String email, UpdateUserProfileRequest updated) {
        Utilizador utilizador = utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));

        if (updated.primeiroNome() != null) {
            if (!updated.primeiroNome().matches("^[\\p{L}]+$")) {
                throw new PedidoInvalidoException("O primeiro nome deve conter apenas letras");
            }
            utilizador.setPrimeiroNome(updated.primeiroNome());
        }
        if (updated.ultimoNome() != null) {
            if (!updated.ultimoNome().matches("^[\\p{L}\\s]*$")) {
                throw new PedidoInvalidoException("O ultimo nome deve conter apenas letras e espacos");
            }
            utilizador.setUltimoNome(updated.ultimoNome());
        }
        if (updated.dataNascimento() != null && !updated.dataNascimento().isBlank()) {
            LocalDate dataNascimento;
            try {
                dataNascimento = LocalDate.parse(updated.dataNascimento());
            } catch (Exception e) {
                throw new PedidoInvalidoException("Data de nascimento invalida");
            }
            if (dataNascimento.isAfter(LocalDate.now())) {
                throw new PedidoInvalidoException("A data de nascimento nao pode ser futura");
            }
            if (dataNascimento.isBefore(LocalDate.of(1900, 1, 1))) {
                throw new PedidoInvalidoException("A data de nascimento deve ser posterior a 1900-01-01");
            }
            utilizador.setDataNascimento(dataNascimento);
            utilizador.setTipoUtilizador(calcularTipoUtilizador(dataNascimento));
        }

        String novoNif = updated.nif();
        if (novoNif != null && novoNif.isBlank()) {
            novoNif = null;
        }
        if (novoNif != null) {
            if (!NifValidator.isValid(novoNif)) {
                throw new PedidoInvalidoException("NIF invalido");
            }
            String nifAtual = utilizador.getNif();
            if (!novoNif.equals(nifAtual) && utilizadorRepository.existsByNif(novoNif)) {
                throw new ConflitoException("NIF ja em uso!");
            }
            utilizador.setNif(novoNif);
        } else if (updated.nif() != null) {
            utilizador.setNif(null);
        }

        return UserMapper.toDTO(utilizadorRepository.save(utilizador));
    }

    public UserDTO updateTipoUtilizador(Long id, TipoUtilizador tipoUtilizador) {
        Utilizador utilizador = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        utilizador.setTipoUtilizador(tipoUtilizador);
        return UserMapper.toDTO(utilizadorRepository.save(utilizador));
    }

    public UserDTO updateRole(Long id, String role) {
        if (role == null || role.isBlank()) {
            throw new PedidoInvalidoException("Role invalida");
        }
        TipoPapel novoRole;
        try {
            novoRole = TipoPapel.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Role invalida");
        }
        Utilizador utilizador = utilizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        utilizador.setRole(novoRole);
        return UserMapper.toDTO(utilizadorRepository.save(utilizador));
    }

    public void deleteUtilizador(Long id) {
        utilizadorRepository.deleteById(id);
    }
}
