package com.confer.imgstoremini.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.confer.imgstoremini.model.ImageType;

import javax.imageio.ImageIO;

public class ImageConversion {

    public static byte[] convertImageToByteArray(Image image, ImageType type) {
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

    public static Image convertBufferedImageToImage(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();

        InputStream inputStream = new ByteArrayInputStream(imageData);

        return new Image(inputStream);
    }

    public static BufferedImage convertImageToBufferedImage(Image image) {
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

    public static Image byteArraytoImage(byte[] imageBytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
        return new Image(byteArrayInputStream);
    }

    public static BufferedImage convertToJpgBufferedImage(BufferedImage inputImage) {
        BufferedImage jpgImage = new BufferedImage(
                inputImage.getWidth(),
                inputImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        // Draw the original image onto the new image
        Graphics2D g2d = jpgImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, null);
        g2d.dispose();

        return jpgImage;
    }


    public static byte[] extractByteArrayFromImage(BufferedImage image) {
        BufferedImage formattedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = formattedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return ((DataBufferByte) formattedImage.getRaster().getDataBuffer()).getData();
    }
}
