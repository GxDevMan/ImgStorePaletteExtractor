package com.confer.imgstoremini;

import com.confer.imgstoremini.controllers.ViewImageController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class ImageStoreMiniApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        RandomImageGenerator generator = new RandomImageGenerator();

        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("MainUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 300, 100);
        stage.setTitle("Image Store Mini");
        stage.setScene(scene);
        stage.show();

//        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("ViewImageUI.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), 300, 100);
//
//        ViewImageController controller = fxmlLoader.getController();
//        controller.setImageView(generator.generateRandomImage(1175,1500),"tags1,tags2,tags3,tags4");
//
//        stage.setTitle("Image Store Mini");
//        stage.setScene(scene);
//        stage.show();

    }

    public static void main(String[] args) {
        ConfigFileHandler configFileHandler = new ConfigFileHandler();
        configFileHandler.checkAndCreateConfigFile();
        launch();
    }
}