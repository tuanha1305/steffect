package com.facebeauty.com.beautysdk.view;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import com.facebeauty.com.beautysdk.display.CameraDisplay2;
import com.facebeauty.com.beautysdk.utils.Accelerometer;
import com.sensetime.stmobile.model.STPoint;

/**
 * Created by wangdi on 2017/10/10.
 */

public class ImageCameraView extends RelativeLayout {
    private SurfaceView mSurfaceViewOverlap;
    private Activity mContext;
    private Accelerometer mAccelerometer = null;

    public ImageCameraView(Context context) {
        super(context);
    }
    public ImageCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ImageCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    private void initView() {
        mAccelerometer = new Accelerometer(mContext.getApplicationContext());
        GLSurfaceView glSurfaceView = new GLSurfaceView(mContext);
        addView(glSurfaceView);
        mSurfaceViewOverlap = new SurfaceView(mContext);
        addView(mSurfaceViewOverlap);
    }
}
