package io.github.julienmerconsulting.speculix.client.exceptions;

public abstract class VncException extends Exception {

    public VncException() {

    }

    public VncException(String message) {
        super(message);
    }

    public VncException(String message, Throwable cause) {
        super(message, cause);
    }

    public VncException(Throwable cause) {
        super(cause);
    }
}
