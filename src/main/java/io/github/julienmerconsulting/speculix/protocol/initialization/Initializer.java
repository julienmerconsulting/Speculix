package io.github.julienmerconsulting.speculix.protocol.initialization;

import io.github.julienmerconsulting.speculix.client.SpeculixConfig;
import io.github.julienmerconsulting.speculix.client.VncSession;
import io.github.julienmerconsulting.speculix.client.rendering.ColorDepth;
import io.github.julienmerconsulting.speculix.protocol.messages.ClientInit;
import io.github.julienmerconsulting.speculix.protocol.messages.Encoding;
import io.github.julienmerconsulting.speculix.protocol.messages.PixelFormat;
import io.github.julienmerconsulting.speculix.protocol.messages.ServerInit;
import io.github.julienmerconsulting.speculix.protocol.messages.SetEncodings;
import io.github.julienmerconsulting.speculix.protocol.messages.SetPixelFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.COPYRECT;
import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.CURSOR;
import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.DESKTOP_SIZE;
import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.EXTENDED_CLIPBOARD;
import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.HEXTILE;
import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.RAW;
import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.RRE;
import static io.github.julienmerconsulting.speculix.protocol.messages.Encoding.ZLIB;

public class Initializer {

    public void initialise(VncSession session) throws IOException {
        OutputStream out = session.getOutputStream();

        ClientInit clientInit = new ClientInit(session.getConfig().isShared());
        clientInit.encode(out);

        ServerInit serverInit = ServerInit.decode(session.getInputStream());
        session.setServerInit(serverInit);
        session.setFramebufferWidth(serverInit.getFramebufferWidth());
        session.setFramebufferHeight(serverInit.getFramebufferHeight());

        SpeculixConfig config = session.getConfig();
        ColorDepth colorDepth = config.getColorDepth();

        PixelFormat pixelFormat = new PixelFormat(
                colorDepth.getBitsPerPixel(),
                colorDepth.getDepth(),
                true,
                colorDepth.isTrueColor(),
                colorDepth.getRedMax(),
                colorDepth.getGreenMax(),
                colorDepth.getBlueMax(),
                colorDepth.getRedShift(),
                colorDepth.getGreenShift(),
                colorDepth.getBlueShift());

        SetPixelFormat setPixelFormat = new SetPixelFormat(pixelFormat);

        List<Encoding> encodings = new ArrayList<>();

        if (config.isEnableZLibEncoding()) {
            encodings.add(ZLIB);
        }

        if (config.isEnableHextileEncoding()) {
            encodings.add(HEXTILE);
        }

        if (config.isEnableRreEncoding()) {
            encodings.add(RRE);
        }

        if (config.isEnableCopyrectEncoding()) {
            encodings.add(COPYRECT);
        }

        if (config.isEnableExtendedClipboard()) {
            encodings.add(EXTENDED_CLIPBOARD);
        }

        encodings.add(RAW);
        encodings.add(DESKTOP_SIZE);

        if (config.isUseLocalMousePointer()) {
            encodings.add(CURSOR);
        }

        SetEncodings setEncodings = new SetEncodings(encodings);

        setPixelFormat.encode(out);
        setEncodings.encode(out);

        session.setPixelFormat(pixelFormat);
    }

}
