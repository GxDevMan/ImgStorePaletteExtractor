package com.confer.imgstoremini.util;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.confer.imgstoremini.model.ImageType;

public class ImageToByteArray {

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
                    jpegImage.createGraphics().drawImage(bufferedImage, 0, 0, null);
                    javax.imageio.ImageIO.write(jpegImage, "jpeg", byteArrayOutputStream);
                }
                default -> throw new UnsupportedOperationException("Unsupported image type: " + type);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public Image byteArraytoImage(byte[] imageBytes){
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
        return new Image(byteArrayInputStream);
    }
}
