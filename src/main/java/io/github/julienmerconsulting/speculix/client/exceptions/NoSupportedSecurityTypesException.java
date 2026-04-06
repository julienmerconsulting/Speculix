package io.github.julienmerconsulting.speculix.client.exceptions;

public class NoSupportedSecurityTypesException extends VncException {

    public NoSupportedSecurityTypesException() {
        super("The server does not support any VNC security types supported by this client");
    }

}
