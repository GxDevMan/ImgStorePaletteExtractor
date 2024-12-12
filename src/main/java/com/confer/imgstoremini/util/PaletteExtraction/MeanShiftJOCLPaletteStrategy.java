package com.confer.imgstoremini.util.PaletteExtraction;

import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import static org.jocl.CL.*;

public class MeanShiftJOCLPaletteStrategy implements PaletteExtractionStrategy {
    ProgressObserver observer;
    Supplier<Boolean> isCancelled;

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        this.observer = observer;
        this.isCancelled = isCancelled;

        boolean cannyEdgeActive = true;

        DataStore dataStore = DataStore.getInstance();
        int maxIterations = (int) dataStore.getObject("default_meanshiftiter");
        double convergenceThreshold = (double) dataStore.getObject("default_convergence_threshold");
        float epsilon = (float) convergenceThreshold;
        double bandwidthStore = (double) dataStore.getObject("default_meanshift_bandwidth");
        float bandwidth = (float) bandwidthStore;

        //System.out.println(String.format("Mean Shift Set Bandwidth: %f", bandwidth));

        int width = image.getWidth();
        int height = image.getHeight();
        int numPixels = width * height;
        float[] rgbPoints = new float[numPixels * 3];

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                rgbPoints[index++] = color.getRed();
                rgbPoints[index++] = color.getGreen();
                rgbPoints[index++] = color.getBlue();
            }
        }

        float[] centroids = initializeCentroids(rgbPoints, colorCount);

        CL.setExceptionsEnabled(true);
        cl_platform_id platform = OpenCLUtils.getPlatform();
        cl_device_id device = OpenCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueueWithProperties(context, device, null, null);

        cl_mem pointBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * rgbPoints.length, Pointer.to(rgbPoints), null);
        cl_mem centroidBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * centroids.length, Pointer.to(centroids), null);
        cl_mem updatedCentroidBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_float * centroids.length, null, null);
        cl_mem neighborCountBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int * rgbPoints.length, null, null);

        String computeDistance = KernelOpenCLENUM.MEANSHIFT_COMPUTE_COLOR_DISTANCE.getKernelCode();
        String findNeighbors = KernelOpenCLENUM.MEANSHIFT_FIND_NEIGHBORS_WITHIN_BANDWIDTH.getKernelCode();
        String updateCentroid = KernelOpenCLENUM.MEANSHIFTUPDATE_CLUSTER_CENTER.getKernelCode();
        String checkConvergence = KernelOpenCLENUM.MEANSHIFTCHECK_CONVERGENCE.getKernelCode();

