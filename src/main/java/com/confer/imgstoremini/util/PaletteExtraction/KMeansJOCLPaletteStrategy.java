package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import static org.jocl.CL.*;

public class KMeansJOCLPaletteStrategy implements PaletteExtractionStrategy {
    private int maxIterations;

    private static final String kernelSourceAssignClusters =
            "__kernel void assign_clusters(\n" +
                    "    __global const float* points,\n" +
                    "    __global const float* centroids,\n" +
                    "    __global int* assignments,\n" +
                    "    const int num_points,\n" +
                    "    const int num_centroids) {\n" +
                    "    int i = get_global_id(0); // Point index\n" +
                    "    if (i >= num_points) return;\n" +
                    "    float min_distance = FLT_MAX;\n" +
                    "    int best_centroid = 0;\n" +
                    "    for (int j = 0; j < num_centroids; j++) {\n" +
                    "        float dx = points[i * 3] - centroids[j * 3];\n" +
                    "        float dy = points[i * 3 + 1] - centroids[j * 3 + 1];\n" +
                    "        float dz = points[i * 3 + 2] - centroids[j * 3 + 2];\n" +
                    "        float distance = dx * dx + dy * dy + dz * dz;\n" +
                    "        if (distance < min_distance) {\n" +
                    "            min_distance = distance;\n" +
                    "            best_centroid = j;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    assignments[i] = best_centroid;\n" +
                    "}";

    private static final String kernelSourceUpdateCentroids =
            "__kernel void update_centroids(\n" +
                    "    __global const float* points,\n" +
                    "    __global const int* assignments,\n" +
                    "    __global float* centroids,\n" +
                    "    __global int* counts,\n" +
                    "    const int num_points,\n" +
                    "    const int num_centroids) {\n" +
                    "    int i = get_global_id(0); // Point index\n" +
                    "    if (i >= num_points) return;\n" +
                    "    \n" +
                    "    // Get the assigned centroid index\n" +
                    "    int centroid_idx = assignments[i];\n" +
                    "    \n" +
                    "    // Use bitwise reinterpretation to safely add float as int\n" +
                    "    atomic_add((__global int*)&centroids[centroid_idx * 3], as_int(points[i * 3]));\n" +
                    "    atomic_add((__global int*)&centroids[centroid_idx * 3 + 1], as_int(points[i * 3 + 1]));\n" +
                    "    atomic_add((__global int*)&centroids[centroid_idx * 3 + 2], as_int(points[i * 3 + 2]));\n" +
                    "    \n" +
                    "    // Increment the count for this centroid\n" +
                    "    atomic_add(&counts[centroid_idx], 1);\n" +
                    "}";

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_kmeansiter");
        List<float[]> pixels = extractPixels(image);
        try {
            return applyKMeansJOCL(pixels, colorCount, observer, isCancelled);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Color> applyKMeansJOCL(List<float[]> pixels, int k, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        int numPoints = pixels.size();
        int numDimensions = 3; // RGB

        // Flatten pixel data
        float[] pointArray = new float[numPoints * numDimensions];
        for (int i = 0; i < numPoints; i++) {
            System.arraycopy(pixels.get(i), 0, pointArray, i * numDimensions, numDimensions);
        }

        // Initialize centroids randomly
        float[] centroidArray = new float[k * numDimensions];
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            int randomIndex = random.nextInt(numPoints);
            System.arraycopy(pointArray, randomIndex * numDimensions, centroidArray, i * numDimensions, numDimensions);
        }

        OpenCLUtils openCLUtils = new OpenCLUtils();

        // Set up OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id platform = openCLUtils.getPlatform();
        cl_device_id device = openCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        // Create OpenCL buffers
        cl_mem pointsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * pointArray.length, Pointer.to(pointArray), null);
        cl_mem centroidsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * centroidArray.length, Pointer.to(centroidArray), null);
        cl_mem assignmentsBuffer = clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, Sizeof.cl_int * numPoints, null, null);
        cl_mem countsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_int * k, null, null);

        // Build kernels
        cl_program program = clCreateProgramWithSource(context, 2, new String[]{kernelSourceAssignClusters, kernelSourceUpdateCentroids}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel assignKernel = clCreateKernel(program, "assign_clusters", null);
        cl_kernel updateKernel = clCreateKernel(program, "update_centroids", null);

        // Set arguments for the assign kernel
        clSetKernelArg(assignKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));         // Points buffer
        clSetKernelArg(assignKernel, 1, Sizeof.cl_mem, Pointer.to(centroidsBuffer));      // Centroids buffer
        clSetKernelArg(assignKernel, 2, Sizeof.cl_mem, Pointer.to(assignmentsBuffer));    // Assignments buffer
        clSetKernelArg(assignKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numPoints})); // Total number of points
        clSetKernelArg(assignKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{k}));         // Total number of centroids

        // Set arguments for the update kernel
        clSetKernelArg(updateKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));         // Points buffer
        clSetKernelArg(updateKernel, 1, Sizeof.cl_mem, Pointer.to(assignmentsBuffer));    // Assignments buffer
        clSetKernelArg(updateKernel, 2, Sizeof.cl_mem, Pointer.to(centroidsBuffer));      // Centroids buffer
        clSetKernelArg(updateKernel, 3, Sizeof.cl_mem, Pointer.to(countsBuffer));         // Counts buffer
        clSetKernelArg(updateKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{numPoints})); // Total number of points
        clSetKernelArg(updateKernel, 5, Sizeof.cl_int, Pointer.to(new int[]{k}));         // Total number of centroids

        // Main loop
        int[] assignments = new int[numPoints];
        Pointer assignmentsPointer = Pointer.to(assignments);
        try {
            for (int iteration = 0; iteration < maxIterations; iteration++) {
                if (isCancelled.get()) {
                    observer.updateStatus("(GPU) K-Means Cancelled");
                    throw new CancellationException("Task Cancelled");
                }

                int dispIteration = iteration + 1;
                observer.updateStatus(String.format("(GPU) Iteration %d / %d", dispIteration, maxIterations));

                // Cluster assignment
                clEnqueueNDRangeKernel(queue, assignKernel, 1, null, new long[]{numPoints}, null, 0, null, null);
                clEnqueueReadBuffer(queue, assignmentsBuffer, CL.CL_TRUE, 0, Sizeof.cl_int * numPoints, assignmentsPointer, 0, null, null);

                // Centroid update
                clEnqueueNDRangeKernel(queue, updateKernel, 1, null, new long[]{k}, null, 0, null, null);

                observer.updateProgress((double) iteration / maxIterations);
            }
        } finally {
            clReleaseKernel(assignKernel);
            clReleaseKernel(updateKernel);
            clReleaseProgram(program);
            clReleaseMemObject(pointsBuffer);
            clReleaseMemObject(centroidsBuffer);
            clReleaseMemObject(assignmentsBuffer);
            clReleaseMemObject(countsBuffer);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }

        // Convert centroids to Java Color objects
        List<Color> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            centroids.add(new Color(
                    Math.round(centroidArray[i * 3]),
                    Math.round(centroidArray[i * 3 + 1]),
                    Math.round(centroidArray[i * 3 + 2])));
        }

        observer.updateStatus(String.format("(GPU) K-Means Complete, Iterations:%d", maxIterations));
        return centroids;
    }

    private List<float[]> extractPixels(BufferedImage image) {
        List<float[]> pixels = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                pixels.add(new float[]{
                        (rgb >> 16) & 0xFF,
                        (rgb >> 8) & 0xFF,
                        rgb & 0xFF
                });
            }
        }
        return pixels;
    }
}
