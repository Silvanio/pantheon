package com.pantheon.service.exception;

public class InvalidCpfCnpjException extends RuntimeException {

    public InvalidCpfCnpjException(String document) {
        super("Invalid CPF/CNPJ: " + document);
    }
}
