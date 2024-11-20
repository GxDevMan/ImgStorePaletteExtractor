package com.confer.imgstoremini.model;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "Image_Table")
public class ImageObj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long imageId;

    private String imageTitle;

    @Column(columnDefinition = "TEXT")
    private String imageTags;

    private String imageType;

    private byte[] thumbnailImageByte;

    private byte[] fullImageByte;

    private Timestamp imageDate;

    public ImageObj() {
    }

    public ImageObj(long imageId, String imageTitle, String imageTags, ImageType imageType, byte[] thumbnailImageByte, byte[] fullImageByte, Timestamp imageDate) {
        this.imageId = imageId;
        this.imageTitle = imageTitle;
        this.imageTags = imageTags;
        this.imageType = imageType.getExtension();
        this.thumbnailImageByte = thumbnailImageByte;
        this.fullImageByte = fullImageByte;
        this.imageDate = imageDate;
    }

    public ImageObj(String imageTitle, String imageTags, ImageType imageType, byte[] thumbnailImageByte, byte[] fullImageByte, Timestamp imageDate) {
        this.imageTitle = imageTitle;
        this.imageTags = imageTags;
        this.imageType = imageType.getExtension();
        this.thumbnailImageByte = thumbnailImageByte;
        this.fullImageByte = fullImageByte;
        this.imageDate = imageDate;
    }

    public byte[] getFullImageByte() {
        return fullImageByte;
    }

    public void setFullImageByte(byte[] fullImageByte) {
        this.fullImageByte = fullImageByte;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(ImageType type) {
        this.imageType = type.getExtension();
    }

    public String getImageTags() {
        return imageTags;
    }

    public void setImageTags(String imageTags) {
        this.imageTags = imageTags;
    }

    public long getImageId() {
        return imageId;
    }

    public void setImageId(long imageId) {
        this.imageId = imageId;
    }

    public Timestamp getImageDate() {
        return imageDate;
    }

    public void setImageDate(Timestamp imageDate) {
        this.imageDate = imageDate;
    }

    public String getImageTitle() {
        return imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }

    public byte[] getThumbnailImageByte() {
        return thumbnailImageByte;
    }

    public void setThumbnailImageByte(byte[] thumbnailImageByte) {
        this.thumbnailImageByte = thumbnailImageByte;
    }
}
