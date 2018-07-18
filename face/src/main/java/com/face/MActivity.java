package com.face;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.MyApplication;
import com.blankj.utilcode.utils.FileUtils;
import com.blankj.utilcode.utils.ZipUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.request.GetRequest;
import com.lzy.okserver.OkDownload;
import com.lzy.okserver.download.DownloadListener;
import com.lzy.okserver.download.DownloadTask;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MActivity extends AppCompatActivity {
    private static final String TAG = "MActivity";
    @BindView(R.id.regist)
    Button regist;
    @BindView(R.id.identify)
    Button identify;
    @BindView(R.id.auto_regist)
    Button auto_regist;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_m);
        ButterKnife.bind(this);
        downloadFeatureZip();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (okDownload != null) {
            okDownload.removeTask(DOWNLOAD_FEATURE_ZIPTAG);
        }
    }

    @OnClick({R.id.regist, R.id.identify,R.id.auto_regist})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.regist:
                startActivity(new Intent(MActivity.this, FaceRegistActivity.class));
                break;
            case R.id.identify:
                startActivity(new Intent(MActivity.this, FaceIdentifyExtActivity.class));

//                startActivity(new Intent(MActivity.this, FaceFragmentActivity.class));
                break;
            case R.id.auto_regist:
                break;
        }
    }

    private OkDownload okDownload;
    private String DOWNLOAD_FEATURE_ZIPTAG = "feature";
    private String FEATURE_ZIP = "feature.zip";

    /**
     * 从服务器下载.data文件,服务器文件是xxx.zip，将其解压后删除
     */
    private void downloadFeatureZip() {
        File[] files = new File(this.getExternalCacheDir().getPath()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                Log.e(TAG, "name=" + pathname.getName());
                return pathname.getName().endsWith(".data");
            }
        });
        if (files.length != 0) {
            Log.e(TAG,"数据不需要更新，已经存在数据...");
            return ;
        }
        String url="http://192.168.1.81:9008//Upfile//FaceSimilarity//f30f807d-a5bc-490a-8a3b-8085179e4cf6/4674e5d1-c79e-45a7-a3e8-61f60b8b77e9.zip";
        if(TextUtils.isEmpty(url)){
            return;
        }
        okDownload = OkDownload.getInstance();
        final MyApplication application = (MyApplication) getApplication();
        final String path = application.mFaceHelperDB.getDBPath();
        okDownload.setFolder(path);
        okDownload.getThreadPool().setCorePoolSize(1);
        GetRequest<File> request = OkGo.get(url);
        DownloadTask task = okDownload.request(DOWNLOAD_FEATURE_ZIPTAG, request).save().register(new DownloadListener(DOWNLOAD_FEATURE_ZIPTAG) {
            @Override
            public void onStart(Progress progress) {
                Log.e(TAG, "start");
            }

            @Override
            public void onProgress(Progress progress) {
            }

            @Override
            public void onError(Progress progress) {
            }

            @Override
            public void onFinish(File file, Progress progress) {
                String path1 = file.getPath();
                Log.e(TAG, "文件路径是" + path1);
                String unZipPath = path +"/"+FEATURE_ZIP;
                deleteOldDataFile(path);
                try {
                    boolean isUnZipRusult = ZipUtils.unzipFile(unZipPath, path);
                    if (isUnZipRusult) {
                        Log.e(TAG, "解压缩成功");
                        FileUtils.deleteFile(unZipPath);
//                        application.mFaceHelperDB.LoadLocalDoNetFileData();
                    } else {
                        Log.e(TAG, "解压缩失败");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "unzip Exception ...");
                }
            }

            @Override
            public void onRemove(Progress progress) {
                Log.e(TAG, "onRemove");
            }
        }).fileName(FEATURE_ZIP);
        task.start();
    }
    /**
     * 必要时，可以开线程去做
     * @param path 文件路径
     */
    private void deleteOldDataFile(String path) {
        File[] files = new File(path).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                Log.e(TAG, "name=" + pathname.getName());
                return pathname.getName().endsWith(".data");
            }
        });
        if (files.length == 0) {
            return;
        }
        for (File file : files) {
            FileUtils.deleteFile(file);
        }
    }
}
