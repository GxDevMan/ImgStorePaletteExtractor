package com.confer.imgstoremini.controllers;


import javafx.scene.image.ImageView;

public interface ImageContract {
    void deleteImage(int imageId);
    void viewImage(ImageView imageView);
}
