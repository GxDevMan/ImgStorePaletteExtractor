package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.util.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ViewImageHelper {

    ViewImageContract contract;
    public ViewImageHelper(ViewImageContract contract){
        this.contract = contract;
    }

    public void showStrategySelectionDialog(ImageView imageDisp) {
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
                setupUIProgress(new KMeansPaletteStrategy(), colorCount, "K-means", imageDisp);
            } else if (response == regionBasedButton) {
                setupUIProgress(new RegionBasedPaletteStrategy(), colorCount, "Region-Based", imageDisp);
            } else if(response == HistogramButton){
                setupUIProgress(new HistogramPaletteStrategy(), colorCount, "Histogram", imageDisp);
            } else if(response == MeanShiftButton){
                setupUIProgress(new EfficientMeanShiftPaletteStrategy(), colorCount, "Mean Shift", imageDisp);
            } else {
            }
        });
    }

    private void setupUIProgress(PaletteExtractionStrategy strategy, int colorCount, String strategyTitle, ImageView imagedisp) {
        Stage progressStage = new Stage();
        ProgressBar progressBar = new ProgressBar(0.0);
        Label statusLabel = new Label("Status: Waiting to start...");
        Button startButton = new Button("Start");
        Button cancelButton = new Button("Cancel");
        Button closeButton = new Button("Close");

        VBox vbox = new VBox(10, progressBar, statusLabel, startButton, cancelButton, closeButton);
        Scene scene = new Scene(vbox, 400, 200);
        progressStage.setScene(scene);
        progressStage.setTitle("Palette Extraction: " + strategyTitle);
        progressStage.initModality(Modality.WINDOW_MODAL);
        progressStage.setResizable(false);

        vbox.getStylesheets().add(ImageStoreMiniApplication.class.getResource("styles/dark-theme.css").toExternalForm());
        DataStore dataStore = DataStore.getInstance();
        Image icon = (Image) dataStore.getObject("image_icon");
        progressStage.getIcons().add(icon);

        progressStage.show();

        startButton.setOnAction(event -> {
            computePalette(strategy, colorCount, progressBar, statusLabel, progressStage, cancelButton, closeButton, imagedisp);
        });

        closeButton.setOnAction(event -> {
            progressStage.close();
        });
    }

    private void computePalette(PaletteExtractionStrategy strategy, int colorCount,
                                ProgressBar progressBar,
                                Label statusLabel,
                                Stage progressStage,
                                Button cancelButton,
                                Button closeButton,
                                ImageView imageDisp) {
        PaletteExtractor paletteExtractor = new PaletteExtractor();
        ImageConversion imageConversion = new ImageConversion();
        PaletteImageGenerator paletteImageGenerator = new PaletteImageGenerator();

        Thread taskThread = new Thread(() -> {
            paletteExtractor.setStrategy(strategy);
            paletteExtractor.setObserver(new ProgressObserver() {
                @Override
                public void updateProgress(double progress) {
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }

                @Override
                public void updateStatus(String status) {
                    Platform.runLater(() -> statusLabel.setText(status));
                }
            });

            try {
                BufferedImage bfrImg = imageConversion.convertImageToBufferedImage(imageDisp.getImage());
                List<Color> paletteList = paletteExtractor.extractPalette(bfrImg, colorCount, () -> Thread.currentThread().isInterrupted());
                BufferedImage extractedPaletteImg = paletteImageGenerator.generatePaletteImage(paletteList, 100);

                Image finalImage = imageConversion.convertBufferedImageToImage(extractedPaletteImg);

                Platform.runLater(() -> {
                    contract.displayPalette(finalImage);
                });
            } catch (Exception e) {
//                Platform.runLater(() -> {
//                    ErrorDialog errorDialog = new ErrorDialog();
//                    errorDialog.errorDialog(e, "Palette Extraction Failed", "There was a problem extracting the palette.");
//                    progressStage.close();
//                });
            }
        });

        cancelButton.setOnAction(event -> {
            taskThread.interrupt();
        });

        closeButton.setOnAction(event -> {
            taskThread.interrupt();
            progressStage.close();
        });

        taskThread.setDaemon(true);
        taskThread.start();
    }
}
