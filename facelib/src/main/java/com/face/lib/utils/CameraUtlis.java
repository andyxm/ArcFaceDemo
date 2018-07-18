package com.face.lib.utils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;


import com.face.lib.listener.CameraListener;

import java.io.IOException;
import java.util.List;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public class CameraUtlis {
    private static final String TAG = "CameraUtlis";
    private Camera mCamera;
    private Camera.Parameters mParams;
//    private boolean isPreviewing = false;
    private float mPreviwRate = -1f;
    private static CameraUtlis mCameraInterface;
    private CameraListener listener;
    public  void setCameraListener(CameraListener listener){
        this.listener=listener;
    }
    public interface CamOpenOverCallback {
         void cameraHasOpened();
    }

    private CameraUtlis() {

    }

    public static synchronized CameraUtlis getInstance() {
        if (mCameraInterface == null) {
            mCameraInterface = new CameraUtlis();
        }
        return mCameraInterface;
    }

    /**
     * 打开Camera
     *
     * @param callback
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressLint("NewApi")
    public void doOpenCamera(CamOpenOverCallback callback) {
        Log.i(TAG, "Camera open....");
//		mCamera = Camera.open();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
//		    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) { // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置  
            try {
                mCamera = Camera.open(camIdx);
                break;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
//		    } 
        }
        Log.i(TAG, "Camera open over....");
        callback.cameraHasOpened();
    }

    /**
     * 开启预览
     *
     * @param holder
     * @param previewRate
     */
    public void doStartPreview(SurfaceHolder holder, float previewRate) {
        Log.i(TAG, "doStartPreview...");
//        if (isPreviewing) {
//            mCamera.stopPreview();
//            return;
//        }
        if (mCamera != null) {
            mParams = mCamera.getParameters();
            mParams.setPictureFormat(PixelFormat.JPEG);//设置拍照后存储的图片格式
//            CamParaUtil.getInstance().printSupportPictureSize(mParams);
//            CamParaUtil.getInstance().printSupportPreviewSize(mParams);
            //设置PreviewSize和PictureSize
			Camera.Size pictureSize = CamParaUtil.getInstance().getPropPictureSize(
					mParams.getSupportedPictureSizes(),previewRate, 800);
			mParams.setPictureSize(pictureSize.width, pictureSize.height);
			Camera.Size previewSize = CamParaUtil.getInstance().getPropPreviewSize(
					mParams.getSupportedPreviewSizes(), previewRate, 800);
			mParams.setPreviewSize(previewSize.width, previewSize.height);

//			mCamera.setDisplayOrientation(90);
            mCamera.setDisplayOrientation(0);
            CamParaUtil.getInstance().printSupportFocusMode(mParams);
            List<String> focusModes = mParams.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                //CAMERA_FACING_FRONT
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mCamera.setParameters(mParams);
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();//开启预览
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//            isPreviewing = true;
            mPreviwRate = previewRate;
            mParams = mCamera.getParameters(); //重新get一次
//			Log.i(TAG, "最终设置:PreviewSize--With = " + mParams.getPreviewSize().width
//					+ "Height = " + mParams.getPreviewSize().height);
//			Log.i(TAG, "最终设置:PictureSize--With = " + mParams.getPictureSize().width
//					+ "Height = " + mParams.getPictureSize().height);
        }
    }

    /**
     * 停止预览，释放Camera
     */
    public void doStopCamera() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
//            isPreviewing = false;
            mPreviwRate = -1f;
            mCamera.release();
//			mCamera.lock();
            mCamera = null;
        }
    }
    /**
     * 拍照
     */
    public void doTakePicture() {
//        if (isPreviewing && (mCamera != null)) {
        if (mCamera != null) {
            mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);
        }
    }

    /*为了实现拍照的快门声音及拍照保存照片需要下面三个回调变量*/
    ShutterCallback mShutterCallback = new ShutterCallback()
            //快门按下的回调，在这里我们可以设置类似播放“咔嚓”声之类的操作。默认的就是咔嚓。
{
        public void onShutter() {
            Log.i(TAG, "myShutterCallback:onShutter...");
        }
    };
    PictureCallback mRawCallback = new PictureCallback()
            // 拍摄的未压缩原数据的回调,可以为null
    {

        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "myRawCallback:onPictureTaken...");
        }
    };
    PictureCallback mJpegPictureCallback = new PictureCallback(){
            //对jpeg图像数据的回调,最重要的一个回调{
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            Log.i(TAG, "myJpegCallback:onPictureTaken...");
            Bitmap b = null;
            if (null != data) {
                b = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图
//                mCamera.stopPreview();
//                isPreviewing = false;
            }
            //保存图片到sdcard
            if (null != b) {
                //设置FOCUS_MODE_CONTINUOUS_VIDEO)之后，myParam.set("rotation", 90)失效。
                //图片竟然不能旋转了，故这里要旋转下
//				Bitmap rotaBitmap = ImageUtil.getRotateBitmap(b, 90.0f);
                Bitmap rotaBitmap = ImageUtil.getRotateBitmap(b, 0.0f);
//                Bitmap rotaBitmap=ImageUtils.rotate(b,0,0,0,true);
                listener.TakePicture(rotaBitmap);
            }
            //再次进入预览
            mCamera.startPreview();
//            isPreviewing = true;
        }
    };
}
