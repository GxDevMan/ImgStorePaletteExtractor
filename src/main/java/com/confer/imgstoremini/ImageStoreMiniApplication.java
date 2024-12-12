package com.confer.imgstoremini;
import com.confer.imgstoremini.controllers.components.ComponentFactory;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.hibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
public class ImageStoreMiniApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Image icon = new Image(ImageStoreMiniApplication.class.getResource("/com/confer/imgstoremini/images/Database.png").toExternalForm());
        DataStore dataStore = DataStore.getInstance();
        dataStore.insertObject("image_icon", icon);

        ComponentFactory.setIcon(stage);
        ComponentFactory.setDefaultTitle(stage);

        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("EntryUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public void stop(){
        hibernateUtil util = hibernateUtil.getInstance();
        if(util != null)
            util.shutdown();
        DataStore dataStore = DataStore.getInstance();
        if(dataStore != null)
            dataStore.DestroyStore();
    }
}