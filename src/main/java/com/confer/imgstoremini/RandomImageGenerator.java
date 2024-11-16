package com.confer.imgstoremini;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

public class RandomImageGenerator {

    public Image generateRandomImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = random.nextInt(256);
                int green = random.nextInt(256);
                int blue = random.nextInt(256);
                Color randomColor = new Color(red, green, blue);
                image.setRGB(x, y, randomColor.getRGB());
            }
        }
        return SwingFXUtils.toFXImage(image, null);
    }
}
