package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.model.ImageType;
import com.confer.imgstoremini.util.DataStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PaletteUIController implements PaletteViewImageContract {

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
    private AnchorPane scrollAnchorPane;

    @FXML
    private TitledPane imagePreviewTitledPane;

    @FXML
    private VBox vboxContainer;

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

        imageScrollPane.prefWidthProperty().bind(scrollAnchorPane.widthProperty());
        imageScrollPane.prefHeightProperty().bind(scrollAnchorPane.heightProperty());
    }

    public void buttonClick(ActionEvent event) {
        if (event.getSource().equals(selectImage)) {
            addSelectedImage();
        } else if (event.getSource().equals(extractPaletteBTN)) {
            processPaletteExtraction();
        } else if (event.getSource().equals(pasteBTN)) {
            pasteImageFromClip();
        } else if (event.getSource().equals(settingBTN)) {
            checkSettings();
        }
    }

    private void processPaletteExtraction() {
        if (imageDisp.getImage() != null) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PaletteStrategyChooserUI.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 380, 210);

                Stage stage = new Stage();
                stage.setTitle("Image Store - Choose Palette Strategy");
                stage.setScene(scene);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.setResizable(false);

                DataStore dataStore = DataStore.getInstance();
                Image icon = (Image) dataStore.getObject("image_icon");
                stage.getIcons().add(icon);

                PaletteChooserController controller = fxmlLoader.getController();
                controller.setViewHelperController(imageDisp, stage, this);
                stage.show();
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(e, "Palette Strategy Chooser Failed", "There was a problem loading the Palette Strategy Chooser UI");
            }
        } else {
            ErrorDialog.showErrorDialog(new Exception("No Image Set"), "Image is not set", "Please set an Image");
        }
    }

    private void checkSettings() {
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
            ErrorDialog.showErrorDialog(e, "Configuration Error", "There was a problem with the Config.json");
        }
    }

    private void addSelectedImage() {
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

    private void pasteImageFromClip() {
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if (clipboard.hasImage()) {
            Image image = clipboard.getImage();
            imageDisp.setImage(image);

            double imageWidth = imageDisp.getImage().getWidth();
            double imageHeight = imageDisp.getImage().getHeight();
            double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
            double viewportHeight = imageScrollPane.getViewportBounds().getHeight();
            double[] zoomLimits = calculateZoomLimits(imageWidth, imageHeight, viewportWidth, viewportHeight);
            minZoom = zoomLimits[0];
            maxZoom = zoomLimits[1];
        }
    }

    public double[] calculateZoomLimits(double imageWidth, double imageHeight, double viewportWidth, double viewportHeight) {
        double minZoomX = viewportWidth / imageWidth;
        double minZoomY = viewportHeight / imageHeight;
        double minZoom = Math.min(minZoomX, minZoomY);

        double maxZoom = 8.0;

        return new double[]{minZoom, maxZoom};
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
            ErrorDialog.showErrorDialog(e, "Palette Viewing Failed", "There was a problem loading the extracted Palette Image");
        }
    }
}
