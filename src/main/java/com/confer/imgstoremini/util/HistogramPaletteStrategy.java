package com.confer.imgstoremini.util;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class HistogramPaletteStrategy implements PaletteExtractionStrategy {
    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount) {
        Map<Color, Integer> colorFrequency = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                colorFrequency.put(color, colorFrequency.getOrDefault(color, 0) + 1);
            }
        }

        return colorFrequency.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(colorCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
