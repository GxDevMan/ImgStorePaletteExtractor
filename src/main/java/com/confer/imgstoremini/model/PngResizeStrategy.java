package com.confer.imgstoremini.model;
import java.awt.*;
import java.awt.image.BufferedImage;

public class PngResizeStrategy implements ResizeStrategy{
    @Override
    public BufferedImage resize(BufferedImage originalImage, int maxWidth, int maxHeight) {
        return performResize(originalImage, maxWidth, maxHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private BufferedImage performResize(BufferedImage originalImage, int maxWidth, int maxHeight, Object interpolationHint) {
        return resizeImageWithHint(originalImage, maxWidth, maxHeight, interpolationHint);
    }

    private static BufferedImage resizeImageWithHint(BufferedImage originalImage, int maxWidth, int maxHeight, Object hint) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double widthScale = (double) maxWidth / originalWidth;
        double heightScale = (double) maxHeight / originalHeight;
        double scaleFactor = Math.min(widthScale, heightScale);

        int newWidth = (int) (originalWidth * scaleFactor);
        int newHeight = (int) (originalHeight * scaleFactor);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }
}
