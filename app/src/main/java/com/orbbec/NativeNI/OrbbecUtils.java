package com.orbbec.NativeNI;

/**
 * Created by huangshengtao on 2017-12-22.
 */

@SuppressWarnings("JniMissingFunction")
public class OrbbecUtils {
    public OrbbecUtils() {
    }

    public static native int CoventFromDepthTORGB(int[] var0, int var1, int var2);

    public static native int ByteToRGBData(byte[] var0, int[] var1, int var2, int var3, int var4);

    public static native int NV21ToMirrorRGBA(byte[] var0, int[] var1, int var2, int var3);

    public static native int NV21ToRGBA(byte[] var0, int[] var1, int var2, int var3);

    public static native int YUV422ToRGBA(byte[] var0, int[] var1, int var2, int var3, boolean var4);

    public static native boolean IsPro();

    public static native int DepthOffset(short[] var0, short[] var1, int var2, int var3, int var4);

    public static native float GetOffsetValue(String var0, float var1);

    public static native int NV21ToBGR(byte[] var0, int[] var1, int var2, int var3);

    public static native int BgrToArgb(int[] var0, int[] var1, int var2, int var3);

    public static native int Exit();

    static {
        System.loadLibrary("orbbecusb");
        System.loadLibrary("OpenNI");
        System.loadLibrary("OrbbecUtils");
        System.loadLibrary("OrbbecUtils_jni");
    }
}
