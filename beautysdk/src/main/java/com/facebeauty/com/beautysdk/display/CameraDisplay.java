package com.facebeauty.com.beautysdk.display;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.facebeauty.com.beautysdk.R;
import com.facebeauty.com.beautysdk.camera.CameraProxy;
import com.facebeauty.com.beautysdk.domain.FileSave;
import com.facebeauty.com.beautysdk.glutils.GlUtil;
import com.facebeauty.com.beautysdk.glutils.OpenGLUtils;
import com.facebeauty.com.beautysdk.glutils.STUtils;
import com.facebeauty.com.beautysdk.glutils.TextureRotationUtil;
import com.facebeauty.com.beautysdk.utils.Accelerometer;
import com.facebeauty.com.beautysdk.utils.FileUtils;
import com.facebeauty.com.beautysdk.utils.LogUtils;
import com.facebeauty.com.beautysdk.view.CameraView;
import com.sensetime.stmobile.STBeautifyNative;
import com.sensetime.stmobile.STBeautyParamsType;
import com.sensetime.stmobile.STCommon;
import com.sensetime.stmobile.STFaceAttribute;
import com.sensetime.stmobile.STFilterParamsType;
import com.sensetime.stmobile.STHumanAction;
import com.sensetime.stmobile.STHumanActionParamsType;
import com.sensetime.stmobile.STMobileFaceAttributeNative;
import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.STMobileObjectTrackNative;
import com.sensetime.stmobile.STMobileStickerNative;
import com.sensetime.stmobile.STMobileStreamFilterNative;
import com.sensetime.stmobile.model.STMobile106;
import com.sensetime.stmobile.model.STPoint;
import com.sensetime.stmobile.model.STRect;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * CameraDisplay is used for camera preview
 */

/**
 * 渲染结果显示的Render, 用户最终看到的结果在这个类中得到并显示.
 * 请重点关注Camera.PreviewCallback的onPreviewFrame/onSurfaceCreated,onSurfaceChanged,onDrawFrame
 * 四个接口, 基本上所有的处理逻辑都是围绕这四个接口展开
 */
public class CameraDisplay implements Renderer {

    private String TAG = "CameraDisplay";
    /**
     * SurfaceTexure texture id
     */
    protected int mTextureId = OpenGLUtils.NO_TEXTURE;

    private int mImageWidth;
    private int mImageHeight;
    private GLSurfaceView mGlSurfaceView;
    private ChangePreviewSizeListener mListener;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private Context mContext;
    public CameraProxy mCameraProxy;
    private SurfaceTexture mSurfaceTexture;
    private String mCurrentSticker;
    private String mCurrentFilterStyle;
    private float mCurrentFilterStrength = 0.5f;//阈值为[0,1]
    private float mFilterStrength = 0.5f;
    private String mFilterStyle;
    private int mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private STGLRender mGLRender;
    private STMobileStickerNative mStStickerNative = new STMobileStickerNative();
    private STBeautifyNative mStBeautifyNative = new STBeautifyNative();
    private STMobileHumanActionNative mSTHumanActionNative = new STMobileHumanActionNative();
    private STMobileStreamFilterNative mSTMobileStreamFilterNative = new STMobileStreamFilterNative();
    private STMobileFaceAttributeNative mSTFaceAttributeNative = new STMobileFaceAttributeNative();
    private STMobileObjectTrackNative mSTMobileObjectTrackNative = new STMobileObjectTrackNative();

    private ByteBuffer mRGBABuffer;
    private int[] mBeautifyTextureId;
    private int[] mTextureOutId;
    private int[] mFilterTextureOutId;
    private int[] mMeiMaoTextureId;
    private boolean mCameraChanging = false;
    private int mCurrentPreview = 0;
    private ArrayList<String> mSupportedPreviewSizes;

    private FpsChangeListener mFpsListener;
    private FaceAttributeChangeListener mFaceAttributeChangeListener;
    private long mStartTime;
    private boolean mShowOriginal = false;
    private boolean mNeedBeautify = true;
    private boolean mNeedFaceAttribute = false;
    private boolean mNeedUpdateFaceAttribute = true;
    private boolean mNeedSticker = false;
    private boolean mNeedFilter = false;
    private boolean mNeedSave = false;
    private boolean mNeedObject = false;
    private FloatBuffer mTextureBuffer;
    private float[] mBeautifyParams = new float[6];
    private STPoint[] stPoint240;
    private File file;


    public static int[] beautyTypes = {
            STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH,
            STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH,
            STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH,
            STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO,
            STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO,
            STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO
    };
    private Handler mHandler;
    private String mFaceAttribute;
    private Handler mHandlerUpdateAtrrbute = new Handler();
    private boolean mIsPaused = false;
    private int mDetectConfig = 0;
    private boolean mIsCreateHumanActionHandleSucceeded = false;
    private Object mHumanActionHandleLock = new Object();

