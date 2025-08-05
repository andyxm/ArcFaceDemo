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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.FaceFRAbsLoop;
import com.FaceHelper;
import com.MyApplication;
import com.blankj.utilcode.util.ToastUtils;
import com.face.lib.FaceCameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.model.StudentModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FaceIdentifyExtActivity extends AppCompatActivity implements FaceCameraGLSurfaceView.GLSurfaceViewListener {
    private static final String TAG = "FaceIdentifyExtActivity";
    @BindView(R.id.ExtractImage)
    ImageView ExtractImage;
    @BindView(R.id.score)
    TextView scoreTV;
    @BindView(R.id.PairMatchingData)
    Button PairMatchingData;
    @BindView(R.id.mSurfaceView)
    CameraSurfaceView mSurfaceView;
    @BindView(R.id.mGLSurfaceView)
    FaceCameraGLSurfaceView mGLSurfaceView;
    @BindView(R.id.close_face)
    Button closeFace;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    StudentModel model = (StudentModel) msg.obj;
                    ToastUtils.showLong("识别成功");
                    scoreTV.setText("卡号:" + model.getFaceCard() + "分数:" + model.getScore());
                    break;
                case FAIL:
                    scoreTV.setText("");
                    ToastUtils.showLong("识别失败");
                    break;
                case OPEN_MATCHING:
                    startM();
                    break;
            }
        }
    };
    private List<StudentModel> students;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_ext_identify);
        ButterKnife.bind(this);
        initSurfaceView();
        MyApplication application = (MyApplication) getApplication();
        students = application.mFaceHelperDB.LoadLocalDoNetFileData();
        mHandler.sendEmptyMessageDelayed(OPEN_MATCHING, 3000);
    }
    @OnClick({R.id.PairMatchingData,R.id.close_face})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.PairMatchingData:
                startM();
                break;
            case R.id.close_face:
                finish();
                break;
        }
    }
    private FaceFRAbsLoop loop;
    private void startM() {
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
        loop = new FaceFRAbsLoop(mHandler, students, mWidth, mHeight,FaceHelper.appid,FaceHelper.ft_key,FaceHelper.fr_key);
        mGLSurfaceView.setFRAbsLoop(loop);
        loop.start();
    }
    private int mWidth, mHeight;
    private void initSurfaceView() {
        initM();
    }
    private void initM() {
        mWidth = 1280;
        mHeight = 960;
        int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        int mFormat = ImageFormat.NV21;
        mGLSurfaceView.initViewEngine(mSurfaceView,mCameraID,mWidth,mHeight,mFormat,FaceHelper.appid,FaceHelper.ft_key,this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLSurfaceView.onDestroy();
    }
    @Override
    public void reset(int width, int height) {
        Log.e(TAG,"重新设置了宽高...w="+mWidth+",h="+mHeight);
        this.mWidth=width;
        this.mHeight=height;
    }
}
