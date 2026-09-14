package com.pantheon.service.exception;

public class InvalidCpfException extends RuntimeException {

    public InvalidCpfException(String cpf) {
        super("Invalid CPF: " + cpf);
    }
}
