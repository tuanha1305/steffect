package com.facedemo.com.facesdkbuild;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.facebeauty.com.beautysdk.domain.Brand;
import com.facebeauty.com.beautysdk.utils.STLicenseUtils;
import com.facebeauty.com.beautysdk.view.CameraView;
import com.facedemo.com.facesdkbuild.adapter.DemoAdapter;
import com.facedemo.com.facesdkbuild.view.HorizontalListView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends Activity {
    public static final String TAG="MainActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private HorizontalListView horizontalList;
    CameraView cameraView;
    private String path = "http://api.7fineday.com/front/api/face/authkey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
 checkPremission();
        }
        STLicenseUtils.checkLicense(this, new STLicenseUtils.OnCheckLicenseListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 授权成功");

                cameraView = (CameraView) findViewById(R.id.cameraView);
                cameraView.init(MainActivity.this);
                horizontalList = (HorizontalListView) findViewById(R.id.horizontalList);
                String data = openAssetsFile("makeuplist.json");
                JSONObject jsonObject = JSON.parseObject(data);
                String dataStr = jsonObject.getString("data");
//       JSONObject dataJsonObject=  jsonObject.getJSONObject("data");
                List<Brand> brandList = JSON.parseArray(dataStr, Brand.class);
                DemoAdapter adapter = new DemoAdapter(MainActivity.this, brandList, cameraView);
                horizontalList.setAdapter(adapter);
            }

            @Override
            public void onFail() {
                Log.d(TAG, "onSuccess: 授权失败");

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


    private void checkPremission() {
        final String permission = Manifest.permission.CAMERA;  //相机权限
        final String permission1 = Manifest.permission.WRITE_EXTERNAL_STORAGE; //写入数据权限
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission1) != PackageManager.PERMISSION_GRANTED) {  //先判断是否被赋予权限，没有则申请权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {  //给出权限申请说明
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            } else { //直接申请权限
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 100); //申请权限，可同时申请多个权限，并根据用户是否赋予权限进行判断
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {  //申请权限的返回值
            case 100:
                int length = grantResults.length;
                final boolean isGranted = length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[length - 1];
                if (isGranted) {  //如果用户赋予权限，则调用相机
                }else{ //未赋予权限，则做出对应提示

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
