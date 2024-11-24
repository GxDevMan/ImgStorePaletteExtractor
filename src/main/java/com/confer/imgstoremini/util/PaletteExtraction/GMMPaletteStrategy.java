package com.confer.imgstoremini.util.PaletteExtraction;

import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import static org.jocl.CL.*;

public class GMMPaletteStrategy implements PaletteExtractionStrategy {
    private int maxIterations;
    private ProgressObserver observer;
    private Supplier<Boolean> isCancelled;

    private static final String kernelSourceEStep =
            "__kernel void e_step(\n" +
                    "    __global const float* points,\n" +
                    "    __global const float* means,\n" +
                    "    __global const float* covariances,\n" +
                    "    __global const float* priors,\n" +
                    "    __global float* responsibilities,\n" +
                    "    const int num_points,\n" +
                    "    const int num_clusters) {\n" +
                    "    int i = get_global_id(0);\n" +
                    "    if (i >= num_points) return;\n" +
                    "    float max_prob = -FLT_MAX;\n" +
                    "    int best_cluster = 0;\n" +
                    "    for (int j = 0; j < num_clusters; j++) {\n" +
                    "        float prob = 0.0f;\n" +
                    "        for (int k = 0; k < 3; k++) {\n" +
                    "            float diff = points[i * 3 + k] - means[j * 3 + k];\n" +
                    "            prob += diff * diff / covariances[j * 3 + k];\n" +
                    "        }\n" +
                    "        prob = exp(-0.5f * prob) / sqrt(covariances[j * 3]);\n" +
                    "        prob *= priors[j];\n" +
                    "        if (prob > max_prob) {\n" +
                    "            max_prob = prob;\n" +
                    "            best_cluster = j;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    responsibilities[i * num_clusters + best_cluster] = 1.0f;\n" +
                    "}" ;

