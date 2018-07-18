package com.face;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
@Deprecated
/**
 * 测试Fragment,可忽略
 * FaceFragment fragment;
 * AccountFragment accountFragment;
 */
public class AccountFragment extends Fragment{
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_face, container, false);
        return view;
    }
}
