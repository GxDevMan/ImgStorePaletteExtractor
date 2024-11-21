package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageObjFactory;
import com.confer.imgstoremini.model.ImageType;
import com.confer.imgstoremini.util.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;


public class ViewImageController {
    private ImageObj imageObj;
    private Stage stage;

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
    private Label dateAddedLbl;

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
    public void initialize() {
        imageDisp.setOnScroll(this::handleZoom);

        imageDisp.setOnMousePressed(this::handleMousePressed);
        imageDisp.setOnMouseDragged(this::handleMouseDragged);

        imageScrollPane.prefWidthProperty().bind(rootPane.widthProperty());
        imageScrollPane.prefHeightProperty().bind(rootPane.heightProperty());
    }

    public void setImageView(ImageObj imageObj, ImageContract contract, Stage stage) {
        this.imageObj = imageObj;
        ImageConversion conversion = new ImageConversion();

        this.imageDisp.setImage(conversion.byteArraytoImage(imageObj.getFullImageByte()));
        tagsImg.setText(imageObj.getImageTags());
        imageTitleField.setText(imageObj.getImageTitle());

        double imageWidth = imageDisp.getImage().getWidth();
        double imageHeight = imageDisp.getImage().getHeight();
        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        double[] zoomLimits = calculateZoomLimits(imageWidth, imageHeight, viewportWidth, viewportHeight);

        minZoom = zoomLimits[0];
        maxZoom = zoomLimits[1];

        this.contract = contract;
        this.stage = stage;

        TimeFormatter timeFormatter = new TimeFormatter();
        String time = timeFormatter.formatNumTime(imageObj.getImageDate());
        String date = timeFormatter.getFormattedDate(imageObj.getImageDate());

        String formatThis = String.format(dateAddedLbl.getText(), date, time);
        dateAddedLbl.setText(formatThis);
        centerImageInScrollPane();
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

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(updateBTN)) {
            updateImage();
        } else if (event.getSource().equals(copyImageBTN)) {
            copyImageToClipBoard(imageDisp.getImage());
        } else if (event.getSource().equals(saveImageBTN)) {
            saveImageToFile(imageDisp.getImage(), this.stage);
        } else if (event.getSource().equals(closeBTN)){
            stage.close();
        } else if (event.getSource().equals(extractPaletteBTN)){
            showStrategySelectionDialog();
        }
    }

    private void showStrategySelectionDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Select Palette Extraction Strategy");
        alert.setHeaderText("Choose a palette extraction strategy and color count:");

        Spinner<Integer> colorCountSpinner = new Spinner<>(1, 50, 10);
        colorCountSpinner.setEditable(true);
        colorCountSpinner.setMaxWidth(60);

        VBox vbox = new VBox();
        vbox.getChildren().addAll(new Label("Select Number of Colors:"), colorCountSpinner);

        ButtonType HistogramButton = new ButtonType("Histogram");
        ButtonType kMeansButton = new ButtonType("K-Means");
        ButtonType regionBasedButton = new ButtonType("Region-Based");
        ButtonType MeanShiftButton = new ButtonType("Mean Shift");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(HistogramButton,kMeansButton, regionBasedButton, MeanShiftButton, cancelButton);
        alert.getDialogPane().setContent(vbox);

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        DataStore dataStore = DataStore.getInstance();
        Image icon = (Image) dataStore.getObject("image_icon");
        stage.getIcons().add(icon);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(ImageStoreMiniApplication.class.getResource("styles/dark-theme.css").toExternalForm());

        alert.showAndWait().ifPresent(response -> {
            int colorCount = colorCountSpinner.getValue();

            if (response == kMeansButton) {
                computePalette(new KMeansPaletteStrategy(), colorCount);
            } else if (response == regionBasedButton) {
                computePalette(new RegionBasedPaletteStrategy(), colorCount);
            } else if(response == HistogramButton){
                computePalette(new HistogramPaletteStrategy(), colorCount);
            } else if(response == MeanShiftButton){
                computePalette(new EfficientMeanShiftPaletteStrategy(), colorCount);
            } else {
            }
        });
    }

    private void computePalette(PaletteExtractionStrategy strategy, int colorCount){
        PaletteExtractor paletteExtractor = new PaletteExtractor();
        ImageConversion imageConversion = new ImageConversion();
        PaletteImageGenerator paletteImageGenerator = new PaletteImageGenerator();
        new Thread(() -> {
            paletteExtractor.setStrategy(strategy);
            BufferedImage bfrImg = imageConversion.convertImageToBufferedImage(imageDisp.getImage());
            List<Color> paletteList = paletteExtractor.extractPalette(bfrImg, colorCount);
            BufferedImage extractedPaletteImg = paletteImageGenerator.generatePaletteImage(paletteList, 100);
            Image image = null;

            try {
                image = imageConversion.convertBufferedImageToImage(extractedPaletteImg);

                Image finalImage = image;
                Platform.runLater(() -> {
                    displayPalette(finalImage);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    ErrorDialog errorDialog = new ErrorDialog();
                    errorDialog.errorDialog(e, "Palette Extraction Failed","There was a problem extracting the Palette of this image");
                });
            }
        }).start();
    }

    private void displayPalette(Image paletteImage){
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
            controller.setPureViewUI(paletteImage);

            stage.show();
        } catch (Exception e) {
            ErrorDialog dialog = new ErrorDialog();
            dialog.errorDialog(e, "Palette Viewing Failed", "There was a problem loading the extracted Palette Image");
        }
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

    private void updateImage() {
        try {
            ImageObjFactory imageObjFactory = new ImageObjFactory();
            imageObjFactory.createNewImageObj(imageTitleField.getText(),
                    tagsImg.getText(),
                    ImageType.fromExtension(imageObj.getImageType()), imageDisp.getImage());
            imageObj.setImageTitle(imageTitleField.getText());
            imageObj.setImageTags(tagsImg.getText());
            this.contract.updateImage(imageObj);
            stage.close();
        } catch (Exception e) {
            messageBox.setText("Error Updating, invalid information provided");
        }
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

    public void saveImageToFile(Image image, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        fileChooser.setInitialFileName(imageObj.getImageTitle());
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            ImageType imageType = ImageType.fromExtension(imageObj.getImageType());
            try {
                switch (imageType) {
                    case PNG:
                        ImageIO.write(bufferedImage, "PNG", file);
                        break;
                    case JPEG:
                    case JPG:
                        BufferedImage jpegImage = new BufferedImage(
                                bufferedImage.getWidth(),
                                bufferedImage.getHeight(),
                                BufferedImage.TYPE_INT_RGB
                        );
                        jpegImage.createGraphics().drawImage(bufferedImage, 0, 0, null);
                        ImageIO.write(jpegImage, "JPEG", file);
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
