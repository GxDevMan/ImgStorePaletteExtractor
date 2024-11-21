package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import static org.jocl.CL.*;

public class KMeansJOCLPaletteStrategy implements PaletteExtractionStrategy {
    private int maxIterations;

    private static final String kernelSource =
            "__kernel void assign_clusters(\n" +
                    "    __global const float* points,\n" +
                    "    __global const float* centroids,\n" +
                    "    __global int* assignments,\n" +
                    "    const int num_points,\n" +
                    "    const int num_centroids) {\n" +
                    "    int i = get_global_id(0); // Point index\n" +
                    "    if (i >= num_points) return;\n" +
                    "\n" +
                    "    float min_distance = FLT_MAX;\n" +
                    "    int best_centroid = 0;\n" +
                    "\n" +
                    "    for (int j = 0; j < num_centroids; j++) {\n" +
                    "        float dx = points[i * 3] - centroids[j * 3];\n" +
                    "        float dy = points[i * 3 + 1] - centroids[j * 3 + 1];\n" +
                    "        float dz = points[i * 3 + 2] - centroids[j * 3 + 2];\n" +
                    "        float distance = dx * dx + dy * dy + dz * dz;\n" +
                    "\n" +
                    "        if (distance < min_distance) {\n" +
                    "            min_distance = distance;\n" +
                    "            best_centroid = j;\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    assignments[i] = best_centroid;\n" +
                    "}";

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_kmeansiter");
        List<float[]> pixels = extractPixels(image);
        try {
            return applyKMeansJOCL(pixels, colorCount, observer, isCancelled);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Color> applyKMeansJOCL(List<float[]> pixels, int k, ProgressObserver observer, Supplier<Boolean> isCancelled) throws IOException {
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

        // Read and compile the kernel
        String programSource = this.kernelSource;
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{programSource}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = clCreateKernel(program, "assign_clusters", null);

        // Set kernel arguments
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(centroidsBuffer));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(assignmentsBuffer));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{k}));

        // Execute the kernel for each iteration
        int[] assignments = new int[numPoints];
        Pointer assignmentsPointer = Pointer.to(assignments);

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            if (isCancelled.get()) {
                observer.updateStatus("K-Means Cancelled");
                throw new CancellationException("Task Cancelled");
            }

            observer.updateStatus("Iteration " + (iteration + 1));
            clEnqueueNDRangeKernel(queue, kernel, 1, null, new long[]{numPoints}, null, 0, null, null);

            // Read assignments from the GPU
            clEnqueueReadBuffer(queue, assignmentsBuffer, CL.CL_TRUE, 0, Sizeof.cl_int * assignments.length, assignmentsPointer, 0, null, null);

            // Update centroids based on assignments (done on CPU for simplicity)
            // Centroid update logic goes here...

            observer.updateProgress((double) iteration / maxIterations);
        }

        // Release OpenCL resources
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseMemObject(pointsBuffer);
        clReleaseMemObject(centroidsBuffer);
        clReleaseMemObject(assignmentsBuffer);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);

        // Convert centroids to Java Color objects
        List<Color> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            centroids.add(new Color(
                    (int) centroidArray[i * 3],
                    (int) centroidArray[i * 3 + 1],
                    (int) centroidArray[i * 3 + 2]));
        }

        observer.updateStatus("K-Means Complete");
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

