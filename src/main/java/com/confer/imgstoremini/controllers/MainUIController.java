package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.model.*;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.DbHandler;
import com.confer.imgstoremini.util.hibernateUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import java.util.List;

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
        } else if (event.getSource().equals(settingsBTN)) {
            checkSettings();
        } else if (event.getSource().equals(goToPaletteExtractorBTN)) {
            goToPaletteExtractor();
        }
    }

    @Override
    public void deleteImage(ImageThumbObjDTO deleteThisImage) {
        boolean result = ComponentFactory.imageConfirmationDeleteDialog(deleteThisImage);
        if (result) {
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
                ErrorDialog.showErrorDialog(e, "Database Error", "There was a problem deleting this image");
                return;
            }
        }
        refreshList();
    }

    @Override
    public void viewImage(ImageThumbObjDTO imageObj) {
        try {
            ComponentFactory.viewImageRecord(imageObj, this);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem loading View Image UI");
        }
    }

    @Override
    public void pureViewImage(ImageThumbObjDTO imageThumbObjDTO) {
        try {
            ComponentFactory.displayPureView(imageThumbObjDTO);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem loading Pure View Image UI");
        }
    }

    @Override
    public void updateImage(ImageObj imageObj) {
        new Thread(() -> {
            try {
                DbHandler.updateImage(imageObj);
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(e, "Database Error", "There was a problem loading updating this image");
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
                ErrorDialog.showErrorDialog(e, "Database Error", "There was a problem saving this image");
            }
            Platform.runLater(() -> {
                refreshList();
            });

        }).start();
    }

    private void checkSettings() {
        try {
            ComponentFactory.checkSettingsUI(true);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Configuration Error", "There was a problem with the Config.json");
        }
    }

    private void goToPaletteExtractor() {
        try {
            ComponentFactory.showPaletteExtractor();
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem loading Palette UI");
        }
    }

    private void goToAddImage() {
        try {
            ComponentFactory.showAddImageUI(this);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem loading Add Image UI");
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
                ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem loading Preview Image Component UI");
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
            ErrorDialog.showErrorDialog(e, "FXML Error", "There was a problem loading Entry UI");
        }

    }

}
