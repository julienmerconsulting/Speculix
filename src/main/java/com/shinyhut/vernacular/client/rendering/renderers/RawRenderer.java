package com.shinyhut.vernacular.client.rendering.renderers;

import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.protocol.messages.PixelFormat;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class RawRenderer implements Renderer {

    private final PixelDecoder pixelDecoder;
    private final PixelFormat pixelFormat;

    public RawRenderer(PixelDecoder pixelDecoder, PixelFormat pixelFormat) {
        this.pixelDecoder = pixelDecoder;
        this.pixelFormat = pixelFormat;
    }

    @Override
    public void render(InputStream in, BufferedImage destination, Rectangle rectangle) throws VncException {
        render(in, destination, rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
    }

    void render(InputStream in, BufferedImage destination, int x, int y, int width, int height) throws VncException {
        try {
            int sx = x;
            int sy = y;
            for (int i = 0; i < width * height; i++) {
                destination.setRGB(sx, sy, pixelDecoder.decodeAsRgb(in, pixelFormat));
                sx++;
                if (sx == x + width) {
                    sx = x;
                    sy++;
                }
            }
        } catch (IOException e) {
            throw new UnexpectedVncException(e);
        }
    }

}
