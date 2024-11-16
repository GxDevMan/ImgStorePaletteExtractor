package com.confer.imgstoremini.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class EntryUIController {

    @FXML
    private Button createNew;

    @FXML
    private Button loadDB;

    @FXML
    protected void buttonClick(ActionEvent event) {
        if(event.getSource().equals(createNew)){

        } else if (event.getSource().equals(loadDB)) {
        }
    }
}