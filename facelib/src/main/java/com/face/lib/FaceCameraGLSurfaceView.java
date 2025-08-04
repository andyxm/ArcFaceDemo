package com.face.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.blankj.utilcode.util.LogUtils;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建日期: 2018/6/20.
 * 创 建 人: xm
 * 内    容:
 */
public class FaceCameraGLSurfaceView extends CameraGLSurfaceView implements View.OnTouchListener, CameraSurfaceView.OnCameraListener, Camera.AutoFocusCallback, BaseFRAbsLoop.FaceListener {
    private static final String TAG = "FaceCameraGLSurfaceView";
    private Camera mCamera;
    private CameraSurfaceView mSurfaceView;
    private int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    //    private int mCameraID= Camera.CameraInfo.CAMERA_FACING_BACK;
    // TODO: 2018/7/2 显示720目前先使用
    private int mWidth = 1280, mHeight =720, mFormat = ImageFormat.NV21;
    private int mCameraRotate;
    private boolean mCameraMirror;
    private List<AFT_FSDKFace> result = new ArrayList<>();
    private byte[] mImageNV21 = null;
    private AFT_FSDKFace mAFT_FSDKFace = null;
    private AFT_FSDKEngine engine = new AFT_FSDKEngine();
    private String appid;
    private String ft_key;

    public FaceCameraGLSurfaceView(Context context) {
        super(context);
    }

    public FaceCameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initViewEngine(CameraSurfaceView mSurfaceView, int mCameraID, int mWidth, int mHeight, int mFormat, String appid, String ft_key, GLSurfaceViewListener listener) {
        this.mSurfaceView = mSurfaceView;
        if (isZero(mWidth)) {
            this.mWidth = mWidth;
        }
        if (isZero(mHeight)) {
            this.mHeight = mHeight;
        }
        if (isZero(mFormat)) {
            this.mFormat = mFormat;
        }
        this.appid = appid;
        this.ft_key = ft_key;
        this.mCameraID = mCameraID;
        this.mListener = listener;
//        setOnTouchListener(this);
        initSurfaceView();
    }

    private boolean isZero(int number) {
        if (number == 0) {
            return true;
        }
        return false;
    }

    private void initSurfaceView() {
        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(appid, ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.e(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        mCameraRotate = mCameraID == 0 ? 0 : 180;
        mCameraMirror = mCameraID == 0 ? false : true;
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(this, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(false, false);
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO: 2018/7/4 如何还是报错可以注释或者添加try
//        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    private BaseFRAbsLoop loop;

    public void setFRAbsLoop(BaseFRAbsLoop loop) {
        this.loop = loop;
        this.loop.setmListener(this);
    }

    @Override
    public Camera setupCamera() {
        mCamera = Camera.open(mCameraID);
        try {
            LogUtils.e(TAG, "mCamera" + mCamera + "细节:" + mCamera.toString());
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mWidth, mHeight);
            parameters.setPreviewFormat(mFormat);
            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
            }
            for (Integer format : parameters.getSupportedPreviewFormats()) {
                Log.d(TAG, "FORMAT:" + format);
            }
            List<int[]> fps = parameters.getSupportedPreviewFpsRange();
            for (int[] count : fps) {
                Log.d(TAG, "T:");
                for (int data : count) {
                    Log.d(TAG, "V=" + data);
                }
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: 2018/6/28 写一个接口，出问题时候重启
        }
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
            Log.e(TAG, "w=" + mWidth + ",h=" + mHeight);
            if (mListener != null) {
                mListener.reset(mWidth, mHeight);
            }
        }
        return mCamera;
    }

    @Override
    public void setupChanged(int format, int width, int height) {

    }

    @Override
    public boolean startPreviewImmediately() {
        return true;
    }

    @Override
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
//        Log.d(TAG, "Face=" +
        // TODO: 2018/7/6 应该判断err值不是MOK就返回
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
            }
        }
        if (loop != null) {
            loop.setImageNV21(mImageNV21, mAFT_FSDKFace);
        }
        //copy rects
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        result.clear();
        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);
    }

    @Override
    public void setBitmap(Bitmap bmp) {

    }

    @Override
    public void MatchingEnd() {
        mImageNV21 = null;
        postDelayed(countDown, 5000);
    }

    private Runnable countDown = new Runnable() {
        @Override
        public void run() {
            if (loop != null) {
                loop.isInMatching = true;
            }
        }
    };

    public void onDestroy() {
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "人脸追踪 =" + err.getCode());
        if (loop != null) {
            loop.over();
            loop = null;
        }
    }

    public interface GLSurfaceViewListener {
        void reset(int width, int height);
    }

    private GLSurfaceViewListener mListener;
}
