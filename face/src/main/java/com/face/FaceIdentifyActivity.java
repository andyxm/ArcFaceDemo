package com.face;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.FaceHelper;
import com.MyApplication;
import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKGender;
import com.arcsoft.genderestimation.ASGE_FSDKVersion;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.model.StudentModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
@Deprecated
public class FaceIdentifyActivity extends AppCompatActivity implements View.OnTouchListener, CameraSurfaceView.OnCameraListener, Camera.AutoFocusCallback {
    private static final String TAG = "FaceIdentifyActivity";
    @BindView(R.id.ExtractImage)
    ImageView ExtractImage;
    @BindView(R.id.score)
    TextView scoreTV;
    @BindView(R.id.PairMatchingData)
    Button PairMatchingData;
    @BindView(R.id.mSurfaceView)
    CameraSurfaceView mSurfaceView;
    @BindView(R.id.mGLSurfaceView)
    CameraGLSurfaceView mGLSurfaceView;
    private Handler mHandler=new Handler();
    List<StudentModel> students;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_identify);
        ButterKnife.bind(this);
        initSurfaceView();
        new Thread(){
            @Override
            public void run() {
                MyApplication application = (MyApplication) getApplication();
                students=application.mFaceHelperDB.LoadLocalDoNetFileData();
            }
        }.start();

    }
    @OnClick({ R.id.PairMatchingData})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.PairMatchingData:
                initMatching();
                break;
        }
    }
    private int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    private int mWidth, mHeight, mFormat;
    private Camera mCamera;
    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    ASAE_FSDKVersion mAgeVersion = new ASAE_FSDKVersion();
    ASAE_FSDKEngine mAgeEngine = new ASAE_FSDKEngine();
    ASGE_FSDKVersion mGenderVersion = new ASGE_FSDKVersion();
    ASGE_FSDKEngine mGenderEngine = new ASGE_FSDKEngine();
    FRAbsLoop mFRAbsLoop = null;
    byte[] mImageNV21 = null;
    AFT_FSDKFace mAFT_FSDKFace = null;
    List<ASAE_FSDKAge> ages = new ArrayList<>();
    List<ASGE_FSDKGender> genders = new ArrayList<>();
    List<AFT_FSDKFace> result = new ArrayList<>();
    boolean isPostted = false;
    private void initMatching() {
        initAFT();
    }
    private void initAFT() {
        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceHelper.appid, FaceHelper.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceHelper.appid, FaceHelper.age_key);
        Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
        error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
        Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

        ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceHelper.appid, FaceHelper.gender_key);
        Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
        error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
        Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());
        mFRAbsLoop = new FRAbsLoop();
        mFRAbsLoop.start();
    }
    class FRAbsLoop extends AbsLoop {
        AFR_FSDKVersion version = new AFR_FSDKVersion();
        AFR_FSDKEngine engine = new AFR_FSDKEngine();
        AFR_FSDKFace result = new AFR_FSDKFace();
        List<ASAE_FSDKFace> face1 = new ArrayList<>();
        List<ASGE_FSDKFace> face2 = new ArrayList<>();
//        List<StudentModel> students= ((MyApplication) getApplication()).mFaceHelperDB.LoadLocalDoNetFileData();
        @Override
        public void setup() {
//            StudentModel model=new StudentModel();
//            String feature=getString(R.string.demo);
//            model.setFaceFeature(feature);
//            model.setCard("1111111111");
//            model.setFaceName("测试1111111111");
//            students.add(model);
            AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceHelper.appid, FaceHelper.fr_key);
            Log.d(TAG, "AFR_FSDK_InitialEngine = " + error.getCode());
            error = engine.AFR_FSDK_GetVersion(version);
            Log.d(TAG, "FR=" + version.toString() + "," + error.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
        }
        @Override
        public void loop() {
//            Log.e("loop","mImageNV21="+mImageNV21);
            if (mImageNV21 != null) {
                final int rotate = mCameraRotate;
                long time = System.currentTimeMillis();
                AFR_FSDKError error = engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), result);
                Log.d(TAG, "AFR_FSDK_ExtractFRFeature cost :" + (System.currentTimeMillis() - time) + "ms");
                Log.d(TAG, "Face=" + result.getFeatureData()[0] + "," + result.getFeatureData()[1] + "," + result.getFeatureData()[2] + "," + error.getCode());
                final AFR_FSDKMatching score = new AFR_FSDKMatching();

                float max = 0.0f;
                String name = null;
                Log.e(TAG, "students 数量是" + students.size());
                for (StudentModel student : students) {
                    AFR_FSDKFace faceFeature = student.getFaceFeature();
                    Log.e(TAG,"卡号:"+student.getFaceCard()+"length="+faceFeature.getFeatureData().length+"采集的长度:"+result.getFeatureData().length);
//                    byte[] faceFeatureByte = ConvertUtils.hexString2Bytes(faceFeature);
//                    AFR_FSDKFace face=new AFR_FSDKFace(faceFeatureByte);
                    error = engine.AFR_FSDK_FacePairMatching(result, faceFeature, score);
                    Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                    if (max < score.getScore()) {
                        max = score.getScore();
                        name = student.getFaceCard();
                    }
                }
                face1.clear();
                face2.clear();
                face1.add(new ASAE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
                face2.add(new ASGE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
                ASAE_FSDKError error1 = mAgeEngine.ASAE_FSDK_AgeEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face1, ages);
                ASGE_FSDKError error2 = mGenderEngine.ASGE_FSDK_GenderEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face2, genders);
                Log.d(TAG, "ASAE_FSDK_AgeEstimation_Image:" + error1.getCode() + ",ASGE_FSDK_GenderEstimation_Image:" + error2.getCode());
                Log.d(TAG, "age:" + ages.get(0).getAge() + ",gender:" + genders.get(0).getGender());
                final String age = ages.get(0).getAge() == 0 ? "年龄未知" : ages.get(0).getAge() + "岁";
                final String gender = genders.get(0).getGender() == -1 ? "性别未知" : (genders.get(0).getGender() == 0 ? "男" : "女");
                //crop
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
                if (max > 0.6f) {
                    //fr success.
                    final float max_score = max;
                    Log.d(TAG, "fit Score:" + max + ", NAME:" + name);
                    final String mNameShow = name;
                    mHandler.removeCallbacks(hide);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            StringBuffer sb = new StringBuffer();
                            sb.append(mNameShow).append("置信度：" + (float) ((int) (max_score * 1000)) / 1000.0);
                            scoreTV.setAlpha(1.0f);
                            scoreTV.setText(sb.toString());
                            scoreTV.setTextColor(Color.RED);
                            scoreTV.setVisibility(View.VISIBLE);
                            scoreTV.setTextColor(Color.RED);
                            ExtractImage.setRotation(rotate);
                            if (mCameraMirror) {
                                ExtractImage.setScaleY(-1);
                            }
                            ExtractImage.setImageAlpha(255);
                            ExtractImage.setImageBitmap(bmp);
                        }
                    });
                } else {
                    final String mNameShow = "未识别";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            StringBuffer sb = new StringBuffer();
                            sb.append(mNameShow).append(gender + "," + age);
                            scoreTV.setAlpha(1.0f);
                            scoreTV.setVisibility(View.VISIBLE);
                            scoreTV.setTextColor(Color.RED);
                            scoreTV.setText(mNameShow);
                            scoreTV.setTextColor(Color.RED);
                            ExtractImage.setImageAlpha(255);
                            ExtractImage.setRotation(rotate);
                            if (mCameraMirror) {
                                ExtractImage.setScaleY(-1);
                            }
                            ExtractImage.setImageBitmap(bmp);
                        }
                    });
                }
                mImageNV21 = null;
            }

        }

        @Override
        public void over() {
            AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
            Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
        }
    }
    Runnable hide = new Runnable() {
        @Override
        public void run() {
            scoreTV.setAlpha(0.5f);
            ExtractImage.setImageAlpha(128);
            isPostted = false;
        }
    };
    private void initSurfaceView() {
            initM();
    }

    private void initM() {

        mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        //换成前置是因为使用小米手机测试
//        mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCameraRotate = mCameraID == 0 ? 0 : 180;
        mCameraMirror = mCameraID == 0 ? false : true;
        DisplayMetrics d = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(d);
        mWidth = d.widthPixels;
        mHeight = 960;
        mFormat = ImageFormat.NV21;
        mGLSurfaceView.setOnTouchListener(this);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(true, false);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mFRAbsLoop!=null){
            mFRAbsLoop.shutdown();
        }
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

        ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
        Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

        ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
        Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
    }
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.d(TAG, "Camera Focus SUCCESS!");
        }
    }
    @Override
    public Camera setupCamera() {
        mCamera = Camera.open(mCameraID);
        try {
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
        }
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
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
        Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        Log.d(TAG, "Face=" + result.size());
        for (AFT_FSDKFace face : result) {
            Log.d(TAG, "Face:" + face.toString());
        }
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
            } else {
                if (!isPostted) {
                    mHandler.removeCallbacks(hide);
                    mHandler.postDelayed(hide, 2000);
                    isPostted = true;
                }
            }
        }
        //copy rects
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        //clear result.
        result.clear();
        //return the rects for render.
        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        mGLSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }
}
