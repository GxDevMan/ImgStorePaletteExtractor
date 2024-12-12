package com.confer.imgstoremini.controllers.components;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.controllers.interfaces.ImageContract;
import com.confer.imgstoremini.exceptions.InvalidImgObjException;
import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageObjFactory;
import com.confer.imgstoremini.model.ImageType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AddImageContoller {
    PureViewUIController pureViewUIController;
    private ImageContract contract;
    private Stage addStage;

    @FXML
    private TextArea tagsImg;

    @FXML
    private TextField imageTitleTxtArea;

    @FXML
    private TextField imageTypeArea;

    @FXML
    private Button addBtn;

    @FXML
    private Button selectImage;

    @FXML
    private Button pasteBTN;

    @FXML
    private StackPane viewImageStackPane;

    private void setAddImageController(Stage stage, Image image) {
        this.addStage = stage;
        viewImageStackPane.getChildren().clear();
        try {
            Pair<BorderPane, PureViewUIController> pairReturn = ComponentFactory.pureViewAsComponent();
            this.pureViewUIController = pairReturn.getValue();
            BorderPane previewComponent = pairReturn.getKey();
            this.pureViewUIController.setPureViewUI(image,stage);
            viewImageStackPane.getChildren().addAll(previewComponent);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem Pure View UI");
        }

    }

    public void setContract(ImageContract contract, Stage stage) {
        this.contract = contract;
        this.addStage = stage;
    }

    public void buttonClick(ActionEvent event) {
        if (event.getSource().equals(selectImage)) {
            addSelectedImage();
        } else if (event.getSource().equals(addBtn)) {
            addImageDB();
        } else if (event.getSource().equals(pasteBTN)) {
            pasteImageFromClip();
        }
    }

    private void pasteImageFromClip() {
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if (clipboard.hasImage()) {
            Image image = clipboard.getImage();
            setAddImageController(this.addStage, image);
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
            String fileName = selectedFile.getName();
            String fileExtension = "";

            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex > 0) {
                fileExtension = fileName.substring(dotIndex + 1);
            }


            this.imageTypeArea.setText(fileExtension);
            Image selectedImage = new Image(selectedFile.toURI().toString());
            setAddImageController(this.addStage, selectedImage);
        }
    }

    private void addImageDB() {
        ImageType imgType;
        try {
            imgType = ImageType.valueOf(imageTypeArea.getText().toUpperCase());
        } catch (IllegalArgumentException e) {
            imgType = ImageType.PNG;
        }
        final ImageType finalImgType = imgType;
            Thread taskThread = new Thread(() -> {
                try {
                    ImageObj newEntry = ImageObjFactory.createNewImageObj(imageTitleTxtArea.getText(), tagsImg.getText(), finalImgType, pureViewUIController.getDispImageView().getImage());
                    contract.addImage(newEntry);
                    Platform.runLater(() -> {
                                addStage.close();
                            }
                    );
                } catch (InvalidImgObjException e){
                    ErrorDialog.showErrorDialog(e, "Invalid Image", "Image Requirements not satisfied");
                }
            });
            taskThread.setDaemon(true);
            taskThread.start();
    }
}
