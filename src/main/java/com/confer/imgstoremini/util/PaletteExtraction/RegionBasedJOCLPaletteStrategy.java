package com.confer.imgstoremini.util.PaletteExtraction;

import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.ProgressObserver;
import org.jocl.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.jocl.CL.*;

public class RegionBasedJOCLPaletteStrategy implements PaletteExtractionStrategy {

    private static final String kernelSource = "\n" +
            "__kernel void compute_dominant_color(\n" +
            "    __global const int* pixels,\n" +
            "    __global int* colorFrequencyMap,\n" +
            "    const int regionWidth,\n" +
            "    const int regionHeight,\n" +
            "    const int imageWidth,\n" +
            "    const int startX,\n" +
            "    const int startY) {\n" +
            "\n" +
            "    int idx = get_global_id(0);\n" +
            "    if (idx >= regionWidth * regionHeight) return;\n" +
            "\n" +
            "    int x = idx % regionWidth + startX;\n" +
            "    int y = idx / regionWidth + startY;\n" +
            "\n" +
            "    int pixel = pixels[y * imageWidth + x];\n" +
            "    int r = (pixel >> 16) & 0xFF;\n" +
            "    int g = (pixel >> 8) & 0xFF;\n" +
            "    int b = pixel & 0xFF;\n" +
            "\n" +
            "    // Quantize colors to reduce bins\n" +
            "    int quantizedColor = (r / 16) * 256 + (g / 16) * 16 + (b / 16);\n" +
            "\n" +
            "    atomic_add(&colorFrequencyMap[quantizedColor], 1);\n" +
            "}";

    @Override
    public List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled) {
        DataStore dataStore = DataStore.getInstance();
        int regionCount = (int) dataStore.getObject("default_regionspalette");
        List<Color> palette = new ArrayList<>();

        observer.updateStatus("(GPU) Designating Regions");
        observer.updateProgress(0.2);
        int regionWidth = image.getWidth() / regionCount;
        int regionHeight = image.getHeight() / regionCount;
        int totalRegions = regionCount * regionCount;

        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicInteger processedRegions = new AtomicInteger(0);

        // Flatten image pixel data for OpenCL
        int[] pixelData = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixelData, 0, image.getWidth());

        CL.setExceptionsEnabled(true);
        cl_platform_id platform = OpenCLUtils.getPlatform();
        cl_device_id device = OpenCLUtils.getDevice(platform);
        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        // Create OpenCL buffers
        cl_mem pixelBuffer = clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * pixelData.length, Pointer.to(pixelData), null);
        cl_mem colorFrequencyMap = clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_int * 16777216, null, null); // enough for 24-bit color space

        // Build the OpenCL program
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{kernelSource}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = clCreateKernel(program, "compute_dominant_color", null);


        int[] zeroArray = new int[16777216];
        int row = 0;
        int col = 0;
        try {
            // Launch OpenCL kernel
            for (row = 0; row < regionCount; row++) {
                for (col = 0; col < regionCount; col++) {
                    int x = col * regionWidth;
                    int y = row * regionHeight;

                    double progress = (double) processedRegions.get() / totalRegions;
                    observer.updateProgress(progress);
                    observer.updateStatus(String.format("Processing region (%d, %d)", row + 1, col + 1));

                    clEnqueueWriteBuffer(queue, colorFrequencyMap, CL.CL_TRUE, 0,
                            Sizeof.cl_int * zeroArray.length, Pointer.to(zeroArray), 0, null, null);

                    // Set kernel arguments for region
                    clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(pixelBuffer));
                    clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(colorFrequencyMap));
                    clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{regionWidth}));
                    clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{regionHeight}));
                    clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{image.getWidth()}));
                    clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{x}));
                    clSetKernelArg(kernel, 6, Sizeof.cl_int, Pointer.to(new int[]{y}));

                    // Execute the kernel for this region
                    clEnqueueNDRangeKernel(queue, kernel, 1, null, new long[]{regionWidth * regionHeight}, null, 0, null, null);

                    // Read back results
                    int[] freqMap = new int[16777216]; // assuming 24-bit color space
                    clEnqueueReadBuffer(queue, colorFrequencyMap, CL.CL_TRUE, 0, Sizeof.cl_int * freqMap.length, Pointer.to(freqMap), 0, null, null);
                    clEnqueueWriteBuffer(queue, colorFrequencyMap, CL.CL_TRUE, 0, Sizeof.cl_int * zeroArray.length, Pointer.to(zeroArray), 0, null, null);

                    // Determine the dominant color for this region
                    Color dominantColor = getDominantColorFromFrequencyMap(freqMap);
                    if (dominantColor != null) {
                        palette.add(dominantColor);
                    }

                    processedRegions.incrementAndGet();
                    observer.updateProgress((double) processedRegions.get() / totalRegions);
                    if (isCancelled.get()) {
                        cancelled.set(true);
                        observer.updateStatus("Operation cancelled");
                        throw new CancellationException("(GPU) Region Based Cancelled");
                    }
                }
            }
        } finally {
            // Clean up OpenCL resources
            clReleaseMemObject(pixelBuffer);
            clReleaseMemObject(colorFrequencyMap);
            clReleaseKernel(kernel);
            clReleaseProgram(program);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }
        observer.updateStatus(String.format("0(GPU) Region Based Computation Complete (%d,%d)",row,col));
        observer.updateProgress(1.0);        // Return the final palette, limited to requested color count
        return palette.stream().distinct().limit(colorCount).collect(Collectors.toList());
    }

    private Color getDominantColorFromFrequencyMap(int[] freqMap) {
        int maxFreq = 0;
        int dominantColor = -1;
        for (int i = 0; i < freqMap.length; i++) {
            if (freqMap[i] > maxFreq) {
                maxFreq = freqMap[i];
                dominantColor = i;
            }
        }
        return new Color(dominantColor);
    }
}
