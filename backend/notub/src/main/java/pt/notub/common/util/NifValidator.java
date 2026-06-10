package pt.notub.common.util;

public final class NifValidator {

    private static final int[] CHECK_DIGITS = {9, 8, 7, 6, 5, 4, 3, 2};

    private NifValidator() {}

    public static boolean isValid(String nif) {
        if (nif == null || nif.length() != 9) {
            return false;
        }
        if (!nif.matches("^[0-9]+$")) {
            return false;
        }
        int firstDigit = Character.getNumericValue(nif.charAt(0));
        if (firstDigit < 1 || firstDigit > 9) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.getNumericValue(nif.charAt(i)) * CHECK_DIGITS[i];
        }
        int remainder = sum % 11;
        int expectedCheckDigit = (remainder < 2) ? 0 : 11 - remainder;
        int actualCheckDigit = Character.getNumericValue(nif.charAt(8));
        return actualCheckDigit == expectedCheckDigit;
    }
}
