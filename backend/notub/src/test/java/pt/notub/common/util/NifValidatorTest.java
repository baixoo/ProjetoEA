package pt.notub.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NifValidatorTest {

    @Test
    void isValid_null_returnsFalse() {
        assertFalse(NifValidator.isValid(null));
    }

    @Test
    void isValid_shortString_returnsFalse() {
        assertFalse(NifValidator.isValid("12345678"));
    }

    @Test
    void isValid_nonNumeric_returnsFalse() {
        assertFalse(NifValidator.isValid("12345678a"));
    }

    @Test
    void isValid_startsWithZero_returnsFalse() {
        assertFalse(NifValidator.isValid("023456789"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "234567899", "999999990"})
    void isValid_validNifs_returnsTrue(String nif) {
        assertTrue(NifValidator.isValid(nif));
    }

    @Test
    void isValid_validNif_123456789() {
        assertTrue(NifValidator.isValid("123456789"));
    }

    @Test
    void isValid_validNif_234567899() {
        assertTrue(NifValidator.isValid("234567899"));
    }

    @Test
    void isValid_invalidCheckDigit_returnsFalse() {
        assertFalse(NifValidator.isValid("123456780"));
    }

    @Test
    void isValid_emptyString_returnsFalse() {
        assertFalse(NifValidator.isValid(""));
    }
}
