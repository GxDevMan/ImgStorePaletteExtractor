package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.List;

import static org.jocl.CL.*;

public class SpectralClusteringJOCLPaletteStrategy implements PaletteExtractionStrategy {
    private int maxIterations;
    private ProgressObserver observer;
    private Supplier<Boolean> isCancelled;

    private static final String kernelSourceAssignClusters =
            "__kernel void assign_clusters(\n" +
                    "    __global const float* points,\n" +
                    "    __global const float* eigenvectors,\n" +
                    "    __global int* assignments,\n" +
                    "    const int num_points,\n" +
                    "    const int num_clusters) {\n" +
                    "    int i = get_global_id(0);\n" +
                    "    if (i >= num_points) return;\n" +
                    "    float min_distance = FLT_MAX;\n" +
                    "    int best_cluster = 0;\n" +
                    "    for (int j = 0; j < num_clusters; j++) {\n" +
                    "        float distance = 0.0f;\n" +
                    "        for (int k = 0; k < 3; k++) {\n" +
                    "            float dx = points[i * 3 + k] - eigenvectors[j * 3 + k];\n" +
                    "            distance += dx * dx;\n" +
                    "        }\n" +
                    "        if (distance < min_distance) {\n" +
                    "            min_distance = distance;\n" +
                    "            best_cluster = j;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    assignments[i] = best_cluster;\n" +
                    "}";

    private static final String kernelSourceUpdateClusters =
            "__kernel void update_clusters(\n" +
                    "    __global const float* points,\n" +
                    "    __global const int* assignments,\n" +
                    "    __global float* eigenvectors,\n" +
                    "    const int num_points,\n" +
                    "    const int num_clusters) {\n" +
                    "    int i = get_global_id(0);\n" +
                    "    if (i >= num_clusters) return;\n" +
                    "    float sum[3] = {0.0f, 0.0f, 0.0f};\n" +
                    "    int count = 0;\n" +
                    "    for (int j = 0; j < num_points; j++) {\n" +
                    "        if (assignments[j] == i) {\n" +
                    "            sum[0] += points[j * 3];\n" +
                    "            sum[1] += points[j * 3 + 1];\n" +
                    "            sum[2] += points[j * 3 + 2];\n" +
                    "            count++;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    if (count > 0) {\n" +
                    "        eigenvectors[i * 3] = sum[0] / count;\n" +
                    "        eigenvectors[i * 3 + 1] = sum[1] / count;\n" +
                    "        eigenvectors[i * 3 + 2] = sum[2] / count;\n" +
                    "    }\n" +
                    "}";

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_spectraliter");
        this.observer = observer;
        this.isCancelled = isCancelled;
        List<float[]> pixels = extractPixels(image);
        try {
            return applySpectralClusteringJOCL(pixels, colorCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Color> applySpectralClusteringJOCL(List<float[]> pixels, int k) {
        int numPoints = pixels.size();
        int numDimensions = 3; // RGB

        // Flatten pixel data
        float[] pointArray = new float[numPoints * numDimensions];
        for (int i = 0; i < numPoints; i++) {
            System.arraycopy(pixels.get(i), 0, pointArray, i * numDimensions, numDimensions);
        }

        // Initialize eigenvectors randomly
        float[] eigenvectorArray = new float[k * numDimensions];
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            int randomIndex = random.nextInt(numPoints);
            System.arraycopy(pointArray, randomIndex * numDimensions, eigenvectorArray, i * numDimensions, numDimensions);
        }


        // Set up OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id platform = OpenCLUtils.getPlatform();
        cl_device_id device = OpenCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        // Create OpenCL buffers
        cl_mem pointsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * pointArray.length, Pointer.to(pointArray), null);
        cl_mem eigenvectorsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * eigenvectorArray.length, Pointer.to(eigenvectorArray), null);
        cl_mem assignmentsBuffer = clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, Sizeof.cl_int * numPoints, null, null);

        // Build kernels
        cl_program program = clCreateProgramWithSource(context, 2, new String[]{kernelSourceAssignClusters, kernelSourceUpdateClusters}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel assignKernel = clCreateKernel(program, "assign_clusters", null);
        cl_kernel updateKernel = clCreateKernel(program, "update_clusters", null);

        // Set arguments for the assign kernel
        clSetKernelArg(assignKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(assignKernel, 1, Sizeof.cl_mem, Pointer.to(eigenvectorsBuffer));
        clSetKernelArg(assignKernel, 2, Sizeof.cl_mem, Pointer.to(assignmentsBuffer));
        clSetKernelArg(assignKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(assignKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{k}));

        // Set arguments for the update kernel
        clSetKernelArg(updateKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(updateKernel, 1, Sizeof.cl_mem, Pointer.to(assignmentsBuffer));
        clSetKernelArg(updateKernel, 2, Sizeof.cl_mem, Pointer.to(eigenvectorsBuffer));
        clSetKernelArg(updateKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(updateKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{k}));

        // Main loop
        int[] assignments = new int[numPoints];
        Pointer assignmentsPointer = Pointer.to(assignments);
        try {
            for (int iteration = 0; iteration < maxIterations; iteration++) {
                if (isCancelled.get()) {
                    observer.updateStatus("(GPU) Spectral Clustering Cancelled");
                    throw new CancellationException("Task Cancelled");
                }

                int dispIteration = iteration + 1;
                observer.updateStatus(String.format("(GPU) Iteration %d / %d", dispIteration, maxIterations));

                // Cluster assignment
                clEnqueueNDRangeKernel(queue, assignKernel, 1, null, new long[]{numPoints}, null, 0, null, null);
                clEnqueueReadBuffer(queue, assignmentsBuffer, CL.CL_TRUE, 0, Sizeof.cl_int * numPoints, assignmentsPointer, 0, null, null);

                // Cluster update
                clEnqueueNDRangeKernel(queue, updateKernel, 1, null, new long[]{k}, null, 0, null, null);

                observer.updateProgress((double) iteration / maxIterations);
            }
        } finally {
            clReleaseKernel(assignKernel);
            clReleaseKernel(updateKernel);
            clReleaseProgram(program);
            clReleaseMemObject(pointsBuffer);
            clReleaseMemObject(eigenvectorsBuffer);
            clReleaseMemObject(assignmentsBuffer);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }

        // Convert eigenvectors to Java Color objects
        List<Color> eigenvectors = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            eigenvectors.add(new Color(
                    Math.round(eigenvectorArray[i * 3]),
                    Math.round(eigenvectorArray[i * 3 + 1]),
                    Math.round(eigenvectorArray[i * 3 + 2])));
        }

        observer.updateStatus(String.format("(GPU) Spectral Clustering Complete, Iterations: %d", maxIterations));
        return eigenvectors;
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
