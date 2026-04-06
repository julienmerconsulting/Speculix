package io.github.julienmerconsulting.speculix.client.rendering.renderers;

import io.github.julienmerconsulting.speculix.client.exceptions.VncException;
import io.github.julienmerconsulting.speculix.protocol.messages.Rectangle;

import java.awt.image.BufferedImage;
import java.io.InputStream;

public interface Renderer {
    void render(InputStream in, BufferedImage destination, Rectangle rectangle) throws VncException;
}
