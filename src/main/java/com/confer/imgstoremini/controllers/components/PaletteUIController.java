package com.confer.imgstoremini.controllers.components;

import com.confer.imgstoremini.controllers.interfaces.PaletteViewImageContract;
import com.confer.imgstoremini.model.ImageType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PaletteUIController implements PaletteViewImageContract {
    Stage stage;
    private PureViewUIController pureViewUIController;
    private boolean isSet;

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

    public void setPaletteUIController(Stage stage, boolean isSet) {
        this.stage = stage;
        this.isSet = isSet;
        try {
            Pair<BorderPane, PureViewUIController> pairResult = ComponentFactory.pureViewAsComponent();
            BorderPane previewComponent = pairResult.getKey();
            this.pureViewUIController = pairResult.getValue();
            this.pureViewUIController.setPureViewUI(null, stage);
            viewImageStackPane.getChildren().addAll(previewComponent);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem Pure View UI");
        }
    }

    private void setPaletteUIController(Stage stage, Image image, boolean isSet) {
        this.isSet = isSet;
        this.stage = stage;
        viewImageStackPane.getChildren().clear();
        try {
            Pair<BorderPane, PureViewUIController> pairResult = ComponentFactory.pureViewAsComponent();
            BorderPane previewComponent = pairResult.getKey();
            this.pureViewUIController = pairResult.getValue();
            this.pureViewUIController.setPureViewUI(image, stage);
            viewImageStackPane.getChildren().addAll(previewComponent);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem Pure View UI");
        }

    }

    private void processPaletteExtraction(ImageView imageDisp) {
        if (imageDisp.getImage() != null) {
            try {
                ComponentFactory.showPaletteMenuExtraction(imageDisp.getImage(),this);
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(e, "Palette Strategy Chooser Failed", "There was a problem loading the Palette Strategy Chooser UI");
            }
        } else {
            ErrorDialog.showErrorDialog(new Exception("No Image Set"), "Image is not set", "Please set an Image");
        }
    }

    private void checkSettings() {
        try {
            ComponentFactory.checkSettingsUI(isSet);
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
            setPaletteUIController(stage, selectedImage, isSet);
        }
    }

    private void pasteImageFromClip() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasImage()) {
            Image image = clipboard.getImage();
            setPaletteUIController(stage, image, isSet);
        }
    }

    @Override
    public void displayPalette(Image paletteImage) {
        try {
            ComponentFactory.displayPureView(paletteImage);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Palette Viewing Failed", "There was a problem loading the extracted Palette Image");
        }
    }
}
