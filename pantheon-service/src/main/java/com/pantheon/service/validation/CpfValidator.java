package com.pantheon.service.validation;

/**
 * Format and check-digit validation for a Brazilian CPF (Cadastro de Pessoas Físicas), the
 * standard two-check-digit mod-11 algorithm. Does not verify the CPF is actually issued/active —
 * just that it is well-formed, which is all that's achievable without an external registry.
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    public static boolean isValid(String rawCpf) {
        if (rawCpf == null) {
            return false;
        }
        String cpf = rawCpf.replaceAll("\\D", "");
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }

        int[] digits = cpf.chars().map(c -> c - '0').toArray();
        return digits[9] == checkDigit(digits, 9) && digits[10] == checkDigit(digits, 10);
    }

    /** Computes the check digit at {@code position} (9 for the first, 10 for the second) from the preceding digits. */
    private static int checkDigit(int[] digits, int position) {
        int sum = 0;
        int weight = position + 1;
        for (int i = 0; i < position; i++) {
            sum += digits[i] * weight;
            weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
