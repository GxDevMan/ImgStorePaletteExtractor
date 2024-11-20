package com.confer.imgstoremini.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.confer.imgstoremini.model.ImageType;

import javax.imageio.ImageIO;

public class ImageConversion {

    public byte[] convertImageToByteArray(Image image, ImageType type) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            switch (type) {
                case PNG -> {
                    javax.imageio.ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
                }
                case JPEG, JPG -> {
                    BufferedImage jpegImage = new BufferedImage(
                            bufferedImage.getWidth(),
                            bufferedImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB
                    );
                    Graphics2D g = jpegImage.createGraphics();
                    g.setBackground(Color.WHITE);
                    g.clearRect(0, 0, jpegImage.getWidth(), jpegImage.getHeight());
                    g.drawImage(bufferedImage, 0, 0, null);
                    g.dispose();
                    javax.imageio.ImageIO.write(jpegImage, "jpeg", byteArrayOutputStream);
                    byteArrayOutputStream.close();
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    return byteArray;
                }
                default -> throw new UnsupportedOperationException("Unsupported image type: " + type);
            }
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public BufferedImage convertImageToBufferedImage(Image image) {
        BufferedImage bufferedImage = null;

        try {
            BufferedImage tempImage = SwingFXUtils.fromFXImage(image, null);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(tempImage, "PNG", byteArrayOutputStream);


            byte[] pngData = byteArrayOutputStream.toByteArray();
            bufferedImage = ImageIO.read(new ByteArrayInputStream(pngData));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bufferedImage;
    }

    public Image byteArraytoImage(byte[] imageBytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
        return new Image(byteArrayInputStream);
    }
}