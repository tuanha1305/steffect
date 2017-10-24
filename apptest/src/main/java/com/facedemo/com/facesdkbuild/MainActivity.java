package com.facedemo.com.facesdkbuild;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.InputStream;
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
        String pathTiezhi = "/storage/emulated/0/Download/bunny.zip";
        cameraView.setTiezhi(3,pathTiezhi);

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
//                String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.jpg";
//                File file = new File(path);
//                cameraView.saveImage(file);
                cameraView.startRecording();

            }
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              String path = cameraView.stopRecording();
             Toast.makeText(MainActivity.this,path,Toast.LENGTH_LONG).show();
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


        Bitmap image = null;
        AssetManager am = getResources().getAssets();
        try
        {
            InputStream is = getClass().getResourceAsStream("/assets/banbaoyanying.png");
            image = BitmapFactory.decodeStream(is);
            is.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        float[] color = {0.0f,0.0f,0.0f,0.0f};

        cameraView.setEyeShadow(image,color);
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