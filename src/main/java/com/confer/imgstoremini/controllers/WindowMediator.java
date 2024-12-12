package com.confer.imgstoremini.controllers;

public interface WindowMediator {
    void switchTo(String screenName, Object data);
    void registerFXMLName(String screenName, String fxmlName);
}
