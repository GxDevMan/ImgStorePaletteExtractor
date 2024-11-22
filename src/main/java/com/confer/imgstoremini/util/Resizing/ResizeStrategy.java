package com.confer.imgstoremini.util.Resizing;
import java.awt.image.BufferedImage;

public interface ResizeStrategy {
    BufferedImage resize(BufferedImage originalImage, int width, int height);
}
