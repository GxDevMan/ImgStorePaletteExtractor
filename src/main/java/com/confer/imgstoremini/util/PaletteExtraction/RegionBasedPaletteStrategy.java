package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RegionBasedPaletteStrategy implements PaletteExtractionStrategy {
    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        int regionCount = (int) dataStore.getObject("default_regionspalette");
        List<Color> palette = new ArrayList<>();

        int regionWidth = image.getWidth() / regionCount;
        int regionHeight = image.getHeight() / regionCount;
        int totalRegions = regionCount * regionCount;

        observer.updateProgress(0);
        observer.updateStatus("Extracting regions...");

        int processedRegions = 0;

        for (int row = 0; row < regionCount; row++) {
            for (int col = 0; col < regionCount; col++) {
                // Check for cancellation
                if (isCancelled.get()) {
                    observer.updateStatus("Operation canceled");
                    observer.updateProgress(0);
                    throw new CancellationException("Region-based extraction canceled.");
                }

                int x = col * regionWidth;
                int y = row * regionHeight;

                // Extract the region
                BufferedImage region = image.getSubimage(x, y, regionWidth, regionHeight);

                // Get the dominant color in this region
                Color dominantColor = getDominantColor(region);
                if (dominantColor != null) {
                    palette.add(dominantColor);
                }

                // Update progress and status
                processedRegions++;
                double progress = (double) processedRegions / totalRegions;
                observer.updateProgress(progress);
                observer.updateStatus(String.format("Processing region (%d, %d)", row + 1, col + 1));
            }
        }

        observer.updateProgress(1.0);
        observer.updateStatus("Region-based complete.");

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

        // Return the most frequent color
        return colorFrequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}

