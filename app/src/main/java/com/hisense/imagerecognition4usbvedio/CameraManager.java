package com.hisense.imagerecognition4usbvedio;

/**
 * Created by huangshengtao on 2017-12-22.
 */


import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hisense.imagerecognition4usbvedio.component.CameraSurfaceView;
import com.orbbec.NativeNI.OrbbecUtils;

import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.ImageRegistrationMode;
import org.openni.OpenNI;
import org.openni.PixelFormat;
import org.openni.SensorType;
import org.openni.VideoFrameRef;
import org.openni.VideoMode;
import org.openni.VideoStream;
import org.openni.android.OpenNIHelper;
import org.openni.android.OpenNIHelper.DeviceOpenListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CameraManager implements ICameraInterface {
    private static final String TAG = "AstraCamera";
    private static CameraManager mCameraInterface = null;
    private ICameraCallback mCameraCallback;
    private Context mContext;
    private Device mDevice;
    private OpenNIHelper mOpenNIHelper;
    private VideoStream mRGBStream;
    private VideoStream mDepthStream;
    List<VideoStream> mRGBStreamList;
    List<VideoStream> mDepthStreamList;
    private Thread mCaptureThread;
    private ICameraPreviewCallback mPreviewCallback;
    private int[] mRGBIntData;
    private byte[] mRGBByteData;
    private short[] mDepthShortData;
    private boolean isCameraInit = false;
    private boolean isCameraRunning = false;
    private boolean isRendering = false;
    private int mWidth = 640;
    private int mHeight = 480;
    private DeviceOpenListener mPermissionCallback = new DeviceOpenListener() {
        public void onDeviceOpened(Device device) {
            CameraManager.this.mDevice = device;
            CameraManager.this.mRGBStream = VideoStream.create(CameraManager.this.mDevice, SensorType.COLOR);
            CameraManager.this.mDepthStream = VideoStream.create(CameraManager.this.mDevice, SensorType.DEPTH);
            List rgbVideoModes = CameraManager.this.mRGBStream.getSensorInfo().getSupportedVideoModes();
            Iterator depthVideoModes = rgbVideoModes.iterator();

            int X;
            while(depthVideoModes.hasNext()) {
                VideoMode mode = (VideoMode)depthVideoModes.next();
                int mode1 = mode.getResolutionX();
                X = mode.getResolutionY();
                if(mode1 == CameraManager.this.mWidth && X == CameraManager.this.mHeight && mode.getPixelFormat() == PixelFormat.RGB888) {
                    CameraManager.this.mRGBStream.setVideoMode(mode);
                }
            }

            List depthVideoModes1 = CameraManager.this.mDepthStream.getSensorInfo().getSupportedVideoModes();
            Iterator mode2 = depthVideoModes1.iterator();

            while(mode2.hasNext()) {
                VideoMode mode3 = (VideoMode)mode2.next();
                X = mode3.getResolutionX();
                int Y = mode3.getResolutionY();
                if(X == CameraManager.this.mWidth && Y == CameraManager.this.mHeight && mode3.getPixelFormat() == PixelFormat.DEPTH_1_MM) {
                    CameraManager.this.mDepthStream.setVideoMode(mode3);
                }
            }

            if(CameraManager.this.mDevice.isImageRegistrationModeSupported(ImageRegistrationMode.DEPTH_TO_COLOR)) {
                CameraManager.this.mDevice.setImageRegistrationMode(ImageRegistrationMode.DEPTH_TO_COLOR);
            }

            CameraManager.this.mDevice.setDepthColorSyncEnabled(true);
            CameraManager.this.mRGBIntData = new int[CameraManager.this.mWidth * CameraManager.this.mHeight];
            CameraManager.this.mRGBByteData = new byte[CameraManager.this.mWidth * CameraManager.this.mHeight * 3];
            CameraManager.this.mDepthShortData = new short[CameraManager.this.mWidth * CameraManager.this.mHeight];
            CameraManager.this.isCameraInit = true;
            CameraManager.this.mPreviewCallback.onCameraOpened(CameraManager.this.mWidth, CameraManager.this.mHeight);
            CameraManager.this.resumeCamera();
        }

        public void onDeviceOpenFailed(String s) {
        }
    };

    private CameraManager(Context context) {
        if(context == null) {
            throw new IllegalArgumentException("Context can\'t be null");
        } else {
            this.isCameraInit = false;
            this.isCameraRunning = false;
            this.mContext = context.getApplicationContext();
            this.mRGBStreamList = new ArrayList();
            this.mDepthStreamList = new ArrayList();
        }
    }

    public static synchronized CameraManager getInstance(Context context) {
        if(mCameraInterface == null) {
            mCameraInterface = new CameraManager(context);
        }

        return mCameraInterface;
    }

    private void startCaptureThread() {
        this.mCaptureThread = new Thread() {
            public void run() {
                while(CameraManager.this.isCameraInit && CameraManager.this.isCameraRunning) {
                    try {
                        OpenNI.waitForAnyStream(CameraManager.this.mRGBStreamList, 2000);
                        OpenNI.waitForAnyStream(CameraManager.this.mDepthStreamList, 2000);
                    } catch (TimeoutException var5) {
                        var5.printStackTrace();
                        continue;
                    }

                    if(CameraManager.this.isRendering) {
                        VideoFrameRef rgbFrame = CameraManager.this.mRGBStream.readFrame();
                        ByteBuffer rgbBuf = rgbFrame.getData();
                        rgbBuf.get(CameraManager.this.mRGBByteData);
                        OrbbecUtils.ByteToRGBData(CameraManager.this.mRGBByteData, CameraManager.this.mRGBIntData, CameraManager.this.mWidth, CameraManager.this.mHeight, 0);
                        rgbFrame.release();
                        VideoFrameRef depthFrame = CameraManager.this.mDepthStream.readFrame();
                        ByteBuffer depthBuf = depthFrame.getData();
                        depthBuf.asShortBuffer().get(CameraManager.this.mDepthShortData);
                        depthFrame.release();
                        CameraManager.this.previewCallback(CameraManager.this.mRGBIntData, CameraManager.this.mRGBByteData, CameraManager.this.mDepthShortData, CameraManager.this.mWidth, CameraManager.this.mHeight);
                    }
                }

            }
        };
        this.mCaptureThread.start();
    }

    private void stopCaptureThread() {
        if(this.mCaptureThread != null) {
            try {
                this.mCaptureThread.join();
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }
        }

    }

    private void previewCallback(int[] img, byte[] data, short[] depth, int x, int y) {
        if(this.isCameraRunning & this.mPreviewCallback != null) {
            this.mPreviewCallback.onPreviewFrame(img, data, depth, x, y);
        }

    }

    public void setPreviewCallback(ICameraPreviewCallback callback) {
        this.mPreviewCallback = callback;
    }

    public void openCamera() {
        if(!this.isCameraInit) {
            this.mOpenNIHelper = new OpenNIHelper(this.mContext);
            OpenNI.setLogAndroidOutput(true);
            OpenNI.setLogMinSeverity(0);
            OpenNI.initialize();
            if(this.getDevList().isEmpty()) {
                if(this.mCameraCallback != null) {
                    this.mCameraCallback.onError(1014);
                }

            } else {
                List devices = OpenNI.enumerateDevices();
                if(devices.isEmpty()) {
                    if(this.mCameraCallback != null) {
                        this.mCameraCallback.onError(1014);
                    }

                } else {
                    int vid = ((DeviceInfo)devices.get(0)).getUsbVendorId();
                    int pid = ((DeviceInfo)devices.get(0)).getUsbProductId();
                    Log.v("AstraCamera", "devices: " + Integer.toHexString(vid) + ", " + Integer.toHexString(pid));
                    String uri = ((DeviceInfo)devices.get(devices.size() - 1)).getUri();
                    Log.e("AstraCamera", "device, uri " + uri);
                    this.mOpenNIHelper.requestDeviceOpen(uri, this.mPermissionCallback);
                }
            }
        }
    }

    public void startPreview() {
        this.resumeCamera();
    }

    public void stopCamera() {
        this.pauseCamera();

        if(this.mDevice != null) {
            this.mDevice.close();
        }

        this.isCameraInit = false;
    }

    public void setListener(ICameraCallback listener) {
        this.mCameraCallback = listener;
    }

    public synchronized void resumeCamera() {
        if(this.isCameraInit && !this.isCameraRunning) {
            this.isCameraRunning = true;
            this.isRendering = true;
            this.mRGBStreamList.add(this.mRGBStream);
            this.mDepthStreamList.add(this.mDepthStream);
            this.mRGBStream.start();
            this.mDepthStream.start();
            this.startCaptureThread();
        }
    }

    public void pauseCamera() {
        if(this.isCameraRunning) {
            this.isRendering = false;
            this.isCameraRunning = false;
            this.stopCaptureThread();
            if(this.mRGBStream != null) {
                this.mRGBStream.stop();
            }

            if(this.mDepthStream != null) {
                this.mDepthStream.stop();
            }

            if(this.mRGBStreamList != null) {
                this.mRGBStreamList.clear();
            }

            if(this.mDepthStreamList != null) {
                this.mDepthStreamList.clear();
            }

        }
    }

    public void stopRender() {
        this.isRendering = false;
    }

    public void startRender() {
        this.isRendering = true;
    }

    @SuppressWarnings("WrongConstant")
    private HashMap<String, UsbDevice> getDevList() {
        UsbManager manager = (UsbManager)this.mContext.getSystemService("usb");
        HashMap deviceList = manager.getDeviceList();
        Iterator iterator = deviceList.values().iterator();

        while(true) {
            int vendorId;
            int productId;
            do {
                do {
                    if(!iterator.hasNext()) {
                        return deviceList;
                    }

                    UsbDevice device = (UsbDevice)iterator.next();
                    vendorId = device.getVendorId();
                    productId = device.getProductId();
                } while(vendorId == 7463 && (productId == 1532 || productId != 1537));
            } while(vendorId == 11205 && productId >= 1025 && productId <= 1279);

            iterator.remove();
        }
    }
}
