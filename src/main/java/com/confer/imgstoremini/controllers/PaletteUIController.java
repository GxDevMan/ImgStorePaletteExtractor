package com.confer.imgstoremini.controllers;
import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.model.ImageType;
import com.confer.imgstoremini.util.DataStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PaletteUIController implements ViewImageContract {

    @FXML
    private Button pasteBTN;

    @FXML
    private Button selectImage;

    @FXML
    private Button extractPaletteBTN;

    @FXML
    private Button settingBTN;

    @FXML
    private ScrollPane imageScrollPane;

    @FXML
    private ImageView imageDisp;

    @FXML
    private AnchorPane rootPane;

    private double scale = 1.0;
    private double deltaScale = 1.1;
    private double maxZoom;
    private double minZoom;

    private double lastMouseX;
    private double lastMouseY;

    @FXML
    public void initialize(){
        imageDisp.setOnScroll(this::handleZoom);

        imageDisp.setOnMousePressed(this::handleMousePressed);
        imageDisp.setOnMouseDragged(this::handleMouseDragged);

        imageScrollPane.prefWidthProperty().bind(rootPane.widthProperty());
        imageScrollPane.prefHeightProperty().bind(rootPane.heightProperty());
    }

    public void buttonClick(ActionEvent event) {
        if(event.getSource().equals(selectImage)){
            addSelectedImage();
        } else if(event.getSource().equals(extractPaletteBTN)){
            processPaletteExtraction();
        } else if(event.getSource().equals(pasteBTN)){
            pasteImageFromClip();
        } else if(event.getSource().equals(settingBTN)){
            checkSettings();
        }
    }

    private void processPaletteExtraction(){
        if(!(imageDisp.getImage() == null)){
            ViewImageHelper viewImageHelper = new ViewImageHelper(this);
            viewImageHelper.showStrategySelectionDialog(imageDisp);
        } else{
            ErrorDialog errorDialog = new ErrorDialog();
            errorDialog.errorDialog(new Exception("Image is Null"),"Image is Null","You have not set an image");
        }
    }

    private void checkSettings(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("SettingsConfigUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 550, 250);

            Stage stage = new Stage();
            stage.setTitle("Image Store");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            SettingsConfigUIController controller = fxmlLoader.getController();
            controller.setConfigurationSetting(stage, true);

            stage.show();
        } catch (Exception e) {
            ErrorDialog errorDialog = new ErrorDialog();
            errorDialog.errorDialog(e,"Configuration Error","There was a problem with the Config.json");
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

    public double[] calculateZoomLimits(double imageWidth, double imageHeight, double viewportWidth, double viewportHeight) {
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

    @Override
    public void displayPalette(Image paletteImage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PureViewUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 200);

            Stage stage = new Stage();
            stage.setTitle("Image Store");
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            PureViewUIController controller = fxmlLoader.getController();
            controller.setPureViewUI(paletteImage, stage);

            stage.show();
        } catch (Exception e) {
            ErrorDialog dialog = new ErrorDialog();
            dialog.errorDialog(e, "Palette Viewing Failed", "There was a problem loading the extracted Palette Image");
        }
    }
}