    private boolean mNeedShowRect = true;
    private int mScreenIndexRectWidth = 0;
    private int mIndexRectWidthSmall = 128;//640X480
    private int mIndexRectWidthLarge = 256;//1280X720

    private Rect mTargetRect = new Rect();
    private Rect mIndexRect = new Rect();
    private boolean mNeedSetObjectTarget = true;
    private boolean mIsObjectTracking = true;
    private  STPoint[] pointsBrowLeft;
    //face extra info swicth
    private boolean mNeedFaceExtraInfo = true;
    private int mHumanActionCreateConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO;

    int textureMId = OpenGLUtils.NO_TEXTURE;
    int texture_left = OpenGLUtils.NO_TEXTURE;
    int texture_right =  OpenGLUtils.NO_TEXTURE;
    int saiHong = OpenGLUtils.NO_TEXTURE;



    public interface ChangePreviewSizeListener {
        void onChangePreviewSize(int previewW, int previewH);
    }

    public CameraDisplay(Context context, ChangePreviewSizeListener listener, GLSurfaceView glSurfaceView) {
        mCameraProxy = new CameraProxy(context);
        mGlSurfaceView = glSurfaceView;
        mListener = listener;
        mContext = context;
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        mTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
        mGLRender = new STGLRender();

        if(mNeedFaceExtraInfo){
            mHumanActionCreateConfig = mHumanActionCreateConfig | STCommon.ST_MOBILE_ENABLE_FACE_240_DETECT;
        }

        //初始化非OpengGL相关的句柄，包括人脸检测及人脸属性
        initHumanAction(); //因为人脸模型加载较慢，建议异步调用
        initFaceAttribute();
        initObjectTrack();

    }

    public void setFpsChangeListener(FpsChangeListener listener) {
        mFpsListener = listener;
    }
    public void setFaceAttributeChangeListener(FaceAttributeChangeListener listener) {
        mFaceAttributeChangeListener = listener;
    }
    public void enableBeautify(boolean needBeautify) {
        mNeedBeautify = needBeautify;
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
    }

    public void enableFaceAttribute(boolean needFaceAttribute) {
        mNeedFaceAttribute = needFaceAttribute;
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
    }

    private String genFaceAttributeString(STFaceAttribute arrayFaceAttribute){
        String attribute = null;
        String gender = arrayFaceAttribute.arrayAttribute[2].label;
        if(gender.equals("male")){
            gender = "男";
        }else{
            gender = "女";
        }
        attribute = "颜值:" + arrayFaceAttribute.arrayAttribute[1].label + " "
                + "性别:" + gender + " "
                + "年龄:"+arrayFaceAttribute.arrayAttribute[0].label + " ";
        return attribute;
    }

    public void enableSticker(boolean needSticker){
        mNeedSticker = needSticker;
    }

    public void enableFilter(boolean needFilter){
        mNeedFilter = needFilter;
    }

    public boolean getFaceAttribute() {
        return mNeedFaceAttribute;
    }

    public String getFaceAttributeString() {
        return mFaceAttribute;
    }

    public boolean getSupportPreviewsize(int size) {
        if(size == 0 && mSupportedPreviewSizes.contains("640x480")){
            return true;
        }else if(size == 1 && mSupportedPreviewSizes.contains("1280x720")){
            return true;
        }else{
            return false;
        }
    }

    public void setSaveImage(File file) {
        this.file =file;
        mNeedSave = true;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view创建的时候调用
     *
     * @param gl
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        LogUtils.i(TAG, "onSurfaceCreated");
        if (mIsPaused == true) {
            return ;
        }
        GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);

        if (mCameraProxy.getCamera() != null) {
            setUpCamera();
        }

