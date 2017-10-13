package com.facebeauty.com.beautysdk.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebeauty.com.beautysdk.MyAndroidSequenceEncoder;
import com.facebeauty.com.beautysdk.R;
import com.facebeauty.com.beautysdk.display.CameraDisplay;
import com.facebeauty.com.beautysdk.display.CameraDisplay2;
import com.facebeauty.com.beautysdk.domain.FileSave;
import com.facebeauty.com.beautysdk.utils.Accelerometer;
import com.facebeauty.com.beautysdk.utils.FileUtils;
import com.sensetime.stmobile.model.STPoint;

import org.jcodec.api.android.SequenceEncoder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by liupan on 17/8/14.
 */

public class CameraView extends RelativeLayout {
    private CameraDisplay2 mCameraDisplay;
    private SurfaceView mSurfaceViewOverlap;
    private Activity mContext;
    private Accelerometer mAccelerometer = null;
    public static final int MSG_SAVING_IMG = 1;
    public static final int MSG_TAKE_SCREEN_SHOT = 2;
    public static final int MSG_TAKE_SCREEN_SHOT_REACH_MAX_TIME = 3;
    public static final int MSG_TAKE_SCREEN_SHOT_END = 4;

    private boolean mTakingScreenShoot = false;
//    LinkedList<Bitmap> byteBuffers = new LinkedList<>();
//    LinkedList<ByteBuffer> byteBuffers = new LinkedList<>();
//    LinkedList<Integer> imageWidths = new LinkedList<>();
//    LinkedList<Integer> imageHeights = new LinkedList<>();
    List<Bitmap> bitmaps = new ArrayList<Bitmap>();
//    int position;
Bitmap bitmap;
    int count;

//    Runnable runnable = new Runnable() {
//        @Override
//        public void run() {
//            while (mTakingScreenShoot) {
//                if (byteBuffers.size() > 0 && position != (byteBuffers.size() - 1)) {
//                    long  time1 = System.currentTimeMillis();
//                    onTakeScreenShot(byteBuffers.get(position), imageWidths.get(position), imageHeights.get(position));
//                    position++;
//                    long time2 = System.currentTimeMillis();
//                    Log.d("liupan", "liupan----CameraView time1==" + time1);
//                    Log.d("liupan", "liupan-----CameraView time2==" + time2);
//                    Log.d("liupan", "liupan-----CameraView preprocess===" + (time2-time1));
//                }
//            }
//        }
//    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SAVING_IMG: {
                    FileSave data = (FileSave) msg.obj;
                    Bundle bundle = msg.getData();
                    int imageWidth = bundle.getInt("imageWidth");
                    int imageHeight = bundle.getInt("imageHeight");
                    onPictureTaken(data.getBitmap(), data.getFile(), imageWidth, imageHeight);
                }

                break;
                case MSG_TAKE_SCREEN_SHOT: {
//                    ByteBuffer byteBuffer = (ByteBuffer) msg.obj;
                     bitmap = (Bitmap) msg.obj;
//                    Bundle bundle = msg.getData();
//                    int imageWidth = bundle.getInt("imageWidth");
//                    int imageHeight = bundle.getInt("imageHeight");
//                    if (mCameraDisplay.getTakingScreenShoot()){
////                        byteBuffers.add(bitmap);
//                        byteBuffers.add(byteBuffer);
//                        imageWidths.add(imageWidth);
//                        imageHeights.add(imageHeight);
                        count++;
                        Log.d("liupan","liupan count =" +count);
//                    }
                    bitmaps.add(bitmap);
                }
                break;
                case MSG_TAKE_SCREEN_SHOT_REACH_MAX_TIME:
                    if (mTakingScreenShoot)
                        endRecoderScreen(true);
                    break;
                case MSG_TAKE_SCREEN_SHOT_END: {
                    mTakingScreenShoot = false;
//                    byteBuffers.clear();
//                    imageHeights.clear();
//                    imageWidths.clear();
                    Log.d("liupan","liupan 录屏结束");
                    Toast.makeText(getContext(), "录屏结束", Toast.LENGTH_SHORT).show();
                }
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
        mCameraDisplay.onResume();
    }

    public interface OnFacePointsChangeListener {
        void onChangeListener(STPoint[] pointsBrowLeft, STPoint[] pointsBrowRight, STPoint[] pointsEyeLeft, STPoint[] pointsEyeRight, STPoint[] pointsLips);
    }

    private List<OnFacePointsChangeListener> mFacePointsListeners = new ArrayList<>();

    //添加监听
    public void registerFacePointsChangeListener(OnFacePointsChangeListener onFacePointsChangeListener) {
        if (onFacePointsChangeListener == null)
            return;
        mFacePointsListeners.add(onFacePointsChangeListener);
    }

    //删除监听
    public void unregisterFacePointsChangeListener(OnFacePointsChangeListener onFacePointsChangeListener) {
        if (onFacePointsChangeListener == null)
            return;
        if (mFacePointsListeners.contains(onFacePointsChangeListener)) {
            mFacePointsListeners.remove(onFacePointsChangeListener);
        }
    }

    //清空所有监听
    public void resetFacePointsChangeListener() {
        mFacePointsListeners.clear();
    }

    private void initView() {
        mAccelerometer = new Accelerometer(mContext.getApplicationContext());
        GLSurfaceView glSurfaceView = new GLSurfaceView(mContext);
        addView(glSurfaceView);
        mSurfaceViewOverlap = new SurfaceView(mContext);
        addView(mSurfaceViewOverlap);
        mCameraDisplay = new CameraDisplay2(mContext.getApplicationContext(), mListener, glSurfaceView);
        mCameraDisplay.setHandler(mHandler);
        mCameraDisplay.enableBeautify(true);
        mCameraDisplay.registerCameraDisplayFacePointsChangeListener(new CameraDisplay2.OnCameraDisplayFacePointsChangeListener() {
            @Override
            public void onChangeListener(STPoint[] pointsBrowLeft, STPoint[] pointsBrowRight, STPoint[] pointsEyeLeft, STPoint[] pointsEyeRight, STPoint[] pointsLips) {
                for (OnFacePointsChangeListener onFacePointsChangeListener : mFacePointsListeners) {
                    onFacePointsChangeListener.onChangeListener(pointsBrowLeft, pointsBrowRight, pointsEyeLeft, pointsEyeRight, pointsLips);
                }
            }
        });
    }

    private void initEvent() {
        mSurfaceViewOverlap.setZOrderOnTop(true);
        mSurfaceViewOverlap.setZOrderMediaOverlay(true);
        mCameraDisplay.setFaceAttributeChangeListener(new CameraDisplay2.FaceAttributeChangeListener() {
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

    private CameraDisplay2.ChangePreviewSizeListener mListener = new CameraDisplay2.ChangePreviewSizeListener() {
        @Override
        public void onChangePreviewSize(final int previewW, int previewH) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    requestLayout();
                }

            });
        }
    };

    /**
     * 眉毛
     */
    public void setEyebrow(Bitmap bitmap, float[] bocolor) {
        mCameraDisplay.setRightMeiMao(bitmap, bocolor);
        mCameraDisplay.setLeftMeiMao(bitmap, bocolor);
    }

    /**
     * 设置眼睫毛，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setEyelash(Bitmap bitmap, float[] bocolor) {
        mCameraDisplay.setYanJieMao(bitmap, bocolor);
    }

    /**
     * 设置眼线，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setEyeliner(Bitmap bitmap, float[] eyeLinerColor) {
        mCameraDisplay.setYanXian(bitmap, eyeLinerColor);
    }
    /**
     * 设置双眼皮呢，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setEyelids(Bitmap bitmap, float[] eyeLinerColor) {
        mCameraDisplay.setYanXian(bitmap, eyeLinerColor);
    }

    /**
     * 设置眼影，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setEyeShadow(Bitmap bitmap, float[] eyeShadowColor) {
        mCameraDisplay.setYanYing(bitmap, eyeShadowColor);
    }

    /**
     * 设置腮红，如果为空，恢复默认设置-默认设置为没有美妆
     *
     * @param bitmap
     */
    public void setBlush(Bitmap bitmap, float[] blushColor) {
        mCameraDisplay.setSaihong(bitmap, blushColor);
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
        float _mousecolors[] = {red / 255f, green / 255f, blue / 255f, alpha};
        mCameraDisplay.setDownMouseColors(_mousecolors);
        mCameraDisplay.setUpMouseColors(_mousecolors);
    }

    public void setFoundation(Bitmap bitmap, float[] foundationColor){
        mCameraDisplay.setFendi(bitmap,foundationColor);
    }


    public void onResume() {
        mAccelerometer.start();
        mCameraDisplay.startSurface();
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

//    private void onTakeScreenShot(ByteBuffer data, int mImageWidth, int mImageHeight) {
//        if (mImageWidth <= 0 || mImageHeight <= 0)
//            return;
//        Bitmap srcBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
//        data.position(0);
//        srcBitmap.copyPixelsFromBuffer(data);
//
//        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/facesdk");
//        if (!directory.exists()) {
//            directory.mkdirs();
//        }
//        String fileName = directory.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".png";
//        try {
//            FileOutputStream out = new FileOutputStream(fileName);
//            srcBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        srcBitmap.recycle();
//    }

    private void onTakeScreenShot(Bitmap srcBitmap, int mImageWidth, int mImageHeight) {
        if (mImageWidth <= 0 || mImageHeight <= 0)
            return;
//        Bitmap srcBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
//        data.position(0);
//        srcBitmap.copyPixelsFromBuffer(data);

        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/facesdk");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = directory.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".png";
        try {
            FileOutputStream out = new FileOutputStream(fileName);
            srcBitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        srcBitmap.recycle();
    }

    public boolean isRecoderScreen() {
//        return mCameraDisplay.getTakingScreenShoot();
        return mTakingScreenShoot;
    }

    public void startRecoderScreen() {
        mCameraDisplay.setTakingScreenShoot(true);
        Toast.makeText(getContext(), "录屏开始", Toast.LENGTH_SHORT).show();
        mTakingScreenShoot = true;
        mHandler.sendEmptyMessageDelayed(MSG_TAKE_SCREEN_SHOT_REACH_MAX_TIME, 15 * 1000);
//        new Thread(runnable).start();
    }

    public void endRecoderScreen() {
        if (mTakingScreenShoot) {
            endRecoderScreen(false);
        } else {
            Toast.makeText(getContext(), "录屏已经结束", Toast.LENGTH_SHORT).show();
        }
    }

    private void endRecoderScreen(final boolean bTimeout) {
        mCameraDisplay.setTakingScreenShoot(false);
        if (bTimeout) {
            Toast.makeText(getContext(), "录屏最长15秒时间,录屏结束中，请稍等", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "录屏结束中，请稍等", Toast.LENGTH_SHORT).show();
        }

////        new Thread(new Runnable() {
////            @Override
////            public void run() {
//                File destdirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/facesdkdest");
//                if (!destdirectory.exists()) {
//                    destdirectory.mkdirs();
//                }
//                String destFileName = destdirectory.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp4";
//                File destFile = new File(destFileName);
//                MyAndroidSequenceEncoder sequenceEncoderMp4;
////                try {
////                    sequenceEncoderMp4 = new MyAndroidSequenceEncoder(destFile);
////
//////                    for (Bitmap frame : byteBuffers) {
//////                        sequenceEncoderMp4.encodeImage(frame);
//////                    }
////
//////                    ByteBuffer byteBuffer = mCameraDisplay.getmTmpBuffer();
//////                    for(int i =0;i<byteBuffer.){
//////
//////                    }
////                    for (int i = 0;i<byteBuffers.size();i++) {
////                         ByteBuffer byteBuffer = byteBuffers.get(i);
////                         Bitmap srcBitmap = Bitmap.createBitmap(imageWidths.get(i), imageHeights.get(i), Bitmap.Config.ARGB_4444);
////                         byteBuffer.position(0);
////                         srcBitmap.copyPixelsFromBuffer(byteBuffer);
////                         sequenceEncoderMp4.encodeImage(srcBitmap);
////                         srcBitmap.recycle();
////                    }
////
////                    sequenceEncoderMp4.finish();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//////                catch (Throwable throwable){
//////                    throwable.printStackTrace();
//////                }finally {
//////                }
//////                for (Bitmap frame : byteBuffers) {
//////                    if(!frame.isRecycled()){
//////                        frame.recycle();
//////                    }
//////                }
////                Message message = Message.obtain();
////                message.what = MSG_TAKE_SCREEN_SHOT_END;
////                mHandler.sendMessage(message);
////            }
////        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File destdirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/facesdkdest");
                if (!destdirectory.exists()) {
                    destdirectory.mkdirs();
                }
                String destFileName = destdirectory.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp4";
                File destFile = new File(destFileName);
                MyAndroidSequenceEncoder sequenceEncoderMp4;
                try {
                    sequenceEncoderMp4 = new MyAndroidSequenceEncoder(destFile);
                    for (Bitmap frame : bitmaps) {
                        sequenceEncoderMp4.encodeImage(frame);
                    }
                    sequenceEncoderMp4.finish();

                    Message message = Message.obtain();
                    message.what = MSG_TAKE_SCREEN_SHOT_END;
                    mHandler.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        } else {
            mCameraDisplay.enableSticker(true);
            mCameraDisplay.setShowSticker(path);
        }
    }

    public Surface getSurfaceView() {
        return mSurfaceViewOverlap.getHolder().getSurface();
    }


    /**
     * 16：9
     *
     * @param mCurrentPreview
     */
    public void changePreviewSize(int mCurrentPreview) {
        mCameraDisplay.changePreviewSize(mCurrentPreview);
    }

    /**
     * 切换摄像头
     */
    public void changeChoice() {
        mCameraDisplay.switchCamera();
    }

    /**
     * 一键卸妆
     */
    public void cleanMakeUp() {
        float[] color = {0, 0, 0, 0};
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.cosmetic_blank);
        setEyebrow(bitmap, color);
        setBlush(bitmap, color);
        setEyeShadow(bitmap, color);
        setEyelash(bitmap, color);
        setEyeliner(bitmap, color);
        setEyebrow(bitmap, color);
        setLip(0, 0, 0, 0);
    }
}
