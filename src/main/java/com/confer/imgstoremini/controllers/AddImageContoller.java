package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageObjFactory;
import com.confer.imgstoremini.model.ImageType;
import com.confer.imgstoremini.util.ImageToByteArray;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class AddImageContoller {

    private AddImageContract contract;
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
    private ScrollPane imageScrollPane;

    @FXML
    public void initialize() {
        imageDisp.setOnDragOver(event -> {
            if (event.getGestureSource() != imageDisp && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        imageDisp.setOnDragDropped(event -> {
            var dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().get(0);
                try {
                    Image image = new Image(file.toURI().toString());
                    imageDisp.setImage(image);
                    success = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }


    public void setContract(AddImageContract contract, Stage stage) {
        this.contract = contract;
        this.addStage = stage;
    }

    public void buttonClick(ActionEvent event) {
        if (event.getSource().equals(selectImage)) {
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
            }
        } else if (event.getSource().equals(addBtn)) {
            ImageToByteArray imgConverter = new ImageToByteArray();
            ImageType imgType;
            try {
                imgType = ImageType.valueOf(imageTypeArea.getText().toUpperCase());
            } catch (IllegalArgumentException e) {
                imgType = ImageType.PNG;
            }

            byte[] imageBytes = imgConverter.convertImageToByteArray(imageDisp.getImage(), ImageType.PNG);
            LocalDate localDate = LocalDate.now();
            java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);

            ImageObjFactory factory = new ImageObjFactory();
            ImageObj newEntry = factory.createNewImageObj(imageTitleTxtArea.getText(), tagsImg.getText(), imgType, imageBytes, sqlDate);
            contract.addImage(newEntry);
            addStage.close();
        }
    }
}
