package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.ProgressObserver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HistogramPaletteStrategy implements PaletteExtractionStrategy {
    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        Map<Color, Integer> colorFrequency = new HashMap<>();

        int totalPixels = image.getWidth() * image.getHeight();
        int processedPixels = 0;

        observer.updateProgress(0);
        observer.updateStatus("Counting Color Frequencies...");

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {


                Color color = new Color(image.getRGB(x, y));
                colorFrequency.put(color, colorFrequency.getOrDefault(color, 0) + 1);
                processedPixels++;

                if (processedPixels % 1000 == 0) {
                    double progress = (double) processedPixels / totalPixels;
                    observer.updateProgress(progress);
                }

                if(isCancelled.get()){
                    observer.updateStatus("Histogram Computation Cancelled");
                    observer.updateProgress(0);
                    throw new CancellationException("Histogram Computation Cancelled");
                }
            }
        }

        observer.updateProgress(1.0);
        observer.updateStatus("Histogram Complete");

        return colorFrequency.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(colorCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
