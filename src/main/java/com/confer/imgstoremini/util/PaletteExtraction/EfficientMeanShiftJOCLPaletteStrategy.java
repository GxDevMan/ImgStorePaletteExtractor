package com.confer.imgstoremini.util.PaletteExtraction;

import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.List;

import static org.jocl.CL.*;

public class EfficientMeanShiftJOCLPaletteStrategy implements PaletteExtractionStrategy {
    private int maxIterations;
    private double convergenceThreshold;

    private Supplier<Boolean> isCancelled;
    private ProgressObserver observer;
    private ColorSpaceConversion colorSpaceConversion;

    private static final String KERNEL_ASSIGN_CL =
            "__kernel void assign_clusters(__global float *points, __global int *assignments, __global float *centroids, int numPoints, int numCentroids) {\n" +
                    "    int i = get_global_id(0);\n" +
                    "    if (i < numPoints) {\n" +
                    "        float minDist = INFINITY;\n" +
                    "        int closestCentroid = 0;\n" +
                    "        for (int j = 0; j < numCentroids; j++) {\n" +
                    "            float dist = 0.0;\n" +
                    "            for (int k = 0; k < 3; k++) {\n" +
                    "                dist += (points[i * 3 + k] - centroids[j * 3 + k]) * (points[i * 3 + k] - centroids[j * 3 + k]);\n" +
                    "            }\n" +
                    "            dist = sqrt(dist);\n" +
                    "            if (dist < minDist) {\n" +
                    "                minDist = dist;\n" +
                    "                closestCentroid = j;\n" +
                    "            }\n" +
                    "        }\n" +
                    "        assignments[i] = closestCentroid;\n" +
                    "    }\n" +
                    "}";

