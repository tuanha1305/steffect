package com.facedemo.com.facesdkbuild;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.facebeauty.com.beautysdk.domain.Brand;
import com.facebeauty.com.beautysdk.utils.LogUtils;
import com.facebeauty.com.beautysdk.utils.STLicenseUtils;
import com.facebeauty.com.beautysdk.view.CameraView;
import com.facedemo.com.facesdkbuild.adapter.DemoAdapter;
import com.facedemo.com.facesdkbuild.view.HorizontalListView;
import com.sensetime.stmobile.model.STPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
public class MainActivity extends Activity {
    public static final String TAG="MainActivity";

    private HorizontalListView horizontalList;
    CameraView cameraView;
    private Button btnStart,btnEnd,btnTest,btnChoice;
    private int mCurrent = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        STLicenseUtils.checkLicense(this, new STLicenseUtils.OnCheckLicenseListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFail() {
            }
        });
        cameraView = (CameraView) findViewById(R.id.cameraView);
        cameraView.init(MainActivity.this);
        cameraView.registerFacePointsChangeListener(new CameraView.OnFacePointsChangeListener() {
            @Override
            public void onChangeListener(STPoint[] pointsBrowLeft, STPoint[] pointsBrowRight, STPoint[] pointsEyeLeft, STPoint[] pointsEyeRight, STPoint[] pointsLips) {
                Log.d("onChangeListener","onChangeListener");
            }
        });

        btnStart = (Button)findViewById(R.id.start);
        btnEnd = (Button)findViewById(R.id.stop);
        btnTest = (Button)findViewById(R.id.test);
        btnChoice = (Button)findViewById(R.id.choice);

        LogUtils.setIsLoggable(false);
//        String pathTiezhi = "/storage/emulated/0/Download/bunny.zip";
//        String pathTiezhi = Environment.getExternalStorageDirectory()+"/Download/banny.zip";
//        cameraView.setTiezhi(3,pathTiezhi);

        horizontalList = (HorizontalListView) findViewById(R.id.horizontalList);
        String data = openAssetsFile("makeuplist.json");
        JSONObject jsonObject = JSON.parseObject(data);
        String dataStr = jsonObject.getString("data");
        List<Brand> brandList = JSON.parseArray(dataStr, Brand.class);
        DemoAdapter adapter = new DemoAdapter(MainActivity.this, brandList, cameraView);
        horizontalList.setAdapter(adapter);


//        DisplayMetrics metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        width =metrics.widthPixels;
//        height = metrics.heightPixels;
//        dpi = metrics.densityDpi;



        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.cleanMakeUp();
//                Toast.makeText(MainActivity.this,"开始录制",Toast.LENGTH_SHORT).show();
//                mediaRecorder =new MediaRecorder();
//
//                mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//                Intent captureIntent =  mediaProjectionManager.createScreenCaptureIntent();
//                startActivityForResult(captureIntent,100);

            }
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,WelcomeActivity.class));
//                Toast.makeText(MainActivity.this,"停止录制",Toast.LENGTH_SHORT).show();
//                stopRecord();
            }
        });
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCurrent==0){
                    mCurrent =1;
                }else {
                    mCurrent = 0;
                }
                cameraView.changePreviewSize(mCurrent);
            }
        });
        btnChoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.changeChoice();
            }
        });
    }

    MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection;
    VirtualDisplay virtualDisplay;
    MediaRecorder mediaRecorder;
    int width = 720;
    int height = 1080;
    int dpi = 1;
    private boolean running;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if(resultCode==RESULT_OK&&requestCode==100){
//            mediaProjection =  mediaProjectionManager.getMediaProjection(resultCode,data);
//            Toast.makeText(MainActivity.this,"init成功",Toast.LENGTH_SHORT).show();
//            startRecord();
//        }
    }
//    public boolean startRecord() {
//        if (mediaProjection == null || running) {
//            return false;
//        }
//
//        initRecorder();
//        createVirtualDisplay();
//        mediaRecorder.start();
//        running = true;
//        return true;
//    }
//
//    public boolean stopRecord() {
//        if (!running) {
//            return false;
//        }
//        running = false;
//        mediaRecorder.stop();
//        mediaRecorder.reset();
//        virtualDisplay.release();
//        mediaProjection.stop();
//
//        return true;
//    }
//    private void initRecorder() {
////        mediaRecorder.setInputSurface(cameraView.getSurfaceView());
////        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setInputSurface(cameraView.mCameraDisplay.mGlSurfaceView.getHolder().getSurface());
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mediaRecorder.setOutputFile(getsaveDirectory() + System.currentTimeMillis() + ".mp4");
//        mediaRecorder.setVideoSize(width, height);
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
////        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
//        mediaRecorder.setVideoFrameRate(30);
//        try {
//            mediaRecorder.prepare();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    private void createVirtualDisplay() {
//        virtualDisplay = mediaProjection.createVirtualDisplay(
//                "MainScreen",
//                width,
//                height,
//                dpi,
//                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                mediaRecorder.getSurface(),
//                null, null);
//    }
    public String getsaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScreenRecord" + "/";

            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }

            Toast.makeText(getApplicationContext(), rootDir, Toast.LENGTH_SHORT).show();
            return rootDir;
        } else {
            return null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }
    private String openAssetsFile(String filename) {
        try {
            InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open(filename));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null)
                Result += line;
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.onDestory();
    }
    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }
}