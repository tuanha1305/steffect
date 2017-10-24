package com.facebeauty.com.beautysdk.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import com.facebeauty.com.beautysdk.display.CameraDisplay2;
import com.facebeauty.com.beautysdk.display.ImageDisplay;
import com.facebeauty.com.beautysdk.domain.FileSave;
import com.facebeauty.com.beautysdk.utils.Accelerometer;
import com.facebeauty.com.beautysdk.utils.FileUtils;
import com.sensetime.stmobile.model.STPoint;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by wangdi on 2017/10/12.
 */

public class ImageCamerView extends RelativeLayout {
    //private GLSurfaceView mGlSurfaceView;
    private GLSurfaceView mGlSurfaceView;
    private Activity mContext;
    private ImageDisplay mImageDisplay;
    public static final int MSG_SAVING_IMG = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SAVING_IMG: {
                    FileSave data = (FileSave) msg.obj;
                    Bundle bundle = msg.getData();
                    int imageWidth = data.getBitFile().getWidth();
                    int imageHeight = data.getBitFile().getHeight();
                    onPictureTaken(data.getBitmap(), data.getFile(), imageWidth, imageHeight);
                }
                break;
            }
        }
    };

    public ImageCamerView(Context context) {
        super(context);
    }

    public ImageCamerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageCamerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void onPictureTaken(ByteBuffer data, File file, int mImageWidth, int mImageHeight) {
        if (mImageWidth <= 0 || mImageHeight <= 0)
            return;
        Bitmap srcBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        data.position(0);
        srcBitmap.copyPixelsFromBuffer(data);
        saveToSDCard(file, srcBitmap);
        srcBitmap.recycle();
    }

    private void saveToSDCard(File file, Bitmap bmp) {

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        if (mHandler != null) {
            String path = file.getAbsolutePath();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            mContext.sendBroadcast(mediaScanIntent);

            if (Build.VERSION.SDK_INT >= 19) {
                MediaScannerConnection.scanFile(mContext, new String[]{path}, null, null);
            }
        }
    }

    private void initView() {
        mGlSurfaceView = new GLSurfaceView(mContext);
        addView(mGlSurfaceView);
        mImageDisplay = new ImageDisplay(mContext.getApplicationContext(), mGlSurfaceView);
//        mImageDisplay.setHandler(mHandler);
        mImageDisplay.enableBeautify(true);
    }

    public void init(Activity context) {
        mContext = context;
        FileUtils.copyModelFiles(context);
        initView();
//        initEvent();
        mImageDisplay.onResume();
    }

    public void setImageBitmap(Bitmap bitmap) {
        mImageDisplay.setImageBitmap(bitmap);
    }

    public void setEyeLips(Bitmap bitmap, float[] foundationColor) {
        mImageDisplay.setEyeLips(bitmap, foundationColor);
    }

    public void setFaceandJaw(float faceVale, float jawValue) {
        mImageDisplay.setJawandFace(faceVale, jawValue);
    }

    public void saveImage(File file) {
           mImageDisplay.setHandler(mHandler);
           mImageDisplay.enableSave(true,file);
//        takeScreenShot();

    }
}
