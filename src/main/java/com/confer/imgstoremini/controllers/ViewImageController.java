package com.confer.imgstoremini.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;

public class ViewImageController {

    @FXML
    private TextArea tagsImg;

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
    public void initialize(){
        imageDisp.setOnScroll(this::handleZoom);

        imageDisp.setOnMousePressed(this::handleMousePressed);
        imageDisp.setOnMouseDragged(this::handleMouseDragged);

        imageScrollPane.prefWidthProperty().bind(rootPane.widthProperty());
        imageScrollPane.prefHeightProperty().bind(rootPane.heightProperty());
    }


    public void setImageView(Image image, String tags){
        this.imageDisp.setImage(image);
        tagsImg.setText(tags);

        double imageWidth = imageDisp.getImage().getWidth();
        double imageHeight = imageDisp.getImage().getHeight();
        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        double[] zoomLimits = calculateZoomLimits(imageWidth, imageHeight, viewportWidth, viewportHeight);

        minZoom = zoomLimits[0];
        maxZoom = zoomLimits[1];
    }

    @FXML
    protected void buttonClick(){
        System.out.println("UPDATE WAS CLICKED");
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
