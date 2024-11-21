package com.confer.imgstoremini.util;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public interface PaletteExtractionStrategy {
    List<Color> extractPalette(BufferedImage image, int colorCount);
}
