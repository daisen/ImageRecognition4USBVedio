package com.hisense.imagerecognition4usbvedio.util;

import android.util.Log;

/**
 * Created by huangshengtao on 2017-12-22.
 */

public class IRLog {
    public static void i(String msg) {
        Log.i("IRLog", msg);
    }

    public static void d(String msg) {
        Log.d("IRLog", msg);
    }

    public static void d(String msg, Throwable tr) {
        Log.d("IRLog", msg, tr);
    }

    public static void e(String err) {
        Log.e("IRLog", err);
    }
}