        //初始化GL相关的句柄，包括美颜，贴纸，滤镜
        initBeauty();
        initSticker();
        initFilter();
        initMeizhuang();
    }

    private void initFaceAttribute() {
        int result = mSTFaceAttributeNative.createInstance(FileUtils.getFaceAttributeModelPath(mContext));
        LogUtils.i(TAG, "the result for createInstance for faceAttribute is %d", result);
    }

    private void initHumanAction() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mHumanActionHandleLock) {
                    int result = mSTHumanActionNative.createInstance(FileUtils.getTrackModelPath(mContext), mHumanActionCreateConfig);
                    LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);

                    if (result == 0) {
                        mIsCreateHumanActionHandleSucceeded = true;
                        mSTHumanActionNative.setParam(STHumanActionParamsType.ST_HUMAN_ACTION_BACKGROUND_BLUR_STRENGTH, 0.35f);
                    }
                }
            }
        }).start();
    }

    private void initSticker() {
        int result = mStStickerNative.createInstance(mCurrentSticker);
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
        LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);
    }

    private void initMeizhuang(){
        if (bSaihongDirty && saihongBitmap != null) {
            textSiaHongId = OpenGLUtils.loadTexture(saihongBitmap, textSiaHongId, false);
        } else {
            Bitmap saihongBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.cosmetic_blank);
            textSiaHongId = OpenGLUtils.loadTexture(saihongBitmap, textSiaHongId, true);
        }

        if (bLeftMeiDirty && leftMeiBitmap != null) {
            textLeftMeiMaoId = OpenGLUtils.loadTexture(leftMeiBitmap, textLeftMeiMaoId, false);
        } else {
            Bitmap leftMeiBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.cosmetic_blank);
            textLeftMeiMaoId = OpenGLUtils.loadTexture(leftMeiBitmap, textLeftMeiMaoId, true);
        }

        if (bRightMeiDirty && rightMeiBitmap != null) {
            textRightMeiMaoId = OpenGLUtils.loadTexture(rightMeiBitmap, textRightMeiMaoId, false);
        } else {
            Bitmap rightMeiMaobitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.cosmetic_blank);
            textRightMeiMaoId = OpenGLUtils.loadTexture(rightMeiMaobitmap, textRightMeiMaoId, true);
        }

        if (bYanXianDirty && yanXianBitmap != null) {
            textYanXianId = OpenGLUtils.loadTexture(yanXianBitmap, textYanXianId, false);
        } else {
            Bitmap yanXianBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.cosmetic_blank);
            textYanXianId = OpenGLUtils.loadTexture(yanXianBitmap, textYanXianId, true);
        }

        if (bYanYingDirty && yanYingBitmap != null) {
            textYanYingId = OpenGLUtils.loadTexture(yanYingBitmap, textYanYingId, false);
        } else {
            Bitmap yanYingBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.cosmetic_blank);
            textYanYingId = OpenGLUtils.loadTexture(yanYingBitmap, textYanYingId, true);
        }

        if (bJieMaoDirty && jieMaoBitmap != null) {
            textJieMaoId = OpenGLUtils.loadTexture(jieMaoBitmap, textJieMaoId, false);
        } else {
            Bitmap jieMaoBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.cosmetic_blank);
            textJieMaoId = OpenGLUtils.loadTexture(jieMaoBitmap, textJieMaoId, true);
        }
    }
    private void initBeauty() {
        // 初始化beautify,preview的宽高
        int result = mStBeautifyNative.createInstance(mImageHeight, mImageWidth);
        if (result == 0) {
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH, 0.45f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, 0.74f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH, 0.02f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO, 0f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO, 0f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO, 0f);
        }
    }

    /**
     * human action detect的配置选项,根据Sticker的TriggerAction和是否需要美颜配置
     *
     * @param needFaceDetect  是否需要开启face detect
     * @param config  sticker的TriggerAction
     */
    private void setHumanActionDetectConfig(boolean needFaceDetect, int config){
        if(needFaceDetect){
            mDetectConfig = config | STMobileHumanActionNative.ST_MOBILE_FACE_DETECT;
        }else{
            mDetectConfig = config;
        }

        if(mNeedFaceExtraInfo){
            mDetectConfig = mDetectConfig | STMobileHumanActionNative.ST_MOBILE_FACE_240_DETECT;
        }
    }

    private void initFilter(){
        mSTMobileStreamFilterNative.createInstance();

        mCurrentFilterStyle = null;
        mFilterStyle = null;
        mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
        mCurrentFilterStrength = mFilterStrength;
        mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
    }

    private void initObjectTrack(){
        int result = mSTMobileObjectTrackNative.createInstance();
    }

    public void setBeautyParam(int index, float value) {
        if(mBeautifyParams[index] != value){
            mStBeautifyNative.setParam(beautyTypes[index], value);
            mBeautifyParams[index] = value;
        }
    }

    public float[] getBeautyParams(){
        float[] values = new float[6];
        for(int i = 0; i< mBeautifyParams.length; i++){
            values[i] = mBeautifyParams[i];
        }

        return values;
    }

    public void setShowOriginal(boolean isShow)
    {
        mShowOriginal = isShow;
    }

    /**
     * 工作在opengl线程, 当前Renderer关联的view尺寸改变的时候调用
     *
     * @param gl
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        LogUtils.i(TAG, "onSurfaceChanged");
        if (mIsPaused == true) {
            return ;
        }
        adjustViewPort(width, height);
        mGLRender.init(mImageWidth, mImageHeight);
        mStartTime = System.currentTimeMillis();
    }

    /**
     * 根据显示区域大小调整一些参数信息
     *
     * @param width
     * @param height
     */
    private void adjustViewPort(int width, int height) {
        mSurfaceHeight = height;
        mSurfaceWidth = width;
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);
    }


    private int mTemTextureId = 0;
    /**
     * 工作在opengl线程, 具体渲染的工作函数
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // during switch camera
        if (mCameraChanging) {
            return ;
        }

        if (mCameraProxy.getCamera() == null) {
            return;
        }

        LogUtils.i(TAG, "onDrawFrame");
        if (mRGBABuffer == null) {
            mRGBABuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        }

        if (mBeautifyTextureId == null) {
            mBeautifyTextureId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mBeautifyTextureId, GLES20.GL_TEXTURE_2D);
        }

        if (mTextureOutId == null) {
            mTextureOutId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mTextureOutId, GLES20.GL_TEXTURE_2D);
        }

        if (mMeiMaoTextureId == null) {
            mMeiMaoTextureId = new int[1];
            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mMeiMaoTextureId, GLES20.GL_TEXTURE_2D);
        }

        if(mSurfaceTexture != null){
            mSurfaceTexture.updateTexImage();
        }else{
            return;
        }

        mStartTime = System.currentTimeMillis();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mRGBABuffer.rewind();

        long preProcessCostTime = System.currentTimeMillis();

        int textureId = mGLRender.preProcess(mTextureId, mRGBABuffer);
        mTemTextureId = textureId;
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

//        stPoint240 = getAllPrint();

        int result = -1;
        if(!mShowOriginal){
            if(mNeedObject) {
                if (mNeedSetObjectTarget) {
                    long startTimeSetTarget = System.currentTimeMillis();
                    mSTMobileObjectTrackNative.setTarget(mRGBABuffer.array(), STCommon.ST_PIX_FMT_RGBA8888, mImageWidth, mImageHeight,
                            new STRect(mTargetRect.left, mTargetRect.top, mTargetRect.right, mTargetRect.bottom));
                    LogUtils.i(TAG, "setTarget cost time: %d", System.currentTimeMillis() - startTimeSetTarget);
                    mNeedSetObjectTarget = false;
                    mIsObjectTracking = true;
                }

                Rect rect = new Rect(0, 0, 0, 0);

                if (mIsObjectTracking) {
                    long startTimeObjectTrack = System.currentTimeMillis();
                    float[] score = new float[1];
                    STRect outputRect = mSTMobileObjectTrackNative.objectTrack(mRGBABuffer.array(), STCommon.ST_PIX_FMT_RGBA8888, mImageWidth, mImageHeight,score);
                    LogUtils.i(TAG, "objectTrack cost time: %d", System.currentTimeMillis() - startTimeObjectTrack);

                    if(outputRect != null && score != null && score.length >0){
                        rect = STUtils.adjustToScreenRect(outputRect.getRect(), mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);

                        if(outputRect.getRect().equals(new Rect(0,0,0,0)) || score[0] < 0.1f){
                            mNeedObject = false;
                            mSTMobileObjectTrackNative.reset();

//                            Message msg = mHandler.obtainMessage(CameraActivity.MSG_MISSED_OBJECT_TRACK);
//                            mHandler.sendMessage(msg);
                        }
                    }

//                    Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE);
//                    msg.obj = rect;
//                    mHandler.sendMessage(msg);
                    mIndexRect = rect;
                }else{
                    if (mNeedShowRect) {
//                        Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE_AND_RECT);
//                        msg.obj = mIndexRect;
//                        mHandler.sendMessage(msg);
                    } else {
//                        Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE);
//                        msg.obj = rect;
//                        mHandler.sendMessage(msg);
                        mIndexRect = rect;
                    }
                }
            }else{
                if(!mNeedFaceExtraInfo || !(mNeedBeautify || mNeedSticker || mNeedFaceAttribute)){
//                    Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
//                    mHandler.sendMessage(msg);
                }
            }

            if(mNeedBeautify || mNeedSticker || mNeedFaceAttribute && mIsCreateHumanActionHandleSucceeded) {
                STMobile106[] arrayFaces = null, arrayOutFaces = null;
                int orientation = getCurrentOrientation();
                long humanActionCostTime = System.currentTimeMillis();
                STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(mRGBABuffer.array(), STCommon.ST_PIX_FMT_RGBA8888,
                        mDetectConfig, orientation, mImageWidth, mImageHeight);
                if(mNeedFaceExtraInfo && humanAction != null && !mNeedObject){
                    if(humanAction.faceExtraInfo != null && humanAction.faceExtraInfo.eyebrowCount == 0 &&
                            humanAction.faceExtraInfo.eyeCount == 0 && humanAction.faceExtraInfo.lipsCount == 0){
//                        Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
//                        mHandler.sendMessage(msg);
                    }
                }

                if(mNeedBeautify || mNeedFaceAttribute){
                    if (humanAction != null) {
                        arrayFaces = humanAction.getMobileFaces();
                        if (arrayFaces != null && arrayFaces.length > 0) {
                            arrayOutFaces = new STMobile106[arrayFaces.length];
                        }
                    }
                }

                if(arrayFaces != null && arrayFaces.length != 0){
                    if (mNeedUpdateFaceAttribute && mNeedFaceAttribute && arrayFaces != null && arrayFaces.length != 0) { // face attribute
                        STFaceAttribute[] arrayFaceAttribute = new STFaceAttribute[arrayFaces.length];
                        long attributeCostTime = System.currentTimeMillis();
                        result = mSTFaceAttributeNative.detect(mRGBABuffer.array(), STCommon.ST_PIX_FMT_RGBA8888, mImageWidth, mImageHeight, arrayFaces, arrayFaceAttribute);
                        LogUtils.i(TAG, "attribute cost time: %d", System.currentTimeMillis() - attributeCostTime);
                        if (result == 0) {
                            if (arrayFaceAttribute[0].attribute_count > 0) {
                                mFaceAttribute = genFaceAttributeString(arrayFaceAttribute[0]);
                            } else {
                                mFaceAttribute = "null";
                            }
                        }
                        mNeedUpdateFaceAttribute = false;
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                mNeedUpdateFaceAttribute = true;
                            }
                        };
                        mHandlerUpdateAtrrbute.postDelayed(runnable, 1000);
                    } else {
                        mFaceAttribute = null;
                    }
                }else{
                    mFaceAttribute = "noFace";
                }

                if (mFaceAttributeChangeListener != null) {
                    mFaceAttributeChangeListener.onFaceAttributeChanged(mFaceAttribute);
                }

                if (mNeedBeautify) {// do beautify
                    long beautyStartTime = System.currentTimeMillis();
                    result = mStBeautifyNative.processTexture(textureId, mImageWidth, mImageHeight, arrayFaces, mBeautifyTextureId[0], arrayOutFaces);
                    long beautyEndTime = System.currentTimeMillis();
                    LogUtils.i(TAG, "beautify cost time: %d", beautyEndTime-beautyStartTime);
                    if (result == 0) {
                        textureId = mBeautifyTextureId[0];
                    }

                    if (arrayOutFaces != null && arrayOutFaces.length != 0 && humanAction != null && result == 0) {
                        boolean replace = humanAction.replaceMobile106(arrayOutFaces);
                        LogUtils.i(TAG, "replace enlarge eye and shrink face action: " + replace);
                    }
                }

//                //调用贴纸API绘制贴纸
                if(mNeedSticker){
                    boolean needOutputBuffer = false; //如果需要输出buffer推流或其他，设置该开关为true
                    long stickerStartTime = System.currentTimeMillis();
                    if (!needOutputBuffer) {
                        result = mStStickerNative.processTexture(textureId, humanAction, orientation, mImageWidth, mImageHeight,
                                false, mTextureOutId[0]);
                    } else {  //如果需要输出buffer用作推流等
                        byte[] imageOut = new byte[mImageWidth * mImageHeight * 4];
                        result = mStStickerNative.processTextureAndOutputBuffer(textureId, humanAction, orientation, mImageWidth,
                                mImageHeight, false, mTextureOutId[0], STCommon.ST_PIX_FMT_RGBA8888, imageOut);
                    }

                    LogUtils.i(TAG, "processTexture result: %d", result);
                    LogUtils.i(TAG, "sticker cost time: %d", System.currentTimeMillis() - stickerStartTime);

                    if (result == 0) {
                        textureId = mTextureOutId[0];
                    }
                }
                /////////
            }

            if(mCurrentFilterStyle != mFilterStyle){
                mCurrentFilterStyle = mFilterStyle;
                mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
            }
            if(mCurrentFilterStrength != mFilterStrength){
                mCurrentFilterStrength = mFilterStrength;
                mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
            }

            if(mFilterTextureOutId == null){
                mFilterTextureOutId = new int[1];
                GlUtil.initEffectTexture(mImageWidth, mImageHeight, mFilterTextureOutId, GLES20.GL_TEXTURE_2D);
            }

            //滤镜
            if(mNeedFilter){
                long filterStartTime = System.currentTimeMillis();
                int ret = mSTMobileStreamFilterNative.processTexture(textureId, mImageWidth, mImageHeight, mFilterTextureOutId[0]);
                LogUtils.i(TAG, "filter cost time: %d", System.currentTimeMillis() - filterStartTime);
                if(ret == 0){
                    textureId = mFilterTextureOutId[0];
                }
            }
            LogUtils.i(TAG, "frame cost time total: %d", System.currentTimeMillis() - mStartTime);
        }else{
//            Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
//            mHandler.sendMessage(msg);
            resetObjectTrack();
        }
        int frameBuffer = mGLRender.getFrameBufferId();
        if(mTemTextureId != textureId)
        {
            frameBuffer = mGLRender.bindFrameBuffer(textureId);
        }
        stPoint240 = getAllPrint(frameBuffer);

        if(mNeedSave) {
            savePicture(textureId,file);
            mNeedSave = false;
        }
        long dt = System.currentTimeMillis() - mStartTime;
        if(mFpsListener != null) {
            mFpsListener.onFpsChanged((int) dt);
        }
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        int textureMId = OpenGLUtils.NO_TEXTURE;
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.bunny);
        int id = OpenGLUtils.loadTexture(bitmap,textureMId,true);
        mGLRender.onDrawFrame(textureId);
        stPoint240 = null;
    }

    private void savePicture(int textureId, File file) {

        FileSave fileSave = new FileSave();
        ByteBuffer mTmpBuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        mGLRender.saveTextureToFrameBuffer(textureId, mTmpBuffer);
        mTmpBuffer.position(0);
        Message msg = Message.obtain(mHandler);
        msg.what = CameraView.MSG_SAVING_IMG;
        fileSave.setBitmap(mTmpBuffer);
        fileSave.setFile(file);
        msg.obj = fileSave;
        Bundle bundle = new Bundle();
        bundle.putInt("imageWidth", mImageWidth);
        bundle.putInt("imageHeight", mImageHeight);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    private int getCurrentOrientation() {
        int dir = Accelerometer.getDirection();
        int orientation = dir - 1;
        if (orientation < 0) {
            orientation = dir ^ 3;
        }

        return orientation;
    }

    private OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (!mCameraChanging) {
                mGlSurfaceView.requestRender();
            }
        }
    };

    /**
     * camera设备startPreview
     */
    private void setUpCamera() {
        // 初始化Camera设备预览需要的显示区域(mSurfaceTexture)
        if (mTextureId == OpenGLUtils.NO_TEXTURE) {
            mTextureId = OpenGLUtils.getExternalOESTextureID();
            mSurfaceTexture = new SurfaceTexture(mTextureId);
            mSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
        }

//        String size = mSupportedPreviewSizes.get(mCurrentPreview);
//        int index = size.indexOf('x');
//        mImageHeight = Integer.parseInt(size.substring(0, index));
//        mImageWidth = Integer.parseInt(size.substring(index + 1));

        mCameraProxy.setPreviewSize(mImageHeight, mImageWidth);

        boolean flipHorizontal = mCameraProxy.isFlipHorizontal();
        mGLRender.adjustTextureBuffer(mCameraProxy.getOrientation(), flipHorizontal);
        mCameraProxy.startPreview(mSurfaceTexture, null);
    }

    public void setShowSticker(String sticker) {
        mCurrentSticker = sticker;
        mStStickerNative.changeSticker(mCurrentSticker);
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
    }

    public void setFilterStyle(String modelPath) {
        mFilterStyle = modelPath;
    }

    public void setFilterStrength(float strength){
        mFilterStrength = strength;
    }

    public int getStickerTriggerAction(){
        return mStStickerNative.getTriggerAction();
    }

    public void onResume() {
        LogUtils.i(TAG, "onResume");
        mIsPaused = false;
        if (mCameraProxy.getCamera() == null) {
            if (mCameraProxy.getNumberOfCameras() == 1) {
                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            mCameraProxy.openCamera(mCameraID);
            mSupportedPreviewSizes = mCameraProxy.getSupportedPreviewSize(new String[]{"1280x720", "640x480"});
            if (mSupportedPreviewSizes.contains("640x480")) {
                mCurrentPreview = mSupportedPreviewSizes.indexOf("640x480");
            }else {
                mCurrentPreview = mSupportedPreviewSizes.indexOf("1280x720");
            }
        }
//        DisplayMetrics dm = new DisplayMetrics();
//        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
//        wm.getDefaultDisplay().getMetrics(dm);//2392=====1440
////        mImageWidth = dm.widthPixels;
////        mImageHeight = dm.heightPixels;
//
//        Log.d("liupan",dm.heightPixels+"====="+ dm.widthPixels);

        List<Camera.Size> sizes = mCameraProxy.getCamera().getParameters().getSupportedPreviewSizes();
        for(Camera.Size size:sizes){
            Log.d("liupan",size.height+"++++++"+size.width);
        }
//       Camera.Size size =  mCameraProxy.getCamera().getParameters().getPreviewSize();
//        Log.d("liupan",size.height+"!!!!!!!"+size.width);
        mImageWidth = sizes.get(sizes.size()-1).height;
        mImageHeight = sizes.get(sizes.size()-1).width;

        mImageWidth =1080;
        mImageHeight = 1920;

        mGlSurfaceView.forceLayout();
        mGlSurfaceView.requestRender();
    }

    public void onPause() {
        LogUtils.i(TAG, "onPause");
        mCurrentSticker = null;
        mIsPaused = true;
        mCameraProxy.releaseCamera();
        LogUtils.d(TAG, "Release camera");


        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mStBeautifyNative.destroyBeautify();
                mStStickerNative.destroyInstance();
                mSTMobileStreamFilterNative.destroyInstance();

                mRGBABuffer = null;
                deleteTextures();
                if(mSurfaceTexture != null){
                    mSurfaceTexture.release();
                }
                mGLRender.destroyFrameBuffers();
            }
        });

        mGlSurfaceView.onPause();
    }

    public void onDestroy()
    {
        //必须释放非opengGL句柄资源,负责内存泄漏
        synchronized (mHumanActionHandleLock)
        {
            mSTHumanActionNative.destroyInstance();
        }
        mSTFaceAttributeNative.destroyInstance();
        mSTMobileObjectTrackNative.destroyInstance();
    }

    /**
     * 释放纹理资源
     */
    protected void deleteTextures() {
        LogUtils.i(TAG, "delete textures");
        deleteCameraPreviewTexture();
        deleteInternalTextures();
    }

    // must in opengl thread
    private void deleteCameraPreviewTexture() {
        if (mTextureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glDeleteTextures(1, new int[]{
                    mTextureId
            }, 0);
        }
        mTextureId = OpenGLUtils.NO_TEXTURE;
    }

    private void deleteInternalTextures() {
        if (mBeautifyTextureId != null) {
            GLES20.glDeleteTextures(1, mBeautifyTextureId, 0);
            mBeautifyTextureId = null;
        }

        if (mTextureOutId != null) {
            GLES20.glDeleteTextures(1, mTextureOutId, 0);
            mTextureOutId = null;
        }

        if(mFilterTextureOutId != null){
            GLES20.glDeleteTextures(1, mFilterTextureOutId, 0);
            mFilterTextureOutId = null;
        }
    }

    public void switchCamera() {
        if (Camera.getNumberOfCameras() == 1
                || mCameraChanging) {
            return;
        }
        mCameraID = 1 - mCameraID;
        mCameraChanging = true;
        mCameraProxy.openCamera(mCameraID);

        if(mNeedObject){
            resetIndexRect();
        }

        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                deleteTextures();
                if (mCameraProxy.getCamera() != null) {
                    setUpCamera();
                }
                mCameraChanging = false;
            }
        });
        mGlSurfaceView.requestRender();
    }

    public int getCameraID(){
        return mCameraID;
    }

    public void changePreviewSize(int currentPreview) {
        if (mCameraProxy.getCamera() == null || mCameraChanging
                || mIsPaused) {
            return;
        }

        mCurrentPreview = currentPreview;

        mCameraChanging = true;
        mCameraProxy.stopPreview();
        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mRGBABuffer != null) {
                    mRGBABuffer.clear();
                }
                mRGBABuffer = null;

                deleteInternalTextures();
                if (mCameraProxy.getCamera() != null) {
                    setUpCamera();
                }

                mGLRender.init(mImageWidth, mImageHeight);
