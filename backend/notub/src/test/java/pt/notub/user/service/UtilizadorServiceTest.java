package pt.notub.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.ConflitoException;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.user.dto.UpdateUserProfileRequest;
import pt.notub.user.dto.UserDTO;
import pt.notub.user.entity.TipoPapel;
import pt.notub.user.entity.TipoUtilizador;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilizadorServiceTest {

    @Mock
    private UtilizadorRepository utilizadorRepository;

    private UtilizadorService service;

    private Utilizador existingUser;

    @BeforeEach
    void setUp() {
        service = new UtilizadorService(utilizadorRepository);
        existingUser = new Utilizador();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setPrimeiroNome("Joao");
        existingUser.setUltimoNome("Silva");
        existingUser.setDataNascimento(LocalDate.of(2000, 1, 1));
    }

    @Test
    void updateUtilizador_userNotFound_throwsException() {
        when(utilizadorRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.updateUtilizador("notfound@example.com", profile(null, null, null, null)));
    }

    @Test
    void updateUtilizador_validNome_updatesSuccessfully() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = service.updateUtilizador("test@example.com", profile("Maria", "Santos", null, null));
        assertEquals("Maria", result.getPrimeiroNome());
        assertEquals("Santos", result.getUltimoNome());
    }

    @Test
    void updateUtilizador_invalidPrimeiroNome_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(PedidoInvalidoException.class,
                () -> service.updateUtilizador("test@example.com", profile("Joao123", null, null, null)));
    }

    @Test
    void updateUtilizador_invalidUltimoNome_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(PedidoInvalidoException.class,
                () -> service.updateUtilizador("test@example.com", profile(null, "Silva@123", null, null)));
    }

    @Test
    void updateUtilizador_futureDate_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        PedidoInvalidoException ex = assertThrows(PedidoInvalidoException.class,
                () -> service.updateUtilizador("test@example.com", profile(null, null, null, LocalDate.now().plusDays(1).toString())));
        assertTrue(ex.getMessage().contains("futura"));
    }

    @Test
    void updateUtilizador_dateBefore1900_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        PedidoInvalidoException ex = assertThrows(PedidoInvalidoException.class,
                () -> service.updateUtilizador("test@example.com", profile(null, null, null, "1899-12-31")));
        assertTrue(ex.getMessage().contains("1900"));
    }

    @Test
    void updateUtilizador_validNif_updatesSuccessfully() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.existsByNif("123456789")).thenReturn(false);
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = service.updateUtilizador("test@example.com", profile(null, null, "123456789", null));
        assertEquals("123456789", result.getNif());
    }

    @Test
    void updateUtilizador_invalidNif_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        PedidoInvalidoException ex = assertThrows(PedidoInvalidoException.class,
                () -> service.updateUtilizador("test@example.com", profile(null, null, "000000000", null)));
        assertTrue(ex.getMessage().contains("NIF"));
    }

    @Test
    void updateUtilizador_duplicateNif_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.existsByNif("234567899")).thenReturn(true);

        ConflitoException ex = assertThrows(ConflitoException.class,
                () -> service.updateUtilizador("test@example.com", profile(null, null, "234567899", null)));
        assertTrue(ex.getMessage().contains("NIF ja em uso"));
    }

    @Test
    void updateUtilizador_nullNif_doesNotClearExisting() {
        existingUser.setNif("123456789");
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = service.updateUtilizador("test@example.com", profile(null, null, null, null));
        assertEquals("123456789", result.getNif());
    }

    @Test
    void updateUtilizador_sameNif_noDuplicateCheck() {
        existingUser.setNif("123456789");
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = service.updateUtilizador("test@example.com", profile(null, null, "123456789", null));
        verify(utilizadorRepository, never()).existsByNif(any());
        assertEquals("123456789", result.getNif());
    }

    @Test
    void updateRole_validRole_updatesSuccessfully() {
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = service.updateRole(1L, "administrador");

        assertEquals(TipoPapel.ADMINISTRADOR, result.getRole());
    }

    @Test
    void updateRole_invalidRole_throwsException() {
        assertThrows(PedidoInvalidoException.class,
                () -> service.updateRole(1L, "invalid-role"));
    }

    @Test
    void updateUtilizador_validDate_updatesTipoUtilizador() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = service.updateUtilizador("test@example.com", profile(null, null, null, LocalDate.of(2015, 1, 1).toString()));
        assertEquals(TipoUtilizador.CRIANCA, result.getTipoUtilizador());
    }

    @Test
    void calcularTipoUtilizador_null_returnsAdulto() {
        assertEquals(TipoUtilizador.ADULTO, UtilizadorService.calcularTipoUtilizador(null));
    }

    @Test
    void calcularTipoUtilizador_age10_returnsCrianca() {
        LocalDate dob = LocalDate.now().minusYears(10);
        assertEquals(TipoUtilizador.CRIANCA, UtilizadorService.calcularTipoUtilizador(dob));
    }

    @Test
    void calcularTipoUtilizador_age20_returnsEstudante() {
        LocalDate dob = LocalDate.now().minusYears(20);
        assertEquals(TipoUtilizador.ESTUDANTE, UtilizadorService.calcularTipoUtilizador(dob));
    }

    @Test
    void calcularTipoUtilizador_age70_returnsSenior() {
        LocalDate dob = LocalDate.now().minusYears(70);
        assertEquals(TipoUtilizador.SENIOR, UtilizadorService.calcularTipoUtilizador(dob));
    }

    private UpdateUserProfileRequest profile(String primeiroNome, String ultimoNome, String nif, String dataNascimento) {
        return new UpdateUserProfileRequest(primeiroNome, ultimoNome, nif, dataNascimento);
    }
}
