package com.face.lib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.blankj.utilcode.utils.ConvertUtils;
import com.blankj.utilcode.utils.LogUtils;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;

import java.io.IOException;
import java.util.List;
/**
 * 创建日期: 2018/6/9.
 * 创 建 人: xm
 * 内    容:
 */
public abstract class BaseFRAbsLoop<T extends BaseFaceMode> extends AbsLoop {
    private  final String TAG=getClass().toString();
    private AFR_FSDKVersion Afr_version = new AFR_FSDKVersion();
    private AFR_FSDKEngine Afr_engine = new AFR_FSDKEngine();
    private AFR_FSDKFace Afr_result = new AFR_FSDKFace();
    protected abstract float MinimumScore();
    protected abstract int IdentifyNumber();
    protected abstract boolean IsShowImage();
    public static final int SUCCESS=0x000001;
    public static final int FAIL=0x000002;
    public static final int OPEN_MATCHING=0x000003;
    private Handler mHandler;
    private AFT_FSDKFace mAFT_FSDKFace;
    private byte[] mImageNV21;
    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    private int mWidth;
    private int mHeight;
    List<T> lists;
    private String appid;
    private String ft_key;
    private String fr_key;
    public BaseFRAbsLoop(Handler mHandler, List<T> lists, int mWidth, int mHeight,String appid,String ft_key,String fr_key) {
        this.mHandler = mHandler;
        this.mWidth=mWidth;
        this.mHeight=mHeight;
        this.lists=lists;
        this.appid=appid;
        this.ft_key=ft_key;
        this.fr_key=fr_key;
        initAFT();
    }
    private void initAFT() {
        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(appid, ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());
//        ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceHelper.appid, FaceHelper.age_key);
//        Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
//        error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
//        Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());
//        ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceHelper.appid, FaceHelper.gender_key);
//        Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
//        error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
//        Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());
    }
    @Override
    public void setup() {
        AFR_FSDKError error = Afr_engine.AFR_FSDK_InitialEngine(appid,fr_key);
        Log.d(TAG, "AFR_FSDK_InitialEngine=" + error.getCode());
        error = Afr_engine.AFR_FSDK_GetVersion(Afr_version);
        Log.d(TAG, "FR=" + Afr_version.toString() + "," + error.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
    }

    public void setImageNV21(byte[] imageNV21, AFT_FSDKFace mAFT_FSDKFace) {
        this.mImageNV21 = imageNV21;
        this.mAFT_FSDKFace= mAFT_FSDKFace;
    }
    public boolean isInMatching= true;
    @Override
    public void loop() {
        if (!isInMatching) {
//            Log.e(TAG,"NOT Matching...");
            return;
        }
        if (mImageNV21 != null) {
            // TODO: 2018/7/10 mAFT_FSDKFace可能为null
//            if(mAFT_FSDKFace==null){
//            }
            long time = System.currentTimeMillis();
            AFR_FSDKError error = Afr_engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), Afr_result);
            Log.d(TAG, "AFR_FSDK_ExtractFRFeature cost :" + (System.currentTimeMillis() - time) + "ms,code="+error.getCode());
            // TODO: 2018/6/20 error.getCode()虽然对比能成功,但有时候会返回3
            final AFR_FSDKMatching score = new AFR_FSDKMatching();
            float max = 0.0f;
            List<T> matchingData = lists;// TODO: 2018/7/11 可能null
            Log.e(TAG, "students 数量是" + matchingData.size());
            for (T t : matchingData) {
                Log.e(TAG,"在比对"+t.getFaceCard());
                AFR_FSDKFace faceFeature = t.getFaceFeature();
//                byte[] featureData = faceFeature.getFeatureData();
//                String FaceFeature = ConvertUtils.bytes2HexString(featureData);
//                LogUtils.e(TAG,"Length="+featureData.length);
//                LogUtils.e(TAG,"数组是="+FaceFeature);
//                Log.e(TAG,"姓名"+t.getFaceCard()+"faceFeature="+faceFeature);
                error = Afr_engine.AFR_FSDK_FacePairMatching(Afr_result, faceFeature, score);
                Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                if (max < score.getScore()) {
                    max = score.getScore();
                }
                if(max>MinimumScore()){
                    isInMatching=false;
                    Message message=Message.obtain();
                    message.what=SUCCESS;
                    double tempScore = (float) ((int) (max * 1000)) / 1000.0;
                    t.setScore(tempScore);
                    message.obj=t;
                    if(mListener!=null){
                        mListener.MatchingEnd();
                        mImageNV21=null;
                    }
                    mHandler.sendMessage(message);
                    if (IsShowImage()) {
                        byte[] data = mImageNV21;
                        YuvImage yuv = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                        ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                        yuv.compressToJpeg(mAFT_FSDKFace.getRect(), 80, ops);
                        final Bitmap bmp = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);
                        try {
                            ops.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(mListener!=null){
                            mListener.setBitmap(bmp);
                        }
                    }
                    return;
                }
            }
            if(max<MinimumScore()){
                Message message=Message.obtain();
                message.what=FAIL;
                message.obj="未识别";
                mHandler.sendMessage(message);
                if(mListener!=null){
                    mListener.MatchingEnd();
                    mImageNV21=null;
                }
            }
            SystemClock.sleep(2000);
        }
    }
    @Override
    public void over() {
        shutdown();
        AFR_FSDKError error = Afr_engine.AFR_FSDK_UninitialEngine();
        Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());
//        ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
//        Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());
//        ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
//        Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
    }
    public interface FaceListener{
        void setBitmap(Bitmap bmp);
        void MatchingEnd();
    }
    private FaceListener mListener;
    public void setmListener(FaceListener mListener) {
        this.mListener = mListener;
    }
}
