package com.confer.imgstoremini.model;
import java.sql.Date;

public class ImageObjFactory {
    public ImageObj createNewImageObj(String imageTitle, String imageTags, ImageType imageType, byte[] imageByte, Date imageDate) {
        return new ImageObj(imageTitle,imageTags,imageType,imageByte,imageDate);
    }
}
