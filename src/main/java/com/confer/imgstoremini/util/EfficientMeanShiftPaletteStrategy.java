package com.confer.imgstoremini.util;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;

public class EfficientMeanShiftPaletteStrategy implements PaletteExtractionStrategy {
    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.1;

    private Supplier<Boolean> isCancelled;
    private ProgressObserver observer;

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        this.isCancelled = isCancelled;
        this.observer = observer;
        return generateColorPalette(image, colorCount, observer, isCancelled);
    }

    private List<Color> generateColorPalette(BufferedImage image, int topColorsCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("Initializing palette extraction...");
        observer.updateProgress(0);

        List<Color> colors = getAllColors(image, observer, isCancelled);
        List<Color> dominantColors = meanShiftClustering(colors, topColorsCount, observer, isCancelled);

        observer.updateStatus("Palette extraction complete!");
        observer.updateProgress(1);
        return dominantColors;
    }

    private List<Color> getAllColors(BufferedImage image, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("Extracting all colors from the image...");
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
        observer.updateStatus("Converting colors to LAB color space...");
        List<double[]> colorPoints = new ArrayList<>();
        int totalColors = colors.size();
        int processedColors = 0;

        for (Color color : colors) {
            checkInterrupt();
            float[] lab = rgbToLab(color);
            colorPoints.add(new double[]{lab[0], lab[1], lab[2]});

            // Update progress for every 10% of colors converted
            processedColors++;
            if (processedColors % (totalColors / 10) == 0) {
                observer.updateProgress(0.3 + 0.1 * processedColors / totalColors); // Progress 30%-40%
            }
        }

        observer.updateStatus("Performing KMeans++ clustering...");
        KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<>(topColorsCount);
        List<DoublePoint> points = new ArrayList<>();
        for (double[] point : colorPoints) {
            checkInterrupt();
            points.add(new DoublePoint(point));
        }

        observer.updateStatus("Clustering centroids...");
        List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);

        observer.updateStatus("Extracting dominant colors...");
        List<Color> dominantColors = new ArrayList<>();
        for (CentroidCluster<DoublePoint> cluster : clusters) {
            checkInterrupt();
            double[] centroid = cluster.getCenter().getPoint();
            dominantColors.add(labToRgb((float) centroid[0], (float) centroid[1], (float) centroid[2]));
        }

        observer.updateProgress(0.9); // Almost complete
        return dominantColors;
    }


    private float[] rgbToLab(Color color) {
        // Convert RGB to LAB using standard conversion
        float[] rgb = new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
        return rgbToLab(rgb);
    }

    private float[] rgbToLab(float[] rgb) {
        // Convert RGB to XYZ color space first
        float[] xyz = rgbToXyz(rgb);

        // Convert XYZ to LAB color space
        float x = xyz[0] / 95.047f;
        float y = xyz[1] / 100.000f;
        float z = xyz[2] / 108.883f;

        x = (x > 0.008856) ? (float) Math.pow(x, 1 / 3.0) : (x * 903.3f + 16.0f) / 116.0f;
        y = (y > 0.008856) ? (float) Math.pow(y, 1 / 3.0) : (y * 903.3f + 16.0f) / 116.0f;
        z = (z > 0.008856) ? (float) Math.pow(z, 1 / 3.0) : (z * 903.3f + 16.0f) / 116.0f;

        float l = (116.0f * y) - 16.0f;
        float a = (x - y) * 500.0f;
        float b = (y - z) * 200.0f;

        return new float[]{l, a, b};
    }

    private float[] rgbToXyz(float[] rgb) {
        // Convert RGB to XYZ color space using standard conversion
        float r = (rgb[0] > 0.04045) ? (float) Math.pow((rgb[0] + 0.055) / 1.055, 2.4) : rgb[0] / 12.92f;
        float g = (rgb[1] > 0.04045) ? (float) Math.pow((rgb[1] + 0.055) / 1.055, 2.4) : rgb[1] / 12.92f;
        float b = (rgb[2] > 0.04045) ? (float) Math.pow((rgb[2] + 0.055) / 1.055, 2.4) : rgb[2] / 12.92f;

        r = r * 100.0f;
        g = g * 100.0f;
        b = b * 100.0f;

        float x = (r * 0.4124564f) + (g * 0.3575761f) + (b * 0.1804375f);
        float y = (r * 0.2126729f) + (g * 0.7151522f) + (b * 0.0721750f);
        float z = (r * 0.0193339f) + (g * 0.1191920f) + (b * 0.9503041f);

        return new float[]{x, y, z};
    }

    private Color labToRgb(float l, float a, float b) {
        // Convert LAB back to RGB
        float[] xyz = labToXyz(l, a, b);
        return xyzToRgb(xyz);
    }

    private float[] labToXyz(float l, float a, float b) {
        float y = (l + 16.0f) / 116.0f;
        float x = a / 500.0f + y;
        float z = y - b / 200.0f;

        x = (x > 0.206893034) ? (float) Math.pow(x, 3) : (x - 16.0f / 116.0f) / 7.787f;
        y = (y > 0.206893034) ? (float) Math.pow(y, 3) : (y - 16.0f / 116.0f) / 7.787f;
        z = (z > 0.206893034) ? (float) Math.pow(z, 3) : (z - 16.0f / 116.0f) / 7.787f;

        x = x * 95.047f;
        y = y * 100.000f;
        z = z * 108.883f;

        return new float[]{x, y, z};
    }

    private Color xyzToRgb(float[] xyz) {
        float x = xyz[0] / 100.0f;
        float y = xyz[1] / 100.0f;
        float z = xyz[2] / 100.0f;

        float r = (x * 3.2406f) + (y * -1.5372f) + (z * -0.4986f);
        float g = (x * -0.9689f) + (y * 1.8758f) + (z * 0.0415f);
        float b = (x * 0.0556f) + (y * -0.2040f) + (z * 1.0570f);

        r = (r > 0.0031308) ? (1.055f * (float) Math.pow(r, 1.0f / 2.4f)) - 0.055f : 12.92f * r;
        g = (g > 0.0031308) ? (1.055f * (float) Math.pow(g, 1.0f / 2.4f)) - 0.055f : 12.92f * g;
        b = (b > 0.0031308) ? (1.055f * (float) Math.pow(b, 1.0f / 2.4f)) - 0.055f : 12.92f * b;

        return new Color(Math.min(255, Math.max(0, (int) (r * 255))),
                Math.min(255, Math.max(0, (int) (g * 255))),
                Math.min(255, Math.max(0, (int) (b * 255))));
    }

    private void checkInterrupt(){
        if(isCancelled.get()){
            observer.updateStatus("Mean Shift Cancelled");
            observer.updateProgress(0);
            throw new CancellationException("Mean Shift Computation Cancelled");
        }
    }

}

