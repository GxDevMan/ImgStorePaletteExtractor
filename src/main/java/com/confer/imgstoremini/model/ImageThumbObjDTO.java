package com.confer.imgstoremini.model;
import java.util.Date;

public class ImageThumbObjDTO {

    private long imageId;
    private String imageTitle;
    private String imageTags;
    private String imageType;
    private byte[] thumbnailImageByte;
    private Date imageDate;

    public ImageThumbObjDTO() {
    }

    public ImageThumbObjDTO(long imageId, String imageTitle, String imageTags, String imageType, byte[] thumbnailImageByte, Date imageDate) {
        this.imageId = imageId;
        this.imageTitle = imageTitle;
        this.imageTags = imageTags;
        this.imageType = imageType;
        this.thumbnailImageByte = thumbnailImageByte;
        this.imageDate = imageDate;
    }

    public long getImageId() {
        return imageId;
    }

    public void setImageId(long imageId) {
        this.imageId = imageId;
    }

    public String getImageTitle() {
        return imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }

    public String getImageTags() {
        return imageTags;
    }

    public void setImageTags(String imageTags) {
        this.imageTags = imageTags;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public byte[] getThumbnailImageByte() {
        return thumbnailImageByte;
    }

    public void setThumbnailImageByte(byte[] thumbnailImageByte) {
        this.thumbnailImageByte = thumbnailImageByte;
    }

    public Date getImageDate() {
        return imageDate;
    }

    public void setImageDate(Date imageDate) {
        this.imageDate = imageDate;
    }
}