//        System.out.println("");
//        System.out.println(computeDistance);
//        System.out.println("");
//        System.out.println(findNeighbors);
//        System.out.println("");
//        System.out.println(updateCentroid);
//        System.out.println("");
//        System.out.println(checkConvergence);

        cl_program program = clCreateProgramWithSource(context, 4, new String[]{computeDistance, findNeighbors, updateCentroid, checkConvergence}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel computeColorDistanceKernel = clCreateKernel(program, "compute_color_distance", null);
        cl_kernel findneighborsKernel = clCreateKernel(program, "find_neighbors_within_bandwidth", null);
        cl_kernel updateCentroidKernel = clCreateKernel(program, "update_cluster_center", null);
        cl_kernel checkConvergenceKernel = clCreateKernel(program, "check_convergence", null);

        boolean converged = false;
        int countedIterations = 0;
        List<Color> palette;
        observer.updateStatus("(GPU) Computing Mean Shift...");
        observer.updateProgress(0.0);
        try {
            while ((!converged) && (countedIterations < maxIterations)) {
                if (isCancelled.get()) {
                    observer.updateStatus("(GPU) Mean Shift Computation Cancelled");
                    throw new CancellationException("(GPU) Mean Shift Computation Cancelled");
                }

                // Step 1: Compute distances between points and centroids
                clSetKernelArg(computeColorDistanceKernel, 0, Sizeof.cl_mem, Pointer.to(pointBuffer));
                clSetKernelArg(computeColorDistanceKernel, 1, Sizeof.cl_mem, Pointer.to(centroidBuffer));
                clSetKernelArg(computeColorDistanceKernel, 2, Sizeof.cl_mem, Pointer.to(neighborCountBuffer));
                clSetKernelArg(computeColorDistanceKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{rgbPoints.length / 3}));
                clEnqueueNDRangeKernel(queue, computeColorDistanceKernel, 1, null,
                        new long[]{rgbPoints.length / 3}, null, 0, null, null);

                // Step 2: Find neighbors within bandwidth
                clSetKernelArg(findneighborsKernel, 0, Sizeof.cl_mem, Pointer.to(pointBuffer));
                clSetKernelArg(findneighborsKernel, 1, Sizeof.cl_mem, Pointer.to(centroidBuffer));
                clSetKernelArg(findneighborsKernel, 2, Sizeof.cl_mem, Pointer.to(updatedCentroidBuffer));
                clSetKernelArg(findneighborsKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numPixels}));
                clSetKernelArg(findneighborsKernel, 4, Sizeof.cl_float, Pointer.to(new float[]{bandwidth}));
                clEnqueueNDRangeKernel(queue, findneighborsKernel, 1, null,
                        new long[]{rgbPoints.length / 3}, null, 0, null, null);

                // Step 3: Update centroids
                clSetKernelArg(updateCentroidKernel, 0, Sizeof.cl_mem, Pointer.to(updatedCentroidBuffer));
                clSetKernelArg(updateCentroidKernel, 1, Sizeof.cl_mem, Pointer.to(neighborCountBuffer));
                clSetKernelArg(updateCentroidKernel, 2, Sizeof.cl_int, Pointer.to(new int[]{centroids.length / 3}));
                clEnqueueNDRangeKernel(queue, updateCentroidKernel, 1, null,
                        new long[]{centroids.length / 3}, null, 0, null, null);

                // Step 4: Check convergence
                cl_mem convergedFlagBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE, Sizeof.cl_int, null, null);
                clSetKernelArg(checkConvergenceKernel, 0, Sizeof.cl_mem, Pointer.to(centroidBuffer));
                clSetKernelArg(checkConvergenceKernel, 1, Sizeof.cl_mem, Pointer.to(updatedCentroidBuffer));
                clSetKernelArg(checkConvergenceKernel, 2, Sizeof.cl_mem, Pointer.to(convergedFlagBuffer));
                clSetKernelArg(checkConvergenceKernel, 3, Sizeof.cl_float, Pointer.to(new float[]{epsilon}));
                clEnqueueNDRangeKernel(queue, checkConvergenceKernel, 1, null,
                        new long[]{centroids.length / 3}, null, 0, null, null);

                // Read convergence flag back to host
                int[] convergedFlag = new int[1];
                clEnqueueReadBuffer(queue, convergedFlagBuffer, CL_TRUE, 0, Sizeof.cl_int,
                        Pointer.to(convergedFlag), 0, null, null);
                converged = (convergedFlag[0] == 1);

                clReleaseMemObject(convergedFlagBuffer);

                double progress = ((countedIterations + 1) / maxIterations);
                observer.updateProgress(progress);
                observer.updateStatus(String.format("(GPU) Mean Shift, Iteration %d / %d", countedIterations, maxIterations));
                countedIterations++;
            }
            float[] finalCentroids = new float[centroids.length];
            clEnqueueReadBuffer(queue, centroidBuffer, CL_TRUE, 0,
                    Sizeof.cl_float * centroids.length, Pointer.to(finalCentroids), 0, null, null);

            palette = new ArrayList<>();
            for (int i = 0; i < finalCentroids.length / 3; i++) {
                int r = Math.round(finalCentroids[i * 3]);
                int g = Math.round(finalCentroids[i * 3 + 1]);
                int b = Math.round(finalCentroids[i * 3 + 2]);
                palette.add(new Color(r, g, b));
            }
        } finally {
            clReleaseMemObject(pointBuffer);
            clReleaseMemObject(centroidBuffer);
            clReleaseMemObject(updatedCentroidBuffer);
            clReleaseMemObject(neighborCountBuffer);
            clReleaseKernel(computeColorDistanceKernel);
            clReleaseKernel(findneighborsKernel);
            clReleaseKernel(updateCentroidKernel);
            clReleaseKernel(checkConvergenceKernel);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }

        observer.updateStatus(String.format("(GPU) Mean Shift Complete, Iterations: %d", countedIterations));
        observer.updateProgress(1.0);
        return palette;
    }

    private float[] initializeCentroids(float[] points, int colorCount) {
        Random random = new Random();
        float[] centroids = new float[colorCount * 3];
        int numPoints = points.length / 3;

        for (int i = 0; i < colorCount; i++) {
            int index = random.nextInt(numPoints) * 3;
            centroids[i * 3] = points[index];
            centroids[i * 3 + 1] = points[index + 1];
            centroids[i * 3 + 2] = points[index + 2];
        }

        return centroids;
    }
}
