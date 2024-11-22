package com.confer.imgstoremini.util.PaletteExtraction;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;

public class EfficientMeanShiftPaletteStrategy implements PaletteExtractionStrategy {
    private int maxIterations;
    private double convergenceThreshold;

    private Supplier<Boolean> isCancelled;
    private ProgressObserver observer;
    private ColorSpaceConversion colorSpaceConversion;

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_meanshiftiter");
        convergenceThreshold = (double) dataStore.getObject("default_convergence_threshold");

        this.isCancelled = isCancelled;
        this.observer = observer;
        this.colorSpaceConversion = new ColorSpaceConversion();
        return generateColorPalette(image, colorCount, observer, isCancelled);
    }

    private List<Color> generateColorPalette(BufferedImage image, int topColorsCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("(CPU) Initializing palette extraction...");
        observer.updateProgress(0);

        List<Color> colors = getAllColors(image, observer, isCancelled);
        List<Color> dominantColors = meanShiftClustering(colors, topColorsCount, observer, isCancelled);

        observer.updateStatus(String.format("(CPU) Mean Shift Complete, Iterations:%d",maxIterations));
        observer.updateProgress(1);
        return dominantColors;
    }

    private List<Color> getAllColors(BufferedImage image, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("(CPU) Extracting all colors from the image...");
        List<Color> colors = new ArrayList<>();
        int totalPixels = image.getWidth() * image.getHeight();
        int processedPixels = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                checkInterrupt();
                colors.add(new Color(image.getRGB(x, y)));

                // Update progress for every 10% of pixels processed
                processedPixels++;
                if (processedPixels % (totalPixels / 10) == 0) {
                    observer.updateProgress(0.1 * processedPixels / totalPixels);
                }
            }
        }
        observer.updateProgress(0.3); // Progress after extracting all colors
        return colors;
    }

    private List<Color> meanShiftClustering(List<Color> colors, int topColorsCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("(CPU) Converting colors to LAB color space...");
        List<double[]> colorPoints = new ArrayList<>();
        int totalColors = colors.size();
        int processedColors = 0;

        for (Color color : colors) {
            checkInterrupt();
            float[] lab = colorSpaceConversion.rgbToLab(color);
            colorPoints.add(new double[]{lab[0], lab[1], lab[2]});

            // Update progress for every 10% of colors converted
            processedColors++;
            if (processedColors % (totalColors / 10) == 0) {
                observer.updateProgress(0.3 + 0.1 * processedColors / totalColors); // Progress 30%-40%
            }
        }

        observer.updateStatus("(CPU) Performing Mean Shift clustering...");

        List<double[]> centroids = new ArrayList<>();
        // Initialize centroids randomly (or use some initial heuristic)
        initializeCentroids(colorPoints, centroids, topColorsCount);

        int iterations = 0;
        boolean converged = false;

        while ((iterations < maxIterations) && !converged) {
            iterations++;

            observer.updateStatus(String.format("(CPU) Iteration %d of %d", iterations, maxIterations));

            // Assign each point to the nearest centroid
            List<int[]> assignments = new ArrayList<>();
            for (double[] point : colorPoints) {
                int[] assignment = new int[2]; // [centroidIndex, distance]
                assignment[0] = assignToClosestCentroid(point, centroids);
                assignments.add(assignment);
            }

            // Update centroids
            List<double[]> newCentroids = new ArrayList<>();
            for (int i = 0; i < centroids.size(); i++) {
                newCentroids.add(updateCentroid(i, colorPoints, assignments));
            }

            // Check convergence
            converged = checkConvergence(centroids, newCentroids);

            // Update centroids with new values
            centroids = newCentroids;

            observer.updateProgress(0.3 + 0.1 * iterations / maxIterations); // Update progress (30%-40%)
        }

        observer.updateStatus(converged ? "(CPU) Mean Shift Converged" : "(CPU) Max Iterations Reached");

        // Convert centroids back to RGB and return
        List<Color> dominantColors = new ArrayList<>();
        for (double[] centroid : centroids) {
            dominantColors.add(colorSpaceConversion.labToRgb((float) centroid[0], (float) centroid[1], (float) centroid[2]));
        }

        observer.updateProgress(0.9); // Almost complete
        return dominantColors;
    }

    private void initializeCentroids(List<double[]> colorPoints, List<double[]> centroids, int topColorsCount) {
        // Initialize centroids (e.g., pick random points or use KMeans++ heuristic)
        for (int i = 0; i < topColorsCount; i++) {
            int randomIndex = (int) (Math.random() * colorPoints.size());
            centroids.add(colorPoints.get(randomIndex));
        }
    }

    private int assignToClosestCentroid(double[] point, List<double[]> centroids) {
        double minDist = Double.MAX_VALUE;
        int closestCentroid = -1;

        for (int i = 0; i < centroids.size(); i++) {
            double dist = calculateDistance(point, centroids.get(i));
            if (dist < minDist) {
                minDist = dist;
                closestCentroid = i;
            }
        }

        return closestCentroid;
    }

    private double calculateDistance(double[] point1, double[] point2) {
        // Euclidean distance
        double sum = 0.0;
        for (int i = 0; i < 3; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }

    private double[] updateCentroid(int centroidIndex, List<double[]> colorPoints, List<int[]> assignments) {
        double[] sum = new double[3];
        int count = 0;

        for (int i = 0; i < colorPoints.size(); i++) {
            if (assignments.get(i)[0] == centroidIndex) {
                for (int j = 0; j < 3; j++) {
                    sum[j] += colorPoints.get(i)[j];
                }
                count++;
            }
        }

        if (count == 0) return colorPoints.get(centroidIndex); // No points assigned, return the original centroid

        for (int j = 0; j < 3; j++) {
            sum[j] /= count;
        }

        return sum;
    }

    private boolean checkConvergence(List<double[]> oldCentroids, List<double[]> newCentroids) {
        double maxDistance = 0.0;

        for (int i = 0; i < oldCentroids.size(); i++) {
            double dist = calculateDistance(oldCentroids.get(i), newCentroids.get(i));
            maxDistance = Math.max(maxDistance, dist);
        }

        return maxDistance < convergenceThreshold;
    }

    private void checkInterrupt(){
        if(isCancelled.get()){
            observer.updateStatus("(CPU) Mean Shift Cancelled");
            observer.updateProgress(0);
            throw new CancellationException("Mean Shift Computation Cancelled");
        }
    }
}

