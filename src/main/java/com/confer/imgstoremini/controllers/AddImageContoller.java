package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.exceptions.InvalidImgObjException;
import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageObjFactory;
import com.confer.imgstoremini.model.ImageType;
import com.confer.imgstoremini.util.ImageConversion;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AddImageContoller {

    private ImageContract contract;
    private Stage addStage;

    @FXML
    private TextArea tagsImg;

    @FXML
    private TextField imageTitleTxtArea;

    @FXML
    private TextField imageTypeArea;

    @FXML
    private ImageView imageDisp;

    @FXML
    private Button addBtn;

    @FXML
    private Button selectImage;

    @FXML
    private Button pasteBTN;

    @FXML
    private ScrollPane imageScrollPane;

    @FXML
    private AnchorPane rootPane;

    private double scale = 1.0;
    private double deltaScale = 1.1;
    private double maxZoom;
    private double minZoom;

    private double lastMouseX;
    private double lastMouseY;

    @FXML
    public void initialize() {
        imageDisp.setOnScroll(this::handleZoom);

        imageDisp.setOnMousePressed(this::handleMousePressed);
        imageDisp.setOnMouseDragged(this::handleMouseDragged);

        imageScrollPane.prefWidthProperty().bind(rootPane.widthProperty());
        imageScrollPane.prefHeightProperty().bind(rootPane.heightProperty());
    }


    public void setContract(ImageContract contract, Stage stage) {
        this.contract = contract;
        this.addStage = stage;
    }

    public void buttonClick(ActionEvent event) {
        if (event.getSource().equals(selectImage)) {
            addSelectedImage();
        } else if (event.getSource().equals(addBtn)) {
            addImageDB(event);
        } else if (event.getSource().equals(pasteBTN)){
            pasteImageFromClip();
        }
    }

    private void pasteImageFromClip(){
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if(clipboard.hasImage()){
            Image image = clipboard.getImage();
            imageDisp.setImage(image);

            double imageWidth = imageDisp.getImage().getWidth();
            double imageHeight = imageDisp.getImage().getHeight();
            double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
            double viewportHeight = imageScrollPane.getViewportBounds().getHeight();
            double[] zoomLimits = calculateZoomLimits(imageWidth, imageHeight, viewportWidth, viewportHeight);
            minZoom = zoomLimits[0];
            maxZoom = zoomLimits[1];

            centerImageInScrollPane();
        }
    }

    private void addSelectedImage(){
        List<String> imgExtensions = new ArrayList<>();
        for (ImageType imageType : ImageType.values()) {
            imgExtensions.add(imageType.getExtension());
        }

        FileChooser fileChooser = new FileChooser();

        for (String imgExtension : imgExtensions) {
            FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Image Files", String.format("*%s", imgExtension));
            fileChooser.getExtensionFilters().add(imageFilter);
        }

        String currentDir = System.getProperty("user.dir");
        fileChooser.setInitialDirectory(new File(currentDir));

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            String fileName = selectedFile.getName();
            String fileExtension = "";

            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex > 0) {
                fileExtension = fileName.substring(dotIndex + 1);
            }


            this.imageTypeArea.setText(fileExtension);
            Image selectedImage = new Image(selectedFile.toURI().toString());
            imageDisp.setImage(selectedImage);

            double imageWidth = imageDisp.getImage().getWidth();
            double imageHeight = imageDisp.getImage().getHeight();
            double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
            double viewportHeight = imageScrollPane.getViewportBounds().getHeight();
            double[] zoomLimits = calculateZoomLimits(imageWidth, imageHeight, viewportWidth, viewportHeight);
            minZoom = zoomLimits[0];
            maxZoom = zoomLimits[1];

            centerImageInScrollPane();
        }
    }

    private void addImageDB(ActionEvent event) {
        ImageConversion imgConverter = new ImageConversion();
        ImageType imgType;
        try {
            imgType = ImageType.valueOf(imageTypeArea.getText().toUpperCase());
        } catch (IllegalArgumentException e) {
            imgType = ImageType.PNG;
        }

        try {

            ImageObjFactory factory = new ImageObjFactory();
            ImageObj newEntry = factory.createNewImageObj(imageTitleTxtArea.getText(), tagsImg.getText(), imgType, imageDisp.getImage());
            contract.addImage(newEntry);
            addStage.close();
        } catch (InvalidImgObjException e) {
            this.imageTypeArea.setText(e.getMessage());
        }
    }

    private void handleZoom(ScrollEvent event) {
        if (imageDisp.getImage() == null) {
            return;
        }

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
        if (imageDisp.getImage() == null) {
            return;
        }

        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (imageDisp.getImage() == null) {
            return;
        }

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

}
