package com.hisense.imagerecognition4usbvedio;

import android.content.Context;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.hisense.imagerecognition4usbvedio.component.CameraSurfaceView;
import com.hisense.imagerecognition4usbvedio.util.IRLog;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VedioCaptureActivity extends AppCompatActivity {

    @BindView(R.id.camera_view)
    CameraSurfaceView mCameraView;

    @BindView(R.id.chk_face)
    CheckBox chkFace;

    @BindView(R.id.chk_palm)
    CheckBox chkPalm;

    private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255);
    private int learn_frames = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;
    private Mat teplateR;
    private Mat teplateL;
    private boolean enableFaceCascade = false;
    private boolean enablePalmCascade = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vedio_capture);
        ButterKnife.bind(this);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        initView();
    }

    private void initView() {
        mCameraView.init();
        mCameraView.setSurfaceViewListener(new CameraSurfaceView.Listener() {
            public void surfaceCreated() {
                IRLog.i("surfaceCreated");
            }

            public void surfaceChanged(int previewWidth, int previewHeight) {

            }

            public void surfaceDestroyed() {
            }

            public void onPreviewFrame(byte[] yuv, Camera camera, int rotation, int cameraNumber) {
            }

            public Mat onPreviewFrame(int[] rgb, byte[] data, short[] depth, int x, int y, int previewX, int previewY) {
                try {
                    Mat previewFrame = new Mat(y, x, CvType.CV_8UC3);
                    previewFrame.put(0, 0, data);
                    findFace(previewFrame);
                    findPalmOrFist(previewFrame);
                    return previewFrame;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                return null;
            }

            public void onSurfaceError(int errorCode) {
                if (errorCode == 1014) {
                    IRLog.e(Integer.toString(errorCode));
                }
            }
        });

        chkFace.setChecked(enableFaceCascade);
        chkFace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableFaceCascade = isChecked;
            }
        });

        chkPalm.setChecked(enablePalmCascade);
        chkPalm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enablePalmCascade = isChecked;
            }
        });
    }

    ///人脸识别开始

    public void findFace(Mat rgbMat) {
        if (!enableFaceCascade || mJavaDetectorFontalFace == null) {
            return;
        }

        Mat grayMat = rgbMat.submat(0, rgbMat.rows(), 0, rgbMat.cols());

        try {
            float fltRelativeFaceSize = 0.2f;
            int height = grayMat.rows();
            int intAbsoluteFaceSize = 0;
            if (Math.round(height * fltRelativeFaceSize) > 0) {
                intAbsoluteFaceSize = Math.round(height * fltRelativeFaceSize);
            }

            MatOfRect faces = new MatOfRect();


            mJavaDetectorFontalFace.detectMultiScale(grayMat, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(), new Size()); //minsize new Size(intAbsoluteFaceSize, fltRelativeFaceSize)

            Rect[] facesArray = faces.toArray();
            for (int i = 0; i < facesArray.length; i++) {
                Imgproc.rectangle(rgbMat, facesArray[i].tl(), facesArray[i].br(),
                        RECT_COLOR, 3);
                double xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
                double yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
                Point center = new Point(xCenter, yCenter);

                Imgproc.circle(rgbMat, center, 10, new Scalar(255, 0, 0, 255), 3);

                Imgproc.putText(rgbMat, "[" + center.x + "," + center.y + "]",
                        new Point(center.x + 20, center.y + 20),
                        Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
                                255));

                Rect r = facesArray[i];
                // compute the eye area
                Rect eyearea = new Rect(r.x + r.width / 8,
                        (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
                        (int) (r.height / 3.0));
                // split it
                Rect eyearea_right = new Rect(r.x + r.width / 16,
                        (int) (r.y + (r.height / 4.5)),
                        (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
                Rect eyearea_left = new Rect(r.x + r.width / 16
                        + (r.width - 2 * r.width / 16) / 2,
                        (int) (r.y + (r.height / 4.5)),
                        (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
                // draw the area - mGray is working grayscale mat, if you want to
                // see area in rgb preview, change mGray to mRgba
                Imgproc.rectangle(rgbMat, eyearea_left.tl(), eyearea_left.br(),
                        new Scalar(255, 0, 0, 255), 2);
                Imgproc.rectangle(rgbMat, eyearea_right.tl(), eyearea_right.br(),
                        new Scalar(255, 0, 0, 255), 2);

                if (learn_frames < 5) {
                    if (teplateL != null) {

                        teplateR.release();
                        teplateL.release();
                    }
                    teplateR = get_template(grayMat, rgbMat, mJavaDetectorEye, eyearea_right, 24);
                    teplateL = get_template(grayMat, rgbMat, mJavaDetectorEye, eyearea_left, 24);
                    learn_frames++;
                } else {
                    // Learning finished, use the new templates for template
                    // matching
                    match_eye(rgbMat, grayMat, eyearea_right, teplateR, 0);
                    match_eye(rgbMat, grayMat, eyearea_left, teplateL, 0);

                }


                // cut eye areas and put them to zoom windows
//            Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2,
//                    mZoomWindow2.size());
//            Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow,
//                    mZoomWindow.size());


            }

        } finally {
            grayMat.release();
        }
    }

    private void match_eye(Mat rgbMat, Mat grayMat, Rect area, Mat mTemplate, int type) {
        Point matchLoc;
        Mat mROI = grayMat.submat(area);
        try {

            int result_cols = mROI.cols() - mTemplate.cols() + 1;
            int result_rows = mROI.rows() - mTemplate.rows() + 1;
            // Check for bad template size
            if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
                return;
            }
            Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

            switch (type) {
                case TM_SQDIFF:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                    break;
                case TM_SQDIFF_NORMED:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult,
                            Imgproc.TM_SQDIFF_NORMED);
                    break;
                case TM_CCOEFF:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                    break;
                case TM_CCOEFF_NORMED:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult,
                            Imgproc.TM_CCOEFF_NORMED);
                    break;
                case TM_CCORR:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                    break;
                case TM_CCORR_NORMED:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult,
                            Imgproc.TM_CCORR_NORMED);
                    break;
            }

            Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
            // there is difference in matching methods - best match is max/min value
            if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
                matchLoc = mmres.minLoc;
            } else {
                matchLoc = mmres.maxLoc;
            }

            Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
            Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                    matchLoc.y + mTemplate.rows() + area.y);

            Imgproc.rectangle(rgbMat, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
                    255));
            Rect rec = new Rect(matchLoc_tx, matchLoc_ty);
        } finally {
            mROI.release();
        }
    }

    private Mat get_template(Mat rgbMat, Mat grayMat, CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = grayMat.submat(area);
        try {
            MatOfRect eyes = new MatOfRect();
            Point iris = new Point();
            Rect eye_template = new Rect();
            clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                    Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                            | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                    new Size());

            Rect[] eyesArray = eyes.toArray();
            for (int i = 0; i < eyesArray.length; ) {
                Rect e = eyesArray[i];
                e.x = area.x + e.x;
                e.y = area.y + e.y;
                Rect eye_only_rectangle = new Rect((int) e.tl().x,
                        (int) (e.tl().y + e.height * 0.4), (int) e.width,
                        (int) (e.height * 0.6));
                mROI = grayMat.submat(eye_only_rectangle);
                Mat vyrez = rgbMat.submat(eye_only_rectangle);


                Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

                Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
                iris.x = mmG.minLoc.x + eye_only_rectangle.x;
                iris.y = mmG.minLoc.y + eye_only_rectangle.y;
                eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                        - size / 2, size, size);
                Imgproc.rectangle(rgbMat, eye_template.tl(), eye_template.br(),
                        new Scalar(255, 0, 0, 255), 2);
                template = (grayMat.submat(eye_template)).clone();
                return template;
            }
            return template;
        } finally {
            mROI.release();
        }

    }

    ///人脸识别结束


    ///手掌和拳头识别开始

    public void findPalmOrFist(Mat rgbMat) {
        if (!enablePalmCascade || mJavaDetectorPalm == null || mJavaDetectorFist == null) {
            return;
        }

        Mat grayMat = rgbMat.submat(0, rgbMat.rows(), 0, rgbMat.cols());
        try {

            float fltRelativeFaceSize = 0.1f;
            int height = grayMat.rows();
            int intAbsoluteFaceSize = 0;
            if (Math.round(height * fltRelativeFaceSize) > 0) {
                intAbsoluteFaceSize = Math.round(height * fltRelativeFaceSize);
            }

            Size minSize = new Size(intAbsoluteFaceSize, fltRelativeFaceSize);

            MatOfRect palms = new MatOfRect();
            mJavaDetectorPalm.detectMultiScale(grayMat, palms, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    minSize, new Size()); //minsize new Size(intAbsoluteFaceSize, fltRelativeFaceSize)

            Rect[] palmsArray = palms.toArray();
            for (int i = 0; i < palmsArray.length; i++) {
                Imgproc.rectangle(rgbMat, palmsArray[i].tl(), palmsArray[i].br(),
                        RECT_COLOR, 3);
            }

            //fist rectangle

            MatOfRect fists = new MatOfRect();
            mJavaDetectorFist.detectMultiScale(grayMat, palms, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    minSize, new Size()); //minsize new Size(intAbsoluteFaceSize, fltRelativeFaceSize)

            Rect[] fistsArray = fists.toArray();
            for (int i = 0; i < fistsArray.length; i++) {
                Rect fist = fistsArray[i];
                Imgproc.rectangle(rgbMat, fist.tl(), fist.br(),
                        RECT_COLOR, 3);
            }


        } finally {
            grayMat.release();
        }

    }

    ///手掌和拳头识别结束

    @Override
    protected void onPause() {
        super.onPause();
    }

    /// OPENCV开始

    private CascadeClassifier mJavaDetectorFontalFace;
    private CascadeClassifier mJavaDetectorEye;
    private CascadeClassifier mJavaDetectorFist;
    private CascadeClassifier mJavaDetectorPalm;

    private CascadeClassifier getCascadeClassifier(int resId) {
        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(resId);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, Math.abs(resId) + ".xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            is.close();
            os.close();
            CascadeClassifier newCC = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            newCC.load(mCascadeFile.getAbsolutePath());
            if (newCC.empty()) {
                IRLog.e("Failed to load cascade classifier");
                return null;
            } else {
                IRLog.i("Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

            mCascadeFile.delete();
            return newCC;
        } catch (IOException e) {
            e.printStackTrace();
            IRLog.e("Failed to load cascade. Exception thrown: " + e);
        }

        return null;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    IRLog.i("OpenCV loaded successfully");

                    //face cascade
                    mJavaDetectorFontalFace = getCascadeClassifier(R.raw.lbpcascade_frontalface);

                    //eye cascade
                    mJavaDetectorEye = getCascadeClassifier(R.raw.haarcascade_lefteye_2splits);

                    //fist cascade
                    mJavaDetectorFist = getCascadeClassifier(R.raw.fist);

                    //palm cascade
                    mJavaDetectorPalm = getCascadeClassifier(R.raw.palm2);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            IRLog.d("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            IRLog.d("OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    ///OPENCV结束
}
