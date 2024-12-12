package com.confer.imgstoremini.controllers;

import com.confer.imgstoremini.controllers.interfaces.WindowMediator;

public abstract class BaseController {
    protected WindowMediator mediator;

    public void setMediator(WindowMediator mediator) {
        this.mediator = mediator;
    }

    public abstract void setupSelectedController(Object data);
}
