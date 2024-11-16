package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.RandomImageGenerator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Random;

public class MainUIController implements ImageContract {
    @FXML
    private Button searchBTN;

    @FXML
    private Button addImgBtn;

    @FXML
    private TextField imgSearchBox;

    @FXML
    private Pagination paginationChoice;

    @FXML
    private ScrollPane scrollViewImg;

    @FXML
    private TilePane imageViews;

    @FXML
    private ScrollPane rootScrollPane;


    public void initialize() {
        RandomImageGenerator generator = new RandomImageGenerator();
        Random random = new Random();

        for (int i = 1; i <= 10; i++) {
            try {
                // Load PreviewImageComponent from its FXML
                FXMLLoader loader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PreviewImageComponentUI.fxml"));
                AnchorPane previewComponent = loader.load();

                PreviewImageComponentUIController controller = loader.getController();
                controller.setComponent(this, generator.generateRandomImage(random.nextInt(500),random.nextInt(500)),random.nextInt(1000) );
                imageViews.getChildren().add(previewComponent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @FXML
    protected void buttonClick(ActionEvent event){
        if(event.getSource().equals(searchBTN)){

        } else if (event.getSource().equals(addImgBtn)) {


        }
    }


    @Override
    public void deleteImage(int imageId) {
        System.out.println("Image Id: " + imageId);
        System.out.println("DELETE IMAGE WAS CLICKED");
    }

    @Override
    public void viewImage(ImageView imageView) {
        System.out.println("VIEW IMAGE WAS CLICKED");

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("ViewImageUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 500);

            ViewImageController controller = fxmlLoader.getController();
            controller.setImageView(imageView.getImage(), "tags1,tags2,tags3,tags4");

            Stage stage = new Stage();
            stage.setTitle("Image Store Mini");
            stage.setScene(scene);

            stage.initModality(Modality.WINDOW_MODAL);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
