package com.effects.senseme.sensemesdk.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.effects.senseme.sensemesdk.display.CameraDisplay;
import com.effects.senseme.sensemesdk.utils.Accelerometer;
import com.effects.senseme.sensemesdk.utils.FileUtils;
import com.effects.senseme.sensemesdk.utils.STLicenseUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

/**
 * Created by liupan on 17/8/14.
 */

public class SensemeView extends RelativeLayout {
    private CameraDisplay mCameraDisplay;
    private SurfaceView mSurfaceViewOverlap;
    private Activity mContext;
    private Accelerometer mAccelerometer = null;

    public SensemeView(Context context) {
        super(context);
    }

    public SensemeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SensemeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(Activity context) {
        mContext = context;

        FileUtils.copyModelFiles(context);

        FileUtils.copyModelFiles(context);
        if (!ImageLoader.getInstance().isInited()) {
            ImageLoaderConfiguration imageLoaderConfiguration = ImageLoaderConfiguration.createDefault(context);
            ImageLoader.getInstance().init(imageLoaderConfiguration);
        }

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
        //        mCameraDisplay.setHandler(mHandler);

        mCameraDisplay.enableBeautify(true);
    }

    private void initEvent() {
        // authority
        if (!STLicenseUtils.checkLicense(mContext)) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext.getApplicationContext(), "You should be authorized first!", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }


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
            Log.d("liupan SensemeView  ", "onChangePreviewSize");
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    mPreviewFrameLayout.requestLayout();
                    requestLayout();
                }

            });
        }
    };

    public void setLeftMeiMao(String leftMeiMaoUrl) {
        if (TextUtils.isEmpty(leftMeiMaoUrl)) {
            mCameraDisplay.setLeftMeiMao(null);
        } else {
            ImageLoader.getInstance().loadImage(leftMeiMaoUrl, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {

                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    if (bitmap != null) {
                        mCameraDisplay.setLeftMeiMao(bitmap);
                    }

                }

                @Override
                public void onLoadingCancelled(String s, View view) {

                }
            });

        }
    }

    public void setRightMeiMao(String rightMeiMaoUrl) {
        if (TextUtils.isEmpty(rightMeiMaoUrl)) {
            mCameraDisplay.setRightMeiMao(null);
        } else {
            ImageLoader.getInstance().loadImage(rightMeiMaoUrl, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {

                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    if (bitmap != null) {
                        mCameraDisplay.setRightMeiMao(bitmap);
                    }

                }

                @Override
                public void onLoadingCancelled(String s, View view) {

                }
            });
        }
    }

    public void setYanJieMao(String yanJieMaoUrl) {
        if (TextUtils.isEmpty(yanJieMaoUrl)) {
            mCameraDisplay.setYanJieMao(null);
        } else {
            ImageLoader.getInstance().loadImage(yanJieMaoUrl, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {

                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    if (bitmap != null) {
                        mCameraDisplay.setYanJieMao(bitmap);
                    }

                }

                @Override
                public void onLoadingCancelled(String s, View view) {

                }
            });
        }

    }

    public void setYanXian(String yanXianUrl) {
        if (TextUtils.isEmpty(yanXianUrl)) {
            mCameraDisplay.setYanXian(null);
        } else {
            ImageLoader.getInstance().loadImage(yanXianUrl, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {

                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    if (bitmap != null) {
                        mCameraDisplay.setYanXian(bitmap);
                    }

                }

                @Override
                public void onLoadingCancelled(String s, View view) {

                }
            });
        }
    }

    public void setYanYing(String yanYingUrl) {
        if (TextUtils.isEmpty(yanYingUrl)) {
            mCameraDisplay.setYanYing(null);
        } else {
            ImageLoader.getInstance().loadImage(yanYingUrl, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {

                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    if (bitmap != null) {
                        mCameraDisplay.setYanYing(bitmap);
                    }

                }

                @Override
                public void onLoadingCancelled(String s, View view) {

                }
            });
        }

    }

    public void setSaihong(String saihongUrl) {
        if (TextUtils.isEmpty(saihongUrl)) {
            mCameraDisplay.setSaihong(null);
        } else {
            ImageLoader.getInstance().loadImage(saihongUrl, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {

                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    if (bitmap != null) {
                        mCameraDisplay.setSaihong(bitmap);
                    }

                }

                @Override
                public void onLoadingCancelled(String s, View view) {

                }
            });
        }
    }

    public void setUpMouse(float red, float green, float blue, float alpha) {
        float _mousecolors[] = {red, green, blue, alpha};
        mCameraDisplay.setUpMouseColors(_mousecolors);
    }

    public void setDownMouse(float red, float green, float blue, float alpha) {
        float _mousecolors[] = {red, green, blue, alpha};
        mCameraDisplay.setDownMouseColors(_mousecolors);
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
}
