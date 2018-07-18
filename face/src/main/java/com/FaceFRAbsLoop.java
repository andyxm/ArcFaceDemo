package com;
import android.os.Handler;

import com.face.lib.BaseFRAbsLoop;
import com.model.StudentModel;

import java.util.List;

/**
 * 创建日期: 2018/6/9.
 * 创 建 人: xm
 * 内    容:
 */
public class FaceFRAbsLoop extends BaseFRAbsLoop<StudentModel> {
    public FaceFRAbsLoop(Handler mHandler, List<StudentModel> lists, int mWidth, int mHeight, String appid, String ft_key, String fr_key) {
        super(mHandler, lists, mWidth, mHeight, appid, ft_key, fr_key);
    }
    @Override
    protected float MinimumScore() {
        return 0.80f;
    }
    @Override
    protected int IdentifyNumber() {
        return 1;
    }

    @Override
    protected boolean IsShowImage() {
        return false;
    }
}
