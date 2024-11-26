package com.confer.imgstoremini.util.PaletteExtraction;

import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import com.confer.imgstoremini.util.Resizing.PngResizeStrategy;
import com.confer.imgstoremini.util.Resizing.ResizeImgContext;
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
                    "            prob += diff * diff / covariances[j * 3 + k];\n" +  // Correctly handling diagonal covariance
                    "        }\n" +
                    "        prob = -0.5f * prob - 0.5f * log(2.0f * M_PI) - 0.5f * log(covariances[j * 3]);\n" +  // Use log to avoid underflow
                    "        prob += log(priors[j]);\n" +  // Incorporate prior
                    "        if (prob > max_prob) {\n" +
                    "            max_prob = prob;\n" +
                    "            best_cluster = j;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    responsibilities[i * num_clusters + best_cluster] = 1.0f;\n" +  // Assign highest responsibility
                    "}";  // Close kernel


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
                    "    // Update covariance (diagonal) here\n" +
                    "    for (int k = 0; k < 3; k++) {\n" +
                    "        float sum_diff_square = 0.0f;\n" +
                    "        for (int i = 0; i < num_points; i++) {\n" +
                    "            float diff = points[i * 3 + k] - means[j * 3 + k];\n" +
                    "            sum_diff_square += responsibilities[i * num_clusters + j] * diff * diff;\n" +
                    "        }\n" +
                    "        covariances[j * 3 + k] = sum_diff_square / total_responsibility;\n" +
                    "    }\n" +
                    "}";  // Close kernel

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_gmmiter");
        this.observer = observer;
        this.isCancelled = isCancelled;

        int width = image.getWidth();
        int height = image.getHeight();

        int maxHeightWidth = (int) dataStore.getObject("default_gmmimageheightwidth");
        if((width > maxHeightWidth) || (height > maxHeightWidth)) {
            ResizeImgContext resizeImgContext = new ResizeImgContext();
            resizeImgContext.setStrategy(new PngResizeStrategy());
            image = resizeImgContext.executeResize(image,maxHeightWidth,maxHeightWidth);
            width = image.getWidth();
            height = image.getHeight();
        }

        int numPoints = width * height;
        int numDimensions = 3; // RGB

        // Flattened pixel data
        float[] pointArray = new float[numPoints * numDimensions];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (y * width + x) * numDimensions;
                int rgb = image.getRGB(x, y);
                pointArray[index] = (rgb >> 16) & 0xFF;
                pointArray[index + 1] = (rgb >> 8) & 0xFF;
                pointArray[index + 2] = rgb & 0xFF;
            }
        }

        // Apply GMM
        try {
            return applyGaussianMixtureModel(pointArray, numPoints, colorCount, numDimensions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Color> applyGaussianMixtureModel(float[] points, int numPoints, int k, int numDimensions) {
        // Initialize GMM parameters
        float[] means = new float[k * numDimensions];
        float[] covariances = new float[k * numDimensions];
        float[] priors = new float[k];
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            int randomIndex = random.nextInt(numPoints);
            System.arraycopy(points, randomIndex * numDimensions, means, i * numDimensions, numDimensions);
            priors[i] = 1.0f / k;
            Arrays.fill(covariances, i * numDimensions, (i + 1) * numDimensions, 1.0f);
        }

        // Set up OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id platform = OpenCLUtils.getPlatform();
        cl_device_id device = OpenCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        cl_mem pointsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * points.length, Pointer.to(points), null);
        cl_mem meansBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * means.length, Pointer.to(means), null);
        cl_mem covariancesBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * covariances.length, Pointer.to(covariances), null);
        cl_mem priorsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * priors.length, Pointer.to(priors), null);
        cl_mem responsibilitiesBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_float * numPoints * k, null, null);

        cl_program program = clCreateProgramWithSource(context, 2, new String[]{kernelSourceEStep, kernelSourceMStep}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel eStepKernel = clCreateKernel(program, "e_step", null);
        cl_kernel mStepKernel = clCreateKernel(program, "m_step", null);

        clSetKernelArg(eStepKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(eStepKernel, 1, Sizeof.cl_mem, Pointer.to(meansBuffer));
        clSetKernelArg(eStepKernel, 2, Sizeof.cl_mem, Pointer.to(covariancesBuffer));
        clSetKernelArg(eStepKernel, 3, Sizeof.cl_mem, Pointer.to(priorsBuffer));
        clSetKernelArg(eStepKernel, 4, Sizeof.cl_mem, Pointer.to(responsibilitiesBuffer));
        clSetKernelArg(eStepKernel, 5, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(eStepKernel, 6, Sizeof.cl_int, Pointer.to(new int[]{k}));

        clSetKernelArg(mStepKernel, 0, Sizeof.cl_mem, Pointer.to(pointsBuffer));
        clSetKernelArg(mStepKernel, 1, Sizeof.cl_mem, Pointer.to(responsibilitiesBuffer));
        clSetKernelArg(mStepKernel, 2, Sizeof.cl_mem, Pointer.to(meansBuffer));
        clSetKernelArg(mStepKernel, 3, Sizeof.cl_mem, Pointer.to(covariancesBuffer));
        clSetKernelArg(mStepKernel, 4, Sizeof.cl_mem, Pointer.to(priorsBuffer));
        clSetKernelArg(mStepKernel, 5, Sizeof.cl_int, Pointer.to(new int[]{numPoints}));
        clSetKernelArg(mStepKernel, 6, Sizeof.cl_int, Pointer.to(new int[]{k}));

        try {
            for (int iteration = 0; iteration < maxIterations; iteration++) {
                if (isCancelled.get()) {
                    observer.updateStatus("(GPU) GMM Clustering Cancelled");
                    observer.updateProgress(0.0);
                    throw new CancellationException("Task Cancelled");
                }

                // Update status every 5 iterations to reduce observer overhead
                if (iteration % 5 == 0 || iteration == maxIterations - 1) {
                    observer.updateStatus(String.format("(GPU) Iteration %d / %d", iteration + 1, maxIterations));
                    observer.updateProgress((double) iteration / maxIterations);
                }

                clEnqueueNDRangeKernel(queue, eStepKernel, 1, null, new long[]{numPoints}, null, 0, null, null);
                clEnqueueNDRangeKernel(queue, mStepKernel, 1, null, new long[]{k}, null, 0, null, null);
            }
        } finally {
            clReleaseMemObject(pointsBuffer);
            clReleaseMemObject(meansBuffer);
            clReleaseMemObject(covariancesBuffer);
            clReleaseMemObject(priorsBuffer);
            clReleaseMemObject(responsibilitiesBuffer);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }

        observer.updateStatus("Getting Colors...");
        observer.updateProgress(0.0);

        List<Color> colors = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            colors.add(new Color(
                    Math.round(means[i * 3]),
                    Math.round(means[i * 3 + 1]),
                    Math.round(means[i * 3 + 2])));

            observer.updateProgress((double) (i + 1) / k); // Accurate progress update
        }

        observer.updateStatus(String.format("(GPU) GMM Clustering Complete, Iterations: %d", maxIterations));
        observer.updateProgress(1.0);
        return colors;
    }
}