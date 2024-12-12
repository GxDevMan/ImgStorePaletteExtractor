module com.confer.imgstoremini {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires java.desktop;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires com.fasterxml.jackson.databind;

    requires javafx.swing;
    requires java.persistence;
    requires java.naming;
    requires java.sql;
    requires org.hibernate.orm.core;
    requires commons.math3;
    requires jocl;
    requires SearchCriteriaUtil;
    requires opencv;

    opens com.confer.imgstoremini.model;
    opens com.confer.imgstoremini to javafx.fxml;
    exports com.confer.imgstoremini;
    exports com.confer.imgstoremini.controllers;
    opens com.confer.imgstoremini.controllers to javafx.fxml;
    exports com.confer.imgstoremini.model;
    exports com.confer.imgstoremini.util;
    opens com.confer.imgstoremini.util to javafx.fxml;
    exports com.confer.imgstoremini.util.PaletteExtraction;
    opens com.confer.imgstoremini.util.PaletteExtraction to javafx.fxml;
    exports com.confer.imgstoremini.util.Resizing;
    opens com.confer.imgstoremini.util.Resizing;
}