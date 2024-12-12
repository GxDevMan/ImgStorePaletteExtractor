package com.confer.imgstoremini.controllers.components;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.controllers.interfaces.ImageContract;
import com.confer.imgstoremini.controllers.interfaces.PaletteViewImageContract;
import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageObjFactory;
import com.confer.imgstoremini.util.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.awt.image.BufferedImage;

public class ViewImageController implements PaletteViewImageContract {
    private ImageObj imageObj;
    private Stage stage;
    private PureViewUIController pureViewUIController;

    ImageContract contract;

    @FXML
    private Button updateBTN;

    @FXML
    private Button copyImageBTN;

    @FXML
    private Button saveImageBTN;

    @FXML
    private Button closeBTN;

    @FXML
    private Button extractPaletteBTN;

    @FXML
    private TextArea tagsImg;

    @FXML
    private TextField messageBox;

    @FXML
    private TextField imageTitleField;

    @FXML
    private StackPane viewImageStackPane;

    @FXML
    private Label dateAddedLbl;

    public void setImageView(ImageObj imageObj, ImageContract contract, Stage stage) {
        this.imageObj = imageObj;
        this.contract = contract;
        this.stage = stage;
        Image image = ImageConversion.byteArraytoImage(imageObj.getFullImageByte());

        imageTitleField.setText(imageObj.getImageTitle());
        tagsImg.setText(imageObj.getImageTags());
        String time = TimeFormatter.formatNumTime(imageObj.getImageDate());
        String date = TimeFormatter.getFormattedDate(imageObj.getImageDate());
        dateAddedLbl.setText(String.format(dateAddedLbl.getText(), date, time));

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

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(updateBTN)) {
            updateImage();
        } else if (event.getSource().equals(copyImageBTN)) {
            copyImageToClipBoard(this.pureViewUIController.getDispImageView().getImage());
        } else if (event.getSource().equals(saveImageBTN)) {
            pureViewUIController.saveImageToFile(pureViewUIController.getDispImageView().getImage(), imageObj.getImageTitle());
        } else if (event.getSource().equals(closeBTN)) {
            stage.close();
        } else if (event.getSource().equals(extractPaletteBTN)) {
            paletteMenuExtraction();
        }
    }

    private void paletteMenuExtraction() {
        try {
            ComponentFactory.showPaletteMenuExtraction(this.pureViewUIController.getDispImageView().getImage(),this);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Palette Strategy Chooser Failed", "There was a problem loading the Palette Strategy Chooser UI");
        }
    }

    private void copyImageToClipBoard(Image image) {
        if (image == null) {
            return;
        }
        BufferedImage bufferedImage = ImageConversion.convertImageToBufferedImage(image);

        if (bufferedImage != null) {
            BufferedImage compatibleImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            compatibleImage.createGraphics().drawImage(bufferedImage, 0, 0, null);
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putImage(SwingFXUtils.toFXImage(compatibleImage, null));
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        }
    }

    private void updateImage() {
        try {
            ImageObjFactory.updateImageObj(imageTitleField.getText(),
                    tagsImg.getText());
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Fields Error", "Required Fields are Missing");
            messageBox.setText("Error Updating, invalid information provided");
            return;
        }

        Thread taskThread = new Thread(() -> {
            imageObj.setImageTags(tagsImg.getText());
            imageObj.setImageTitle(imageTitleField.getText());
            this.contract.updateImage(imageObj);
            Platform.runLater(() -> {
                stage.close();
            });

        });
        taskThread.setDaemon(true);
        taskThread.start();
    }

    public void displayPalette(Image paletteImage) {
        try {
            ComponentFactory.displayPureView(paletteImage);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Palette Viewing Failed", "There was a problem loading the extracted Palette Image");
        }
    }
}
