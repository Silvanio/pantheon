package com.pantheon.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CnpjValidatorTest {

    @Test
    void acceptsWellKnownValidCnpj() {
        assertThat(CnpjValidator.isValid("11.222.333/0001-81")).isTrue();
        assertThat(CnpjValidator.isValid("11222333000181")).isTrue();
    }

    @Test
    void rejectsWrongCheckDigits() {
        assertThat(CnpjValidator.isValid("11.222.333/0001-82")).isFalse();
    }

    @Test
    void rejectsAllRepeatedDigits() {
        assertThat(CnpjValidator.isValid("11.111.111/1111-11")).isFalse();
        assertThat(CnpjValidator.isValid("00000000000000")).isFalse();
    }

    @Test
    void rejectsWrongLength() {
        assertThat(CnpjValidator.isValid("123456789")).isFalse();
        assertThat(CnpjValidator.isValid("1234567890123456")).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(CnpjValidator.isValid(null)).isFalse();
    }
}
