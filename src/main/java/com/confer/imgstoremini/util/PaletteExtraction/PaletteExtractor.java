package com.confer.imgstoremini.util.PaletteExtraction;

import com.confer.imgstoremini.util.ProgressObserver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Supplier;

public class PaletteExtractor {
    private PaletteExtractionStrategy strategy;
    private ProgressObserver observer;

    public void setStrategy(PaletteExtractionStrategy strategy) {
        this.strategy = strategy;
    }

    public void setObserver(ProgressObserver observer){
        this.observer = observer;
    }

    public List<Color> extractPalette(BufferedImage image, int colorCount, Supplier<Boolean> isCancelled) {
        if (strategy == null) {
            throw new IllegalStateException("No palette extraction strategy set");
        }
        return strategy.extractPalette(image, colorCount, observer, isCancelled);
    }
}
