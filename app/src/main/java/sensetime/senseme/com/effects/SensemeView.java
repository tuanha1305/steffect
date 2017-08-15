package sensetime.senseme.com.effects;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import sensetime.senseme.com.effects.display.CameraDisplay;
import sensetime.senseme.com.effects.glutils.OpenGLUtils;
import sensetime.senseme.com.effects.utils.Accelerometer;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.STLicenseUtils;

/**
 * Created by liupan on 17/8/10.
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

    public void setLeftMeiMao(String leftMeiMaoUrl){
        if(TextUtils.isEmpty(leftMeiMaoUrl)){
            mCameraDisplay.setLeftMeiMao(null);
        }else {
//            mCameraDisplay.setLeftMeiMao(bitmap);
        }
    }
    public void setRightMeiMao(String rightMeiMaoUrl){
        if(TextUtils.isEmpty(rightMeiMaoUrl)){
            mCameraDisplay.setRightMeiMao(null);
        }else {
//            mCameraDisplay.setRightMeiMao(bitmap);
        }
    }
    public void setYanJieMao(String yanJieMaoUrl){
        if(TextUtils.isEmpty(yanJieMaoUrl)){
            mCameraDisplay.setYanJieMao(null);
        }else {
//            mCameraDisplay.setYanJieMao(bitmap);
        }

    }
    public void setYanXian(String yanXianUrl){
        if(TextUtils.isEmpty(yanXianUrl)){
            mCameraDisplay.setYanXian(null);
        }else {
//            mCameraDisplay.setYanXian(bitmap);
        }
    }
    public void setYanYing(String yanYingUrl){
        if(TextUtils.isEmpty(yanYingUrl)){
            mCameraDisplay.setYanYing(null);
        }else {
//            mCameraDisplay.setYanYing(bitmap);
        }

    }

    public void setSaihong(String saihongUrl) {//int resourceId
        if(TextUtils.isEmpty(saihongUrl)){
            mCameraDisplay.setSaihong(null);
        }else {
//            mCameraDisplay.setSaihong(bitmap);
        }
//        Bitmap bitmap = null;
//        if (resourceId!=-1){
//            bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId);
//        }
//        mCameraDisplay.setSaihong(bitmap);
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
