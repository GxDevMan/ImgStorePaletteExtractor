package com.confer.imgstoremini.util.PaletteExtraction;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PaletteImageGenerator {

    public static BufferedImage generatePaletteImage(List<Color> colors, int swatchSize) {
        // Step 1: Sort the colors based on similarity
        colors = sortColorsBySimilarity(colors);

        // Step 2: Compute the palette size
        Dimension dimension = computePaletteSize(colors.size(), swatchSize);
        int paletteWidth = (int) dimension.getWidth();
        int paletteHeight = (int) dimension.getHeight();

        BufferedImage paletteImage = new BufferedImage(paletteWidth, paletteHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = paletteImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int colorCount = colors.size();
        int cols = (int) Math.ceil(Math.sqrt(colorCount));
        int rows = (int) Math.ceil((double) colorCount / cols);

        int spacing = 5;
        int totalSize = swatchSize + spacing;

        // Step 3: Center the palette
        int xOffset = (paletteWidth - cols * totalSize + spacing) / 2;
        int yOffset = (paletteHeight - rows * totalSize + spacing) / 2;

        // Step 4: Draw circles
        for (int i = 0; i < colorCount; i++) {
            int row = i / cols;
            int col = i % cols;

            int x = xOffset + col * totalSize;
            int y = yOffset + row * totalSize;

            graphics.setColor(colors.get(i));
            graphics.fillOval(x, y, swatchSize, swatchSize); // Draw circle
        }

        graphics.dispose();
        return paletteImage;
    }

    private static Dimension computePaletteSize(int colorCount, int swatchSize) {
        int cols = (int) Math.ceil(Math.sqrt(colorCount));
        int rows = (int) Math.ceil((double) colorCount / cols);

        int spacing = 5; // Space between circles
        int paletteWidth = cols * (swatchSize + spacing) - spacing;
        int paletteHeight = rows * (swatchSize + spacing) - spacing;

        return new Dimension(paletteWidth, paletteHeight);
    }

    private static List<Color> sortColorsBySimilarity(List<Color> colors) {
        if (colors.isEmpty()) {
            return colors;
        }

        List<Color> sortedColors = new ArrayList<>();
        List<Color> remainingColors = new ArrayList<>(colors);

        // Start with the first color
        sortedColors.add(remainingColors.remove(0));

        while (!remainingColors.isEmpty()) {
            Color lastColor = sortedColors.get(sortedColors.size() - 1);

            // Find the closest color
            Color closestColor = remainingColors.stream()
                    .min(Comparator.comparingDouble(c -> colorDistance(lastColor, c)))
                    .orElse(remainingColors.get(0));

            sortedColors.add(closestColor);
            remainingColors.remove(closestColor);
        }

        return sortedColors;
    }

    private static double colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        // Euclidean distance in RGB space
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
}