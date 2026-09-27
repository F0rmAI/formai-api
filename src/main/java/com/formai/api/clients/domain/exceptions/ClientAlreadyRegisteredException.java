package com.formai.api.clients.domain.exceptions;

public class ClientAlreadyRegisteredException extends RuntimeException {

    public ClientAlreadyRegisteredException(String email) {
        super("A client with the email " + email + " is already registered in your list");
    }
}
