package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicInteger processedRegions = new AtomicInteger(0);

        // Use parallel streams to process regions concurrently
        List<Color> dominantColors = new ArrayList<>();
        for (int row = 0; row < regionCount; row++) {
            for (int col = 0; col < regionCount; col++) {
                if (cancelled.get()) {
                    observer.updateStatus("Region Based Cancelled");
                    break;
                }
                int x = col * regionWidth;
                int y = row * regionHeight;
                BufferedImage region = image.getSubimage(x, y, regionWidth, regionHeight);

                // Extract the dominant color in this region
                Color dominantColor = getDominantColor(region);
                if (dominantColor != null) {
                    dominantColors.add(dominantColor);
                }

                // Update progress and status
                processedRegions.incrementAndGet();
                double progress = (double) processedRegions.get() / totalRegions;
                observer.updateProgress(progress);
                observer.updateStatus(String.format("Processing region (%d, %d)", row + 1, col + 1));

                if (isCancelled.get()) {
                    cancelled.set(true);
                    observer.updateStatus("Operation canceled");
                    observer.updateProgress(0);
                    break;
                }
            }
            if (cancelled.get()) {
                break;
            }
        }

        observer.updateProgress(1.0);
        observer.updateStatus("Region-based extraction complete.");

        // Limit the palette to the requested color count
        return dominantColors.stream()
                .distinct()
                .limit(colorCount)
                .collect(Collectors.toList());
    }

    private Color getDominantColor(BufferedImage region) {
        Map<Color, Integer> colorFrequencyMap = new HashMap<>();
        int width = region.getWidth();
        int height = region.getHeight();

        // Sample every pixel in the region
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = region.getRGB(x, y);
                Color color = new Color(rgb);

                // Count the frequency of each color
                colorFrequencyMap.merge(color, 1, Integer::sum);
            }
        }

        // Return the most frequent color
        return colorFrequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
