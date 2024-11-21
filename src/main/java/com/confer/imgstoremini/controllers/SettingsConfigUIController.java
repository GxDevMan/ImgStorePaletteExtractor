package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ConfigFileHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SettingsConfigUIController {

    Stage stage;
    private Map<String, String> configData;

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

    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        pageSizeSPN.setValueFactory(valueFactory);
        pageSizeSPN.setEditable(true);
        addValidation(pageSizeSPN, 1, Integer.MAX_VALUE);

        SpinnerValueFactory<Integer> valueFactory2 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        regioncutsSPN.setValueFactory(valueFactory2);
        regioncutsSPN.setEditable(true);
        addValidation(regioncutsSPN, 1, Integer.MAX_VALUE);

        SpinnerValueFactory<Integer> valueFactory3 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1);
        kmeansSPN.setValueFactory(valueFactory3);
        kmeansSPN.setEditable(true);
        addValidation(kmeansSPN, 1, Integer.MAX_VALUE);
    }

    public void setConfigurationSetting(Stage stage){
        ConfigFileHandler configFileHandler = new ConfigFileHandler();
        this.stage = stage;
        this.configData = configFileHandler.getConfig();

        String defaultDb = configData.get("default_db");
        int defaultPageSize = Integer.parseInt(configData.get("default_pagesize"));
        int defaultRegionPalette = Integer.parseInt(configData.get("default_regionspalette"));
        int defaultKmeanIter = Integer.parseInt(configData.get("default_kmeansiter"));

        selectedDefaultTxtField.setText(defaultDb);
        pageSizeSPN.getValueFactory().setValue(defaultPageSize);
        regioncutsSPN.getValueFactory().setValue(defaultRegionPalette);
        kmeansSPN.getValueFactory().setValue(defaultKmeanIter);
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
        } else if (event.getSource().equals(newdbBTN)){
           setNewDBName();
        }
    }

    private void setNewDBName(){
        String newDBName = dbNameTxtField.getText();
        selectedDefaultTxtField.setText("");
        selectedDefaultTxtField.setText(String.format("%s.db", newDBName));
    }

    private void changeDefaultDBLocation(){
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter dbFilter = new FileChooser.ExtensionFilter("Set Default DB", String.format("*%s", ".db"));
        fileChooser.getExtensionFilters().add(dbFilter);
        String currentDir = System.getProperty("user.dir");
        fileChooser.setInitialDirectory(new File(currentDir));

        File selectedFile = fileChooser.showOpenDialog(null);

        if(selectedFile != null){
            String fileName = selectedFile.getAbsolutePath();
            selectedDefaultTxtField.setText(fileName);
        }
    }

    private void saveConfiguration(){
        ConfigFileHandler configFileHandler = new ConfigFileHandler();
        Map<String, String> newConfiguration = new HashMap<>();
        newConfiguration.put("default_pagesize", pageSizeSPN.getValue().toString());
        newConfiguration.put("default_db",selectedDefaultTxtField.getText());
        newConfiguration.put("default_regionspalette",regioncutsSPN.getValue().toString());
        newConfiguration.put("default_kmeansiter", kmeansSPN.getValue().toString());
        configFileHandler.createCustomConfigFile(newConfiguration);
    }
    private void addValidation(Spinner<Integer> spinner, int min, int max) {
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
}
