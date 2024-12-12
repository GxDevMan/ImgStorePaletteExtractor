package com.confer.imgstoremini.controllers.components;
import com.confer.imgstoremini.ImageStoreMiniApplication;
import com.confer.imgstoremini.controllers.interfaces.ImageContract;
import com.confer.imgstoremini.controllers.interfaces.PaletteViewImageContract;
import com.confer.imgstoremini.model.ImageThumbObjDTO;
import com.confer.imgstoremini.model.ImageType;
import com.confer.imgstoremini.util.DataStore;
import com.confer.imgstoremini.util.DbHandler;
import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.util.ImageConversion;
import com.confer.imgstoremini.util.Resizing.JpegResizeStrategy;
import com.confer.imgstoremini.util.Resizing.PngResizeStrategy;
import com.confer.imgstoremini.util.Resizing.ResizeImgContext;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import java.awt.image.BufferedImage;
import java.util.Optional;

public class ComponentFactory {

    public static void checkSettingsUI(boolean isSet) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/SettingsConfigUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 550, 250);

        Stage stage = new Stage();
        stage.setScene(scene);
        setDefaultTitlewithAdd(stage, "Settings");
        setIcon(stage);
        stage.initModality(Modality.APPLICATION_MODAL);

        SettingsConfigUIController controller = fxmlLoader.getController();
        controller.setConfigurationSetting(stage, isSet);
        stage.show();
    }

    public static void displayPureView(ImageThumbObjDTO imageThumbObjDTO) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/PureViewUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 500);

        Stage stage = new Stage();
        setDefaultTitlewithAdd(stage, imageThumbObjDTO.getImageTitle());
        setIcon(stage);
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);

        PureViewUIController controller = fxmlLoader.getController();
        ImageObj imageObjPure = DbHandler.getImage(imageThumbObjDTO);
        Image imageFull = ImageConversion.byteArraytoImage(imageObjPure.getFullImageByte());
        controller.setPureViewUI(imageFull, stage);
        stage.show();
    }

    public static void displayPureView(Image image) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/PureViewUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 500);

        Stage stage = new Stage();
        setDefaultTitlewithAdd(stage, "Palette Extracted");
        setIcon(stage);
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);

        PureViewUIController controller = fxmlLoader.getController();
        controller.setPureViewUI(image, stage);
        stage.show();
    }

    public static Pair<BorderPane, PureViewUIController> pureViewAsComponent() throws Exception {
        FXMLLoader loader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/PureViewUI.fxml"));
        BorderPane previewComponent = loader.load();
        PureViewUIController controller = loader.getController();
        return new Pair<>(previewComponent,controller);
    }

    public static void viewImageRecord(ImageThumbObjDTO imageThumbObjDTO, ImageContract contract) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/ViewImageUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 600);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.WINDOW_MODAL);
        setDefaultTitlewithAdd(stage, imageThumbObjDTO.getImageTitle());
        setIcon(stage);

        ViewImageController controller = fxmlLoader.getController();
        controller.setImageView(DbHandler.getImage(imageThumbObjDTO), contract, stage);

        stage.show();
    }

    public static void showPaletteExtractor(boolean isSet) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/PaletteUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 500);

        Stage stage = new Stage();
        stage.setScene(scene);
        setDefaultTitlewithAdd(stage, "Palette Extractor");
        setIcon(stage);
        stage.initModality(Modality.WINDOW_MODAL);

        PaletteUIController controller = fxmlLoader.getController();
        controller.setPaletteUIController(stage, isSet);
        stage.show();
    }

    public static void showPaletteMenuExtraction(Image image, PaletteViewImageContract contract) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/PaletteStrategyChooserUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 380, 210);

        Stage stage = new Stage();
        stage.setScene(scene);
        setDefaultTitlewithAdd(stage,"Choose Palette Strategy");
        setIcon(stage);
        stage.initModality(Modality.WINDOW_MODAL);

        PaletteChooserController controller = fxmlLoader.getController();
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        controller.setViewHelperController(imageView,stage,contract);
        stage.show();
    }

    public static void showAddImageUI(ImageContract contract) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageStoreMiniApplication.class.getResource("components/AddImageUI.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);

        Stage stage = new Stage();
        stage.setScene(scene);
        setDefaultTitlewithAdd(stage, "Add Image to Database");
        setIcon(stage);

        AddImageContoller contoller = fxmlLoader.getController();
        contoller.setContract(contract, stage);

        stage.show();
    }

    public static boolean imageConfirmationDeleteDialog(ImageThumbObjDTO deleteThisImage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Confirmation");
        alert.setHeaderText("Are you sure you want to delete this image?");
        alert.setContentText("Image Title: " + deleteThisImage.getImageTitle());
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(ImageStoreMiniApplication.class.getResource("styles/dark-theme.css").toExternalForm());

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        setIcon(stage);

        Image ThumbNail = ImageConversion.byteArraytoImage(deleteThisImage.getThumbnailImageByte());

        ResizeImgContext resizeImgContext = new ResizeImgContext();
        switch (ImageType.fromExtension(deleteThisImage.getImageType())) {
            case JPG, JPEG -> resizeImgContext.setStrategy(new JpegResizeStrategy());
            case PNG -> resizeImgContext.setStrategy(new PngResizeStrategy());
            default -> {
                return false;
            }
        }
        BufferedImage bufferedImage = resizeImgContext.executeResize(
                SwingFXUtils.fromFXImage(ThumbNail, null),
                100, 100);

        try {
            ThumbNail = ImageConversion.convertBufferedImageToImage(bufferedImage);
        } catch (Exception e) {
            ErrorDialog.showErrorDialog(e, "Buffered Image to Image Conversion failed", "Image Conversion Failed");
        }
        ImageView imageView = new ImageView(ThumbNail);
        alert.setGraphic(imageView);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void setIcon(Stage stage) {
        DataStore dataStore = DataStore.getInstance();
        Image icon = (Image) dataStore.getObject("image_icon");
        stage.getIcons().add(icon);
    }

    public static void setDefaultTitle(Stage stage) {
        stage.setTitle("Image Store");
    }

    public static void setDefaultTitlewithAdd(Stage stage, String additional) {
        stage.setTitle(String.format("Image Store - %s", additional));
    }

}
