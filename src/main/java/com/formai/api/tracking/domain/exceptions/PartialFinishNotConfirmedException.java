package com.formai.api.tracking.domain.exceptions;

public class PartialFinishNotConfirmedException extends RuntimeException {

    public PartialFinishNotConfirmedException() {
        super("Some exercises have no sets recorded. Confirm to finish the session as partial");
    }
}
