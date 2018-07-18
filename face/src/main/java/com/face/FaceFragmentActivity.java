package com.face;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
@Deprecated
/**
 * 测试Fragment,可忽略
 * FaceFragment fragment;
 * AccountFragment accountFragment;
 */
public class FaceFragmentActivity extends AppCompatActivity {
    private static final String TAG = "FaceFragmentActivity";
    @BindView(R.id.account)
    TextView account;
    @BindView(R.id.face)
    TextView face;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_fragment);
        ButterKnife.bind(this);
        fragment = new FaceFragment();
        ToastUtils.showLongToast("开启了fragment");
        Log.e(TAG, "开启了fragment");
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.framelayout, fragment);
        transaction.commit();
        previousFragment=fragment;
    }
    FaceFragment fragment;
    AccountFragment accountFragment;
    private void switchF(Fragment fragment,int postion){
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if(previousFragment==fragment){
            return;
        }
        if (!fragment.isAdded()) {
            transaction.add(R.id.framelayout, fragment).commit();
        }else{
            transaction.hide(previousFragment).show(fragment).commit();
        }
        previousFragment=fragment;
    }
    private Fragment previousFragment;
    @OnClick({R.id.account, R.id.face})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.account:
                if (accountFragment==null){
                    accountFragment =new AccountFragment();
                }
                switchF(accountFragment,0);
                break;
            case R.id.face:
                switchF(fragment,1);
                break;
        }
    }
}
