package com.confer.imgstoremini.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class PreviewImageComponentUIController {

    private ImageContract contract;
    private int imageId;

    @FXML
    private ImageView imagePlace;

    @FXML
    private Button deleteButton;

    @FXML
    private Button viewImgBtn;

    public void setComponent(ImageContract contract,
                                             Image image, int imageId) {
        imagePlace.setImage(image);
        this.imageId = imageId;
        this.contract = contract;
    }

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(viewImgBtn)) {
            contract.viewImage(this.imagePlace);
        } else if (event.getSource().equals(deleteButton)) {
            contract.deleteImage(imageId);
        }
    }
}
