package com.pantheon.service.exception;

/** Thrown when a supplier CNPJ-prefix search is attempted with fewer than 5 characters. */
public class CnpjPrefixTooShortException extends RuntimeException {

    public CnpjPrefixTooShortException() {
        super("CNPJ prefix must have at least 5 characters");
    }
}
