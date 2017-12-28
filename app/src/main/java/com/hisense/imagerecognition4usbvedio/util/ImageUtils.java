package com.hisense.imagerecognition4usbvedio.util;

import android.graphics.Rect;

/**
 * Created by huangshengtao on 2017-12-22.
 */

public class ImageUtils {
    public ImageUtils() {
    }

    public static Rect cropImage(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        float srcRatio = (float)srcWidth * 1.0F / (float)srcHeight;
        float dstRatio = (float)dstWidth * 1.0F / (float)dstHeight;
        Rect rect = new Rect();
        int fixedHeight;
        if(srcRatio >= dstRatio) {
            fixedHeight = dstWidth * srcHeight / dstHeight;
            rect.left = (srcWidth - fixedHeight) / 2;
            rect.right = rect.left + fixedHeight;
            rect.top = 0;
            rect.bottom = srcHeight;
        } else {
            rect.left = 0;
            rect.right = srcWidth;
            fixedHeight = dstHeight * srcWidth / dstWidth;
            rect.top = (srcHeight - fixedHeight) / 2;
            rect.bottom = rect.top + fixedHeight;
        }

        return rect;
    }
}
