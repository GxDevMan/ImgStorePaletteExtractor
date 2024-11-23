package com.confer.imgstoremini.util.PaletteExtraction;
import org.jocl.*;

public class OpenCLUtils {
    public static cl_platform_id getPlatform() {
        int[] numPlatformsArray = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        CL.clGetPlatformIDs(platforms.length, platforms, null);

        return platforms[0];
    }

    public static cl_device_id getDevice(cl_platform_id platform) {
        int[] numDevicesArray = new int[1];
        CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_GPU, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        if (numDevices == 0) {
            throw new RuntimeException("No GPU devices found on the platform.");
        }

        cl_device_id[] devices = new cl_device_id[numDevices];
        CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_GPU, devices.length, devices, null);

        return devices[0];
    }
}