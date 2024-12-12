package com.confer.imgstoremini.controllers.interfaces;
import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageThumbObjDTO;

public interface ImageContract {
    void deleteImage(ImageThumbObjDTO deleteThisImage);
    void viewImage(ImageThumbObjDTO imageObj);
    void updateImage(ImageObj imageObj);
    void addImage(ImageObj imageObj);
    void pureViewImage(ImageThumbObjDTO imageThumbObjDTO);
}
