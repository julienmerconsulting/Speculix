package io.github.julienmerconsulting.speculix.protocol.handshaking;

import io.github.julienmerconsulting.speculix.client.VncSession;
import io.github.julienmerconsulting.speculix.client.exceptions.AuthenticationFailedException;
import io.github.julienmerconsulting.speculix.client.exceptions.VncException;
import io.github.julienmerconsulting.speculix.protocol.auth.SecurityHandler;
import io.github.julienmerconsulting.speculix.protocol.messages.SecurityResult;

import java.io.IOException;

public class Handshaker {

    private final ProtocolVersionNegotiator protocolVersionNegotiator;
    private final SecurityTypeNegotiator securityTypeNegotiator;

    public Handshaker() {
        protocolVersionNegotiator = new ProtocolVersionNegotiator();
        securityTypeNegotiator = new SecurityTypeNegotiator();
    }

    public void handshake(VncSession session) throws VncException, IOException {
        protocolVersionNegotiator.negotiate(session);

        SecurityHandler securityHandler = securityTypeNegotiator.negotiate(session);
        SecurityResult securityResult = securityHandler.authenticate(session);

        if (!securityResult.isSuccess()) {
            if (securityResult.getErrorMessage() != null) {
                throw new AuthenticationFailedException(securityResult.getErrorMessage());
            } else {
                throw new AuthenticationFailedException();
            }
        }
    }
}
