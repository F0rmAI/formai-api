package com.formai.api.clients.domain.exceptions;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException() {
        super("Client not found");
    }
}
