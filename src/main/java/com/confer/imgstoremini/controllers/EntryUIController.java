package com.confer.imgstoremini.controllers;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class EntryUIController {

    @FXML
    private Button createNew;

    @FXML
    private Button loadDB;

    @FXML
    private TextField outputArea;

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(createNew)) {
            if (checkDefaultDb()) {
                outputArea.setText("Default DB already exists");
                return;
            }
            DataStore dataStore = DataStore.getInstance();
            hibernateUtil.getInstance((String) dataStore.getObject("dbLoc"));
            goToNextMenu(event);
        } else if (event.getSource().equals(loadDB)) {
            String filePath = openDbFileChooser();
            Optional<String> OptFilepath = Optional.ofNullable(filePath);
            if (OptFilepath.isPresent()) {
                hibernateUtil.getInstance(OptFilepath.get());
                goToNextMenu(event);
            } else {
                outputArea.setText("Invalid File Path");
            }
        }
    }

    private void goToNextMenu(ActionEvent event){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("MainUI.fxml"));
            Parent viewParent = fxmlLoader.load();
            Scene viewScene = new Scene(viewParent);

            Stage sourceWin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            sourceWin.setScene(viewScene);

            sourceWin.show();
        } catch (Exception e) {
            outputArea.setText("Something went wrong");
        }

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
        DataStore dataStore = DataStore.getInstance();
        String filepath = (String) dataStore.getObject("dbLoc");
        File defaultDb = new File(filepath);
        return defaultDb.exists();
    }
}