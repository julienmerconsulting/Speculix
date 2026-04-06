package com.shinyhut.vernacular.client.rendering.renderers;

import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.protocol.messages.PixelFormat;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
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
        int totalPixels = width * height;
        int bpp = pixelFormat.getBytesPerPixel();
        int totalBytes = totalPixels * bpp;
        byte[] data = new byte[totalBytes];
        try {
            new DataInputStream(in).readFully(data);
        } catch (IOException e) {
            throw new UnexpectedVncException(e);
        }
        renderFromBytes(data, 0, destination, x, y, width, height);
    }

    void renderFromBytes(byte[] data, int dataOffset, BufferedImage destination, int x, int y, int width, int height) {
        int totalPixels = width * height;
        int[] pixels = new int[totalPixels];
        pixelDecoder.decodeBulk(data, dataOffset, pixels, 0, totalPixels, pixelFormat);
        destination.setRGB(x, y, width, height, pixels, 0, width);
    }

}
