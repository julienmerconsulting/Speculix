package io.github.julienmerconsulting.speculix.protocol.handshaking;

import io.github.julienmerconsulting.speculix.client.VncSession;
import io.github.julienmerconsulting.speculix.client.exceptions.NoSupportedSecurityTypesException;
import io.github.julienmerconsulting.speculix.client.exceptions.VncException;
import io.github.julienmerconsulting.speculix.protocol.auth.MsLogon2AuthenticationHandler;
import io.github.julienmerconsulting.speculix.protocol.auth.NoSecurityHandler;
import io.github.julienmerconsulting.speculix.protocol.auth.SecurityHandler;
import io.github.julienmerconsulting.speculix.protocol.auth.VncAuthenticationHandler;
import io.github.julienmerconsulting.speculix.protocol.messages.SecurityType;
import io.github.julienmerconsulting.speculix.protocol.messages.ServerSecurityType;
import io.github.julienmerconsulting.speculix.protocol.messages.ServerSecurityTypes;

import java.io.IOException;
import java.util.List;

import static io.github.julienmerconsulting.speculix.protocol.messages.SecurityType.*;
import static java.util.Collections.singletonList;

public class SecurityTypeNegotiator {

    public SecurityHandler negotiate(VncSession session) throws IOException, VncException {
        if (session.getProtocolVersion().equals(3, 3)) {
            ServerSecurityType serverSecurityType = ServerSecurityType.decode(session.getInputStream());
            return resolve(singletonList(serverSecurityType.getSecurityType()));
        } else {
            ServerSecurityTypes serverSecurityTypes = ServerSecurityTypes.decode(session.getInputStream());
            return resolve(serverSecurityTypes.getSecurityTypes());
        }
    }

    private static SecurityHandler resolve(List<SecurityType> securityTypes) throws  VncException {
        if (securityTypes.contains(NONE)) {
            return new NoSecurityHandler();
        } else if (securityTypes.contains(VNC)) {
            return new VncAuthenticationHandler();
        } else if (securityTypes.contains(MS_LOGON_2)) {
            return new MsLogon2AuthenticationHandler();
        } else {
            throw new NoSupportedSecurityTypesException();
        }
    }

}
