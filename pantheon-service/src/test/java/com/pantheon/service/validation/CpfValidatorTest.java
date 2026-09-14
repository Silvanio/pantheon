package com.pantheon.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfValidatorTest {

    @Test
    void acceptsWellKnownValidCpf() {
        assertThat(CpfValidator.isValid("111.444.777-35")).isTrue();
        assertThat(CpfValidator.isValid("11144477735")).isTrue();
    }

    @Test
    void rejectsWrongCheckDigits() {
        assertThat(CpfValidator.isValid("111.444.777-36")).isFalse();
    }

    @Test
    void rejectsAllRepeatedDigits() {
        assertThat(CpfValidator.isValid("111.111.111-11")).isFalse();
        assertThat(CpfValidator.isValid("00000000000")).isFalse();
    }

    @Test
    void rejectsWrongLength() {
        assertThat(CpfValidator.isValid("123456789")).isFalse();
        assertThat(CpfValidator.isValid("123456789012")).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(CpfValidator.isValid(null)).isFalse();
    }
}
