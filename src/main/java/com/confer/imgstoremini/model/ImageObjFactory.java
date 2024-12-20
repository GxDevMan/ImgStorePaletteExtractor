package com.confer.imgstoremini.model;

import com.confer.imgstoremini.exceptions.InvalidImgObjException;
import com.confer.imgstoremini.util.ImageConversion;
import com.confer.imgstoremini.util.Resizing.JpegResizeStrategy;
import com.confer.imgstoremini.util.Resizing.PngResizeStrategy;
import com.confer.imgstoremini.util.Resizing.ResizeImgContext;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ImageObjFactory {
    public static ImageObj createNewImageObj(String imageTitle, String imageTags, ImageType imageType, Image image) throws InvalidImgObjException {
        byte[] imageByte;
        try {
            imageByte = ImageConversion.convertImageToByteArray(image, imageType);
        } catch (Exception e) {
            throw new InvalidImgObjException("Full Image Byte conversion Failed");
        }

        if (isNullOrEmpty(imageTitle)) {
            throw new InvalidImgObjException("No Image Title provided");
        }

        if (isNullOrEmpty(imageTags)) {
            throw new InvalidImgObjException("No Image Tags provided");
        }

        if (imageType == null) {
            throw new InvalidImgObjException("No Image Type provided");
        }

        ResizeImgContext resizeImgContext = new ResizeImgContext();
        switch (imageType) {
            case PNG -> resizeImgContext.setStrategy(new PngResizeStrategy());
            case JPG, JPEG -> resizeImgContext.setStrategy(new JpegResizeStrategy());
            default -> throw new InvalidImgObjException("Invalid Image Type");
        }

        if (imageByte == null || imageByte.length == 0) {
            throw new InvalidImgObjException("Image byte array is null or empty");
        }


        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        java.sql.Timestamp date = java.sql.Timestamp.valueOf(localDateTime);

        BufferedImage resizedImagedBfr = resizeImgContext.executeResize(SwingFXUtils.fromFXImage(image, null), 500, 500);
        Image resizedImage = SwingFXUtils.toFXImage(resizedImagedBfr, null);

        byte[] thumbnailByte;
        try {
           thumbnailByte = ImageConversion.convertImageToByteArray(resizedImage, imageType);
        } catch (Exception e) {
            throw new InvalidImgObjException("Thumbnail Conversion Failed");
        }

        if (thumbnailByte == null || thumbnailByte.length == 0)
            throw new InvalidImgObjException("Resizing failed");

        return new ImageObj(imageTitle.trim(), imageTags.trim(), imageType, thumbnailByte, imageByte, date);
    }

    public static void updateImageObj(String imageTitle, String imageTags) throws InvalidImgObjException {

        if (isNullOrEmpty(imageTitle)) {
            throw new InvalidImgObjException("No Image Title provided");
        }

        if (isNullOrEmpty(imageTags)) {
            throw new InvalidImgObjException("No Image Tags provided");
        }
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

}
