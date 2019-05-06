// Copyright 2019 JD.com Inc. JD AI

package me.daquexian.dabnn;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import me.daquexian.dabnn.Net;
import pub.devrel.easypermissions.EasyPermissions;

public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";

    private TextView textView;
    private CameraBridgeViewBase mOpenCvCameraView;
    private List<String> synsetWords = new ArrayList<>();
    private Net model;
    private final String[] perms = {Manifest.permission.CAMERA};
    private final int REQUEST_CODE = 321;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_camera);

        textView = findViewById(R.id.result);

        mOpenCvCameraView = findViewById(R.id.java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getAssets().open("synset_words.txt")
            ));
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                synsetWords.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        initModel();
        if (!EasyPermissions.hasPermissions(this, perms)) {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "Please grant",
                    REQUEST_CODE, perms);
        }
    }

    void initModel() {
        model = new Net().readAsset(getAssets(), "model_imagenet_stem.dab");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if (model != null) {
            model.dispose();
        }
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int INPUT_SIDE_LENGTH = 224;
        Mat rgba = inputFrame.rgba();
        Mat imageMat = new Mat();
        Imgproc.cvtColor(rgba, imageMat, Imgproc.COLOR_RGBA2RGB);
        imageMat = scaleAndCenterCrop(imageMat, INPUT_SIDE_LENGTH);
        imageMat.convertTo(imageMat, CvType.CV_32FC3, 1. / 255);
        imageMat = normalize(imageMat,
                new Scalar(0.485, 0.456, 0.406), new Scalar(0.229, 0.224, 0.225));

        float[] inputData = new float[imageMat.width() * imageMat.height() * imageMat.channels()];

        imageMat.get(0, 0, inputData);

        long startTime = System.currentTimeMillis();
        model.predict(inputData);
        float[] result = model.getBlob("216");
        long endTime = System.currentTimeMillis();

        int predictNumber = getMaxIndex(result);

        runOnUiThread(() -> {
            final String text = "Time: " + (endTime - startTime) + "ms, result: " + synsetWords.get(predictNumber);
            textView.setText(text);
        });

        return rgba;
    }

    /**
     *
     * @param mat const image mat, 32FC3
     * @param mean mean value scalar
     * @param std standard deviation scalar
     * @return the normalized mat
     */
    private Mat normalize(Mat mat, Scalar mean, Scalar std) {
        Mat _mat = mat.clone();
        Core.subtract(_mat, mean, _mat);
        Core.divide(_mat, std, _mat);
        return _mat;
    }

    private Mat scaleAndCenterCrop(Mat mat, int sideLength) {
        Mat _mat = mat.clone();

        double rate;
        if (_mat.height() > _mat.width()) {
            rate = 1. * sideLength / _mat.width();
        } else {
            rate = 1. * sideLength / _mat.height();
        }

        Imgproc.resize(_mat, _mat, new Size(0, 0), rate, rate, Imgproc.INTER_LINEAR);

        if (_mat.height() > _mat.width()) {
            _mat = new Mat(_mat, new Rect(0, (_mat.height() - _mat.width()) / 2, _mat.width(), _mat.width()));
        } else {
            _mat = new Mat(_mat, new Rect((_mat.width() - _mat.height()) / 2, 0, _mat.height(), _mat.height()));
        }
        return _mat;
    }

    private int getMaxIndex(float[] arr) {
        if (arr.length == 0) {
            return -1;
        }
        float max = arr[0];
        int maxIndex = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
