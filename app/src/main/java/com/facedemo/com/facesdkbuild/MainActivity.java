package com.facedemo.com.facesdkbuild;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.facebeauty.com.beautysdk.domain.Brand;
import com.facebeauty.com.beautysdk.utils.STLicenseUtils;
import com.facebeauty.com.beautysdk.view.CameraView;
import com.facedemo.com.facesdkbuild.adapter.DemoAdapter;
import com.facedemo.com.facesdkbuild.view.HorizontalListView;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
public class MainActivity extends Activity {
    public static final String TAG="MainActivity";

    private HorizontalListView horizontalList;
    CameraView cameraView;
    private String path = "http://api.7fineday.com/front/api/face/authkey";
    private Button btnStart,btnEnd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        STLicenseUtils.getTokenLicense(this);
        STLicenseUtils.checkLicense(this, new STLicenseUtils.OnCheckLicenseListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 授权成功");

            }

            @Override
            public void onFail() {
                Log.d(TAG, "onSuccess: 授权失败");
            }
        });
        cameraView = (CameraView) findViewById(R.id.cameraView);
        cameraView.init(MainActivity.this);
        btnStart = (Button)findViewById(R.id.start);
        btnEnd = (Button)findViewById(R.id.stop);

        String pathTiezhi = "/storage/emulated/0/Android/data/com.sensetime.senseme.effects/files/bunny.zip";
        cameraView.setTiezhi(3,pathTiezhi);
        horizontalList = (HorizontalListView) findViewById(R.id.horizontalList);
        String data = openAssetsFile("makeuplist.json");
        JSONObject jsonObject = JSON.parseObject(data);
        String dataStr = jsonObject.getString("data");
//       JSONObject dataJsonObject=  jsonObject.getJSONObject("data");
        List<Brand> brandList = JSON.parseArray(dataStr, Brand.class);
        DemoAdapter adapter = new DemoAdapter(MainActivity.this, brandList, cameraView);
        horizontalList.setAdapter(adapter);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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