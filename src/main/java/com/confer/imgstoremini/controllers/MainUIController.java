package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.model.*;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.DbHandler;
import com.confer.imgstoremini.util.ImageConversion;
import com.confer.imgstoremini.util.Resizing.JpegResizeStrategy;
import com.confer.imgstoremini.util.Resizing.PngResizeStrategy;
import com.confer.imgstoremini.util.Resizing.ResizeImgContext;
import com.confer.imgstoremini.util.hibernateUtil;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import java.awt.image.BufferedImage;
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
    private Button settingsBTN;

    @FXML
    private Button backBtn;

    @FXML
    private Button resetBTN;

    @FXML
    private Button goToPaletteExtractorBTN;

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

    public void setMainUiController() {
        refreshList();
    }

    @FXML
    protected void buttonClick(ActionEvent event) {
        if (event.getSource().equals(searchBTN)) {
            refreshListSearch(imgSearchBox.getText());
        } else if (event.getSource().equals(addImgBtn)) {
            goToAddImage();
        } else if (event.getSource().equals(regexSearchBTN)) {
            refreshListSearchRegex(regeximgSearchBox.getText().trim());
        } else if (event.getSource().equals(backBtn)) {
            goToNextMenu(event);
        } else if (event.getSource().equals(resetBTN)) {
            imgSearchBox.setText("");
            regeximgSearchBox.setText("");
            refreshList();
        } else if (event.getSource().equals(settingsBTN)){
            checkSettings();
        } else if (event.getSource().equals(goToPaletteExtractorBTN)){
            goToPaletteExtractor();
        }
    }

    @Override
    public void deleteImage(ImageThumbObjDTO deleteThisImage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Confirmation");
        alert.setHeaderText("Are you sure you want to delete this image?");
        alert.setContentText("Image Title: " + deleteThisImage.getImageTitle());
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(ImageStoreMiniApplication.class.getResource("styles/dark-theme.css").toExternalForm());

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        DataStore dataStore = DataStore.getInstance();
        Image icon = (Image) dataStore.getObject("image_icon");
        stage.getIcons().add(icon);

        Image ThumbNail = ImageConversion.byteArraytoImage(deleteThisImage.getThumbnailImageByte());

        ResizeImgContext resizeImgContext = new ResizeImgContext();
        switch (ImageType.fromExtension(deleteThisImage.getImageType())) {
            case JPG, JPEG -> resizeImgContext.setStrategy(new JpegResizeStrategy());
            case PNG -> resizeImgContext.setStrategy(new PngResizeStrategy());
            default -> {
                return;
            }
        }
        BufferedImage bufferedImage = resizeImgContext.executeResize(
                SwingFXUtils.fromFXImage(ThumbNail, null),
                100, 100);

        try {
            ThumbNail = ImageConversion.convertBufferedImageToImage(bufferedImage);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Buffered Image to Image Conversion failed","Image Conversion Failed");
            return;
        }
        ImageView imageView = new ImageView(ThumbNail);
        alert.setGraphic(imageView);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                new Thread(() -> {
                    try {
                        DbHandler.deleteImage(deleteThisImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        refreshList();
                    });

                }).start();
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(e,"Database Error","There was a problem deleting this image", imageView,deleteThisImage);
                return;
            }
        }
        refreshList();
    }

    @Override
    public void viewImage(ImageThumbObjDTO imageObj) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("ViewImageUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 600);

            Stage stage = new Stage();
            stage.setTitle(String.format("Image Store - %s", imageObj.getImageTitle()));
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            ViewImageController controller = fxmlLoader.getController();
            controller.setImageView(DbHandler.getImage(imageObj), this, stage);

            stage.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"FXML Error","There was a problem loading View Image UI");
        }
    }

    @Override
    public void pureViewImage(ImageThumbObjDTO imageThumbObjDTO) {
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PureViewUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 500);

            Stage stage = new Stage();
            stage.setTitle(String.format("Image Store - %s",imageThumbObjDTO.getImageTitle()));
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            PureViewUIController controller = fxmlLoader.getController();
            ImageObj imageObjPure = DbHandler.getImage(imageThumbObjDTO);
            ImageConversion conversionImg = new ImageConversion();
            Image imageFull = conversionImg.byteArraytoImage(imageObjPure.getFullImageByte());
            controller.setPureViewUI(imageFull, stage);

            stage.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"FXML Error","There was a problem loading Pure View Image UI");
        }
    }

    @Override
    public void updateImage(ImageObj imageObj) {
        new Thread(() -> {
            try {
                DbHandler.updateImage(imageObj);
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(e,"Database Error","There was a problem loading updating this image");
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
                DbHandler.saveImage(imageObj);
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(e,"Database Error","There was a problem saving this image");
            }
            Platform.runLater(() -> {
                refreshList();
            });

        }).start();
    }

    private void checkSettings(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("SettingsConfigUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 550, 250);

            Stage stage = new Stage();
            stage.setTitle("Image Store");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            SettingsConfigUIController controller = fxmlLoader.getController();
            controller.setConfigurationSetting(stage, true);

            stage.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"Configuration Error","There was a problem with the Config.json");
        }
    }

    private void goToPaletteExtractor() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("PaletteUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 500);

            PaletteUIController controller = fxmlLoader.getController();


            Stage stage = new Stage();
            stage.setTitle("Image Store - Palette Extractor");
            stage.setScene(scene);
            stage.initModality(Modality.WINDOW_MODAL);
            controller.setPaletteUIController(stage);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            stage.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"FXML Error","There was a problem loading Palette UI");
        }
    }

    private void goToAddImage() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("AddImageUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 500);
            AddImageContoller imgController = fxmlLoader.getController();

            Stage stage = new Stage();
            stage.setTitle("Add Image to Database");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);

            DataStore dataStore = DataStore.getInstance();
            Image icon = (Image) dataStore.getObject("image_icon");
            stage.getIcons().add(icon);

            imgController.setContract(this, stage);
            stage.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"FXML Error","There was a problem loading Add Image UI");
        }
    }

    private void refreshList() {
        DataStore dataStore = DataStore.getInstance();
        int totalPages = (int) dataStore.getObject("default_pagesize");

        paginationChoice.setPageCount(DbHandler.calculateTotalPages(totalPages));

        paginationChoice.setPageFactory(pageIndex -> {
            updatePage(pageIndex);
            return new Label("");
        });
        paginationChoice.setCurrentPageIndex(0);
    }

    private void refreshListSearch(String searchKey) {
        DataStore dataStore = DataStore.getInstance();
        int totalPages = (int) dataStore.getObject("default_pagesize");
        dataStore.insertObject("search_key", searchKey);
        paginationChoice.setPageCount(DbHandler.calculateTotalPages(totalPages, searchKey));

        paginationChoice.setPageFactory(pageIndex -> {
            updatePageSearch(pageIndex);
            return new Label("");
        });
        paginationChoice.setCurrentPageIndex(0);
    }

    private void refreshListSearchRegex(String searchKey) {
        DataStore dataStore = DataStore.getInstance();
        int totalPages = (int) dataStore.getObject("default_pagesize");
        dataStore.insertObject("search_key", searchKey);
        paginationChoice.setPageCount(DbHandler.calculateTotalPagesRegex(totalPages, searchKey));

        paginationChoice.setPageFactory(pageIndex -> {
            updatePageSearchRegex(pageIndex);
            return new Label("");
        });
        paginationChoice.setCurrentPageIndex(0);
    }

    private void updatePage(int pageIndex) {
        int totalPages = (int) DataStore.getInstance().getObject("default_pagesize");

        DataStore dataStore = DataStore.getInstance();
        String ascending = (String) dataStore.getObject("date_sorting");
        boolean ascendingCheck = ascending.equals("Ascending");

        List<ImageThumbObjDTO> imageObjList = DbHandler.getImagesForPageThumb(pageIndex + 1, totalPages, ascendingCheck);
        displayImages(imageObjList);
    }

    private void updatePageSearch(int pageIndex) {
        DataStore dataStore = DataStore.getInstance();
        String searchKey = (String) dataStore.getObject("search_key");
        int totalPages = (int) DataStore.getInstance().getObject("default_pagesize");

        String ascending = (String) dataStore.getObject("date_sorting");
        boolean ascendingCheck = ascending.equals("Ascending");

        List<ImageThumbObjDTO> imageObjList = DbHandler.getImagesForPageThumb(pageIndex + 1, totalPages, ascendingCheck, searchKey);
        displayImages(imageObjList);
    }

    private void updatePageSearchRegex(int pageIndex) {
        DataStore dataStore = DataStore.getInstance();
        String searchKey = (String) dataStore.getObject("search_key");
        int totalPages = (int) DataStore.getInstance().getObject("default_pagesize");

        String ascending = (String) dataStore.getObject("date_sorting");
        boolean ascendingCheck = ascending.equals("Ascending");

        List<ImageThumbObjDTO> imageObjList = DbHandler.getImagesForPageThumbRegex(pageIndex + 1, totalPages, ascendingCheck, searchKey);
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
                ErrorDialog.showErrorDialog(e,"FXML Error","There was a problem loading Preview Image Component UI");
            }
        }
    }

    private void goToNextMenu(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("EntryUI.fxml"));
            Parent viewParent = fxmlLoader.load();
            Scene viewScene = new Scene(viewParent);

            Stage sourceWin = (Stage) ((Node) event.getSource()).getScene().getWindow();
            sourceWin.setHeight(200);
            sourceWin.setWidth(400);
            sourceWin.setScene(viewScene);

            sourceWin.setTitle("Image Store");

            hibernateUtil util = hibernateUtil.getInstance();
            if (util != null)
                util.shutdown();

            sourceWin.show();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e,"FXML Error","There was a problem loading Entry UI");
        }

    }

}