    private static final String KERNEL_UPDATE_CL =
            "__kernel void update_centroids(__global float *points, __global int *assignments, __global float *centroids, int numPoints, int numCentroids) {\n" +
                    "    int i = get_global_id(0);\n" +
                    "    if (i < numCentroids) {\n" +
                    "        float sum[3] = {0.0, 0.0, 0.0};\n" +
                    "        int count = 0;\n" +
                    "        for (int j = 0; j < numPoints; j++) {\n" +
                    "            if (assignments[j] == i) {\n" +
                    "                for (int k = 0; k < 3; k++) {\n" +
                    "                    sum[k] += points[j * 3 + k];\n" +
                    "                }\n" +
                    "                count++;\n" +
                    "            }\n" +
                    "        }\n" +
                    "        if (count > 0) {\n" +
                    "            for (int k = 0; k < 3; k++) {\n" +
                    "                centroids[i * 3 + k] = sum[k] / count;\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_meanshiftiter");
        convergenceThreshold = (double) dataStore.getObject("default_convergence_threshold");

        this.isCancelled = isCancelled;
        this.observer = observer;
        this.colorSpaceConversion = new ColorSpaceConversion();
        try {
            return generateColorPalette(image, colorCount, observer, isCancelled);
        } catch (Exception e) {
            throw new RuntimeException("Mean Shift GPU Failed");
        }
    }

    private List<Color> generateColorPalette(BufferedImage image, int topColorsCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("(GPU) Initializing palette extraction...");
        observer.updateProgress(0);

        List<Color> colors = getAllColors(image, observer, isCancelled);
        List<Color> dominantColors = meanShiftClustering(colors, topColorsCount, observer, isCancelled);

        observer.updateStatus(String.format("(GPU) Mean Shift Complete, Iterations:%d",maxIterations));
        observer.updateProgress(1);
        return dominantColors;
    }

    private List<Color> getAllColors(BufferedImage image, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("(GPU) -> Extracting all colors from the image...");
        List<Color> colors = new ArrayList<>();
        int totalPixels = image.getWidth() * image.getHeight();
        int processedPixels = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                checkInterrupt();
                colors.add(new Color(image.getRGB(x, y)));

                processedPixels++;
                if (processedPixels % (totalPixels / 10) == 0) {
                    observer.updateProgress(0.1 * processedPixels / totalPixels);
                }
            }
        }
        observer.updateProgress(0.3);
        return colors;
    }

    private List<Color> meanShiftClustering(List<Color> colors, int topColorsCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        observer.updateStatus("(GPU) Converting colors to LAB color space...");
        List<float[]> colorPoints = new ArrayList<>();
        int totalColors = colors.size();
        int processedColors = 0;

        for (Color color : colors) {
            checkInterrupt();
            float[] lab = colorSpaceConversion.rgbToLab(color);
            colorPoints.add(new float[]{lab[0], lab[1], lab[2]});

            processedColors++;
            if (processedColors % (totalColors / 10) == 0) {
                observer.updateProgress(0.3 + 0.1 * processedColors / totalColors);
            }
        }
        observer.updateProgress(1);
        observer.updateProgress(0.0);
        observer.updateStatus("(GPU) Initializing OpenCL for Mean Shift...");
        return runMeanShiftOnGPU(colorPoints, topColorsCount, observer, isCancelled);
    }

    private List<Color> runMeanShiftOnGPU(List<float[]> colorPoints, int topColorsCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        OpenCLUtils openCLUtils = new OpenCLUtils();
        CL.setExceptionsEnabled(true);
        cl_platform_id platform = openCLUtils.getPlatform();
        cl_device_id device = openCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        int numPoints = colorPoints.size();
        float[] pointArray = new float[numPoints * 3];
        for (int i = 0; i < numPoints; i++) {
            pointArray[i * 3] = colorPoints.get(i)[0];
            pointArray[i * 3 + 1] = colorPoints.get(i)[1];
            pointArray[i * 3 + 2] = colorPoints.get(i)[2];
        }

        cl_mem pointsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * pointArray.length, Pointer.to(pointArray), null);
        cl_mem centroidsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * topColorsCount * 3, null, null);
        cl_mem assignmentsBuffer = clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, Sizeof.cl_int * numPoints, null, null);

        String kernelSource = KERNEL_ASSIGN_CL + KERNEL_UPDATE_CL;
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, null);
        clBuildProgram(program, 0, null, null, null, null);

        cl_kernel assignKernel = clCreateKernel(program, "assign_clusters", null);
        cl_kernel updateKernel = clCreateKernel(program, "update_centroids", null);

        clSetKernelArg(assignKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(assignKernel, 1, Sizeof.cl_mem, Pointer.to(assignmentsBuffer));
        clSetKernelArg(assignKernel, 2, Sizeof.cl_mem, Pointer.to(centroidsBuffer));
        clSetKernelArg(assignKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(assignKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{topColorsCount}));

        clSetKernelArg(updateKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(updateKernel, 1, Sizeof.cl_mem, Pointer.to(assignmentsBuffer));
        clSetKernelArg(updateKernel, 2, Sizeof.cl_mem, Pointer.to(centroidsBuffer));
        clSetKernelArg(updateKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(updateKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{topColorsCount}));

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            if (isCancelled.get()) {
                observer.updateStatus("(GPU) Mean Shift Cancelled");
                throw new CancellationException("Mean Shift Computation Cancelled");
            }

            int displayIteration = iteration + 1;

            observer.updateStatus( String.format("(GPU) Iteration %d / %d", displayIteration, maxIterations));
            observer.updateProgress((double) iteration / maxIterations);

            clEnqueueNDRangeKernel(queue, assignKernel, 1, null, new long[]{numPoints}, null, 0, null, null);
            clEnqueueNDRangeKernel(queue, updateKernel, 1, null, new long[]{topColorsCount}, null, 0, null, null);

            float[] centroidArray = new float[topColorsCount * 3];
            clEnqueueReadBuffer(queue, centroidsBuffer, CL.CL_TRUE, 0, Sizeof.cl_float * centroidArray.length, Pointer.to(centroidArray), 0, null, null);

            boolean converged = true;
            for (int i = 1; i < topColorsCount; i++) {  // Start from 1 to avoid accessing invalid index for (i-1)
                float[] centroid = Arrays.copyOfRange(centroidArray, i * 3, (i + 1) * 3);
                float[] previousCentroid = Arrays.copyOfRange(centroidArray, (i - 1) * 3, i * 3);

                if (calculateDistance(centroid, previousCentroid) > convergenceThreshold) {
                    converged = false;
                    break;
                }
            }

            if (converged) {
                break;
            }

        }
        List<Color> extractedColors = extractCentroidColors(topColorsCount, centroidsBuffer, queue, context, observer);

        // Release resources
        clReleaseKernel(assignKernel);
        clReleaseKernel(updateKernel);
        clReleaseProgram(program);
        clReleaseMemObject(pointsBuffer);
        clReleaseMemObject(centroidsBuffer);
        clReleaseMemObject(assignmentsBuffer);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);

        return extractedColors;
    }

    private double calculateDistance(float[] centroid, float[] floats) {
        if (centroid.length != 3 || floats.length != 3) {
            throw new IllegalArgumentException("Both centroid and floats arrays must have 3 elements.");
        }

        double deltaL = centroid[0] - floats[0];
        double deltaA = centroid[1] - floats[1];
        double deltaB = centroid[2] - floats[2];

        return Math.sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB);
    }

    private List<Color> extractCentroidColors(int topColorsCount, cl_mem centroidsBuffer, cl_command_queue queue, cl_context context, ProgressObserver observer) {
        float[] centroidArray = new float[topColorsCount * 3];
        clEnqueueReadBuffer(queue, centroidsBuffer, CL.CL_TRUE, 0, Sizeof.cl_float * centroidArray.length, Pointer.to(centroidArray), 0, null, null);

        List<Color> dominantColors = new ArrayList<>();
        for (int i = 0; i < topColorsCount; i++) {
            float[] centroid = Arrays.copyOfRange(centroidArray, i * 3, (i + 1) * 3);
            Color color = colorSpaceConversion.labToRgb(centroid[0], centroid[1], centroid[2]);
            dominantColors.add(color);
        }

        observer.updateProgress(1);
        return dominantColors;
    }

    private void checkInterrupt() {
        if (isCancelled.get()) {
            observer.updateProgress(0);
            observer.updateStatus("(GPU) Mean Shift Cancelled");
            throw new CancellationException("Process Cancelled");
        }
    }
}
