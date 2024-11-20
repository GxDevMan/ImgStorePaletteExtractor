package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.model.ImageThumbObjDTO;
import com.confer.imgstoremini.util.ImageConversion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class PreviewImageComponentUIController {

    private ImageContract contract;
    private ImageThumbObjDTO imageObj;

    @FXML
    private Label dateAddedLbl;

    @FXML
    private ImageView imagePlace;

    @FXML
    private Button deleteButton;

    @FXML
    private Button viewImgBtn;

    @FXML
    private Button pureViewBtn;

    @FXML
    private Label imageTitleLbl;

    public void setComponent(ImageContract contract, ImageThumbObjDTO imageObj) {
        ImageConversion conversion = new ImageConversion();
        Image image = conversion.byteArraytoImage(imageObj.getThumbnailImageByte());
        imagePlace.setImage(image);
        this.imageObj = imageObj;
        this.contract = contract;
        imageTitleLbl.setText(imageObj.getImageTitle());
        String formatThis = String.format(dateAddedLbl.getText(),imageObj.getImageDate().toString());
        dateAddedLbl.setText(formatThis);
    }

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(viewImgBtn)) {
            contract.viewImage(imageObj);
        } else if (event.getSource().equals(deleteButton)) {
            contract.deleteImage(imageObj);
        } else if (event.getSource().equals(pureViewBtn)){
            contract.pureViewImage(imageObj);
        }
    }
}