//                InitPrograme();

                if(mNeedObject){
                    resetIndexRect();
                }

                mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);
                if (mListener != null) {
                    mListener.onChangePreviewSize(mImageHeight, mImageWidth);
                }

                mCameraChanging = false;
                mGlSurfaceView.requestRender();
                LogUtils.d(TAG, "exit  change Preview size queue event");
            }
        });


    }


    public interface FpsChangeListener {
        void onFpsChanged(int value);
    }

    public interface FaceAttributeChangeListener{
        void onFaceAttributeChanged(String string);
    }

    public void enableObject(boolean enabled){
        mNeedObject = enabled;

        if(mNeedObject){
            resetIndexRect();
        }
    }

    public void setIndexRect(int x, int y, boolean needRect){
        mIndexRect = new Rect(x, y, x + mScreenIndexRectWidth, y + mScreenIndexRectWidth);
        mNeedShowRect = needRect;
    }

    public Rect getIndexRect(){
        return mIndexRect;
    }

    public void setObjectTrackRect(){
        mNeedSetObjectTarget = true;
        mIsObjectTracking = false;
        mTargetRect = STUtils.adjustToImageRect(getIndexRect(), mSurfaceWidth, mSurfaceHeight, mImageWidth,mImageHeight);
    }

    public void disableObjectTracking(){
        mIsObjectTracking = false;
    }

    public void resetObjectTrack(){
        mSTMobileObjectTrackNative.reset();
    }

    public void resetIndexRect(){
        if(mImageWidth == 0){
            return;
        }

        if(mImageWidth > 600){
            mScreenIndexRectWidth = (mIndexRectWidthLarge * mSurfaceWidth/mImageWidth);
        }else {
            mScreenIndexRectWidth = (mIndexRectWidthSmall * mSurfaceWidth/mImageWidth);
        }

        mIndexRect.left = (mSurfaceWidth - mScreenIndexRectWidth)/2;
        mIndexRect.top = (mSurfaceHeight - mScreenIndexRectWidth)/2;
        mIndexRect.right = mIndexRect.left + mScreenIndexRectWidth;
        mIndexRect.bottom = mIndexRect.top + mScreenIndexRectWidth;

        mNeedShowRect = true;
        mNeedSetObjectTarget = false;
        mIsObjectTracking = false;
    }

    private int checkFlag(int action, int flag) {
        int res = action & flag;
        return res == 0 ? 0 : 1;
    }

    /**
     * 获取所有的点
     * @return
     */
    public STPoint[] getAllPrint(int frameBuffer){
        STMobile106[] arrayFaces = null, arrayOutFaces = null;
        int orientation = getCurrentOrientation();
        long humanActionCostTime = System.currentTimeMillis();
        STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(mRGBABuffer.array(), STCommon.ST_PIX_FMT_RGBA8888,
                mDetectConfig, orientation, mImageWidth, mImageHeight);
        if(mNeedFaceExtraInfo && humanAction != null && !mNeedObject){
            if(humanAction.faceExtraInfo != null){
                arrayFaces = humanAction.getMobileFaces();
                if(arrayFaces!=null) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
                    for (int i = 0; i < arrayFaces.length; i++) {
                        STPoint[] stPoints = arrayFaces[i].getPoints_array();
                        stPoint240 = new STPoint[240];
                        STPoint[] points = humanAction.faceExtraInfo.getAllPoints();
                        pointsBrowLeft = humanAction.faceExtraInfo.getEyebrowLeftPoints(0);
                        STPoint[] pointsBrowRight = humanAction.faceExtraInfo.getEyebrowRightPoints(0);
                        STPoint[] pointsEyeLeft = humanAction.faceExtraInfo.getEyeLeftPoints(0);
                        STPoint[] pointsEyeRight = humanAction.faceExtraInfo.getEyeRightPoints(0);
                        STPoint[] pointsLips = humanAction.faceExtraInfo.getLipsPoints(0);
                        //106+左眼+右眼+做眉毛+右眉毛+嘴
                        for (int j = 0; j < 106; j++) {
                            stPoint240[j] = stPoints[j];
                        }
                        //左眼
                        for (int j = 0; j < 22; j++) {
                            stPoint240[106 + j] = pointsEyeLeft[j];
                        }
                        //右眼
                        for (int j = 0; j < 22; j++) {
                            stPoint240[106 + 22 + j] = pointsEyeRight[j];
                        }
                        //左眉毛
                        for (int j = 0; j < 13; j++) {
                            stPoint240[106 + 22 + 22 + j] = pointsBrowLeft[j];
                        }
                        //右眉毛
                        for (int j = 0; j < 13; j++) {
                            stPoint240[106 + 22 * 2 + 13 + j] = pointsBrowRight[j];
                        }
                        //嘴
                        for (int j = 0; j < 64; j++) {
                            stPoint240[106 + 22 * 2 + 13 * 2 + j] = pointsLips[j];
                        }
//                        mGLRender.drawMeizhuang(stPoint240,texture_left,texture_right,textSiaHongId,downMouseColors);
                        if(bSaihongDirty||bLeftMeiDirty||bRightMeiDirty||bYanXianDirty||bYanYingDirty||bJieMaoDirty){
                         initMeizhuang();
                        }
                        mGLRender.drawMeizhuang(stPoint240,textLeftMeiMaoId,textRightMeiMaoId,textJieMaoId ,textYanXianId,textYanYingId,textSiaHongId,upMouseColors,downMouseColors);
                    }
                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                }
            }
        }
        return stPoint240;
    }


    int textLeftMeiMaoId = OpenGLUtils.NO_TEXTURE;
    int textRightMeiMaoId = OpenGLUtils.NO_TEXTURE;
    int textYanXianId = OpenGLUtils.NO_TEXTURE;
    int textJieMaoId = OpenGLUtils.NO_TEXTURE;
    int textYanYingId = OpenGLUtils.NO_TEXTURE;
    int textSiaHongId = OpenGLUtils.NO_TEXTURE;
    float[] upMouseColors = {0.0f, 0.0f, 0.0f, 0.0f};
    float[] downMouseColors = {0.0f, 0.0f, 0.0f, 0.0f};
    boolean bLeftMeiDirty = false;
    Bitmap leftMeiBitmap;
    boolean bRightMeiDirty = false;
    Bitmap rightMeiBitmap;
    boolean bYanXianDirty = false;
    Bitmap yanXianBitmap;
    boolean bJieMaoDirty = false;
    Bitmap jieMaoBitmap;
    boolean bYanYingDirty = false;
    Bitmap yanYingBitmap;
    boolean bSaihongDirty = false;
    Bitmap saihongBitmap;

    public void setLeftMeiMao(Bitmap bitmap) {
        bLeftMeiDirty =true;
        leftMeiBitmap = bitmap;
    }

    public void setRightMeiMao(Bitmap bitmap) {
        bRightMeiDirty =true;
        rightMeiBitmap = bitmap;
    }

    public void setYanXian(Bitmap bitmap) {
        bYanXianDirty = true;
        yanXianBitmap =bitmap;

    }

    public void setYanJieMao(Bitmap bitmap) {
        bJieMaoDirty = true;
        jieMaoBitmap = bitmap;

    }
    public void setYanYing(Bitmap bitmap) {
        bYanYingDirty =true;
        yanYingBitmap = bitmap;

    }
    public void setSaihong(Bitmap bitmap) {
        bSaihongDirty = true;
        saihongBitmap = bitmap;
    }

    public void setUpMouseColors(float[] upMouseColors) {
        this.upMouseColors=upMouseColors;
    }

    public void setDownMouseColors(float[] downMouseColors) {
        this.downMouseColors=downMouseColors;
    }



}
