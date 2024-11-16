package com.confer.imgstoremini.model;
import javax.persistence.*;

@Entity
@Table(name = "Image_Table")
public class ImageObj {

    @Id
    @GeneratedValue
    private long imageId;

    @Column(columnDefinition = "TEXT")
    private String imageTags;
    private String imageType;
    private Byte[] imageByte;

    public ImageObj() {
    }

    public ImageObj(long imageId, String imageTags, String imageType, Byte[] imageByte) {
        this.imageId = imageId;
        this.imageTags = imageTags;
        this.imageType = imageType;
        this.imageByte = imageByte;
    }

    public Byte[] getImageByte() {
        return imageByte;
    }

    public void setImageByte(Byte[] imageByte) {
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
}
