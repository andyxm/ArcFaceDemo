package com.face;

import static com.face.lib.BaseFRAbsLoop.FAIL;
import static com.face.lib.BaseFRAbsLoop.OPEN_MATCHING;
import static com.face.lib.BaseFRAbsLoop.SUCCESS;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.Constance;
import com.FaceFRAbsLoop;
import com.FaceHelper;
import com.MyApplication;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.face.databinding.ActivityDetecterBinding;
import com.face.lib.FaceCameraGLSurfaceView;
import com.face.lib.utils.FileUtil;
import com.google.gson.Gson;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.model.StudentModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Route(path = Constance.ACTIVITY_URL_DETECTER)
public class DetecterActivity extends AppCompatActivity implements FaceCameraGLSurfaceView.GLSurfaceViewListener {
    private static final String TAG = "DetecterActivity";
    private FaceFRAbsLoop loop;
    private List<StudentModel> students = new ArrayList<>();
    private static final int DATA_ERROR=999;
    private static final int RIGIST_SUCCESS = 1000;
    private ActivityDetecterBinding binding;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    StudentModel model = (StudentModel) msg.obj;
                    binding.mGLSurfaceView.onDestroy();
                    uploadFace(model);
                    break;
                case FAIL:
                    binding.score.setText("");
                    showLongToast("识别失败");
                    countdown--;
                    if(countdown==0){
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.post(mRunnable);
                    }
                    break;
                case OPEN_MATCHING:
                    startM();
                    break;
                case DATA_ERROR:
                    showError();
                    break;
                case RIGIST_SUCCESS:
                    showLongToast("注册成功");
                    finish();
                    break;
            }
        }
    };
    
    private void showLongToast(String message) {
        Log.e(TAG, message);
    }
    
    private int countdown=5;
    private void uploadFace(StudentModel model) {
        LogUtils.e("uploadFace", "model=" + model.getFaceCard() + ",length=" + model.getFaceFeature().getFeatureData().length);
        String FaceFeature = ConvertUtils.bytes2HexString(model.getFaceFeature().getFeatureData());
        LogUtils.e("uploadFace", "FaceFeature=" + FaceFeature);
        MyApplication application = (MyApplication) this.getApplication();
        application.mFaceHelperDB.writeSdcard(model);
        String dbPath = application.mFaceHelperDB.getDBPath();
        if (TextUtils.isEmpty(dbPath)) {
            throw new NullPointerException(".data path is null");
        }
        String dotDataPath = dbPath + "/" + model.getFaceCard() + ".data";
        LogUtils.e(TAG, "mFilePath=" + dotDataPath);
        if (FileUtils.isFileExists(dotDataPath)) {
            String faceCard = model.getFaceCard();
            String[] split = faceCard.split("_");
            if (split.length == 2) {
                String newFacePath =dbPath + "/"+ split[0] + ".data";
                if (FileUtils.isFileExists(newFacePath)){
                    FileUtils.delete(newFacePath);
                    LogUtils.e(TAG,"文件存在,先删除");
                }
                boolean rename = FileUtils.rename(dotDataPath, split[0] + ".data");//重命名
                LogUtils.e(TAG, "上传路径" + newFacePath+",重命名名称"+split[0] + ".data"+",rename="+rename);
                if (rename){
                    boolean deleteDir = FileUtils.deleteAllInDir(FileUtil.IMAGE_REGIST_PATH);
                    if (deleteDir){
                        LogUtils.e(TAG,"照片删除成功...");
                    }
                    uploadFace(newFacePath);
                }else{
                    showError();
                }
            }
        }else{
            showError();
        }
    }
    private void uploadFace(String newFacePath) {
        String uploadFacePath="";
        if(TextUtils.isEmpty(uploadFacePath)){
            return;
        }
        File uploadFile=new File(newFacePath);
        OkGo.<String>post(uploadFacePath)
                .tag(this)
                .isMultipart(true)
                .params("file", uploadFile)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String data = response.body();//这个就是返回来的结果
                        if (TextUtils.isEmpty(data)) {
                            mHandler.sendEmptyMessage(DATA_ERROR);
                            return;
                        }
                        Gson gson = new Gson();
                        if(true){
                            mHandler.sendEmptyMessage(RIGIST_SUCCESS);
                        }
                    }
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        mHandler.sendEmptyMessage(DATA_ERROR);
                    }
                });


    }
    private void showError(){
       showLongToast("网络连接异常,请重新注册...");
       finish();
   }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetecterBinding.inflate(getLayoutInflater());
        initSurfaceView();
        ARouter.getInstance().inject(this);
        EventBus.getDefault().register(this);
        mHandler.postDelayed(mRunnable,60000);
    }
    private Runnable mRunnable=new Runnable() {
        @Override
        public void run() {
            showLongToast("注册超时,请重新注册");
            finish();
        }
    };
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void receiveMessage(List<StudentModel> students) {
        this.students = students;
        LogUtils.e(TAG, "SIZE=" + students.size());
        mHandler.sendEmptyMessageDelayed(OPEN_MATCHING, 1000);
    }

    private void startM() {
        if (students.isEmpty()) finish();
        if (loop != null) {
            if (loop.isAlive()) {
                Log.e(TAG, "线程已经启动...");
                return;
            } else {
                Log.e(TAG, "线程没有启动...");
                loop.start();
            }
            return;
        }
        loop = new FaceFRAbsLoop(mHandler, students, mWidth, mHeight, FaceHelper.appid, FaceHelper.ft_key, FaceHelper.fr_key);
        binding.mGLSurfaceView.setFRAbsLoop(loop);
        loop.start();
    }

    private int mWidth, mHeight;

    private void initSurfaceView() {
        initM();
    }

    private void initM() {
        mWidth = 1280;
        mHeight = 720;
        int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        int mFormat = ImageFormat.NV21;
        binding.mGLSurfaceView.initViewEngine(binding.mSurfaceView, mCameraID, mWidth, mHeight, mFormat, FaceHelper.appid, FaceHelper.ft_key, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mHandler.removeCallbacks(mRunnable);
        binding.mGLSurfaceView.onDestroy();
    }

    @Override
    public void reset(int width, int height) {
        Log.e(TAG, "重新设置了宽高...w=" + mWidth + ",h=" + mHeight);
        this.mWidth = width;
        this.mHeight = height;
    }
}
