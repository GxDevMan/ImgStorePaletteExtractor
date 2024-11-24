package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ConfigFileHandler;
import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.hibernateUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class EntryUIController {

    @FXML
    private Button createNew;

    @FXML
    private Button loadDB;

    @FXML
    private Button settingsBTN;

    @FXML
    private Button loadDefaultBtn;

    @FXML
    private Button goToPaletteExtractorBTN;

    @FXML
    private TextField outputArea;



    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(createNew)) {
            createNewDBandGoTo();
        } else if (event.getSource().equals(loadDB)) {
            goToSelectedDB(event);
        } else if (event.getSource().equals(loadDefaultBtn)) {
            loadDefault(event);
        } else if (event.getSource().equals(settingsBTN)) {
            goToSettingsConfigUI();
        } else if (event.getSource().equals(goToPaletteExtractorBTN)){
            goToPaletteExtractor();
        }
    }

    private void createNewDBandGoTo() {
        if (checkDefaultDb()) {
            outputArea.setText("Default DB already exists");
            return;
        }
        hibernateUtil util = hibernateUtil.getInstance(getConfigSetting("default_db"));
        util.shutdown();
    }

    private void goToSelectedDB(ActionEvent event) {
        String filePath = openDbFileChooser();
        Optional<String> OptFilepath = Optional.ofNullable(filePath);
        if (OptFilepath.isPresent()) {
            hibernateUtil.getInstance(OptFilepath.get());
            goToNextMenu(event);
        } else {
            outputArea.setText("Invalid File Path");
        }
    }

    private void goToSettingsConfigUI() {
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
            controller.setConfigurationSetting(stage, false);

            stage.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"Configuration Error","There was a problem with the Config.json");
        }
    }

    private void goToPaletteExtractor() {
        loadFinalDataStoreConfiguration();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PaletteUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 500);
            PaletteUIController controller = fxmlLoader.getController();


            Stage stage = new Stage();
            stage.setTitle("Image Store - Palette Extractor");
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            controller.setPaletteUIController(stage);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            stage.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"FXML Error","There was a problem loading Palette UI");
        }
    }

    private void loadDefault(ActionEvent event) {
        try {
            hibernateUtil.getInstance(getConfigSetting("default_db"));
            goToNextMenu(event);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"Error Loading Default","Specified Default Database cannot be loaded");
            outputArea.setText("Error loading Default");
        }
    }

    private void goToNextMenu(ActionEvent event) {
        try {
            loadFinalDataStoreConfiguration();
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("MainUI.fxml"));
            Parent viewParent = fxmlLoader.load();
            Scene viewScene = new Scene(viewParent);

            MainUIController mainUIController = fxmlLoader.getController();
            mainUIController.setMainUiController();

            Stage sourceWin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            sourceWin.setHeight(700);
            sourceWin.setWidth(700);

            String stageTitle = sourceWin.getTitle();
            DataStore dataStore = DataStore.getInstance();
            String connectedDB = (String) dataStore.getObject("db_name");
            sourceWin.setTitle(String.format("%s - %s", stageTitle, connectedDB));
            sourceWin.setScene(viewScene);

            sourceWin.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"Something went wrong","Cannot Load Main UI");
            outputArea.setText("Something went wrong");
        }

    }

    private void loadFinalDataStoreConfiguration() {
        DataStore dataStore = DataStore.getInstance();
        Map<String, String> savedConfiguration = ConfigFileHandler.getConfig();
        dataStore.insertObject("dbLoc", savedConfiguration.get("default_db"));
        dataStore.insertObject("default_pagesize", Integer.parseInt(savedConfiguration.get("default_pagesize")));
        dataStore.insertObject("default_regionspalette", Integer.parseInt(savedConfiguration.get("default_regionspalette")));
        dataStore.insertObject("default_kmeansiter", Integer.parseInt(savedConfiguration.get("default_kmeansiter")));
        dataStore.insertObject("default_meanshiftiter", Integer.parseInt(savedConfiguration.get("default_meanshiftiter")));
        dataStore.insertObject("default_convergence_threshold",Double.parseDouble(savedConfiguration.get("default_convergence_threshold")));
        dataStore.insertObject("preferred_processor", savedConfiguration.get("preferred_processor"));
        dataStore.insertObject("default_spectraliter", Integer.parseInt(savedConfiguration.get("default_spectraliter")));
        dataStore.insertObject("default_gmmiter", Integer.parseInt(savedConfiguration.get("default_gmmiter")));
        dataStore.insertObject("date_sorting", savedConfiguration.get("date_sorting"));
    }

    public String openDbFileChooser() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter dbFilter = new FileChooser.ExtensionFilter("Database Files (*.db)", "*.db");
        fileChooser.getExtensionFilters().add(dbFilter);

        String currentDir = System.getProperty("user.dir");
        fileChooser.setInitialDirectory(new File(currentDir));

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            return selectedFile.getAbsolutePath();
        } else {
            return null;
        }

    }

    private boolean checkDefaultDb() {
        return ConfigFileHandler.checkDBSpecifiedInConfigFile();
    }

    private String getConfigSetting(String key) {
        Map<String, String> loadedConfiguration = ConfigFileHandler.getConfig();
        return loadedConfiguration.get(key);
    }
}