    private static final String kernelSourceMStep =
            "__kernel void m_step(\n" +
                    "    __global const float* points,\n" +
                    "    __global const float* responsibilities,\n" +
                    "    __global float* means,\n" +
                    "    __global float* covariances,\n" +
                    "    __global float* priors,\n" +
                    "    const int num_points,\n" +
                    "    const int num_clusters) {\n" +
                    "    int j = get_global_id(0);\n" +
                    "    if (j >= num_clusters) return;\n" +
                    "    float sum[3] = {0.0f, 0.0f, 0.0f};\n" +
                    "    float total_responsibility = 0.0f;\n" +
                    "    for (int i = 0; i < num_points; i++) {\n" +
                    "        float resp = responsibilities[i * num_clusters + j];\n" +
                    "        total_responsibility += resp;\n" +
                    "        for (int k = 0; k < 3; k++) {\n" +
                    "            sum[k] += resp * points[i * 3 + k];\n" +
                    "        }\n" +
                    "    }\n" +
                    "    for (int k = 0; k < 3; k++) {\n" +
                    "        means[j * 3 + k] = sum[k] / total_responsibility;\n" +
                    "    }\n" +
                    "    priors[j] = total_responsibility / num_points;\n" +
                    "    // Update covariance here (for simplicity, assume a diagonal covariance matrix)\n" +
                    "    for (int k = 0; k < 3; k++) {\n" +
                    "        float sum_diff_square = 0.0f;\n" +
                    "        for (int i = 0; i < num_points; i++) {\n" +
                    "            float diff = points[i * 3 + k] - means[j * 3 + k];\n" +
                    "            sum_diff_square += responsibilities[i * num_clusters + j] * diff * diff;\n" +
                    "        }\n" +
                    "        covariances[j * 3 + k] = sum_diff_square / total_responsibility;\n" +
                    "    }\n" +
                    "}" ;

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_gmmiter");
        this.observer = observer;
        this.isCancelled = isCancelled;
        List<float[]> pixels = extractPixels(image);
        try {
            return applyGaussianMixtureModel(pixels, colorCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Color> applyGaussianMixtureModel(List<float[]> pixels, int k) {
        int numPoints = pixels.size();
        int numDimensions = 3; // RGB

        // Flatten pixel data
        float[] pointArray = new float[numPoints * numDimensions];
        for (int i = 0; i < numPoints; i++) {
            System.arraycopy(pixels.get(i), 0, pointArray, i * numDimensions, numDimensions);
        }

        // Initialize GMM parameters randomly
        float[] means = new float[k * numDimensions];
        float[] covariances = new float[k * numDimensions]; // For simplicity, assume diagonal covariance
        float[] priors = new float[k];
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            int randomIndex = random.nextInt(numPoints);
            System.arraycopy(pointArray, randomIndex * numDimensions, means, i * numDimensions, numDimensions);
            priors[i] = 1.0f / k;  // Uniform priors
            Arrays.fill(covariances, i * numDimensions, (i + 1) * numDimensions, 1.0f); // Diagonal covariance
        }

        // Set up OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id platform = OpenCLUtils.getPlatform();
        cl_device_id device = OpenCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        // Create OpenCL buffers
        cl_mem pointsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * pointArray.length, Pointer.to(pointArray), null);
        cl_mem meansBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * means.length, Pointer.to(means), null);
        cl_mem covariancesBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * covariances.length, Pointer.to(covariances), null);
        cl_mem priorsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * priors.length, Pointer.to(priors), null);
        cl_mem responsibilitiesBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * numPoints * k, null, null);

        // Build kernels
        cl_program program = clCreateProgramWithSource(context, 2, new String[]{kernelSourceEStep, kernelSourceMStep}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel eStepKernel = clCreateKernel(program, "e_step", null);
        cl_kernel mStepKernel = clCreateKernel(program, "m_step", null);

        // Set arguments for the E-step kernel
        clSetKernelArg(eStepKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(eStepKernel, 1, Sizeof.cl_mem, Pointer.to(meansBuffer));
        clSetKernelArg(eStepKernel, 2, Sizeof.cl_mem, Pointer.to(covariancesBuffer));
        clSetKernelArg(eStepKernel, 3, Sizeof.cl_mem, Pointer.to(priorsBuffer));
        clSetKernelArg(eStepKernel, 4, Sizeof.cl_mem, Pointer.to(responsibilitiesBuffer));
        clSetKernelArg(eStepKernel, 5, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(eStepKernel, 6, Sizeof.cl_int, Pointer.to(new int[]{k}));

        // Set arguments for the M-step kernel
        clSetKernelArg(mStepKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(mStepKernel, 1, Sizeof.cl_mem, Pointer.to(responsibilitiesBuffer));
        clSetKernelArg(mStepKernel, 2, Sizeof.cl_mem, Pointer.to(meansBuffer));
        clSetKernelArg(mStepKernel, 3, Sizeof.cl_mem, Pointer.to(covariancesBuffer));
        clSetKernelArg(mStepKernel, 4, Sizeof.cl_mem, Pointer.to(priorsBuffer));
        clSetKernelArg(mStepKernel, 5, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(mStepKernel, 6, Sizeof.cl_int, Pointer.to(new int[]{k}));

        // Main loop
        float[] responsibilitiesArray = new float[numPoints * k];
        Pointer responsibilitiesPointer = Pointer.to(responsibilitiesArray);
        try {
            for (int iteration = 0; iteration < maxIterations; iteration++) {
                if (isCancelled.get()) {
                    observer.updateStatus("(GPU) GMM Clustering Cancelled");
                    throw new CancellationException("Task Cancelled");
                }

                int dispIteration = iteration + 1;
                observer.updateStatus(String.format("(GPU) Iteration %d / %d", dispIteration, maxIterations));

                // Expectation step
                clEnqueueNDRangeKernel(queue, eStepKernel, 1, null, new long[]{numPoints}, null, 0, null, null);
                clEnqueueReadBuffer(queue, responsibilitiesBuffer, CL.CL_TRUE, 0, Sizeof.cl_float * numPoints * k, responsibilitiesPointer, 0, null, null);

                // Maximization step
                clEnqueueNDRangeKernel(queue, mStepKernel, 1, null, new long[]{k}, null, 0, null, null);

                observer.updateProgress((double) iteration / maxIterations);
            }
        } finally {
            clReleaseKernel(eStepKernel);
            clReleaseKernel(mStepKernel);
            clReleaseProgram(program);
            clReleaseMemObject(pointsBuffer);
            clReleaseMemObject(meansBuffer);
            clReleaseMemObject(covariancesBuffer);
            clReleaseMemObject(priorsBuffer);
            clReleaseMemObject(responsibilitiesBuffer);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }

        // Convert means to Java Color objects
        List<Color> colors = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            colors.add(new Color(
                    Math.round(means[i * 3]),
                    Math.round(means[i * 3 + 1]),
                    Math.round(means[i * 3 + 2])));
        }

        observer.updateStatus(String.format("(GPU) GMM Clustering Complete, Iterations: %d", maxIterations));
        return colors;
    }

    private List<float[]> extractPixels(BufferedImage image) {
        List<float[]> pixels = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                pixels.add(new float[] {
                        (rgb >> 16) & 0xFF,
                        (rgb >> 8) & 0xFF,
                        rgb & 0xFF
                });
            }
        }
        return pixels;
    }
}