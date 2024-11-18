package com.confer.imgstoremini.model;

public enum ImageType {
    PNG(".png"),
    JPEG(".jpeg"),
    JPG(".jpg");

    private final String extension;

    ImageType(String extension) {
        this.extension = extension;
    }

    // Getter for the file extension
    public String getExtension() {
        return extension;
    }
}
