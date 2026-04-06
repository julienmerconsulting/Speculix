package io.github.julienmerconsulting.speculix.protocol.handshaking;

import io.github.julienmerconsulting.speculix.client.VncSession;
import io.github.julienmerconsulting.speculix.client.exceptions.UnsupportedProtocolVersionException;
import io.github.julienmerconsulting.speculix.client.exceptions.VncException;
import io.github.julienmerconsulting.speculix.protocol.messages.ProtocolVersion;

import java.io.IOException;

import static java.lang.Math.min;

public class ProtocolVersionNegotiator {

    private static final int MAJOR_VERSION = 3;
    private static final int MIN_MINOR_VERSION = 3;
    private static final int MAX_MINOR_VERSION = 8;

    public void negotiate(VncSession session) throws IOException, VncException {
        ProtocolVersion serverVersion = ProtocolVersion.decode(session.getInputStream());

        if (!serverVersion.atLeast(MAJOR_VERSION, MIN_MINOR_VERSION)) {
            throw new UnsupportedProtocolVersionException(
                    serverVersion.getMajor(),
                    serverVersion.getMinor(),
                    MAJOR_VERSION,
                    MIN_MINOR_VERSION
            );
        }

        ProtocolVersion clientVersion = new ProtocolVersion(
                MAJOR_VERSION,
                min(serverVersion.getMinor(), MAX_MINOR_VERSION)
        );

        session.setProtocolVersion(clientVersion);
        clientVersion.encode(session.getOutputStream());
    }
}
