package com.confer.imgstoremini.util.PaletteExtraction;
import org.jocl.*;

import static org.jocl.CL.*;

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

    public static void printPlatformInfo(cl_platform_id platform) {
        try {
            // Get platform name
            String platformName = getPlatformInfo(platform, CL_PLATFORM_NAME);
            System.out.println("Platform Name: " + platformName);

            // You can add more platform-related information as needed
            String platformVendor = getPlatformInfo(platform, CL_PLATFORM_VENDOR);
            System.out.println("Platform Vendor: " + platformVendor);
        } catch (Exception e) {
            System.out.println("Failed to retrieve platform info: " + e.getMessage());
        }
    }

    public static void printDeviceVersion(cl_device_id device) {
        try {
            String deviceVersion = getDeviceInfo(device, CL_DEVICE_VERSION);
            System.out.println("Device OpenCL Version: " + deviceVersion);
        } catch (Exception e) {
            System.out.println("Failed to retrieve device version: " + e.getMessage());
        }
    }

    public static String getPlatformInfo(cl_platform_id platform, int paramName) {
        byte[] buffer = new byte[1024];
        long[] size = new long[1];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), size);
        return new String(buffer, 0, (int) size[0]).trim();
    }


    public static void printPlatformVersion(cl_platform_id platform) {
        try {
            String platformVersion = getPlatformInfo(platform, CL_PLATFORM_VERSION);
            System.out.println("Platform OpenCL Version: " + platformVersion);
        } catch (Exception e) {
            System.out.println("Failed to retrieve platform version: " + e.getMessage());
        }
    }

    public static void printDeviceInfo(cl_device_id device) {
        try {
            // Get device name
            String deviceName = getDeviceInfo(device, CL_DEVICE_NAME);
            System.out.println("Device Name: " + deviceName);

            // You can add more device-related information as needed
            String deviceVendor = getDeviceInfo(device, CL_DEVICE_VENDOR);
            System.out.println("Device Vendor: " + deviceVendor);

            long globalMemSize = getDeviceLongInfo(device, CL_DEVICE_GLOBAL_MEM_SIZE);
            System.out.println("Global Memory Size: " + globalMemSize / (1024 * 1024) + " MB");

            long maxComputeUnits = getDeviceLongInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS);
            System.out.println("Max Compute Units: " + maxComputeUnits);
        } catch (Exception e) {
            System.out.println("Failed to retrieve device info: " + e.getMessage());
        }
    }

    public static String getDeviceInfo(cl_device_id device, int paramName) {
        byte[] buffer = new byte[1024];
        long[] size = new long[1];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), size);
        return new String(buffer, 0, (int) size[0]).trim();
    }

    public static long getDeviceLongInfo(cl_device_id device, int paramName) {
        long[] buffer = new long[1];
        clGetDeviceInfo(device, paramName, Sizeof.cl_long, Pointer.to(buffer), null);
        return buffer[0];
    }
}