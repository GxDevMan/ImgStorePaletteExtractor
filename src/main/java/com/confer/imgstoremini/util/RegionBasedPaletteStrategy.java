package com.confer.imgstoremini.util;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RegionBasedPaletteStrategy implements PaletteExtractionStrategy {
    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount) {
        DataStore dataStore = DataStore.getInstance();
        int regionCount = (int) dataStore.getObject("default_regionspalette");
        List<Color> palette = new ArrayList<>();

        int regionWidth = image.getWidth() / regionCount;
        int regionHeight = image.getHeight() / regionCount;

        for (int row = 0; row < regionCount; row++) {
            for (int col = 0; col < regionCount; col++) {
                int x = col * regionWidth;
                int y = row * regionHeight;

                // Extract the region
                BufferedImage region = image.getSubimage(x, y, regionWidth, regionHeight);

                // Get the dominant color in this region
                Color dominantColor = getDominantColor(region);
                if (dominantColor != null) {
                    palette.add(dominantColor);
                }
            }
        }

        return palette.stream()
                .limit(colorCount)
                .collect(Collectors.toList());
    }

    private Color getDominantColor(BufferedImage region) {
        Map<Color, Integer> colorFrequencyMap = new HashMap<>();

        for (int y = 0; y < region.getHeight(); y++) {
            for (int x = 0; x < region.getWidth(); x++) {
                int rgb = region.getRGB(x, y);
                Color color = new Color(rgb);

                // Count the frequency of each color
                colorFrequencyMap.put(color, colorFrequencyMap.getOrDefault(color, 0) + 1);
            }
        }

        return colorFrequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
