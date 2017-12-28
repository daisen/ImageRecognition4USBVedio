package com.hisense.imagerecognition4usbvedio.component;

/**
 * Created by huangshengtao on 2017-12-22.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import com.hisense.imagerecognition4usbvedio.CameraManager;
import com.hisense.imagerecognition4usbvedio.ICameraCallback;
import com.hisense.imagerecognition4usbvedio.ICameraPreviewCallback;
import com.hisense.imagerecognition4usbvedio.util.DisplayUtil;
import com.hisense.imagerecognition4usbvedio.util.IRLog;
import com.hisense.imagerecognition4usbvedio.util.ImageUtils;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class CameraSurfaceView extends SurfaceView implements Callback {
    private Bitmap mBufferBitmap;
    Context mContext;
    SurfaceHolder mSurfaceHolder;
    float mPreviewRate;
    CameraSurfaceView.Listener mSurfaceViewListener = null;
    private Bitmap mCacheBitmap;
    private int mScale = 0;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context.getApplicationContext();
        this.mPreviewRate = DisplayUtil.getScreenRate(this.mContext);
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.setFormat(-2);
        this.mSurfaceHolder.setType(3);
        this.mSurfaceHolder.addCallback(this);
    }

    public void init() {
        CameraManager.getInstance(this.mContext).openCamera();
        CameraManager.getInstance(this.mContext).setListener(new ICameraCallback() {
            public void onError(int error) {
                if(CameraSurfaceView.this.mSurfaceViewListener != null) {
                    CameraSurfaceView.this.mSurfaceViewListener.onSurfaceError(error);
                }

            }
        });
        CameraManager.getInstance(this.mContext).setPreviewCallback(new ICameraPreviewCallback() {
            @Override
            public void onCameraOpened(int width, int height) {
                CameraSurfaceView.this.mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }

            public void onPreviewFrame(int[] img, byte[] data, short[] depth, int x, int y) {

                if(CameraSurfaceView.this.mSurfaceViewListener != null) {
                    Mat imgMat = CameraSurfaceView.this.mSurfaceViewListener.onPreviewFrame(img, data, depth, x, y,
                            CameraSurfaceView.this.mSurfaceHolder.getSurfaceFrame().width(), CameraSurfaceView.this.mSurfaceHolder.getSurfaceFrame().height());
                    if (imgMat != null) {
                        CameraSurfaceView.this.renderPreview(imgMat);
                        return;
                    }
                }
            }

            public void onPreviewFrame(byte[] yuv, Camera camera) {
            }
        });
    }

    public int getScale() {
        return mScale;
    }

    public void setScale(int mScale) {
        this.mScale = mScale;
    }

    public void surfaceCreated(final SurfaceHolder holder) {
        try {
            CameraManager.getInstance(this.mContext).resumeCamera();
        } catch (Exception var3) {
            if(this.mSurfaceViewListener != null) {
                this.mSurfaceViewListener.onSurfaceError(1014);
            }

            return;
        }

        if(this.mSurfaceViewListener != null) {
            this.mSurfaceViewListener.surfaceCreated();
        }

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        IRLog.i("surfaceChanged...");
        if(this.mSurfaceViewListener != null) {
            this.mSurfaceViewListener.surfaceChanged(width, height);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        IRLog.i("surfaceDestroyed...");
        if (mBufferBitmap != null) {
            mBufferBitmap.recycle();
        }

        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
        }
        CameraManager.getInstance(this.mContext).pauseCamera();
        if(this.mSurfaceViewListener != null) {
            this.mSurfaceViewListener.surfaceDestroyed();
        }
    }

    public void renderPreview(int[] data) {
        renderPreview(data, 640, 480);
    }

     public void renderPreview(int[] data, int srcWidth, int srcHeight) {
        if(mSurfaceHolder != null) {
            Canvas canvas = null;

            try {

                int dstWidth = this.getWidth();
                int dstHeight = this.getHeight();

                canvas = mSurfaceHolder.lockCanvas();
                if(canvas != null) {
                    Paint e = new Paint();
                    e.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    canvas.drawPaint(e);
                    this.mBufferBitmap.setPixels(data, 0, srcWidth, 0, 0, srcWidth, srcHeight);
                    Rect srcRect = ImageUtils.cropImage(srcWidth, srcHeight, dstWidth, dstHeight);
                    Rect dstRect = new Rect(0, 0, dstWidth, dstHeight);
                    if(this.mBufferBitmap != null) {
                        canvas.drawBitmap(this.mBufferBitmap, srcRect, dstRect, (Paint)null);
                    }

                    this.mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            } catch (Exception var6) {
                if(canvas != null && this.mSurfaceHolder != null) {
                    this.mSurfaceHolder.unlockCanvasAndPost(canvas);
                }

                var6.printStackTrace();
            }
        }

    }

    protected void renderPreview(Mat modified) {

        try {
            boolean bmpValid = true;
            if (modified != null) {
                try {
                    Utils.matToBitmap(modified, mCacheBitmap);
                } catch(Exception e) {
                    IRLog.e("Mat type: " + modified);
                    IRLog.e("Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
                    IRLog.e("Utils.matToBitmap() throws an exception: " + e.getMessage());
                    bmpValid = false;
                }
            }

            if (bmpValid && mCacheBitmap != null) {
                Canvas canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                    IRLog.e("mStretch value: " + mScale);

                    if (mScale != 0) {
                        canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                                new Rect((int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2),
                                        (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2),
                                        (int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2 + mScale*mCacheBitmap.getWidth()),
                                        (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2 + mScale*mCacheBitmap.getHeight())), null);
                    } else {
                        canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                                new Rect((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
                                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2,
                                        (canvas.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
                                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2 + mCacheBitmap.getHeight()), null);
                    }

                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        } finally {
            if (modified != null) {
                modified.release();
            }
        }
    }

    public SurfaceHolder getSurfaceHolder() {
        return this.mSurfaceHolder;
    }

    public void setSurfaceViewListener(CameraSurfaceView.Listener listener) {
        this.mSurfaceViewListener = listener;
    }

    public interface Listener {
        void surfaceCreated();

        void surfaceChanged(int width, int height);

        void surfaceDestroyed();

        void onPreviewFrame(byte[] yuv, Camera camera, int rotation, int number);

        Mat onPreviewFrame(int[] rgb, byte[] data, short[] depth, int x, int y, int width, int height);

        void onSurfaceError(int errCode);
    }
}