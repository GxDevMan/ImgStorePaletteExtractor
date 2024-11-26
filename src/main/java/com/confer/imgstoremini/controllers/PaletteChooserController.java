package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.util.*;
import com.confer.imgstoremini.util.PaletteExtraction.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

public class PaletteChooserController {

    PaletteViewImageContract contract;
    Stage stage;
    private ImageView imageDisp;

    @FXML
    private Button kmeansBTN;

    @FXML
    private Button meanShiftBTN;

    @FXML
    private Button spectralClusteringBTN;

    @FXML
    private Button GmmBTN;

    @FXML
    private Button regionBasedBTN;

    @FXML
    private Button histogramBTN;

    @FXML
    private Button closeBTN;

    public void setViewHelperController(ImageView imageDisp, Stage stage, PaletteViewImageContract contract) {
        this.contract = contract;
        this.imageDisp = imageDisp;
        this.stage = stage;
    }

    @FXML
    public void buttonClick(ActionEvent event) {
        if (event.getSource().equals(kmeansBTN)) {
            kmeansSelected(this.imageDisp);
        } else if (event.getSource().equals(meanShiftBTN)) {
            meanShiftSelected(imageDisp);
        } else if (event.getSource().equals(spectralClusteringBTN)) {
            spectralClusteringSelected(imageDisp);
        } else if (event.getSource().equals(GmmBTN)) {
            GMMSelected(imageDisp);
        } else if (event.getSource().equals(regionBasedBTN)) {
            regionBasedSelected(imageDisp);
        } else if (event.getSource().equals(histogramBTN)) {
            histogramSelected(imageDisp);
        } else if (event.getSource().equals(closeBTN)) {
            this.stage.close();
        }
    }

    private void kmeansSelected(ImageView imageDisp) {
        String strategyTitle = "K-means";
        DataStore dataStore = DataStore.getInstance();
        String preferredProcessor = (String) dataStore.getObject("preferred_processor");

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

    private void GMMSelected(ImageView imageDisp) {
        try {
            OpenCLUtils.getDevice(OpenCLUtils.getPlatform());
            setupUIProgress(new GMMJOCLPaletteStrategy(), "Gaussian Mixture Model", imageDisp);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "GMM Requirement Error", "Requires GPU");
        }
    }

    private void meanShiftSelected(ImageView imageDisp) {
        DataStore dataStore = DataStore.getInstance();
        String preferredProcessor = (String) dataStore.getObject("preferred_processor");
        if (preferredProcessor.equals("GPU")) {
            try {
                OpenCLUtils.getDevice(OpenCLUtils.getPlatform());
                setupUIProgress(new MeanShiftJOCLPaletteStrategy(), "Mean Shift", imageDisp);
            } catch (Exception e) {
                setupUIProgress(new EfficientMeanShiftPaletteStrategy(), "Mean Shift", imageDisp);
            }
        } else {
            setupUIProgress(new EfficientMeanShiftPaletteStrategy(), "Mean Shift", imageDisp);
        }
    }

    private void histogramSelected(ImageView imageDisp) {
        DataStore dataStore = DataStore.getInstance();
        String preferredProcessor = (String) dataStore.getObject("preferred_processor");
        if (preferredProcessor.equals("GPU")) {
            try {
                OpenCLUtils.getDevice(OpenCLUtils.getPlatform());
                setupUIProgress(new HistogramJOCLPaletteStrategy(), "Histogram", imageDisp);
            } catch (Exception e) {
                setupUIProgress(new HistogramPaletteStrategy(), "Histogram", imageDisp);
            }
        } else {
            setupUIProgress(new HistogramPaletteStrategy(), "Histogram", imageDisp);
        }
    }

    private void regionBasedSelected(ImageView imageDisp) {
        DataStore dataStore = DataStore.getInstance();
        String preferredProcessor = (String) dataStore.getObject("preferred_processor");
        if (preferredProcessor.equals("GPU")) {
            try {
                //OpenCLUtils.getDevice(OpenCLUtils.getPlatform());
                setupUIProgress(new RegionBasedPaletteStrategy(),"Region Based", imageDisp);
            } catch (Exception e) {
                setupUIProgress(new RegionBasedPaletteStrategy(), "Region Based", imageDisp);
            }
        } else{
            setupUIProgress(new RegionBasedPaletteStrategy(), "Region Based", imageDisp);
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
                e.printStackTrace();
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
