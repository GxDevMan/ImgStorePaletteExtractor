package com.confer.imgstoremini.util;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class PaletteImageGenerator {

    public BufferedImage generatePaletteImage(List<Color> colors, int swatchSize) {
        Dimension dimension = computePaletteSize(colors.size(),swatchSize);
        int paletteWidth = (int) dimension.getWidth();
        int paletteHeight = (int) dimension.getHeight();

        int colorCount = colors.size();
        int blockWidth = paletteWidth / colorCount;
        BufferedImage paletteImage = new BufferedImage(paletteWidth, paletteHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = paletteImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < colorCount; i++) {
            int x = i * blockWidth;
            graphics.setColor(colors.get(i));
            graphics.fillRect(x, 0, blockWidth, paletteHeight);
        }

        graphics.dispose();
        return paletteImage;
    }

    public Dimension computePaletteSize(int colorCount, int swatchSize) {
        int cols = (int) Math.ceil(Math.sqrt(colorCount));
        int rows = (int) Math.ceil((double) colorCount / cols);

        int paletteWidth = cols * swatchSize;
        int paletteHeight = rows * swatchSize;

        return new Dimension(paletteWidth, paletteHeight);
    }
}
