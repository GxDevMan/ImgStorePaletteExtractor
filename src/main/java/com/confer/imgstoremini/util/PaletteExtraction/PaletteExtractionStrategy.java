package com.confer.imgstoremini.util.PaletteExtraction;
import com.confer.imgstoremini.util.ProgressObserver;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Supplier;

public interface PaletteExtractionStrategy {
    List<Color> extractPalette(BufferedImage image, int colorCount, ProgressObserver observer, Supplier<Boolean> isCancelled);
}
