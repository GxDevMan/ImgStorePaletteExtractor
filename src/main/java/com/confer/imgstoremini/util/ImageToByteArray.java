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
            javax.imageio.ImageIO.write(bufferedImage, type.name(), byteArrayOutputStream);
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
