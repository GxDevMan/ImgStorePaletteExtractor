package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageThumbObjDTO;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.DbHandler;
import com.confer.imgstoremini.util.ImageToByteArray;
import com.confer.imgstoremini.util.hibernateUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;
import java.util.Optional;

public class MainUIController implements ImageContract {

    @FXML
    private Button searchBTN;

    @FXML
    private Button regexSearchBTN;

    @FXML
    private Button addImgBtn;

    @FXML
    private Button backBtn;

    @FXML
    private TextField imgSearchBox;

    @FXML
    private TextField regeximgSearchBox;

    @FXML
    private Pagination paginationChoice;

    @FXML
    private ScrollPane imageScrollPane;

    @FXML
    private TilePane imageViews;

    @FXML
    private ScrollPane rootScrollPane;

    private DbHandler handleImages;

    public void setMainUiController() {
        handleImages = new DbHandler();
        refreshList();
    }

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(searchBTN)) {
            refreshListSearch(imgSearchBox.getText());
        } else if (event.getSource().equals(addImgBtn)) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("AddImageUI.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 500, 500);
                AddImageContoller imgController = fxmlLoader.getController();
                Stage stage = new Stage();
                stage.setTitle("Add Image to Database");
                stage.setScene(scene);
                stage.initModality(Modality.APPLICATION_MODAL);
                imgController.setContract(this, stage);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(event.getSource().equals(regexSearchBTN)){
            refreshListSearchRegex(regeximgSearchBox.getText().trim());
        }
        else if (event.getSource().equals(backBtn)) {
            goToNextMenu(event);
        }
    }

    @Override
    public void deleteImage(ImageThumbObjDTO deleteThisImage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Confirmation");
        alert.setHeaderText("Are you sure you want to delete this image?");
        alert.setContentText("Image Title: " + deleteThisImage.getImageTitle());
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                new Thread(() -> {
                    try {
                        handleImages.deleteImage(deleteThisImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        refreshList();
                    });

                }).start();
            } catch (Exception e) {
                Alert alert2 = new Alert(Alert.AlertType.CONFIRMATION);
                alert2.setTitle("Deletion Failed");
                alert2.setHeaderText("There was a problem deleting this image");
                alert2.setContentText("Image Title: " + deleteThisImage.getImageTitle());
                alert2.showAndWait();

            }
        }
        refreshList();
    }

    @Override
    public void viewImage(ImageThumbObjDTO imageObj) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("ViewImageUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 500);

            Stage stage = new Stage();
            stage.setTitle("Image Store Mini");
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);

            ViewImageController controller = fxmlLoader.getController();
            controller.setImageView(handleImages.getImage(imageObj), this, stage);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pureViewImage(ImageThumbObjDTO imageThumbObjDTO) {
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PureViewUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 500);

            Stage stage = new Stage();
            stage.setTitle("Image Store Mini");
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);

            PureViewUIController controller = fxmlLoader.getController();
            ImageObj imageObjPure = handleImages.getImage(imageThumbObjDTO);
            ImageToByteArray conversionImg = new ImageToByteArray();
            Image imageFull = conversionImg.byteArraytoImage(imageObjPure.getFullImageByte());
            controller.setPureViewUI(imageFull);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateImage(ImageObj imageObj) {
        new Thread(() -> {
            try {
                handleImages.updateImage(imageObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                refreshList();
            });

        }).start();
    }

    @Override
    public void addImage(ImageObj imageObj) {
        new Thread(() -> {
            try {
                handleImages.saveImage(imageObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                refreshList();
            });

        }).start();
    }

    private void refreshList() {
        DataStore dataStore = DataStore.getInstance();
        int totalPages = (int) dataStore.getObject("default_pagesize");

        paginationChoice.setPageCount(handleImages.calculateTotalPages(totalPages));

        paginationChoice.setPageFactory(pageIndex -> {
            updatePage(pageIndex);
            return new Label("");
        });
        paginationChoice.setCurrentPageIndex(0);
    }

    private void refreshListSearch(String searchKey){
        DataStore dataStore = DataStore.getInstance();
        int totalPages = (int) dataStore.getObject("default_pagesize");
        dataStore.insertObject("search_key",searchKey);
        paginationChoice.setPageCount(handleImages.calculateTotalPages(totalPages, searchKey));

        paginationChoice.setPageFactory(pageIndex -> {
            updatePageSearch(pageIndex);
            return new Label("");
        });
        paginationChoice.setCurrentPageIndex(0);
    }

    private void refreshListSearchRegex(String searchKey){
        DataStore dataStore = DataStore.getInstance();
        int totalPages = (int) dataStore.getObject("default_pagesize");
        dataStore.insertObject("search_key",searchKey);
        paginationChoice.setPageCount(handleImages.calculateTotalPagesRegex(totalPages, searchKey));

        paginationChoice.setPageFactory(pageIndex -> {
            updatePageSearchRegex(pageIndex);
            return new Label("");
        });
        paginationChoice.setCurrentPageIndex(0);
    }

    private void updatePage(int pageIndex) {
        int totalPages = (int) DataStore.getInstance().getObject("default_pagesize");
        List<ImageThumbObjDTO> imageObjList = handleImages.getImagesForPageThumb(pageIndex + 1, totalPages, true);
        displayImages(imageObjList);
    }

    private void updatePageSearch(int pageIndex) {
        DataStore dataStore = DataStore.getInstance();
        String searchKey = (String) dataStore.getObject("search_key");
        int totalPages = (int) DataStore.getInstance().getObject("default_pagesize");
        List<ImageThumbObjDTO> imageObjList = handleImages.getImagesForPageThumb(pageIndex + 1, totalPages, true, searchKey);
        displayImages(imageObjList);
    }

    private void updatePageSearchRegex(int pageIndex) {
        DataStore dataStore = DataStore.getInstance();
        String searchKey = (String) dataStore.getObject("search_key");
        int totalPages = (int) DataStore.getInstance().getObject("default_pagesize");
        List<ImageThumbObjDTO> imageObjList = handleImages.getImagesForPageThumbRegex(pageIndex + 1, totalPages, true, searchKey);
        displayImages(imageObjList);
    }

    private void displayImages(List<ImageThumbObjDTO> imageObjList) {
        imageViews.getChildren().clear();
        for (ImageThumbObjDTO imageInstance : imageObjList) {
            try {
                FXMLLoader loader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PreviewImageComponentUI.fxml"));
                AnchorPane previewComponent = loader.load();

                PreviewImageComponentUIController controller = loader.getController();
                controller.setComponent(this, imageInstance);
                imageViews.getChildren().add(previewComponent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void goToNextMenu(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("EntryUI.fxml"));
            Parent viewParent = fxmlLoader.load();
            Scene viewScene = new Scene(viewParent);

            Stage sourceWin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            sourceWin.setScene(viewScene);

            hibernateUtil util = hibernateUtil.getInstance();
            util.shutdown();

            sourceWin.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
