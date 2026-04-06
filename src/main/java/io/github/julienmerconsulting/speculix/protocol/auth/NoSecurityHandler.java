package io.github.julienmerconsulting.speculix.protocol.auth;

import io.github.julienmerconsulting.speculix.client.VncSession;
import io.github.julienmerconsulting.speculix.protocol.messages.ProtocolVersion;
import io.github.julienmerconsulting.speculix.protocol.messages.SecurityResult;

import java.io.DataOutputStream;
import java.io.IOException;

import static io.github.julienmerconsulting.speculix.protocol.messages.SecurityType.NONE;

public class NoSecurityHandler implements SecurityHandler {

    @Override
    public SecurityResult authenticate(VncSession session) throws IOException {
        ProtocolVersion protocolVersion = session.getProtocolVersion();
        if (!protocolVersion.equals(3, 3)) {
            new DataOutputStream(session.getOutputStream()).writeByte(NONE.getCode());
        }
        if (protocolVersion.equals(3, 8)) {
            return SecurityResult.decode(session.getInputStream(), session.getProtocolVersion());
        } else {
            return new SecurityResult(true);
        }
    }
}
