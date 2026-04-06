package io.github.julienmerconsulting.speculix.protocol.messages;

import io.github.julienmerconsulting.speculix.client.exceptions.HandshakingFailedException;
import io.github.julienmerconsulting.speculix.client.exceptions.NoSupportedSecurityTypesException;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.github.julienmerconsulting.speculix.protocol.messages.SecurityType.resolve;

public class ServerSecurityType {

    private final SecurityType securityType;

    private ServerSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }

    public SecurityType getSecurityType() {
        return securityType;
    }

    public static ServerSecurityType decode(InputStream in) throws HandshakingFailedException, NoSupportedSecurityTypesException, IOException {
        DataInputStream dataInput = new DataInputStream(in);
        int type = dataInput.readInt();

        if (type == 0) {
            ErrorMessage errorMessage = ErrorMessage.decode(in);
            throw new HandshakingFailedException(errorMessage.getMessage());
        }

        return resolve(type).map(ServerSecurityType::new).orElseThrow(NoSupportedSecurityTypesException::new);
    }
}
