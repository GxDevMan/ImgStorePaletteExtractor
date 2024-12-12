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
    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        maxIterations = (int) dataStore.getObject("default_kmeansiter");

        try {
            return applyKMeansJOCL(image, colorCount, observer, isCancelled);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Color> applyKMeansJOCL(BufferedImage image, int k, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        List<float[]> pixels = extractPixels(image);

        int numPoints = pixels.size();
        int numDimensions = 3; // RGB

        // Flatten pixel data
        float[] pointArray = new float[numPoints * numDimensions];
        for (int i = 0; i < numPoints; i++) {
            System.arraycopy(pixels.get(i), 0, pointArray, i * numDimensions, numDimensions);
        }

        // Initialize centroids randomly
        float[] centroidArray = initializeCentroids(pointArray, numPoints, numDimensions, k);
        ;

        // Set up OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id platform = OpenCLUtils.getPlatform();
        cl_device_id device = OpenCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueueWithProperties(context, device, null, null);

        // Create OpenCL buffers
        cl_mem pointsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * pointArray.length, Pointer.to(pointArray), null);
        cl_mem centroidsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * centroidArray.length, Pointer.to(centroidArray), null);
        cl_mem assignmentsBuffer = clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY, Sizeof.cl_int * numPoints, null, null);
        cl_mem countsBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_int * k, null, null);

        String assignClusterCode = KernelOpenCLENUM.KMEANS_ASSIGN_CLUSTER.getKernelCode();
        String updateCentroidsCode = KernelOpenCLENUM.KMEANS_UPDATE_CENTROIDS.getKernelCode();

//        System.out.println("");
//        System.out.println(assignClusterCode);
//        System.out.println("");
//        System.out.println(updateCentroidsCode);

        // Build kernel
        cl_program program = clCreateProgramWithSource(context, 2, new String[]{assignClusterCode, updateCentroidsCode}, null, null);
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
            int r = Math.min(255, Math.max(0, Math.round(centroidArray[i * 3])));
            int g = Math.min(255, Math.max(0, Math.round(centroidArray[i * 3 + 1])));
            int b = Math.min(255, Math.max(0, Math.round(centroidArray[i * 3 + 2])));

            centroids.add(new Color(r, g, b));
        }

        observer.updateStatus(String.format("(GPU) K-Means Complete, Iterations: %d", maxIterations));
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

    public float[] initializeCentroids(float[] pointArray, int numPoints, int numDimensions, int k) {
        float[] centroidArray = new float[k * numDimensions];
        Random random = new Random();

        for (int i = 0; i < k; i++) {
            int randomIndex = random.nextInt(numPoints);
            System.arraycopy(pointArray, randomIndex * numDimensions, centroidArray, i * numDimensions, numDimensions);
        }

        return centroidArray;
    }

//    public float[] cannyInitializeCentroids(
//            BufferedImage image, int numDimensions, int k, boolean getBackground) {
//
//        OpenCV.loadLocally();
//        float[] centroidArray = new float[k * numDimensions];
//
//        BufferedImage convertedToJpg = ImageConversion.convertToJpgBufferedImage(image);
//        byte[] matData = ImageConversion.extractByteArrayFromImage(convertedToJpg);
//
//        int width = convertedToJpg.getWidth();
//        int height = convertedToJpg.getHeight();
//
//        double minThreshold = 10;
//        double maxThreshold = 200;
//
//        // Perform Canny edge detection
//        Mat mat = new Mat(height, width, CvType.CV_8UC3);
//        mat.put(0, 0, matData);
//        Mat grayImage = new Mat(height, width, CvType.CV_8UC1);
//        Imgproc.cvtColor(mat, grayImage, Imgproc.COLOR_BGR2GRAY);
//        Mat edges = new Mat(height, width, CvType.CV_8UC1);
//        Imgproc.Canny(grayImage, edges, minThreshold, maxThreshold);
//
//        // Fill contours
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//        Mat mask = Mat.zeros(height, width, CvType.CV_8UC1);
//        Imgproc.drawContours(mask, contours, -1, new Scalar(255), Core.FILLED);
//
//        Random random = new Random();
//        int placedCentroids = 0;
//
//        while (placedCentroids < k) {
//            int x = random.nextInt(width);
//            int y = random.nextInt(height);
//
//            // Check if the pixel is valid (foreground or background based on `getBackground`)
//            if ((mask.get(y, x)[0] == 255) != getBackground) {
//                centroidArray[placedCentroids * numDimensions] = x;
//                centroidArray[placedCentroids * numDimensions + 1] = y;
//
//                // Sample RGB from the image at (x, y)
//                int rgb = image.getRGB(x, y);
//                centroidArray[placedCentroids * numDimensions + 0] = (rgb >> 16) & 0xFF;  // R
//                centroidArray[placedCentroids * numDimensions + 1] = (rgb >> 8) & 0xFF;   // G
//                centroidArray[placedCentroids * numDimensions + 2] = rgb & 0xFF;          // B
//
//                placedCentroids++;
//            }
//        }
//
//        return centroidArray;
//    }
}
