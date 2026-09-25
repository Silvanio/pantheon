package com.pantheon.service.validation;

/**
 * Format and check-digit validation for a Brazilian CNPJ (Cadastro Nacional da Pessoa Jurídica),
 * the standard two-check-digit mod-11 algorithm. Does not verify the CNPJ is actually
 * issued/active — just that it is well-formed, which is all that's achievable without an
 * external registry.
 */
public final class CnpjValidator {

    private CnpjValidator() {
    }

    public static boolean isValid(String rawCnpj) {
        if (rawCnpj == null) {
            return false;
        }
        String cnpj = rawCnpj.replaceAll("\\D", "");
        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) {
            return false;
        }

        int[] digits = cnpj.chars().map(c -> c - '0').toArray();
        return digits[12] == checkDigit(digits, 12) && digits[13] == checkDigit(digits, 13);
    }

    /**
     * Computes the check digit at {@code position} (12 for the first, 13 for the second) from the
     * preceding digits, with the CNPJ-specific weight sequence that counts down from
     * {@code position - 7} to 2 before wrapping back to 9.
     */
    private static int checkDigit(int[] digits, int position) {
        int sum = 0;
        int weight = position - 7;
        for (int i = 0; i < position; i++) {
            sum += digits[i] * weight;
            weight--;
            if (weight < 2) {
                weight = 9;
            }
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
