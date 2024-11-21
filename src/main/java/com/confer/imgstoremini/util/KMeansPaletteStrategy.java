package com.confer.imgstoremini.util;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class KMeansPaletteStrategy implements PaletteExtractionStrategy {
    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount) {
        return applyKMeans(image, colorCount);
    }

    private List<Color> applyKMeans(BufferedImage image, int k) {
        List<Color> pixels = extractPixels(image);

        // Step 1: Initialize centroids (randomly select `k` pixels)
        List<Color> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            centroids.add(pixels.get(random.nextInt(pixels.size())));
        }

        // Step 2: K-Means iteration
        Map<Color, List<Color>> clusters;
        boolean centroidsChanged;
        do {
            // Assign colors to the nearest centroid
            clusters = new HashMap<>();
            for (Color centroid : centroids) {
                clusters.put(centroid, new ArrayList<>());
            }
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
        } while (centroidsChanged);

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

