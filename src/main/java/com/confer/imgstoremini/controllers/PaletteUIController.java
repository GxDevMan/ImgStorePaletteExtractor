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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PaletteUIController implements PaletteViewImageContract {
    Stage stage;
    private PureViewUIController pureViewUIController;

    @FXML
    private Button pasteBTN;

    @FXML
    private Button selectImage;

    @FXML
    private Button extractPaletteBTN;

    @FXML
    private Button settingBTN;

    @FXML
    private StackPane viewImageStackPane;

    public void buttonClick(ActionEvent event) {
        if (event.getSource().equals(selectImage)) {
            addSelectedImage();
        } else if (event.getSource().equals(extractPaletteBTN)) {
            processPaletteExtraction(pureViewUIController.getDispImageView());
        } else if (event.getSource().equals(pasteBTN)) {
            pasteImageFromClip();
        } else if (event.getSource().equals(settingBTN)) {
            checkSettings();
        }
    }

    public void setPaletteUIController(Stage stage) {
        this.stage = stage;
        try {
            FXMLLoader loader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PureViewUI.fxml"));
            BorderPane previewComponent = loader.load();
            PureViewUIController controller = loader.getController();
            this.pureViewUIController = controller;

            controller.setPureViewUI(null, stage);
            viewImageStackPane.getChildren().addAll(previewComponent);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem Pure View UI");
        }

    }

    private void setPaletteUIController(Stage stage, Image image) {
        this.stage = stage;
        viewImageStackPane.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PureViewUI.fxml"));
            BorderPane previewComponent = loader.load();
            PureViewUIController controller = loader.getController();
            this.pureViewUIController = controller;

            controller.setPureViewUI(image, stage);
            viewImageStackPane.getChildren().addAll(previewComponent);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem Pure View UI");
        }

    }

    private void processPaletteExtraction(ImageView imageDisp) {
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
            setPaletteUIController(stage, selectedImage);
        }
    }

    private void pasteImageFromClip() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasImage()) {
            Image image = clipboard.getImage();
            setPaletteUIController(stage, image);
        }
    }

    @Override
    public void displayPalette(Image paletteImage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PureViewUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 200, 200);

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
