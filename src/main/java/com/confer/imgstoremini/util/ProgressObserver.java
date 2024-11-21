package com.confer.imgstoremini.util;

public interface ProgressObserver {
    void updateProgress(double progress);
    void updateStatus(String status);
}
