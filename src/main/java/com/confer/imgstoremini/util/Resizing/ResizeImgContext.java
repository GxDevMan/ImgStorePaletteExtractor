package com.confer.imgstoremini.util.Resizing;

import java.awt.image.BufferedImage;

public class ResizeImgContext {
    private ResizeStrategy strategy;

    public void setStrategy(ResizeStrategy strategy){
        this.strategy = strategy;
    }

    public BufferedImage executeResize(BufferedImage originalImage, int maxWidth, int maxHeight) {
        if (strategy == null) {
            throw new IllegalStateException("No Resize Strategy Set");
        }
        return strategy.resize(originalImage, maxWidth, maxHeight);
    }
}
