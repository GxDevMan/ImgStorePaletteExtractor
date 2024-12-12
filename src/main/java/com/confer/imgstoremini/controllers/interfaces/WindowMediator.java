package com.confer.imgstoremini.controllers.interfaces;

public interface WindowMediator {
    void switchTo(String screenName, Object data);
    void registerFXMLName(String screenName, String fxmlName);
}
