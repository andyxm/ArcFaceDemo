package com.face;
import android.app.Fragment;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.FaceFRAbsLoop;
import com.FaceHelper;
import com.MyApplication;
import com.face.lib.FaceCameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.model.StudentModel;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.face.lib.BaseFRAbsLoop.FAIL;
import static com.face.lib.BaseFRAbsLoop.OPEN_MATCHING;
import static com.face.lib.BaseFRAbsLoop.SUCCESS;
@Deprecated
/**
 * 测试Fragment,可忽略
 * FaceFragment fragment;
 * AccountFragment accountFragment;
 */
public class FaceFragment extends Fragment implements FaceCameraGLSurfaceView.GLSurfaceViewListener{
    private String TAG="FaceFragment";
    @BindView(R.id.mSurfaceView)
    CameraSurfaceView mSurfaceView;
    @BindView(R.id.mGLSurfaceView)
    FaceCameraGLSurfaceView mGLSurfaceView;
    @BindView(R.id.score)
    TextView score;
    @BindView(R.id.PairMatchingData)
    Button PairMatchingData;
    @BindView(R.id.close_face)
    Button closeFace;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    StudentModel model = (StudentModel) msg.obj;
                    ToastUtils.showLongToast("识别成功");
                    score.setText("卡号:" + model.getFaceCard() + "分数:" + model.getScore());
                    break;
                case FAIL:
                    score.setText("");
                    ToastUtils.showLongToast("识别失败");
                    break;
                case OPEN_MATCHING:
                    startM();
                    break;
            }
        }
    };
    private FaceFRAbsLoop loop;
    private List<StudentModel> students;
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
        loop = new FaceFRAbsLoop(mHandler, students, mWidth, mHeight, FaceHelper.appid,FaceHelper.ft_key,FaceHelper.fr_key);
        mGLSurfaceView.setFRAbsLoop(loop);
        loop.start();
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_face, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        ButterKnife.bind(this, view);
        initSurfaceView();
        MyApplication application = (MyApplication) getActivity().getApplication();
        students = application.mFaceHelperDB.LoadLocalDoNetFileData();
        mHandler.sendEmptyMessageDelayed(OPEN_MATCHING, 5000);
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
    @OnClick({R.id.PairMatchingData, R.id.close_face})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.PairMatchingData:
                startM();
                break;
            case R.id.close_face:
                break;
        }
    }
    @Override
    public void reset(int width, int height) {
        Log.e(TAG,"重新设置了宽高...w="+mWidth+",h="+mHeight);
        this.mWidth=width;
        this.mHeight=height;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (loop != null) {
            loop.shutdown();
            loop = null;
        }
        mGLSurfaceView.onDestroy();
    }
}
