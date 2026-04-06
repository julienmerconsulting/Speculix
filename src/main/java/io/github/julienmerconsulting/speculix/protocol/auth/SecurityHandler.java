package io.github.julienmerconsulting.speculix.protocol.auth;

import io.github.julienmerconsulting.speculix.client.VncSession;
import io.github.julienmerconsulting.speculix.client.exceptions.VncException;
import io.github.julienmerconsulting.speculix.protocol.messages.SecurityResult;

import java.io.IOException;

public interface SecurityHandler {
    SecurityResult authenticate(VncSession session) throws VncException, IOException;
}
