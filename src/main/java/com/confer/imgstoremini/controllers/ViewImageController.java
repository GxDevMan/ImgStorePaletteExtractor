package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageObjFactory;
import com.confer.imgstoremini.model.ImageType;
import com.confer.imgstoremini.util.ImageToByteArray;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ViewImageController {
    private ImageObj imageObj;
    private Stage stage;

    ImageContract contract;

    @FXML
    private TextArea tagsImg;

    @FXML
    private TextField messageBox;

    @FXML
    private TextField imageTitleField;

    @FXML
    private Label dateAddedLbl;

    @FXML
    private ImageView imageDisp;

    @FXML
    private ScrollPane imageScrollPane;

    @FXML
    private AnchorPane rootPane;

    private double lastMouseX;
    private double lastMouseY;

    private double scale = 1.0;
    private double deltaScale = 1.1;
    private double maxZoom;
    private double minZoom;


    @FXML
    public void initialize() {
        imageDisp.setOnScroll(this::handleZoom);

        imageDisp.setOnMousePressed(this::handleMousePressed);
        imageDisp.setOnMouseDragged(this::handleMouseDragged);

        imageScrollPane.prefWidthProperty().bind(rootPane.widthProperty());
        imageScrollPane.prefHeightProperty().bind(rootPane.heightProperty());
    }


    public void setImageView(ImageObj imageObj, ImageContract contract, Stage stage) {
        this.imageObj = imageObj;
        ImageToByteArray conversion = new ImageToByteArray();

        this.imageDisp.setImage(conversion.byteArraytoImage(imageObj.getFullImageByte()));
        tagsImg.setText(imageObj.getImageTags());
        imageTitleField.setText(imageObj.getImageTitle());

        double imageWidth = imageDisp.getImage().getWidth();
        double imageHeight = imageDisp.getImage().getHeight();
        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        double[] zoomLimits = calculateZoomLimits(imageWidth, imageHeight, viewportWidth, viewportHeight);

        minZoom = zoomLimits[0];
        maxZoom = zoomLimits[1];

        this.contract = contract;
        this.stage = stage;

        String formatThis = String.format(dateAddedLbl.getText(),imageObj.getImageDate().toString());
        dateAddedLbl.setText(formatThis);
        centerImageInScrollPane();
    }

    private void centerImageInScrollPane() {
        Image image = imageDisp.getImage();
        if (image == null) return;

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        double scaleX = viewportWidth / imageWidth;
        double scaleY = viewportHeight / imageHeight;
        double scale = Math.min(scaleX, scaleY);

        imageDisp.setFitWidth(imageWidth * scale);
        imageDisp.setFitHeight(imageHeight * scale);
        imageDisp.setPreserveRatio(true);

        double contentWidth = imageDisp.getBoundsInParent().getWidth();
        double contentHeight = imageDisp.getBoundsInParent().getHeight();

        double hValue = (contentWidth - viewportWidth) / 2 / (contentWidth - viewportWidth);
        double vValue = (contentHeight - viewportHeight) / 2 / (contentHeight - viewportHeight);

        imageScrollPane.setHvalue(Math.max(0, Math.min(1, hValue)));
        imageScrollPane.setVvalue(Math.max(0, Math.min(1, vValue)));
    }

    @FXML
    protected void buttonClick() {
        try {
            ImageObjFactory imageObjFactory = new ImageObjFactory();
            imageObjFactory.createNewImageObj(imageTitleField.getText(),
                    tagsImg.getText(),
                    ImageType.fromExtension(imageObj.getImageType()), imageDisp.getImage(),
                    imageObj.getImageDate());

            imageObj.setImageTitle(imageTitleField.getText());
            imageObj.setImageTags(tagsImg.getText());
            this.contract.updateImage(imageObj);
            stage.close();
        } catch (Exception e) {
            messageBox.setText("Error Updating, invalid information provided");
        }
    }

    private void handleZoom(ScrollEvent event) {
        if (event.getDeltaY() > 0) {
            scale *= deltaScale;
        } else {
            scale /= deltaScale;
        }

        if (scale < minZoom) {
            scale = minZoom;
        } else if (scale > maxZoom) {
            scale = maxZoom;
        }

        imageDisp.setScaleX(scale);
        imageDisp.setScaleY(scale);

        imageScrollPane.setVvalue(imageScrollPane.getVvalue());
        imageScrollPane.setHvalue(imageScrollPane.getHvalue());

        event.consume();
    }

    private void handleMousePressed(MouseEvent event) {
        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();
    }

    private void handleMouseDragged(MouseEvent event) {
        double deltaX = lastMouseX - event.getSceneX();
        double deltaY = lastMouseY - event.getSceneY();

        imageScrollPane.setHvalue(imageScrollPane.getHvalue() + deltaX / imageScrollPane.getContent().getBoundsInLocal().getWidth());
        imageScrollPane.setVvalue(imageScrollPane.getVvalue() + deltaY / imageScrollPane.getContent().getBoundsInLocal().getHeight());

        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();
    }

    public static double[] calculateZoomLimits(double imageWidth, double imageHeight, double viewportWidth, double viewportHeight) {
        double minZoomX = viewportWidth / imageWidth;
        double minZoomY = viewportHeight / imageHeight;
        double minZoom = Math.min(minZoomX, minZoomY);

        double maxZoom = 3.0;

        return new double[]{minZoom, maxZoom};
    }
}
