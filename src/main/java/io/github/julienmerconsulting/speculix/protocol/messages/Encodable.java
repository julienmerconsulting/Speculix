package io.github.julienmerconsulting.speculix.protocol.messages;

import java.io.IOException;
import java.io.OutputStream;

public interface Encodable {
    void encode(OutputStream out) throws IOException;
}
