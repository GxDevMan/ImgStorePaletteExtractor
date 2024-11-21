package com.confer.imgstoremini.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class PaletteExtractor {
    private PaletteExtractionStrategy strategy;

    public void setStrategy(PaletteExtractionStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Color> extractPalette(BufferedImage image, int colorCount) {
        if (strategy == null) {
            throw new IllegalStateException("No palette extraction strategy set");
        }
        return strategy.extractPalette(image, colorCount);
    }
}
