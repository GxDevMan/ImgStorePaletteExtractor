package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.util.ImageConversion;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;

import java.awt.image.BufferedImage;


public class PureViewUIController {

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private ImageView dispImageView;

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
        dispImageView.setOnScroll(this::handleZoom);

        dispImageView.setOnMousePressed(this::handleMousePressed);
        dispImageView.setOnMouseDragged(this::handleMouseDragged);

        scrollPane.prefWidthProperty().bind(rootPane.widthProperty());
        scrollPane.prefHeightProperty().bind(rootPane.heightProperty());
    }

    public void setPureViewUI(Image image){
        dispImageView.setImage(image);

        double imageWidth = dispImageView.getImage().getWidth();
        double imageHeight = dispImageView.getImage().getHeight();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        double[] zoomLimits = calculateZoomLimits(imageWidth, imageHeight, viewportWidth, viewportHeight);

        minZoom = zoomLimits[0];
        maxZoom = zoomLimits[1];

        centerImageInScrollPane();

        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyImageItem = new MenuItem("Copy Image");
        copyImageItem.setOnAction(e -> {
            copyImageToClipBoard(dispImageView.getImage());
        });
        contextMenu.getItems().addAll(copyImageItem);

        dispImageView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(dispImageView, event.getScreenX(), event.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    private void centerImageInScrollPane() {
        Image image = dispImageView.getImage();
        if (image == null) return;

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        double scaleX = viewportWidth / imageWidth;
        double scaleY = viewportHeight / imageHeight;
        double scale = Math.min(scaleX, scaleY);

        dispImageView.setFitWidth(imageWidth * scale);
        dispImageView.setFitHeight(imageHeight * scale);
        dispImageView.setPreserveRatio(true);

        double contentWidth = dispImageView.getBoundsInParent().getWidth();
        double contentHeight = dispImageView.getBoundsInParent().getHeight();

        double hValue = (contentWidth - viewportWidth) / 2 / (contentWidth - viewportWidth);
        double vValue = (contentHeight - viewportHeight) / 2 / (contentHeight - viewportHeight);

        scrollPane.setHvalue(Math.max(0, Math.min(1, hValue)));
        scrollPane.setVvalue(Math.max(0, Math.min(1, vValue)));
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

        dispImageView.setScaleX(scale);
        dispImageView.setScaleY(scale);

        scrollPane.setVvalue(scrollPane.getVvalue());
        scrollPane.setHvalue(scrollPane.getHvalue());
        event.consume();
    }

    private void handleMouseDragged(MouseEvent event) {
        double deltaX = lastMouseX - event.getSceneX();
        double deltaY = lastMouseY - event.getSceneY();

        scrollPane.setHvalue(scrollPane.getHvalue() + deltaX / scrollPane.getContent().getBoundsInLocal().getWidth());
        scrollPane.setVvalue(scrollPane.getVvalue() + deltaY / scrollPane.getContent().getBoundsInLocal().getHeight());

        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();
    }

    private void handleMousePressed(MouseEvent event) {
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

    private void copyImageToClipBoard(Image image) {
        if (image == null) {
            return;
        }
        ImageConversion imageConversion = new ImageConversion();
        BufferedImage bufferedImage = imageConversion.convertImageToBufferedImage(image);

        if (bufferedImage != null) {
            BufferedImage compatibleImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            compatibleImage.createGraphics().drawImage(bufferedImage, 0, 0, null);
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putImage(SwingFXUtils.toFXImage(compatibleImage, null));
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        }
    }

}
