package com.confer.imgstoremini.util.PaletteExtraction;

import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.jocl.CL.*;

public class HistogramJOCLPaletteStrategy implements PaletteExtractionStrategy {
    private ProgressObserver observer;
    private Supplier<Boolean> isCancelled;

    private static final String kernelSourceComputeHistogram = "\n" +
            "__kernel void compute_histogram(\n" +
            "    __global const unsigned char* data,\n" +
            "    __global unsigned int* histogram,\n" +
            "    const unsigned int data_size) {\n" +
            "\n" +
            "    // Global thread ID\n" +
            "    int gid = get_global_id(0);\n" +
            "\n" +
            "    // Check bounds\n" +
            "    if (gid >= data_size / 3) return;\n" +
            "\n" +
            "    // Compute histogram index from RGB values\n" +
            "    int r = data[gid * 3];\n" +
            "    int g = data[gid * 3 + 1];\n" +
            "    int b = data[gid * 3 + 2];\n" +
            "    int index = r * 65536 + g * 256 + b;\n" +
            "\n" +
            "    // Increment the histogram bin atomically\n" +
            "    atomic_inc(&histogram[index]);\n" +
            "}\n";

    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        this.observer = observer;
        this.isCancelled = isCancelled;
        List<int[]> pixels = extractPixels(image);
        try {
            return applyHistogramComputationJOCL(pixels, colorCount);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private List<Color> applyHistogramComputationJOCL(List<int[]> pixels, int colorCount) {
        int numPoints = pixels.size();
        int numBins = 256 * 256 * 256;

        // Flatten pixel data (using all three color channels)
        observer.updateStatus("(GPU) Flattening Pixel Data ...");
        observer.updateProgress(0.2);
        byte[] pointArray = new byte[numPoints * 3];
        for (int i = 0; i < numPoints; i++) {
            pointArray[i * 3] = (byte) pixels.get(i)[0]; // Red channel
            pointArray[i * 3 + 1] = (byte) pixels.get(i)[1]; // Green channel
            pointArray[i * 3 + 2] = (byte) pixels.get(i)[2]; // Blue channel
        }

        // Initialize OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id platform = OpenCLUtils.getPlatform();
        cl_device_id device = OpenCLUtils.getDevice(platform);

        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        cl_program program = clCreateProgramWithSource(context, 1, new String[]{kernelSourceComputeHistogram}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = clCreateKernel(program, "compute_histogram", null);

        cl_mem dataBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_char * pointArray.length, Pointer.to(pointArray), null);
        cl_mem histogramBuffer = clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
                Sizeof.cl_int * numBins, null, null);

        int[] histogram = null;
        try {
            // Set kernel arguments
            clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(dataBuffer));
            clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(histogramBuffer));
            clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{pointArray.length / 3}));

            observer.updateStatus("(GPU) Computing Histogram ...");
            observer.updateProgress(0.4);

            // Execute kernel
            clEnqueueNDRangeKernel(queue, kernel, 1, null, new long[]{numPoints}, null, 0, null, null);
            clFinish(queue);

            observer.updateStatus("(GPU) Reading Histogram Data ...");
            observer.updateProgress(0.8);

            // Read the histogram back from the GPU
            histogram = new int[numBins];
            Pointer histogramPointer = Pointer.to(histogram);
            clEnqueueReadBuffer(queue, histogramBuffer, CL.CL_TRUE, 0, Sizeof.cl_int * numBins, histogramPointer, 0, null, null);
        } finally {
            clReleaseKernel(kernel);
            clReleaseProgram(program);
            clReleaseMemObject(dataBuffer);
            clReleaseMemObject(histogramBuffer);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }

        observer.updateStatus("(GPU) Histogram Computation complete");
        observer.updateProgress(1.0);
        return extractTopColorsFromHistogram(histogram, colorCount);
    }

    private List<Color> extractTopColorsFromHistogram(int[] histogram, int colorCount) {
        List<Color> colors = new ArrayList<>();
        for (int i = 0; i < colorCount; i++) {
            int maxIndex = 0;
            for (int j = 1; j < histogram.length; j++) {
                if (histogram[j] > histogram[maxIndex]) {
                    maxIndex = j;
                }
            }
            int r = (maxIndex >> 16) & 0xFF;
            int g = (maxIndex >> 8) & 0xFF;
            int b = maxIndex & 0xFF;


            colors.add(new Color(r, g, b));
            histogram[maxIndex] = -1;
        }
        return colors;
    }

    private List<int[]> extractPixels(BufferedImage image) {
        List<int[]> pixels = new ArrayList<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                pixels.add(new int[]{
                        (rgb >> 16) & 0xFF,
                        (rgb >> 8) & 0xFF,
                        rgb & 0xFF
                });
            }
        }
        return pixels;
    }
}