package com.shinyhut.vernacular.client.rendering.renderers;

import com.shinyhut.vernacular.protocol.messages.ColorMapEntry;
import com.shinyhut.vernacular.protocol.messages.PixelFormat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public class PixelDecoder {

    private static final ColorMapEntry BLACK = new ColorMapEntry(0, 0, 0);

    private final Map<Long, ColorMapEntry> colorMap;

    public PixelDecoder(Map<Long, ColorMapEntry> colorMap) {
        this.colorMap = colorMap;
    }

    public Pixel decode(InputStream in, PixelFormat pixelFormat) throws IOException {
        int rgb = decodeAsRgb(in, pixelFormat);
        return new Pixel((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public int decodeAsRgb(InputStream in, PixelFormat pixelFormat) throws IOException {
        int bytesToRead = pixelFormat.getBytesPerPixel();
        long value = 0L;

        for (int i = 0; i < bytesToRead; i++) {
            value <<= 8;
            value |= in.read();
        }

        return toRgb(value, pixelFormat);
    }

    public int decodeAsRgb(byte[] data, int offset, PixelFormat pixelFormat) {
        int bytesToRead = pixelFormat.getBytesPerPixel();
        long value = 0L;

        for (int i = 0; i < bytesToRead; i++) {
            value <<= 8;
            value |= data[offset + i] & 0xFF;
        }

        return toRgb(value, pixelFormat);
    }

    public void decodeBulk(byte[] data, int dataOffset, int[] pixels, int pixelOffset, int count, PixelFormat pixelFormat) {
        int bpp = pixelFormat.getBytesPerPixel();
        int off = dataOffset;
        for (int i = 0; i < count; i++) {
            long value = 0L;
            for (int b = 0; b < bpp; b++) {
                value <<= 8;
                value |= data[off++] & 0xFF;
            }
            pixels[pixelOffset + i] = toRgb(value, pixelFormat);
        }
    }

    public void decodeBulk(byte[] data, int dataOffset, int[] pixels, int pixelOffset,
                           int width, int height, int scanline, PixelFormat pixelFormat) {
        int bpp = pixelFormat.getBytesPerPixel();
        int off = dataOffset;
        for (int row = 0; row < height; row++) {
            int rowStart = pixelOffset + row * scanline;
            for (int col = 0; col < width; col++) {
                long value = 0L;
                for (int b = 0; b < bpp; b++) {
                    value <<= 8;
                    value |= data[off++] & 0xFF;
                }
                pixels[rowStart + col] = toRgb(value, pixelFormat);
            }
        }
    }

    private int toRgb(long value, PixelFormat pixelFormat) {
        int red;
        int green;
        int blue;

        if (pixelFormat.isTrueColor()) {
            red = (int) (value >> pixelFormat.getRedShift()) & pixelFormat.getRedMax();
            green = (int) (value >> pixelFormat.getGreenShift()) & pixelFormat.getGreenMax();
            blue = (int) (value >> pixelFormat.getBlueShift()) & pixelFormat.getBlueMax();

            red = stretch(red, pixelFormat.getRedMax());
            green = stretch(green, pixelFormat.getGreenMax());
            blue = stretch(blue, pixelFormat.getBlueMax());
        } else {
            ColorMapEntry color = Optional.ofNullable(colorMap.get(value)).orElse(BLACK);
            red = shrink(color.getRed());
            green = shrink(color.getGreen());
            blue = shrink(color.getBlue());
        }

        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int stretch(int value, int max) {
        return max == 255 ? value : (int) (value * ((double) 255 / max));
    }

    private static int shrink(int colorMapValue) {
        return (int) Math.round(((double) colorMapValue) / 257);
    }
}
