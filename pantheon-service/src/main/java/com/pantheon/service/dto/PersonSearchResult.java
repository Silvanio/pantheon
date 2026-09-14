package com.pantheon.service.dto;

/** A person already known to the acting company (staff or any obra's team), matched by CPF or email prefix. */
public record PersonSearchResult(String name, String email, String cpf, String phone) {
}
