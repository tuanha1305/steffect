package com.facedemo.com.facesdkbuild;

import android.app.Activity;
import android.os.Bundle;
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
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";

    private HorizontalListView horizontalList;
    CameraView cameraView;
    private Button btnStart, btnEnd, btnTest, btnChoice;
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
                Log.d("onChangeListener", "onChangeListener" + pointsBrowLeft.length);
            }
        });
        btnStart = (Button) findViewById(R.id.start);
        btnEnd = (Button) findViewById(R.id.stop);
        btnTest = (Button) findViewById(R.id.test);
        btnChoice = (Button) findViewById(R.id.choice);

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
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                cameraView.cleanMakeUp();
                if(cameraView.isRecoderScreen()){
                    Toast.makeText(MainActivity.this,"正在录屏中，",Toast.LENGTH_SHORT).show();
                    return;
                }
                cameraView.startRecoderScreen();
            }
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!cameraView.isRecoderScreen()){
                    Toast.makeText(MainActivity.this,"请先开始录屏",Toast.LENGTH_SHORT).show();
                    return;
                }
                cameraView.endRecoderScreen();
//                startActivity(new Intent(MainActivity.this,WelcomeActivity.class));
            }
        });
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrent == 0) {
                    mCurrent = 1;
                } else {
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