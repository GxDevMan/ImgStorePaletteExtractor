package com.confer.imgstoremini.model;

import javax.persistence.*;
import java.sql.Date;

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

    private byte[] imageByte;

    private Date imageDate;

    public ImageObj() {
    }

    public ImageObj(long imageId, String imageTags, ImageType imageType, byte[] imageByte, Date imageDate) {
        this.imageId = imageId;
        this.imageTags = imageTags;
        this.imageType = imageType.getExtension();
        this.imageByte = imageByte;
        this.imageDate = imageDate;
    }

    public ImageObj(String imageTitle, String imageTags, ImageType imageType, byte[] imageByte, Date imageDate) {
        this.imageTitle = imageTitle;
        this.imageTags = imageTags;
        this.imageType = imageType.getExtension();
        this.imageByte = imageByte;
        this.imageDate = imageDate;
    }

    public byte[] getImageByte() {
        return imageByte;
    }

    public void setImageByte(byte[] imageByte) {
        this.imageByte = imageByte;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(ImageType type) {
        this.imageType = type.name();
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

    public Date getImageDate() {
        return imageDate;
    }

    public void setImageDate(Date imageDate) {
        this.imageDate = imageDate;
    }

    public String getImageTitle() {
        return imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }
}
