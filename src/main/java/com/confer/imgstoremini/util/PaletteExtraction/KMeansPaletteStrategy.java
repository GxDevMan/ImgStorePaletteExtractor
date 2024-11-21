package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

public class KMeansPaletteStrategy implements PaletteExtractionStrategy {
    private int kmeansIterations;
    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        kmeansIterations = (int) dataStore.getObject("default_kmeansiter");
        return applyKMeans(image, colorCount, observer, isCancelled);
    }

    private List<Color> applyKMeans(BufferedImage image, int k, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        List<Color> pixels = extractPixels(image);

        observer.updateProgress(0);
        observer.updateStatus("Initializing Centroids");

        // Step 1: Initialize centroids (randomly select `k` pixels)
        List<Color> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            centroids.add(pixels.get(random.nextInt(pixels.size())));
        }

        observer.updateProgress(10);

        // Step 2: K-Means iteration
        Map<Color, List<Color>> clusters;
        boolean centroidsChanged;
        int iteration = 0;
        int maxIterations = kmeansIterations; // Limit the number of iterations to avoid infinite loops

        do {
            if (isCancelled.get()){
                observer.updateStatus("K-means Cancelled");
                throw new CancellationException("Task Cancelled");
            }
            observer.updateStatus("Iteration " + (iteration + 1) + " in progress...");
            clusters = new HashMap<>();
            for (Color centroid : centroids) {
                clusters.put(centroid, new ArrayList<>());
            }

            // Assign colors to the nearest centroid
            for (Color pixel : pixels) {
                Color closestCentroid = findClosestCentroid(pixel, centroids);
                clusters.get(closestCentroid).add(pixel);
            }

            // Update centroids to be the average of their clusters
            centroidsChanged = false;
            List<Color> newCentroids = new ArrayList<>();
            for (Map.Entry<Color, List<Color>> entry : clusters.entrySet()) {
                Color newCentroid = calculateAverageColor(entry.getValue());
                newCentroids.add(newCentroid);
                if (!newCentroid.equals(entry.getKey())) {
                    centroidsChanged = true;
                }
            }

            centroids = newCentroids;

            // Increment iteration and update progress
            iteration++;
            double progress = Math.min(0.1 + 0.9 * (double) iteration / maxIterations, 1.0);
            observer.updateProgress(progress);

        } while (centroidsChanged && (iteration < maxIterations));

        observer.updateStatus("K-means Complete");
        observer.updateProgress(1.0);

        return centroids;
    }

    private List<Color> extractPixels(BufferedImage image) {
        List<Color> pixels = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels.add(new Color(image.getRGB(x, y)));
            }
        }
        return pixels;
    }

    private Color findClosestCentroid(Color pixel, List<Color> centroids) {
        Color closest = null;
        double minDistance = Double.MAX_VALUE;
        for (Color centroid : centroids) {
            double distance = calculateColorDistance(pixel, centroid);
            if (distance < minDistance) {
                minDistance = distance;
                closest = centroid;
            }
        }
        return closest;
    }

    private double calculateColorDistance(Color c1, Color c2) {
        int redDiff = c1.getRed() - c2.getRed();
        int greenDiff = c1.getGreen() - c2.getGreen();
        int blueDiff = c1.getBlue() - c2.getBlue();
        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }

    private Color calculateAverageColor(List<Color> colors) {
        if (colors.isEmpty()) {
            return new Color(0, 0, 0);
        }
        int redSum = 0, greenSum = 0, blueSum = 0;
        for (Color color : colors) {
            redSum += color.getRed();
            greenSum += color.getGreen();
            blueSum += color.getBlue();
        }
        int size = colors.size();
        return new Color(redSum / size, greenSum / size, blueSum / size);
    }
}

