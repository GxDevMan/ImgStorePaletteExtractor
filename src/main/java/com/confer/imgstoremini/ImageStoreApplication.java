package com.confer.imgstoremini;
import com.confer.imgstoremini.controllers.components.ComponentFactory;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.hibernateUtil;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ImageStoreApplication extends Application {

    @Override
    public void start(Stage stage) {
        Image icon = new Image(ImageStoreApplication.class.getResource("/com/confer/imgstoremini/images/Database.png").toExternalForm());
        DataStore dataStore = DataStore.getInstance();
        dataStore.insertObject("image_icon", icon);

        ComponentFactory.setIcon(stage);
        ComponentFactory.setDefaultTitle(stage);

        AppMediator mediator = new AppMediator(stage);
        mediator.registerFXMLName("mainUI","MainUI.fxml");
        mediator.registerFXMLName("entryUI","EntryUI.fxml");
        mediator.switchTo("entryUI", null);
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