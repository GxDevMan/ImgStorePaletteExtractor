package com.confer.imgstoremini.model;

public enum ImageType {
    PNG(".png"),
    JPEG(".jpeg"),
    JPG(".jpg");

    private final String extension;

    ImageType(String extension) {
        this.extension = extension;
    }

    public static ImageType fromExtension(String extension) {
        for (ImageType type : ImageType.values()) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for extension: " + extension);
    }

    public String getExtension() {
        return extension;
    }
}
