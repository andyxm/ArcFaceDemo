package com.face;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.FaceHelper;
import com.MyApplication;
import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.blankj.utilcode.utils.ConvertUtils;
import com.blankj.utilcode.utils.FileUtils;
import com.blankj.utilcode.utils.ThreadPoolUtils;
import com.blankj.utilcode.utils.TimeUtils;
import com.face.lib.FaceDataHelper;
import com.face.lib.listener.CameraListener;
import com.face.lib.utils.CameraUtlis;
import com.face.lib.utils.FileUtil;
import com.guo.android_extend.image.ImageConverter;
import com.model.StudentModel;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
public class FaceRegistActivity extends AppCompatActivity implements CameraUtlis.CamOpenOverCallback, CameraListener, SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";
    @BindView(R.id.surfaceView)
    SurfaceView surfaceView;
    @BindView(R.id.ExtractImage)
    ImageView ExtractImage;
    @BindView(R.id.TakePicture)
    Button TakePicture;
    private SurfaceHolder mSurfaceHolder;
    private Thread thread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_regist);
        ButterKnife.bind(this);
        initSurfaceView();
    }
    private Bitmap mBitmap;
    private Rect src = new Rect();
    private Rect dst = new Rect();
    private AFR_FSDKFace mAFR_FSDKFace;

    private void saveFaceFeature(String pic_name) {
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
        String mFilePath = null;
        for (File file : files) {
            if(file.getPath().contains(pic_name)){
                mFilePath=file.getPath();
                break;
            }
        }
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
                AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(FaceHelper.appid, FaceHelper.fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
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
                        AFR_FSDKError error1 = engine1.AFR_FSDK_InitialEngine(FaceHelper.appid, FaceHelper.fr_key);
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
//                        app.mFaceDB.addFace(pic_time, mAFR_FSDKFace);
                        byte[] featureData = mAFR_FSDKFace.getFeatureData();
                        String FaceFeature = ConvertUtils.bytes2HexString(featureData);
                        Log.e(TAG,"特征码byte长度="+featureData.length);
                        Log.e(TAG,FaceFeature);
                        StudentModel model=new StudentModel();
                        model.setFaceFeature(mAFR_FSDKFace);
                        model.setFaceCard(pic_time_name);
                        app.mFaceHelperDB.addFace(model);
                        break;
                    case MSG_EVENT_NO_FEATURE:
                        Toast.makeText(FaceRegistActivity.this, "人脸特征无法检测，请换一张图片", Toast.LENGTH_SHORT).show();
                        break;
                    case MSG_EVENT_NO_FACE:
                        Toast.makeText(FaceRegistActivity.this, "没有检测到人脸，请换一张图片", Toast.LENGTH_SHORT).show();
                        break;
                    case MSG_EVENT_FD_ERROR:
                        Toast.makeText(FaceRegistActivity.this, "FD初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                        break;
                    case MSG_EVENT_FR_ERROR:
                        Toast.makeText(FaceRegistActivity.this, "FR初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };
    private String pic_time_name;
    @OnClick({R.id.TakePicture})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.TakePicture:
                long currentTimeMillis = System.currentTimeMillis();
                pic_time_name = TimeUtils.millis2String(currentTimeMillis, "mm_ss");
                CameraUtlis.getInstance().doTakePicture();
                break;
        }
    }
    private void initSurfaceView() {
        CameraUtlis.getInstance().setCameraListener(this);
//        surfaceView.getHolder().addCallback(this);
    }
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            SystemClock.sleep(400);
            CameraUtlis.getInstance().doOpenCamera(FaceRegistActivity.this);
        }
    };
    private ThreadPoolUtils threadPoolUtils = new ThreadPoolUtils(ThreadPoolUtils.Type.FixedThread, 2);
    @Override
    protected void onResume() {
        super.onResume();
        threadPoolUtils.execute(runnable);
    }
    @Override
    public void cameraHasOpened() {
        SurfaceHolder holder = surfaceView.getHolder();
        CameraUtlis.getInstance().doStartPreview(holder, -1);
    }

    @Override
    public void TakePicture(Bitmap bitmap) {
        StringBuffer sb=new StringBuffer();
        sb.append(pic_time_name).append(".jpg");
        FileUtil.saveBitmap(bitmap, sb.toString(),false);
        ExtractImage.setImageBitmap(bitmap);
        saveFaceFeature(sb.toString());
        // TODO: 2018/6/20 拍照后可能需要释放相机，避免相机服务被kill
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
}
