package com.confer.imgstoremini;

import com.confer.imgstoremini.controllers.BaseController;
import com.confer.imgstoremini.controllers.components.ComponentFactory;
import com.confer.imgstoremini.controllers.components.ErrorDialog;
import com.confer.imgstoremini.controllers.interfaces.WindowMediator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class AppMediator implements WindowMediator {
    private final Stage primaryStage;
    private final Map<String, String> controllerMap = new HashMap<>();

    public AppMediator(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public void switchTo(String screenName, Object data) {
        String fxmlName = controllerMap.get(screenName);
        if (fxmlName != null) {
            try {
                FXMLLoader loader = new FXMLLoader(ImageStoreApplication.class.getResource(fxmlName));
                primaryStage.setScene(new Scene(loader.load()));

                ComponentFactory.setDefaultTitle(primaryStage);
                BaseController controller = loader.getController();

                controller.setMediator(this);
                controller.setupSelectedController(data);

                primaryStage.show();
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(e, "FXML loading Error", String.format("There was an error loading %s", screenName));
            }
        } else {
            ErrorDialog.showErrorDialog(new Exception("No Such UI"),"FXML loading Error", "Specified UI does not exist");
        }
    }

    @Override
    public void registerFXMLName(String screenName, String fxmlName){
        controllerMap.put(screenName,fxmlName);
    }
}
