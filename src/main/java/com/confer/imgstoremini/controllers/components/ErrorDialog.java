package com.confer.imgstoremini.controllers.components;

import com.confer.imgstoremini.ImageStoreApplication;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

public class ErrorDialog {

    public static void showErrorDialog(Exception e, String title, String header){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(e.getMessage());

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        ComponentFactory.setIcon(stage);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(ImageStoreApplication.class.getResource("styles/dark-theme.css").toExternalForm());
        alert.showAndWait();
    }
}
