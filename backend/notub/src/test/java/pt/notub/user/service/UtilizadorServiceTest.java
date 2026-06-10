package pt.notub.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.notub.common.exception.RecursoNaoEncontradoException;
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
        Utilizador updated = new Utilizador();
        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.updateUtilizador("notfound@example.com", updated));
    }

    @Test
    void updateUtilizador_validNome_updatesSuccessfully() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Utilizador updated = new Utilizador();
        updated.setPrimeiroNome("Maria");
        updated.setUltimoNome("Santos");

        Utilizador result = service.updateUtilizador("test@example.com", updated);
        assertEquals("Maria", result.getPrimeiroNome());
        assertEquals("Santos", result.getUltimoNome());
    }

    @Test
    void updateUtilizador_invalidPrimeiroNome_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        Utilizador updated = new Utilizador();
        updated.setPrimeiroNome("Joao123");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateUtilizador("test@example.com", updated));
    }

    @Test
    void updateUtilizador_invalidUltimoNome_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        Utilizador updated = new Utilizador();
        updated.setUltimoNome("Silva@123");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateUtilizador("test@example.com", updated));
    }

    @Test
    void updateUtilizador_futureDate_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        Utilizador updated = new Utilizador();
        updated.setDataNascimento(LocalDate.now().plusDays(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUtilizador("test@example.com", updated));
        assertTrue(ex.getMessage().contains("futura"));
    }

    @Test
    void updateUtilizador_dateBefore1900_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        Utilizador updated = new Utilizador();
        updated.setDataNascimento(LocalDate.of(1899, 12, 31));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUtilizador("test@example.com", updated));
        assertTrue(ex.getMessage().contains("1900"));
    }

    @Test
    void updateUtilizador_validNif_updatesSuccessfully() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.existsByNif("123456789")).thenReturn(false);
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Utilizador updated = new Utilizador();
        updated.setNif("123456789");

        Utilizador result = service.updateUtilizador("test@example.com", updated);
        assertEquals("123456789", result.getNif());
    }

    @Test
    void updateUtilizador_invalidNif_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        Utilizador updated = new Utilizador();
        updated.setNif("000000000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUtilizador("test@example.com", updated));
        assertTrue(ex.getMessage().contains("NIF"));
    }

    @Test
    void updateUtilizador_duplicateNif_throwsException() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.existsByNif("234567899")).thenReturn(true);

        Utilizador updated = new Utilizador();
        updated.setNif("234567899");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUtilizador("test@example.com", updated));
        assertTrue(ex.getMessage().contains("NIF ja em uso"));
    }

    @Test
    void updateUtilizador_nullNif_doesNotClearExisting() {
        existingUser.setNif("123456789");
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Utilizador updated = new Utilizador();
        updated.setNif(null);

        Utilizador result = service.updateUtilizador("test@example.com", updated);
        assertEquals("123456789", result.getNif());
    }

    @Test
    void updateUtilizador_sameNif_noDuplicateCheck() {
        existingUser.setNif("123456789");
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Utilizador updated = new Utilizador();
        updated.setNif("123456789");

        Utilizador result = service.updateUtilizador("test@example.com", updated);
        verify(utilizadorRepository, never()).existsByNif(any());
        assertEquals("123456789", result.getNif());
    }

    @Test
    void updateRole_validRole_updatesSuccessfully() {
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Utilizador result = service.updateRole(1L, "administrador");

        assertEquals(TipoPapel.ADMINISTRADOR, result.getRole());
    }

    @Test
    void updateRole_invalidRole_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateRole(1L, "invalid-role"));
    }

    @Test
    void updateUtilizador_validDate_updatesTipoUtilizador() {
        when(utilizadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(utilizadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Utilizador updated = new Utilizador();
        updated.setDataNascimento(LocalDate.of(2015, 1, 1));

        Utilizador result = service.updateUtilizador("test@example.com", updated);
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
}
