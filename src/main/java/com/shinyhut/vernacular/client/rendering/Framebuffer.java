package com.shinyhut.vernacular.client.rendering;

import com.shinyhut.vernacular.client.VncSession;
import com.shinyhut.vernacular.client.exceptions.UnexpectedVncException;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.renderers.*;
import com.shinyhut.vernacular.protocol.messages.*;
import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static com.shinyhut.vernacular.protocol.messages.Encoding.*;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class Framebuffer {

    private static final Logger BENCH = Logger.getLogger("speculix.bench");

    private final VncSession session;
    private final Map<Long, ColorMapEntry> colorMap = new ConcurrentHashMap<>();
    private final Map<Encoding, Renderer> renderers = new ConcurrentHashMap<>();
    private final CursorRenderer cursorRenderer;
    private final ZLibRenderer zLibRenderer;

    private volatile BufferedImage frame;

    public Framebuffer(VncSession session) {
        PixelDecoder pixelDecoder = new PixelDecoder(colorMap);
        RawRenderer rawRenderer = new RawRenderer(pixelDecoder, session.getPixelFormat());
        zLibRenderer = new ZLibRenderer(rawRenderer);
        renderers.put(RAW, rawRenderer);
        renderers.put(COPYRECT, new CopyRectRenderer());
        renderers.put(RRE, new RRERenderer(pixelDecoder, session.getPixelFormat()));
        renderers.put(HEXTILE, new HextileRenderer(rawRenderer, pixelDecoder, session.getPixelFormat()));
        renderers.put(ZLIB, zLibRenderer);
        cursorRenderer = new CursorRenderer(rawRenderer);

        frame = new BufferedImage(session.getFramebufferWidth(), session.getFramebufferHeight(), TYPE_INT_RGB);
        this.session = session;
    }

    public void close() {
        zLibRenderer.close();
    }

    public void processUpdate(FramebufferUpdate update) throws VncException {
        long t0 = System.nanoTime();
        InputStream in = session.getInputStream();
        int rects = update.getNumberOfRectangles();
        int totalPixels = 0;
        try {
            for (int i = 0; i < rects; i++) {
                Rectangle rectangle = Rectangle.decode(in);
                if (rectangle.getEncoding() == DESKTOP_SIZE) {
                    resizeFramebuffer(rectangle);
                } else if (rectangle.getEncoding() == CURSOR) {
                    updateCursor(rectangle, in);
                } else {
                    renderers.get(rectangle.getEncoding()).render(in, frame, rectangle);
                    totalPixels += rectangle.getWidth() * rectangle.getHeight();
                }
            }
            long renderNs = System.nanoTime() - t0;
            paint();
            long totalNs = System.nanoTime() - t0;
            BENCH.fine(() -> String.format("frame: %d rects, %d px, render=%d.%03dms, total=%d.%03dms",
                    rects, totalPixels,
                    renderNs / 1_000_000, (renderNs / 1_000) % 1000,
                    totalNs / 1_000_000, (totalNs / 1_000) % 1000));
            session.framebufferUpdated();
        } catch (IOException e) {
            throw new UnexpectedVncException(e);
        }
    }

    private void paint() {
        Consumer<Image> listener = session.getConfig().getScreenUpdateListener();
        if (listener != null) {
            ColorModel colorModel = frame.getColorModel();
            WritableRaster raster = frame.copyData(null);
            BufferedImage copy = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
            listener.accept(copy);
        }
    }

    public void updateColorMap(SetColorMapEntries update) {
        for (int i = 0; i < update.getColors().size(); i++) {
            colorMap.put((long) i + update.getFirstColor(), update.getColors().get(i));
        }
    }

    private void resizeFramebuffer(Rectangle newSize) {
        int width = newSize.getWidth();
        int height = newSize.getHeight();
        session.setFramebufferWidth(width);
        session.setFramebufferHeight(height);
        BufferedImage resized = new BufferedImage(width, height, TYPE_INT_RGB);
        Graphics g = resized.getGraphics();
        try {
            g.drawImage(frame, 0, 0, null);
        } finally {
            g.dispose();
        }
        frame = resized;
    }

    private void updateCursor(Rectangle cursor, InputStream in) throws VncException {
        if (cursor.getWidth() > 0 && cursor.getHeight() > 0) {
            BufferedImage cursorImage = new BufferedImage(cursor.getWidth(), cursor.getHeight(), TYPE_INT_ARGB);
            cursorRenderer.render(in, cursorImage, cursor);
            BiConsumer<Image, Point> listener = session.getConfig().getMousePointerUpdateListener();
            if (listener != null) {
                listener.accept(cursorImage, new Point(cursor.getX(), cursor.getY()));
            }
        }
    }

}
