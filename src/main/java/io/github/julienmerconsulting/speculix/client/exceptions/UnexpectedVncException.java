package io.github.julienmerconsulting.speculix.client.exceptions;

public class UnexpectedVncException extends VncException {

    public UnexpectedVncException(Throwable cause) {
        super("An unexpected exception occurred: " + cause.getClass().getSimpleName(), cause);
    }

}
