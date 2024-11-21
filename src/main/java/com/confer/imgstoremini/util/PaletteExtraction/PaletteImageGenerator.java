package com.confer.imgstoremini.util.PaletteExtraction;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class PaletteImageGenerator {

    public BufferedImage generatePaletteImage(List<Color> colors, int swatchSize) {
        Dimension dimension = computePaletteSize(colors.size(), swatchSize);
        int paletteWidth = (int) dimension.getWidth();
        int paletteHeight = (int) dimension.getHeight();

        int colorCount = colors.size();
        int cols = (int) Math.ceil(Math.sqrt(colorCount));  // Number of columns
        int rows = (int) Math.ceil((double) colorCount / cols);  // Number of rows

        BufferedImage paletteImage = new BufferedImage(paletteWidth, paletteHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = paletteImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int xOffset = (paletteWidth - (cols * swatchSize + (cols - 1) * swatchSize)) / 2; // Center the palette
        int yOffset = (paletteHeight - (rows * swatchSize + (rows - 1) * swatchSize)) / 2;

        for (int i = 0; i < colorCount; i++) {
            int row = i / cols;
            int col = i % cols;

            int x = xOffset + col * (swatchSize + swatchSize); // Add space between circles
            int y = yOffset + row * (swatchSize + swatchSize); // Add space between circles

            graphics.setColor(colors.get(i));
            graphics.fillOval(x, y, swatchSize, swatchSize);  // Draw circle instead of rectangle
        }

        graphics.dispose();
        return paletteImage;
    }

    public Dimension computePaletteSize(int colorCount, int swatchSize) {
        int cols = (int) Math.ceil(Math.sqrt(colorCount));
        int rows = (int) Math.ceil((double) colorCount / cols);

        // Add extra space between circles
        int paletteWidth = cols * swatchSize + (cols - 1) * swatchSize;
        int paletteHeight = rows * swatchSize + (rows - 1) * swatchSize;

        return new Dimension(paletteWidth, paletteHeight);
    }
}
