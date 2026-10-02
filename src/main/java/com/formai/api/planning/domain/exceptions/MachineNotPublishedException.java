package com.formai.api.planning.domain.exceptions;

public class MachineNotPublishedException extends RuntimeException {

    public MachineNotPublishedException() {
        super("The machine is not published in the catalog");
    }
}
