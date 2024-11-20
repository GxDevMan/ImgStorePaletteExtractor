package com.confer.imgstoremini;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.hibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
public class ImageStoreMiniApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("EntryUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 100);
        stage.setTitle("Image Store Mini");
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