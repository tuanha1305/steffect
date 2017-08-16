package sensetime.senseme.com.effects;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.effects.senseme.sensemesdk.utils.STLicenseUtils;


public class WelcomeActivity extends Activity {
    Button mCameraStartBtn, mImageStartBtn, mCpuFilterBtn;
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private boolean mIsImageBtn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mCameraStartBtn = (Button) findViewById(R.id.start_camera_streaming_btn);
        mImageStartBtn = (Button) findViewById(R.id.start_image_btn);
        mCpuFilterBtn = (Button) findViewById(R.id.cpu_filter_btn);

        if (!STLicenseUtils.checkLicense(this)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "授权失败", Toast.LENGTH_SHORT).show();
                }
            });
        }

        mCameraStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Permission has not been granted and must be requested.
                        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                            // Provide an additional rationale to the user if the permission was not granted
                            // and the user would benefit from additional context for the use of the permission.
                        }
                        // Request the permission. The result will be received in onRequestPermissionResult()
                        requestPermissions(new String[]{Manifest.permission.CAMERA},
                                PERMISSION_REQUEST_CAMERA);
                    } else {
                        // Permission is already available, start camera preview
                        startActivity(new Intent(getApplicationContext(), CameraActivityNew.class));
                    }
                } else {
                    startActivity(new Intent(getApplicationContext(), CameraActivityNew.class));
                }
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            // Request for camera permission.
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                startActivity(new Intent(getApplicationContext(), CameraActivityNew.class));
            } else {
                // Permission request was denied.
                Toast.makeText(this, "Camera权限被拒绝", Toast.LENGTH_SHORT)
                        .show();
            }
        }
//        else if(requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE){
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//                if(mIsImageBtn){
//                    startActivity(new Intent(getApplicationContext(), ImageActivity.class));
//                }else{
//                    startActivity(new Intent(getApplicationContext(), CpuFilterActivity.class));
//                }
//            } else {
//                // Permission request was denied.
//                Toast.makeText(this, "存储卡读写权限被拒绝", Toast.LENGTH_SHORT)
//                        .show();
//            }
//        }
    }
}
