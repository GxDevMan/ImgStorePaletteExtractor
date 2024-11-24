package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ConfigFileHandler;
import com.confer.imgstoremini.util.DataStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SettingsConfigUIController {

    Stage stage;
    private Map<String, String> configData;
    private boolean isSet;

    @FXML
    private Button selectDefaultBTN;

    @FXML
    private Button cancelBTN;

    @FXML
    private Button saveBTN;

    @FXML
    private Button newdbBTN;

    @FXML
    private TextField selectedDefaultTxtField;

    @FXML
    private TextField dbNameTxtField;

    @FXML
    private Spinner pageSizeSPN;

    @FXML
    private Spinner regioncutsSPN;

    @FXML
    private Spinner kmeansSPN;

    @FXML
    private Spinner meanshiftSPN;

    @FXML
    private Spinner spectralSPN;

    @FXML
    private Spinner gmmSPN;

    @FXML
    private Spinner meanShiftConvergenceSPN;

    @FXML
    private ChoiceBox<String> processorChoiceBox;

    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        pageSizeSPN.setValueFactory(valueFactory);
        pageSizeSPN.setEditable(true);
        addIntegerValidation(pageSizeSPN, 1, Integer.MAX_VALUE);

        SpinnerValueFactory<Integer> valueFactory2 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        regioncutsSPN.setValueFactory(valueFactory2);
        regioncutsSPN.setEditable(true);
        addIntegerValidation(regioncutsSPN, 1, Integer.MAX_VALUE);

        SpinnerValueFactory<Integer> valueFactory3 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        kmeansSPN.setValueFactory(valueFactory3);
        kmeansSPN.setEditable(true);
        addIntegerValidation(kmeansSPN, 1, Integer.MAX_VALUE);

        SpinnerValueFactory<Integer> valueFactory4 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        meanshiftSPN.setValueFactory(valueFactory4);
        meanshiftSPN.setEditable(true);
        addIntegerValidation(meanshiftSPN, 1, Integer.MAX_VALUE);

        SpinnerValueFactory<Double> valueFactory5 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, Double.MAX_VALUE, 0.1, 0.1);
        meanShiftConvergenceSPN.setValueFactory(valueFactory5);
        meanShiftConvergenceSPN.setEditable(true);
        addDoubleValidation(meanShiftConvergenceSPN, 0.1,Double.MAX_VALUE);

        SpinnerValueFactory<Integer> valueFactory6 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        spectralSPN.setValueFactory(valueFactory6);
        spectralSPN.setEditable(true);
        addIntegerValidation(spectralSPN, 1, Integer.MAX_VALUE);

        SpinnerValueFactory<Integer> valueFactory7 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        gmmSPN.setValueFactory(valueFactory7);
        gmmSPN.setEditable(true);
        addIntegerValidation(gmmSPN, 1, Integer.MAX_VALUE);

        ObservableList<String> processorChoices = FXCollections.observableArrayList("CPU", "GPU");
        processorChoiceBox.setItems(processorChoices);
        processorChoiceBox.setValue("CPU");
    }

    public void setConfigurationSetting(Stage stage, boolean isSet) {
        this.isSet = isSet;
        this.stage = stage;

        stage.setHeight(700);
        stage.setWidth(600);
        stage.setTitle("Image Store - Settings");

        String defaultDb;
        int defaultPageSize;
        int defaultRegionPalette;
        int defaultKmeanIter;
        int default_meanshiftIter;
        int default_spectraliter;
        double default_convergence_threshold;
        String preferred_processor;
        int default_gmmiter;

        if (!this.isSet) {
            this.configData = ConfigFileHandler.getConfig();
            defaultDb = configData.get("default_db");
            defaultPageSize = Integer.parseInt(configData.get("default_pagesize"));
            defaultRegionPalette = Integer.parseInt(configData.get("default_regionspalette"));
            defaultKmeanIter = Integer.parseInt(configData.get("default_kmeansiter"));
            default_meanshiftIter = Integer.parseInt(configData.get("default_meanshiftiter"));
            default_convergence_threshold = Double.parseDouble(configData.get("default_convergence_threshold"));
            default_spectraliter = Integer.parseInt(configData.get("default_spectraliter"));
            preferred_processor = configData.get("preferred_processor");
            default_gmmiter = Integer.parseInt(configData.get("default_gmmiter"));
        } else {
            DataStore dataStore = DataStore.getInstance();
            defaultDb = (String) dataStore.getObject("db_name");
            defaultPageSize = (int) dataStore.getObject("default_pagesize");
            defaultRegionPalette = (int) dataStore.getObject("default_regionspalette");
            defaultKmeanIter = (int) dataStore.getObject("default_kmeansiter");
            default_meanshiftIter = (int) dataStore.getObject("default_meanshiftiter");
            default_convergence_threshold = (double) dataStore.getObject("default_convergence_threshold");
            preferred_processor = (String) dataStore.getObject("preferred_processor");
            default_spectraliter = (int) dataStore.getObject("default_spectraliter");
            default_gmmiter = (int) dataStore.getObject("default_gmmiter");

            dbNameTxtField.setDisable(true);
            newdbBTN.setDisable(true);
            selectDefaultBTN.setDisable(true);
            saveBTN.setText("Set");
        }

        selectedDefaultTxtField.setText(defaultDb);
        pageSizeSPN.getValueFactory().setValue(defaultPageSize);
        regioncutsSPN.getValueFactory().setValue(defaultRegionPalette);
        kmeansSPN.getValueFactory().setValue(defaultKmeanIter);
        meanshiftSPN.getValueFactory().setValue(default_meanshiftIter);
        spectralSPN.getValueFactory().setValue(default_spectraliter);
        meanShiftConvergenceSPN.getValueFactory().setValue(default_convergence_threshold);
        gmmSPN.getValueFactory().setValue(default_gmmiter);
        processorChoiceBox.setValue(preferred_processor);
    }

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(selectDefaultBTN)) {
            changeDefaultDBLocation();
        } else if (event.getSource().equals(saveBTN)) {
            saveConfiguration();
            stage.close();
        } else if (event.getSource().equals(cancelBTN)) {
            stage.close();
        } else if (event.getSource().equals(newdbBTN)) {
            setNewDBName();
        }
    }

    private void setNewDBName() {
        String newDBName = dbNameTxtField.getText();
        selectedDefaultTxtField.setText("");
        selectedDefaultTxtField.setText(String.format("%s.db", newDBName));
    }

    private void changeDefaultDBLocation() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter dbFilter = new FileChooser.ExtensionFilter("Set Default DB", String.format("*%s", ".db"));
        fileChooser.getExtensionFilters().add(dbFilter);
        String currentDir = System.getProperty("user.dir");
        fileChooser.setInitialDirectory(new File(currentDir));

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            String fileName = selectedFile.getAbsolutePath();
            selectedDefaultTxtField.setText(fileName);
        }
    }

    private void saveConfiguration() {
        if (!isSet) {
            Map<String, String> newConfiguration = new HashMap<>();
            newConfiguration.put("default_pagesize", pageSizeSPN.getValue().toString());
            newConfiguration.put("default_db", selectedDefaultTxtField.getText());
            newConfiguration.put("default_regionspalette", regioncutsSPN.getValue().toString());
            newConfiguration.put("default_kmeansiter", kmeansSPN.getValue().toString());
            newConfiguration.put("default_meanshiftiter", meanshiftSPN.getValue().toString());
            newConfiguration.put("default_convergence_threshold", meanShiftConvergenceSPN.getValue().toString());
            newConfiguration.put("default_spectraliter", spectralSPN.getValue().toString());
            newConfiguration.put("preferred_processor", processorChoiceBox.getValue());
            newConfiguration.put("default_gmmiter", gmmSPN.getValue().toString());
            ConfigFileHandler.createCustomConfigFile(newConfiguration);
        }
        else{
            DataStore dataStore = DataStore.getInstance();
            dataStore.insertObject("default_pagesize",pageSizeSPN.getValue());
            dataStore.insertObject("default_regionspalette", regioncutsSPN.getValue());
            dataStore.insertObject("default_kmeansiter", kmeansSPN.getValue());
            dataStore.insertObject("default_meanshiftiter", meanshiftSPN.getValue());
            dataStore.insertObject("default_convergence_threshold", meanShiftConvergenceSPN.getValue());
            dataStore.insertObject("preferred_processor", processorChoiceBox.getValue());
            dataStore.insertObject("default_spectraliter",spectralSPN.getValue());
            dataStore.insertObject("default_gmmiter", gmmSPN.getValue());
        }
    }

    private void addIntegerValidation(Spinner<Integer> spinner, int min, int max) {
        TextField editor = spinner.getEditor();
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            editor.setText(String.valueOf(newValue));
        });

        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*")) {
                editor.setText(oldText);
            } else {
                try {
                    int value = Integer.parseInt(newText);
                    if (value < min || value > max) {
                        spinner.getValueFactory().setValue(value < min ? min : max);
                    } else {
                        spinner.getValueFactory().setValue(value);
                    }
                } catch (NumberFormatException e) {
                    spinner.getValueFactory().setValue(min);
                }
            }
        });
    }

    private void addDoubleValidation(Spinner<Double> spinner, double min, double max) {
        TextField editor = spinner.getEditor();
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            editor.setText(String.format("%.2f", newValue));
        });

        editor.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("-?\\d*(\\.\\d*)?")) {
                editor.setText(oldText);
            } else {
                try {
                    Double value = Double.parseDouble(newText);
                    if (value < min || value > max) {
                        spinner.getValueFactory().setValue(value < min ? min : max);
                    } else {
                        spinner.getValueFactory().setValue(value);
                    }
                } catch (NumberFormatException e) {
                    spinner.getValueFactory().setValue(min);
                }
            }
        });
    }
}
