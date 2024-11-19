package com.confer.imgstoremini.model;

import com.confer.imgstoremini.exceptions.InvalidImgObjException;
import com.confer.imgstoremini.util.ImageToByteArray;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.sql.Date;

public class ImageObjFactory {
    public ImageObj createNewImageObj(String imageTitle, String imageTags, ImageType imageType, Image image, Date imageDate) throws InvalidImgObjException {
        ImageToByteArray conversion = new ImageToByteArray();
        byte[] imageByte = conversion.convertImageToByteArray(image, imageType);

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
            case JPG, JPEG -> resizeImgContext.setStrategy(new JpegResizeStrategy());
            case PNG -> resizeImgContext.setStrategy(new PngResizeStrategy());
            default -> throw new InvalidImgObjException("Invalid Image Type");
        }

        if (imageByte == null || imageByte.length == 0) {
            throw new InvalidImgObjException("Image byte array is null or empty");
        }

        if (imageDate == null) {
            throw new InvalidImgObjException("Image date is null");
        }

        BufferedImage resizedImagedBfr = resizeImgContext.executeResize(SwingFXUtils.fromFXImage(image, null), 500, 500);
        Image resizedImage = SwingFXUtils.toFXImage(resizedImagedBfr, null);
        byte[] thumbnailByte = conversion.convertImageToByteArray(resizedImage, imageType);

        if (thumbnailByte == null || thumbnailByte.length == 0)
            throw new InvalidImgObjException("Resizing failed");


        return new ImageObj(imageTitle.trim(), imageTags.trim(), imageType, thumbnailByte, imageByte, imageDate);
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

}
