package com.hisense.imagerecognition4usbvedio;

import android.hardware.Camera;

/**
 * Created by huangshengtao on 2017-12-22.
 */

public interface ICameraPreviewCallback extends Camera.PreviewCallback {
    void onCameraOpened(int width, int height);
    void onPreviewFrame(int[] img, byte[] data, short[] depth, int x, int y);
}
