package com.shinyhut.vernacular.client;

import com.shinyhut.vernacular.protocol.messages.Rectangle;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Listener for framebuffer updates with dirty rectangle information.
 * Receives the live framebuffer and the list of rectangles that changed,
 * avoiding full-frame copies.
 */
@FunctionalInterface
public interface FramebufferUpdateListener {

    /**
     * Called after all rectangles in a framebuffer update have been rendered.
     *
     * @param framebuffer the live framebuffer — do not modify
     * @param
dirtyRects the rectangles that changed in this update
     */
    void onFramebufferUpdate(BufferedImage framebuffer, List<Rectangle> dirtyRects);
}
