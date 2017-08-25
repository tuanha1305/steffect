package com.facebeauty.com.beautysdk.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import com.facebeauty.com.beautysdk.display.CameraDisplay;
import com.facebeauty.com.beautysdk.domain.FileSave;
import com.facebeauty.com.beautysdk.utils.Accelerometer;
import com.facebeauty.com.beautysdk.utils.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by liupan on 17/8/14.
 */

public class CameraView extends RelativeLayout {
    private CameraDisplay mCameraDisplay;
    private SurfaceView mSurfaceViewOverlap;
    private Activity mContext;
    private Accelerometer mAccelerometer = null;
    public static final int MSG_SAVING_IMG = 1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_SAVING_IMG:
                    FileSave data = (FileSave) msg.obj;
                    Bundle bundle = msg.getData();
                    int imageWidth = bundle.getInt("imageWidth");
                    int imageHeight = bundle.getInt("imageHeight");
                    onPictureTaken(data.getBitmap(),data.getFile(), imageWidth, imageHeight);
                    break;
            }
        }
    };

    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(Activity context) {
        mContext = context;
        FileUtils.copyModelFiles(context);
        initView();
        initEvent();

    }

    private void initView() {
        mAccelerometer = new Accelerometer(mContext.getApplicationContext());
        GLSurfaceView glSurfaceView = new GLSurfaceView(mContext);
        addView(glSurfaceView);
        mSurfaceViewOverlap = new SurfaceView(mContext);
        addView(mSurfaceViewOverlap);
        mCameraDisplay = new CameraDisplay(mContext.getApplicationContext(), mListener, glSurfaceView);
        mCameraDisplay.setHandler(mHandler);
        mCameraDisplay.enableBeautify(true);
    }

    private void initEvent() {
        mSurfaceViewOverlap.setZOrderOnTop(true);
        mSurfaceViewOverlap.setZOrderMediaOverlay(true);
        mSurfaceViewOverlap.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//
//        mPaint = new Paint();
//        mPaint.setColor(Color.rgb(240, 100, 100));
//        int strokeWidth = 10;
//        mPaint.setStrokeWidth(strokeWidth);
//        mPaint.setStyle(Paint.Style.STROKE);

        mCameraDisplay.setFaceAttributeChangeListener(new CameraDisplay.FaceAttributeChangeListener() {
            @Override
            public void onFaceAttributeChanged(final String attribute) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mCameraDisplay.getFaceAttribute()) {
                            showFaceAttributeInfo();
                        }
                    }
                });
            }
        });

    }

    void showFaceAttributeInfo() {
        if (mCameraDisplay.getFaceAttributeString() != null) {
            if (mCameraDisplay.getFaceAttributeString().equals("noFace")) {
//                mAttributeText.setText("");
            } else {
//                mAttributeText.setText("第1张人脸: " + mCameraDisplay.getFaceAttributeString());
            }
        }
    }

    private CameraDisplay.ChangePreviewSizeListener mListener = new CameraDisplay.ChangePreviewSizeListener() {
        @Override
        public void onChangePreviewSize(final int previewW, final int previewH) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    mPreviewFrameLayout.requestLayout();
                    requestLayout();
                }

            });
        }
    };

    /**
     * 眉毛
     */
    public void setEyebrow(Bitmap bitmap,float[] eyeBrowColor){
        mCameraDisplay.setRightMeiMao(bitmap,eyeBrowColor);
        mCameraDisplay.setLeftMeiMao(bitmap,eyeBrowColor);
    }

    /**
     * 设置眼睫毛，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setEyelash(Bitmap bitmap,float[] eyelashColor) {
        mCameraDisplay.setYanJieMao(bitmap,eyelashColor);
    }

    /**
     * 设置眼线，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setEyeliner(Bitmap bitmap,float[] eyeLinerColor) {
        mCameraDisplay.setYanXian(bitmap,eyeLinerColor);
    }

    /**
     * 设置眼影，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setEyeShadow(Bitmap bitmap,float[] eyeShadowColor) {
        mCameraDisplay.setYanYing(bitmap,eyeShadowColor);
    }

    /**
     * 设置腮红，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setBlush(Bitmap bitmap,float[] blushColor) {
        mCameraDisplay.setSaihong(bitmap,blushColor);
    }

    /**
     * 设置上嘴唇，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param red
     * @param green
     * @param blue
     * @param alpha
     */
//    public void setUpMouse(float red, float green, float blue, float alpha) {
//        float _mousecolors[] = {red, green, blue, alpha};
//    }

    /**
     * 设置下嘴唇，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param red
     * @param green
     * @param blue
     * @param alpha
     */
    public void setLip(float red, float green, float blue, float alpha) {
       float _mousecolors[] = {red/255f, green/255f, blue/255f, alpha};
        mCameraDisplay.setDownMouseColors(_mousecolors);
        mCameraDisplay.setUpMouseColors(_mousecolors);
    }

    public void onResume() {
        mAccelerometer.start();
        mCameraDisplay.onResume();
    }

    public void onPause() {
        mAccelerometer.stop();
        mCameraDisplay.onPause();
    }

    public void onDestory() {
        mCameraDisplay.onDestroy();
    }

    public void saveImage(File file) {
        mCameraDisplay.setHandler(mHandler);
        mCameraDisplay.setSaveImage(file);
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

//            mHandler.sendEmptyMessage(CameraActivity.MSG_SAVED_IMG);
        }
    }

    /**
     * 1.关闭贴纸  2.无贴纸  3.贴纸
     *
     * @param position
     * @param path
     */
    public void setTiezhi(int position, String path) {
        if (position == 0) {
            mCameraDisplay.enableSticker(false);
        } else if (position == 1) {
            mCameraDisplay.enableSticker(true);
            mCameraDisplay.setShowSticker(null);
        } else if(position==2){
            mCameraDisplay.enableSticker(true);
            mCameraDisplay.setShowSticker(path);
        }
    }

    public void addTest(){

    }
}
