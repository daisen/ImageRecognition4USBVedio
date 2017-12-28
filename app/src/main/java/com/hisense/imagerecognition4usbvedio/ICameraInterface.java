package com.hisense.imagerecognition4usbvedio;

import android.view.SurfaceHolder;

/**
 * Created by huangshengtao on 2017-12-22.
 */

public interface ICameraInterface {
    void openCamera();

    void startPreview();

    void stopCamera();

    void setListener(ICameraCallback var1);

    void setPreviewCallback(ICameraPreviewCallback var1);
}