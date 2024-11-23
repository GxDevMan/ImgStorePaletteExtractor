package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.util.*;
import com.confer.imgstoremini.util.PaletteExtraction.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

public class ViewImageHelper {

    PaletteViewImageContract contract;

    public ViewImageHelper(PaletteViewImageContract contract) {
        this.contract = contract;
    }

    public void showStrategySelectionDialog(ImageView imageDisp) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Select Palette Extraction Strategy");
        alert.setHeaderText("Choose a palette extraction strategy:");

        ButtonType kMeansButton = new ButtonType("K-Means");
        ButtonType MeanShiftButton = new ButtonType("Mean Shift");
        ButtonType SpectralClusteringButton = new ButtonType("Spectral Clustering");
        ButtonType regionBasedButton = new ButtonType("Region-Based");
        ButtonType HistogramButton = new ButtonType("Histogram");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(kMeansButton, MeanShiftButton, SpectralClusteringButton, regionBasedButton, HistogramButton, cancelButton);
        alert.getDialogPane().setContent(new VBox(10));  // Empty VBox for now as no spinner needed here

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        DataStore dataStore = DataStore.getInstance();
        Image icon = (Image) dataStore.getObject("image_icon");
        stage.getIcons().add(icon);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(ImageStoreMiniApplication.class.getResource("styles/dark-theme.css").toExternalForm());
        String preferredProcessor = (String) dataStore.getObject("preferred_processor");

        alert.showAndWait().ifPresent(response -> {
            if (response == kMeansButton) {
                kmeansSelected(preferredProcessor, imageDisp);
            } else if (response == MeanShiftButton) {
                setupUIProgress(new EfficientMeanShiftPaletteStrategy(), "Mean Shift", imageDisp);
            } else if (response == regionBasedButton) {
                setupUIProgress(new RegionBasedPaletteStrategy(), "Region-Based", imageDisp);
            } else if (response == HistogramButton) {
                setupUIProgress(new HistogramPaletteStrategy(), "Histogram", imageDisp);
            } else if (response == SpectralClusteringButton) {
                spectralClusteringSelected(imageDisp);
            }
        });
    }

    private void kmeansSelected(String preferredProcessor, ImageView imageDisp) {
        String strategyTitle = "K-means";

        if (preferredProcessor.equals("GPU")) {
            try {
                OpenCLUtils.getDevice(OpenCLUtils.getPlatform());
                setupUIProgress(new KMeansJOCLPaletteStrategy(), strategyTitle, imageDisp);
            } catch (Exception e) {
                setupUIProgress(new KMeansPaletteStrategy(), strategyTitle, imageDisp);
            }
        } else {
            setupUIProgress(new KMeansPaletteStrategy(), strategyTitle, imageDisp);
        }
    }

    private void spectralClusteringSelected(ImageView imageDisp) {
        try {
            OpenCLUtils.getDevice(OpenCLUtils.getPlatform());
            setupUIProgress(new SpectralClusteringJOCLPaletteStrategy(), "Spectral Clustering", imageDisp);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Spectral Clustering Requirement Error", "Requires GPU");
        }
    }

    private void setupUIProgress(PaletteExtractionStrategy strategy, String strategyTitle, ImageView imageDisp) {
        Stage progressStage = new Stage();

        // Create ProgressBar, Labels, Buttons, and Spinner
        ProgressBar progressBar = new ProgressBar(0.0);
        Label statusLabel = new Label("Status: Waiting to start...");
        Label colorCountLabel = new Label("Select Colors to Extract:");

        // Create the spinner and make it wider
        Spinner<Integer> colorCountSpinner = new Spinner<>(1, Integer.MAX_VALUE, 10);  // Default to 10 colors
        colorCountSpinner.setEditable(true);
        colorCountSpinner.setMaxWidth(100);
        colorCountSpinner.setStyle("-fx-background-color: white; -fx-pref-width: 100px;");  // Apply custom width

        Button startButton = new Button("Start");
        Button cancelButton = new Button("Cancel");
        Button closeButton = new Button("Close");

        HBox buttonBox = new HBox(10, startButton, cancelButton, closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(15, colorCountLabel, colorCountSpinner, progressBar, statusLabel, buttonBox);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        Scene scene = new Scene(vbox, 400, 250);
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
            int colorCount = colorCountSpinner.getValue();  // Get the value from the spinner
            computePalette(strategy, colorCount, progressBar, statusLabel, progressStage, cancelButton, closeButton, imageDisp);
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
                BufferedImage bfrImg = ImageConversion.convertImageToBufferedImage(imageDisp.getImage());
                List<Color> paletteList = paletteExtractor.extractPalette(bfrImg, colorCount, () -> Thread.currentThread().isInterrupted());
                BufferedImage extractedPaletteImg = PaletteImageGenerator.generatePaletteImage(paletteList, 100);

                Image finalImage = ImageConversion.convertBufferedImageToImage(extractedPaletteImg);

                Platform.runLater(() -> {
                    contract.displayPalette(finalImage);
                });
            } catch (Exception e) {
//                e.printStackTrace();
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
