package sensetime.senseme.com.effects;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tatata.hearst.R;

import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;

public class WelcomeActivity extends Activity {
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 2;

    private static final int MSG_LOADING_SHOW = 100;
    private static final int MSG_LOADING_GONE = 101;

    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView mLoading;
    private boolean mIsPaused = false;

    private Handler mHandleMessege = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_LOADING_GONE:
                    mLoading.setVisibility(View.INVISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);

                    break;

                case MSG_LOADING_SHOW:
                    mLoading.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.VISIBLE);

                    break;

                default:

                    break;
            }
        }
    };

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(mIsPaused)return;

            startActivity(new Intent(getApplicationContext(), CameraActivity.class));
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mContext = this;
        mProgressBar = (ProgressBar) findViewById(R.id.process_loading);
        mProgressBar.setVisibility(View.INVISIBLE);
        mLoading = (TextView)findViewById(R.id.tv_loading);
        mLoading.setVisibility(View.INVISIBLE);

        if (!STLicenseUtils.checkLicense(this)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "请检查License授权！", Toast.LENGTH_SHORT).show();
                }
            });
        }

        checkCameraPermission();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsPaused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsPaused = true;
        finish();
    }

    @Override
    public void onBackPressed() {
        mHandler.removeCallbacks(mRunnable);
        mHandleMessege.removeMessages(MSG_LOADING_GONE);
        mHandleMessege.removeMessages(MSG_LOADING_SHOW);
        finish();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkWritePermission();
            } else {
                Toast.makeText(this, "Camera权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAudioPermission();
            } else {
                Toast.makeText(this, "存储卡读写权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode == PERMISSION_REQUEST_RECORD_AUDIO){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity();
            } else {
                Toast.makeText(this, "麦克风权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkCameraPermission(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {}
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }else {
                checkWritePermission();
            }
        }else{
            startCameraActivity();
        }
    }

    private void checkWritePermission(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }else {
                startCameraActivity();
            }
        }
    }

    private void checkAudioPermission(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_RECORD_AUDIO);
            }else {
                startCameraActivity();
            }
        }
    }

    private void startCameraActivity(){
        new Thread(){
            public void run() {
                Message msg = mHandleMessege.obtainMessage(MSG_LOADING_SHOW);
                mHandleMessege.sendMessage(msg);

                FileUtils.copyStickerFiles(mContext, "2D");
                FileUtils.copyStickerFiles(mContext, "3D");
                FileUtils.copyStickerFiles(mContext, "hand_action");
                FileUtils.copyStickerFiles(mContext, "segment");
                FileUtils.copyStickerFiles(mContext, "deformation");

                if(mIsPaused)return;

                Message msg1 = mHandleMessege.obtainMessage(MSG_LOADING_GONE);
                mHandleMessege.sendMessage(msg1);

                mHandler.postDelayed(mRunnable, 1000);
            }
        }.start();
    }

}
