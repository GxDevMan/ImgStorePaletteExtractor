package com.confer.imgstoremini.util.PaletteExtraction;

import java.awt.*;

public class ColorSpaceConversion {
    public static float[] rgbToLab(Color color) {
        float[] rgb = new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
        return rgbToLab(rgb);
    }

    public static float[] rgbToLab(float[] rgb) {
        float[] xyz = rgbToXyz(rgb);

        float x = xyz[0] / 95.047f;
        float y = xyz[1] / 100.000f;
        float z = xyz[2] / 108.883f;

        x = (x > 0.008856) ? (float) Math.pow(x, 1 / 3.0) : (x * 903.3f + 16.0f) / 116.0f;
        y = (y > 0.008856) ? (float) Math.pow(y, 1 / 3.0) : (y * 903.3f + 16.0f) / 116.0f;
        z = (z > 0.008856) ? (float) Math.pow(z, 1 / 3.0) : (z * 903.3f + 16.0f) / 116.0f;

        float l = (116.0f * y) - 16.0f;
        float a = (x - y) * 500.0f;
        float b = (y - z) * 200.0f;

        return new float[]{l, a, b};
    }

    public static float[] rgbToXyz(float[] rgb) {
        float r = (rgb[0] > 0.04045) ? (float) Math.pow((rgb[0] + 0.055) / 1.055, 2.4) : rgb[0] / 12.92f;
        float g = (rgb[1] > 0.04045) ? (float) Math.pow((rgb[1] + 0.055) / 1.055, 2.4) : rgb[1] / 12.92f;
        float b = (rgb[2] > 0.04045) ? (float) Math.pow((rgb[2] + 0.055) / 1.055, 2.4) : rgb[2] / 12.92f;

        r = r * 100.0f;
        g = g * 100.0f;
        b = b * 100.0f;

        float x = (r * 0.4124564f) + (g * 0.3575761f) + (b * 0.1804375f);
        float y = (r * 0.2126729f) + (g * 0.7151522f) + (b * 0.0721750f);
        float z = (r * 0.0193339f) + (g * 0.1191920f) + (b * 0.9503041f);

        return new float[]{x, y, z};
    }

    public static Color labToRgb(float l, float a, float b) {
        float[] xyz = labToXyz(l, a, b);
        return xyzToRgb(xyz);
    }

    private static float[] labToXyz(float l, float a, float b) {
        float y = (l + 16.0f) / 116.0f;
        float x = a / 500.0f + y;
        float z = y - b / 200.0f;

        x = (x > 0.206893034) ? (float) Math.pow(x, 3) : (x - 16.0f / 116.0f) / 7.787f;
        y = (y > 0.206893034) ? (float) Math.pow(y, 3) : (y - 16.0f / 116.0f) / 7.787f;
        z = (z > 0.206893034) ? (float) Math.pow(z, 3) : (z - 16.0f / 116.0f) / 7.787f;

        x = x * 95.047f;
        y = y * 100.000f;
        z = z * 108.883f;

        return new float[]{x, y, z};
    }

    public static Color xyzToRgb(float[] xyz) {
        float x = xyz[0] / 100.0f;
        float y = xyz[1] / 100.0f;
        float z = xyz[2] / 100.0f;

        float r = (x * 3.2406f) + (y * -1.5372f) + (z * -0.4986f);
        float g = (x * -0.9689f) + (y * 1.8758f) + (z * 0.0415f);
        float b = (x * 0.0556f) + (y * -0.2040f) + (z * 1.0570f);

        r = (r > 0.0031308) ? (1.055f * (float) Math.pow(r, 1.0f / 2.4f)) - 0.055f : 12.92f * r;
        g = (g > 0.0031308) ? (1.055f * (float) Math.pow(g, 1.0f / 2.4f)) - 0.055f : 12.92f * g;
        b = (b > 0.0031308) ? (1.055f * (float) Math.pow(b, 1.0f / 2.4f)) - 0.055f : 12.92f * b;

        return new Color(Math.min(255, Math.max(0, (int) (r * 255))),
                Math.min(255, Math.max(0, (int) (g * 255))),
                Math.min(255, Math.max(0, (int) (b * 255))));
    }
}
