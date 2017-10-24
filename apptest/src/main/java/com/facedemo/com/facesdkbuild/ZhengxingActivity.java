package com.facedemo.com.facesdkbuild;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.facebeauty.com.beautysdk.glutils.STUtils;
import com.facebeauty.com.beautysdk.utils.FileUtils;
import com.facebeauty.com.beautysdk.view.ImageCamerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ZhengxingActivity extends AppCompatActivity {

    private final int REQUEST_PICK_IMAGE = 1;
    private ImageCamerView camerView;
    private Button button,save,shuangyanpin;
    private float count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zhengxing);
        camerView = (ImageCamerView) findViewById(R.id.myImageCameraView);
        camerView.init(this);

        button = (Button)findViewById(R.id.set);
        save = (Button)findViewById(R.id.save);
        shuangyanpin = (Button)findViewById(R.id.shuangyanpin);
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/1.jpg";
                File file = new File(path);
                camerView.saveImage(file);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camerView.setFaceandJaw(1f,1f);

            }
        });

        shuangyanpin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                                Bitmap image = null;
                AssetManager am = getResources().getAssets();
                try
                {
                    InputStream is = getClass().getResourceAsStream("/assets/shuangyanpi.png");
                    image = BitmapFactory.decodeStream(is);
                    is.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
//                float[] color = {0.0f,0.0f,0.0f,0.0f};
                camerView.setEyeLips(image,null);
            }
        });


    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();
                        Bitmap bitmap = null;
                        if("file".equals(uri.getScheme())){
                            bitmap = STUtils.getBitmapFromFile(uri);
                        } else {
                            bitmap  = STUtils.getBitmapAfterRotate(uri, ZhengxingActivity.this);
                        }
                        if(bitmap != null && camerView != null) {
                            camerView.setImageBitmap(bitmap);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }



    /**
     * 保存图片
     *
     * @param b
     * @param strFileName
     */
    private static void savePic(Bitmap b, String strFileName) {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(strFileName);
            if (null != fos) {
                b.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
