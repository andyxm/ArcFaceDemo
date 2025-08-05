package com.face;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.FaceDB;
import com.MyApplication;
import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
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
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.face.lib.listener.CameraListener;
import com.face.lib.utils.CameraUtlis;
import com.face.lib.utils.FileUtil;
import com.guo.android_extend.image.ImageConverter;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
@Deprecated
public class MainActivity extends AppCompatActivity implements CameraUtlis.CamOpenOverCallback, View.OnTouchListener, CameraListener, SurfaceHolder.Callback, CameraSurfaceView.OnCameraListener, Camera.AutoFocusCallback {
    private static final String TAG = "MainActivity";
    @BindView(R.id.surfaceView)
    SurfaceView surfaceView;
    @BindView(R.id.ExtractImage)
    ImageView ExtractImage;
    @BindView(R.id.score)
    TextView scoreTV;
    @BindView(R.id.TakePicture)
    Button TakePicture;
    @BindView(R.id.PairMatchingData)
    Button PairMatchingData;
//    @BindView(R.id.mSurfaceView)
//    CameraSurfaceView mSurfaceView;
    @BindView(R.id.typeSurfaceView)
    SurfaceView typeSurfaceView;
//    @BindView(R.id.mGLSurfaceView)
//    CameraGLSurfaceView mGLSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Thread thread;
    CameraSurfaceView mSurfaceView;
    CameraGLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((MyApplication) getApplication()).mFaceDB.loadFaces();
        ButterKnife.bind(this);
        initSurfaceView();
    }

    private Bitmap mBitmap;
    private Rect src = new Rect();
    private Rect dst = new Rect();
    private AFR_FSDKFace mAFR_FSDKFace;

    private void initdata() {
        if (!FileUtils.isDir(FileUtil.IMAGE_FOLDER_PATH)) {
            Log.e(TAG, "目录不存在...");
            return;
        }
        File[] files = new File(FileUtil.IMAGE_FOLDER_PATH).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                Log.e(TAG, "name=" + pathname.getName());
                return pathname.getName().endsWith(".jpg");
            }
        });
        if (files == null || files.length == 0) {
            return;
        }
        String mFilePath = files[0].getPath();
        Log.e(TAG, mFilePath);
        mBitmap = MyApplication.decodeImage(mFilePath);
        thread = new Thread() {
            @Override
            public void run() {
                byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
                try {
                    ImageConverter convert = new ImageConverter();
                    convert.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
                    if (convert.convert(mBitmap, data)) {
                        Log.d(TAG, "convert ok!");
                    }
                    convert.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AFD_FSDKEngine engine = new AFD_FSDKEngine();
                AFD_FSDKVersion version = new AFD_FSDKVersion();
                List<AFD_FSDKFace> result = new ArrayList<AFD_FSDKFace>();
                AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
                Log.d(TAG, "AFD_FSDK_InitialFaceEngine = " + err.getCode());
                Log.e(TAG, "1111111111111111111111111");
                if (err.getCode() != AFD_FSDKError.MOK) {
                    Log.e(TAG, "22222222222222222222222222");
                    Message reg = Message.obtain();
                    reg.what = MSG_CODE;
                    reg.arg1 = MSG_EVENT_FD_ERROR;
                    reg.arg2 = err.getCode();
                    mHandler.sendMessage(reg);
                } else {
                    err = engine.AFD_FSDK_GetVersion(version);
                    Log.d(TAG, "AFD_FSDK_GetVersion =" + version.toString() + ", " + err.getCode());
                    err = engine.AFD_FSDK_StillImageFaceDetection(data, mBitmap.getWidth(), mBitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, result);
                    Log.d(TAG, "AFD_FSDK_StillImageFaceDetection =" + err.getCode() + "<" + result.size());
                    while (mSurfaceHolder != null) {
                        Log.e(TAG, "3333333333333333333333");
                        Canvas canvas = mSurfaceHolder.lockCanvas();
                        if (canvas != null) {
                            Paint mPaint = new Paint();
                            boolean fit_horizontal = canvas.getWidth() / (float) src.width() < canvas.getHeight() / (float) src.height() ? true : false;
                            float scale = 1.0f;
                            if (fit_horizontal) {
                                scale = canvas.getWidth() / (float) src.width();
                                dst.left = 0;
                                dst.top = (canvas.getHeight() - (int) (src.height() * scale)) / 2;
                                dst.right = dst.left + canvas.getWidth();
                                dst.bottom = dst.top + (int) (src.height() * scale);
                            } else {
                                scale = canvas.getHeight() / (float) src.height();
                                dst.left = (canvas.getWidth() - (int) (src.width() * scale)) / 2;
                                dst.top = 0;
                                dst.right = dst.left + (int) (src.width() * scale);
                                dst.bottom = dst.top + canvas.getHeight();
                            }
                            canvas.drawBitmap(mBitmap, src, dst, mPaint);
                            canvas.save();
                            canvas.scale((float) dst.width() / (float) src.width(), (float) dst.height() / (float) src.height());
                            canvas.translate(dst.left / scale, dst.top / scale);
                            for (AFD_FSDKFace face : result) {
                                mPaint.setColor(Color.RED);
                                mPaint.setStrokeWidth(10.0f);
                                mPaint.setStyle(Paint.Style.STROKE);
                                canvas.drawRect(face.getRect(), mPaint);
                            }
                            canvas.restore();
                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                            break;
                        }
                    }
                    if (!result.isEmpty()) {
                        Log.e(TAG, "444444444444444444444444");
                        AFR_FSDKVersion version1 = new AFR_FSDKVersion();
                        AFR_FSDKEngine engine1 = new AFR_FSDKEngine();
                        AFR_FSDKFace result1 = new AFR_FSDKFace();
                        AFR_FSDKError error1 = engine1.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
                        Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error1.getCode());
                        if (error1.getCode() != AFD_FSDKError.MOK) {
                            Message reg = Message.obtain();
                            reg.what = MSG_CODE;
                            reg.arg1 = MSG_EVENT_FR_ERROR;
                            reg.arg2 = error1.getCode();
                            mHandler.sendMessage(reg);
                        }
                        error1 = engine1.AFR_FSDK_GetVersion(version1);
                        Log.d("com.arcsoft", "FR=" + version.toString() + "," + error1.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
                        error1 = engine1.AFR_FSDK_ExtractFRFeature(data, mBitmap.getWidth(), mBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(result.get(0).getRect()), result.get(0).getDegree(), result1);
                        Log.d("com.arcsoft", "Face=" + result1.getFeatureData()[0] + "," + result1.getFeatureData()[1] + "," + result1.getFeatureData()[2] + "," + error1.getCode());
                        if (error1.getCode() == error1.MOK) {
                            Log.e(TAG, "55555555555555555");
                            mAFR_FSDKFace = result1.clone();
                            int width = result.get(0).getRect().width();
                            int height = result.get(0).getRect().height();
                            Bitmap face_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                            Canvas face_canvas = new Canvas(face_bitmap);
                            face_canvas.drawBitmap(mBitmap, result.get(0).getRect(), new Rect(0, 0, width, height), null);
                            Message reg = Message.obtain();
                            reg.what = MSG_CODE;
                            reg.arg1 = MSG_EVENT_REG;
                            reg.obj = face_bitmap;
                            mHandler.sendMessage(reg);
                        } else {
                            Message reg = Message.obtain();
                            reg.what = MSG_CODE;
                            reg.arg1 = MSG_EVENT_NO_FEATURE;
                            mHandler.sendMessage(reg);
                        }
                        error1 = engine1.AFR_FSDK_UninitialEngine();
                        Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error1.getCode());
                    } else {
                        Message reg = Message.obtain();
                        reg.what = MSG_CODE;
                        reg.arg1 = MSG_EVENT_NO_FACE;
                        mHandler.sendMessage(reg);
                    }
                    err = engine.AFD_FSDK_UninitialFaceEngine();
                    Log.d(TAG, "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
                }
            }
        };
        thread.start();
    }

    private final static int MSG_CODE = 0x1000;
    private final static int MSG_EVENT_REG = 0x1001;
    private final static int MSG_EVENT_NO_FACE = 0x1002;
    private final static int MSG_EVENT_NO_FEATURE = 0x1003;
    private final static int MSG_EVENT_FD_ERROR = 0x1004;
    private final static int MSG_EVENT_FR_ERROR = 0x1005;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CODE) {
                switch (msg.arg1) {
                    case MSG_EVENT_REG:
                        MyApplication app = (MyApplication) getApplication();
                        long currentTimeMillis = System.currentTimeMillis();
                        String pic_time = TimeUtils.millis2String(currentTimeMillis, "mm_ss");
                        app.mFaceDB.addFace(pic_time, mAFR_FSDKFace);
                        break;
                    case MSG_EVENT_NO_FEATURE:
                        Toast.makeText(MainActivity.this, "人脸特征无法检测，请换一张图片", Toast.LENGTH_SHORT).show();
                        break;
                    case MSG_EVENT_NO_FACE:
                        Toast.makeText(MainActivity.this, "没有检测到人脸，请换一张图片", Toast.LENGTH_SHORT).show();
                        break;
                    case MSG_EVENT_FD_ERROR:
                        Toast.makeText(MainActivity.this, "FD初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                        break;
                    case MSG_EVENT_FR_ERROR:
                        Toast.makeText(MainActivity.this, "FR初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };
   private String pic_name;
    @OnClick({R.id.TakePicture, R.id.PairMatchingData})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.TakePicture:
                long currentTimeMillis = System.currentTimeMillis();
                StringBuffer sb = new StringBuffer();
                String pic_time = TimeUtils.millis2String(currentTimeMillis, "yyyy_MM_dd_HH_mm_ss");
                sb.append(pic_time).append(".jpg");
                pic_name = sb.toString();
                CameraUtlis.getInstance().doTakePicture();
                break;
            case R.id.PairMatchingData:
                CameraUtlis.getInstance().doStopCamera();
                SystemClock.sleep(500);
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
        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceDB.appid, FaceDB.age_key);
        Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
        error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
        Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

        ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceDB.appid, FaceDB.gender_key);
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
        List<FaceDB.FaceRegist> mResgist = ((MyApplication) getApplicationContext()).mFaceDB.mRegister;
        List<ASAE_FSDKFace> face1 = new ArrayList<>();
        List<ASGE_FSDKFace> face2 = new ArrayList<>();

        @Override
        public void setup() {
            AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
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
                Log.e(TAG, "mResgist 数量是" + mResgist.size());
                for (FaceDB.FaceRegist fr : mResgist) {
                    for (AFR_FSDKFace face : fr.mFaceList) {
                        error = engine.AFR_FSDK_FacePairMatching(result, face, score);
                        Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                        if (max < score.getScore()) {
                            max = score.getScore();
                            name = fr.mName;
                        }
                    }
                }
                //age & gender
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
    boolean openM = false;
    private void initSurfaceView() {
        CameraUtlis.getInstance().setCameraListener(this);
        typeSurfaceView.getHolder().addCallback(this);
        if (openM) {
            initM();
        }
    }

    private void initM() {
        mSurfaceView=findViewById(R.id.mSurfaceView);
        mGLSurfaceView=findViewById(R.id.mGLSurfaceView);
        mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
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

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            SystemClock.sleep(400);
            CameraUtlis.getInstance().doOpenCamera(MainActivity.this);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!openM) {
            ThreadUtils.getSinglePool().execute(runnable);
        }
    }

    @Override
    public void cameraHasOpened() {
        SurfaceHolder holder = surfaceView.getHolder();
        CameraUtlis.getInstance().doStartPreview(holder, -1);
    }

    @Override
    public void TakePicture(Bitmap bitmap) {
        FileUtil.saveBitmap(bitmap, pic_name,false);
        ExtractImage.setImageBitmap(bitmap);
        initdata();
    }

    @Override
    protected void onStop() {
        super.onStop();
        CameraUtlis.getInstance().doStopCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraUtlis.getInstance().doStopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraUtlis.getInstance().doStopCamera();
        super.onDestroy();
        mFRAbsLoop.shutdown();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

        ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
        Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

        ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
        Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
