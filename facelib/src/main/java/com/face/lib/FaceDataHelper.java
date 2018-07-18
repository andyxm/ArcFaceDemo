package com.face.lib;

import android.text.TextUtils;
import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.blankj.utilcode.utils.ConvertUtils;
import com.blankj.utilcode.utils.FileUtils;
import com.blankj.utilcode.utils.LogUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建日期: 2018/6/7.
 * 创 建 人: xm
 * 内    容:
 */
public abstract class FaceDataHelper<T extends BaseFaceMode> {
    private final String TAG = this.getClass().toString();
    protected abstract String appid();

    protected abstract String ft_key();

    protected abstract String fd_key();

    protected abstract String fr_key();

    protected abstract String age_key();

    protected abstract String gender_key();

    protected abstract boolean isSaveSdcard();

    protected abstract T getT(String card);

    private List<T> localFaces;
    private AFR_FSDKEngine mFREngine;
    private AFR_FSDKVersion mFRVersion;
    private String DBPath;

    public void setLocalFaces(List<T> localFaces) {
        this.localFaces = localFaces;
    }

    public FaceDataHelper() {
        init();
    }

    public List<T> getLocalFaces() {
        return localFaces;
    }

    public String getDBPath() {
        return DBPath;
    }

    private void init() {
        localFaces = new ArrayList<>();
        mFRVersion = new AFR_FSDKVersion();
        mFREngine = new AFR_FSDKEngine();
        AFR_FSDKError error = mFREngine.AFR_FSDK_InitialEngine(appid(), fr_key());
        if (error.getCode() != AFR_FSDKError.MOK) {
            Log.e(TAG, "AFR_FSDK_InitialEngine fail! error code :" + error.getCode());
        } else {
            mFREngine.AFR_FSDK_GetVersion(mFRVersion);
            Log.d(TAG, "AFR_FSDK_GetVersion=" + mFRVersion.toString());
        }
    }

    public FaceDataHelper(String path) {
        this.DBPath = path;
        init();
    }

    public void addFace(T model) {
        boolean add;
        add = localFaces.add(model);
        if (isSaveSdcard()) {
            writeSdcard(model);
        }
        if (add) {
            Log.e(TAG, "addFace SUCCESS ");
        } else {
            Log.e(TAG, "addFace Fail");
        }
    }

    public void writeSdcard(T model) {
        if (TextUtils.isEmpty(DBPath)) {
            Log.e(TAG, "writeSdcard fail DBPath is null");
            return;
        }
        //TODO: 2018/6/8 安卓端比其他平台多4个字节22024个,其它22020个,.data中多 00 00 56 04 等等
        //Arrays.copyOfRange(arr,1,3);
        try {
            //save new feature
            String filePath = DBPath + "/" + model.getFaceCard() + ".data";
            if (FileUtils.isFile(filePath)) {
                FileUtils.deleteFile(filePath);
            }
            FileOutputStream fs = new FileOutputStream(filePath, true);
            BufferedOutputStream bos=new BufferedOutputStream(fs);
            bos.write(model.getFaceFeature().getFeatureData());
            bos.close();
            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return 加载本地所有的.data文件放入集合
     */
    public List<T> LoadLocalDoNetFileData() {
        if (!localFaces.isEmpty()){
            localFaces.clear();
        }
        if(TextUtils.isEmpty(DBPath)||!FileUtils.isDir(DBPath)){
            Log.e(TAG,"DBPath is null or dir not exists..");
            return localFaces;
        }
        File[] files = new File(DBPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                Log.e(TAG, "name=" + pathname.getName());
                return pathname.getName().endsWith(".data");
            }
        });
        if (files.length == 0) {
            return localFaces;
        }
        FileInputStream fs = null;
        // TODO: 2018/6/20  ExtInputStream去读取数据会出现byte都是000000000000000或者是多四个字节
        BufferedInputStream bis = null;
        AFR_FSDKFace afr;
        T t;
        try {
            for (File file : files) {
                fs = new FileInputStream(file);
                bis = new BufferedInputStream(fs);
                String path = file.getPath();
                int start = path.lastIndexOf("/") + 1;
                int end = path.lastIndexOf(".");
                path = path.substring(start, end);
                t = getT(path);
                t.setFaceCard(path);
                afr=new AFR_FSDKFace();
                while (bis.read(afr.getFeatureData()) != -1){
                      LogUtils.e(TAG,"!=-1");
                }
                String FaceFeature = ConvertUtils.bytes2HexString(afr.getFeatureData());
                LogUtils.e(TAG,FaceFeature);
                t.setFaceFeature(afr);
                localFaces.add(t);
            }
            Log.e(TAG, "local data size :" + localFaces.size());
            bis.close();
            fs.close();
            return localFaces;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            CloseUtils.closeIO(bos);
//            CloseUtils.closeIO(fs);
        }
        return localFaces;
    }

    public void delete(T model) {
        boolean delete;
        delete = localFaces.remove(model);
        if (delete) {
            Log.e(TAG, "DELETE SUCCESS ");
        } else {
            Log.e(TAG, "DELETE Fail");
        }
    }

    public void destroy() {
        if (mFREngine != null) {
            mFREngine.AFR_FSDK_UninitialEngine();
        }
    }
}
