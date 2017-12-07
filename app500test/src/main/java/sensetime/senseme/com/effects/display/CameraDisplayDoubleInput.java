package sensetime.senseme.com.effects.display;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

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
import com.sensetime.stmobile.STRotateType;
import com.sensetime.stmobile.model.STMobile106;
import com.sensetime.stmobile.model.STPoint;
import com.sensetime.stmobile.model.STRect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import sensetime.senseme.com.effects.CameraActivity;
import sensetime.senseme.com.effects.camera.CameraProxy;
import sensetime.senseme.com.effects.encoder.MediaVideoEncoder;
import sensetime.senseme.com.effects.glutils.GlUtil;
import sensetime.senseme.com.effects.glutils.OpenGLUtils;
import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.glutils.TextureRotationUtil;
import sensetime.senseme.com.effects.utils.Accelerometer;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.LogUtils;

/**
 * CameraDisplayDoubleInput is used for camera preview
 */

/**
 * 渲染结果显示的Render, 用户最终看到的结果在这个类中得到并显示.
 * 请重点关注Camera.PreviewCallback的onPreviewFrame/onSurfaceCreated,onSurfaceChanged,onDrawFrame
 * 四个接口, 基本上所有的处理逻辑都是围绕这四个接口展开
 */
public class CameraDisplayDoubleInput implements Renderer {

    private String TAG = "CameraDisplayDoubleInput";
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
    private boolean mCameraChanging = false;
    private int mCurrentPreview = 0;
    private ArrayList<String> mSupportedPreviewSizes;
    private boolean mSetPreViewSizeSucceed = false;
    private boolean mIsChangingPreviewSize = false;

    private long mStartTime;
    private boolean mShowOriginal = false;
    private boolean mNeedBeautify = false;
    private boolean mNeedFaceAttribute = false;
    private boolean mNeedUpdateFaceAttribute = true;
    private boolean mNeedSticker = false;
    private boolean mNeedFilter = false;
    private boolean mNeedSave = false;
    private boolean mNeedObject = false;
    private FloatBuffer mTextureBuffer;
    private float[] mBeautifyParams = {0.36f, 0.74f, 0.30f, 0.13f, 0.11f, 0.1f};

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
    private long mDetectConfig = 0;
    private boolean mIsCreateHumanActionHandleSucceeded = false;
    private Object mHumanActionHandleLock = new Object();
    private Object mImageDataLock = new Object();

    private boolean mNeedShowRect = true;
    private int mScreenIndexRectWidth = 0;
    private int mIndexRectWidthSmall = 128;//640X480
    private int mIndexRectWidthLarge = 256;//1280X720

    private Rect mTargetRect = new Rect();
    private Rect mIndexRect = new Rect();
    private boolean mNeedSetObjectTarget = false;
    private boolean mIsObjectTracking = false;

    private int mHumanActionCreateConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO;

    private STHumanAction mHumanAction = new STHumanAction();
    private STHumanAction mHumanActionNoMirrow = new STHumanAction();
    private byte[] mRotateData;
    private byte[] mNv21ImageData;

    private HandlerThread mProcessImageThread;
    private Handler mProcessImageHandler;
    private static final int MESSAGE_PROCESS_IMAGE = 100;
    private byte[] mImageData;
    private long mRotateCost = 0;
    private long mObjectCost = 0;
    private long mFaceAttributeCost = 0;
    private int mFrameCount = 0;

    //for test fps
    private float mFps;
    private int mCount = 0;
    private long mCurrentTime = 0;
    private boolean mIsFirstCount = true;
    private int mFrameCost = 0;

    private MediaVideoEncoder mVideoEncoder;
    private final float[] mStMatrix = new float[16];
    private int[] mVideoEncoderTexture;
    private boolean mNeedResetEglContext = false;

    //face extra info swicth
    private boolean mNeedFaceExtraInfo = false;

    public interface ChangePreviewSizeListener {
        void onChangePreviewSize(int previewW, int previewH);
    }

    public CameraDisplayDoubleInput(Context context, ChangePreviewSizeListener listener, GLSurfaceView glSurfaceView) {
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

        //初始化非OpengGL相关的句柄，包括人脸检测及人脸属性
        initHumanAction(); //因为人脸模型加载较慢，建议异步调用
        initFaceAttribute();
        initObjectTrack();

        mProcessImageThread = new HandlerThread("ProcessImageThread");
        mProcessImageThread.start();
        mProcessImageHandler = new Handler(mProcessImageThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_PROCESS_IMAGE && !mIsPaused && !mCameraChanging) {
                    objectTrack();
                }
            }
        };
    }

    public void enableBeautify(boolean needBeautify) {
        mNeedBeautify = needBeautify;
        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
        mNeedResetEglContext = true;
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
        //reset humanAction config
        if(!needSticker){
            setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
        }

        mNeedResetEglContext = true;
    }

    public void enableFilter(boolean needFilter){
        mNeedFilter = needFilter;
        mNeedResetEglContext = true;
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

    public void setSaveImage() {
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

        while (!mCameraProxy.isCameraOpen())
        {
            if(mCameraProxy.cameraOpenFailed()){
                return;
            }
            try {
                Thread.sleep(10,0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (mCameraProxy.getCamera() != null) {
            setUpCamera();
        }

        //初始化GL相关的句柄，包括美颜，贴纸，滤镜
        initBeauty();
        initSticker();
        initFilter();
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
                    //从sd读取model路径，创建handle
                    //int result = mSTHumanActionNative.createInstance(FileUtils.getTrackModelPath(mContext), mHumanActionCreateConfig);

                    //从asset资源文件夹读取model到内存，再使用底层st_mobile_human_action_create_from_buffer接口创建handle
                    int result = mSTHumanActionNative.createInstanceFromAssetFile(FileUtils.getActionModelName(), mHumanActionCreateConfig, mContext.getAssets());
                    LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);

                    if (result == 0) {
                        mIsCreateHumanActionHandleSucceeded = true;

                        if(mNeedFaceExtraInfo){
                            result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.getEyeBallCenterModelName(), mContext.getAssets());
                            LogUtils.i(TAG, "add eyeball center model result %d", result);
                            result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.getEyeBallContourModelName(), mContext.getAssets());
                            LogUtils.i(TAG, "add eyeball contour model result %d", result);
                            result = mSTHumanActionNative.addSubModelFromAssetFile(FileUtils.getFaceExtraModelName(), mContext.getAssets());
                            LogUtils.i(TAG, "add face extra model result %d", result);
                        }

                        mSTHumanActionNative.setParam(STHumanActionParamsType.ST_HUMAN_ACTION_PARAM_BACKGROUND_BLUR_STRENGTH, 0.35f);
                    }
                }
            }
        }).start();
    }

    private void initSticker() {
        int result = mStStickerNative.createInstance(mContext, null);

        if(mNeedSticker){
            mStStickerNative.changeSticker(mCurrentSticker);
        }

        setHumanActionDetectConfig(mNeedBeautify|mNeedFaceAttribute, mStStickerNative.getTriggerAction());
        LogUtils.i(TAG, "the result for createInstance for human_action is %d", result);
    }

    private void initBeauty() {
        // 初始化beautify,preview的宽高
        int result = mStBeautifyNative.createInstance();
        LogUtils.i(TAG, "the result is for initBeautify " + result);
        if (result == 0) {
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH, mBeautifyParams[0]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, mBeautifyParams[1]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH, mBeautifyParams[2]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO, mBeautifyParams[3]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO, mBeautifyParams[4]);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO, mBeautifyParams[5]);
        }
    }

    /**
     * human action detect的配置选项,根据Sticker的TriggerAction和是否需要美颜配置
     *
     * @param needFaceDetect  是否需要开启face detect
     * @param config  sticker的TriggerAction
     */
    private void setHumanActionDetectConfig(boolean needFaceDetect, long config){
        if(!mNeedSticker || mCurrentSticker == null){
            config = 0;
        }

        if(needFaceDetect){
            mDetectConfig = config |STMobileHumanActionNative.ST_MOBILE_FACE_DETECT;
        }else{
            mDetectConfig = config;
        }

        if(mNeedFaceExtraInfo){
            mDetectConfig = mDetectConfig | STMobileHumanActionNative.ST_MOBILE_DETECT_EXTRA_FACE_POINTS
                | STMobileHumanActionNative.ST_MOBILE_DETECT_EYEBALL_CENTER | STMobileHumanActionNative.ST_MOBILE_DETECT_EYEBALL_CONTOUR;
        }
    }

    private void initFilter(){
        int result = mSTMobileStreamFilterNative.createInstance();
        LogUtils.i(TAG, "filter create instance result %d", result);

        mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);

        mCurrentFilterStrength = mFilterStrength;
        mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
    }

    private void initObjectTrack(){
        int result = mSTMobileObjectTrackNative.createInstance();
    }

    private void objectTrack(){
        if(mImageData == null || mImageData.length == 0){
            return;
        }

        if(mNeedObject) {
            if (mNeedSetObjectTarget) {
                long startTimeSetTarget = System.currentTimeMillis();

                STRect inputRect = new STRect(mTargetRect.left, mTargetRect.top, mTargetRect.right, mTargetRect.bottom);
                if(mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    inputRect = new STRect(mImageWidth - mTargetRect.right, mTargetRect.top, mImageWidth - mTargetRect.left, mTargetRect.bottom);
                }
                mSTMobileObjectTrackNative.setTarget(mImageData, STCommon.ST_PIX_FMT_NV21, mImageWidth, mImageHeight, inputRect);
                LogUtils.i(TAG, "setTarget cost time: %d", System.currentTimeMillis() - startTimeSetTarget);
                mNeedSetObjectTarget = false;
                mIsObjectTracking = true;
            }

            Rect rect = new Rect(0, 0, 0, 0);

            if (mIsObjectTracking) {
                long startTimeObjectTrack = System.currentTimeMillis();
                float[] score = new float[1];
                STRect outputRect = mSTMobileObjectTrackNative.objectTrack(mImageData, STCommon.ST_PIX_FMT_NV21, mImageWidth, mImageHeight,score);
                LogUtils.i(TAG, "objectTrack cost time: %d", System.currentTimeMillis() - startTimeObjectTrack);
                mObjectCost = System.currentTimeMillis() - startTimeObjectTrack;

                if(outputRect != null && score != null && score.length >0){
                    if(mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT){
                        outputRect = new STRect(mImageWidth - outputRect.getRect().right, outputRect.getRect().top, mImageWidth - outputRect.getRect().left, outputRect.getRect().bottom);
                    }

                    rect = STUtils.adjustToScreenRect(outputRect.getRect(), mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);

                    if(outputRect.getRect().equals(new Rect(0,0,0,0)) || score[0] < 0.1f){
                        mNeedObject = false;
                        mSTMobileObjectTrackNative.reset();

                        Message msg = mHandler.obtainMessage(CameraActivity.MSG_MISSED_OBJECT_TRACK);
                        mHandler.sendMessage(msg);
                    }
                }

                Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE);
                msg.obj = rect;
                mHandler.sendMessage(msg);
                mIndexRect = rect;
            }else{
                if (mNeedShowRect) {
                    Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE_AND_RECT);
                    msg.obj = mIndexRect;
                    mHandler.sendMessage(msg);
                } else {
                    Message msg = mHandler.obtainMessage(CameraActivity.MSG_DRAW_OBJECT_IMAGE);
                    msg.obj = rect;
                    mHandler.sendMessage(msg);
                    mIndexRect = rect;
                }
            }
        }else{
            mObjectCost = 0;

            if(!mNeedFaceExtraInfo || !(mNeedBeautify || mNeedSticker || mNeedFaceAttribute)){
                Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
                mHandler.sendMessage(msg);
            }
        }
    }

    private void faceAttribute(byte[] data){

        if (mHumanActionNoMirrow != null && data != null && data.length > 0) {
            STMobile106[] arrayFaces = null;
            arrayFaces = mHumanActionNoMirrow.getMobileFaces();

            if (arrayFaces != null && arrayFaces.length != 0) {
                if (mNeedFaceAttribute && arrayFaces != null && arrayFaces.length != 0) { // face attribute
                    STFaceAttribute[] arrayFaceAttribute = new STFaceAttribute[arrayFaces.length];
                    long attributeCostTime = System.currentTimeMillis();
                    int result = mSTFaceAttributeNative.detect(data, STCommon.ST_PIX_FMT_NV21, mImageWidth, mImageHeight, arrayFaces, arrayFaceAttribute);
                    LogUtils.i(TAG, "attribute cost time: %d", System.currentTimeMillis() - attributeCostTime);
                    mFaceAttributeCost = System.currentTimeMillis() - attributeCostTime;
                    if (result == 0) {
                        if (arrayFaceAttribute[0].attribute_count > 0) {
                            mFaceAttribute = genFaceAttributeString(arrayFaceAttribute[0]);
                        } else {
                            mFaceAttribute = "null";
                        }
                    }
                } else {
                    mFaceAttribute = null;
                    mFaceAttributeCost = 0;
                }
            } else {
                mFaceAttribute = "noFace";
                mFaceAttributeCost = 0;
            }
        }
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

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {

            if (mCameraChanging || mCameraProxy.getCamera() == null) {
                return ;
            }

            if(mRotateData == null || mRotateData.length != mImageHeight * mImageWidth *3/2){
                mRotateData = new byte[mImageHeight * mImageWidth *3/2];
            }

            int orientation = getRotateOrientation();

            long startRotate = System.currentTimeMillis();
            int ret = STCommon.stImageRotate(data, mRotateData, mImageHeight, mImageWidth, STCommon.ST_PIX_FMT_NV21, orientation);
            LogUtils.i(TAG, "rotate cost time: %d", System.currentTimeMillis() - startRotate);
            mRotateCost = System.currentTimeMillis() - startRotate;

            if(mImageData == null || mImageData.length != mImageHeight * mImageWidth *3/2){
                mImageData = new byte[mImageWidth * mImageHeight* 3/2];
            }
            synchronized (mImageDataLock) {
                System.arraycopy(mRotateData, 0, mImageData, 0, mRotateData.length);
            }

            mProcessImageHandler.removeMessages(MESSAGE_PROCESS_IMAGE);
            mProcessImageHandler.sendEmptyMessage(MESSAGE_PROCESS_IMAGE);

            mGlSurfaceView.requestRender();
        }
    };

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

        if (mVideoEncoderTexture == null) {
            mVideoEncoderTexture = new int[1];
        }

        if(mSurfaceTexture != null && !mIsPaused){
            mSurfaceTexture.updateTexImage();
        }else{
            return;
        }

        mStartTime = System.currentTimeMillis();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mRGBABuffer.rewind();

        long preProcessCostTime = System.currentTimeMillis();
        int textureId = mGLRender.preProcess(mTextureId, null);
        LogUtils.i(TAG, "preprocess cost time: %d", System.currentTimeMillis() - preProcessCostTime);

        int result = -1;

        if(!mShowOriginal){

            if((mNeedBeautify || mNeedSticker || mNeedFaceAttribute) && mIsCreateHumanActionHandleSucceeded) {

                if(mImageData == null){
                    return;
                }

                if(mNv21ImageData == null || mNv21ImageData.length != mImageHeight * mImageWidth *3/2){
                    mNv21ImageData = new byte[mImageWidth * mImageHeight* 3/2];
                }

                if(mCameraChanging || mImageData.length != mNv21ImageData.length){
                    return;
                }
                synchronized (mImageDataLock) {
                    System.arraycopy(mImageData, 0, mNv21ImageData, 0, mImageData.length);
                }

                long startHumanAction = System.currentTimeMillis();
                STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(mNv21ImageData, STCommon.ST_PIX_FMT_NV21,
                        mDetectConfig, getHumanActionOrientation(), mImageWidth, mImageHeight);
                LogUtils.i(TAG, "human action cost time: %d", System.currentTimeMillis() - startHumanAction);

                mHumanActionNoMirrow = humanAction;

                if(humanAction != null && mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    humanAction  = mSTHumanActionNative.humanActionMirror(mImageWidth, humanAction);
                }

                if(humanAction != null){
                    if(humanAction.imageResult){
                        LogUtils.i(TAG, "human action background result: %d", 1);
                    }else{
                        LogUtils.i(TAG, "human action background result: %d", 0);
                    }

                    if(humanAction.hands != null && humanAction.hands.length > 0){
                        LogUtils.i(TAG, "hand action holdup(托手): %d", checkFlag(humanAction.hands[0].handAction, STMobileHumanActionNative.ST_MOBILE_HAND_HOLDUP));
                        LogUtils.i(TAG, "hand action congratulate(抱拳): %d", checkFlag(humanAction.hands[0].handAction, STMobileHumanActionNative.ST_MOBILE_HAND_CONGRATULATE));
                        LogUtils.i(TAG, "hand action fingerHeart(单手比爱心): %d", checkFlag(humanAction.hands[0].handAction, STMobileHumanActionNative.ST_MOBILE_HAND_FINGER_HEART));
                        LogUtils.i(TAG, "hand action good(大拇哥): %d", checkFlag(humanAction.hands[0].handAction, STMobileHumanActionNative.ST_MOBILE_HAND_GOOD));
                        LogUtils.i(TAG, "hand action love(爱心): %d", checkFlag(humanAction.hands[0].handAction, STMobileHumanActionNative.ST_MOBILE_HAND_LOVE));
                        LogUtils.i(TAG, "hand action palm(手掌): %d", checkFlag(humanAction.hands[0].handAction, STMobileHumanActionNative.ST_MOBILE_HAND_PALM));
                        LogUtils.i(TAG, "hand action two index finger(平行手指): %d", checkFlag(humanAction.hands[0].handAction, STMobileHumanActionNative.ST_MOBILE_HAND_TWO_INDEX_FINGER));
                    }
                }

                //人脸属性
                if(mFrameCount <= 20){
                    mFrameCount++;
                }else{
                    mFrameCount = 0;
                    faceAttribute(mNv21ImageData);//do face attribute
                }

                STMobile106[] arrayFaces = null, arrayOutFaces = null;
                int orientation = getCurrentOrientation();


                //美颜
                if (mNeedBeautify) {// do beautify
                    if (humanAction != null) {
                        arrayFaces = humanAction.getMobileFaces();
                        if (arrayFaces != null && arrayFaces.length > 0) {
                            arrayOutFaces = new STMobile106[arrayFaces.length];
                        }
                    }

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

                //调用贴纸API绘制贴纸
                if(mNeedSticker){
                    /**
                     * 1.在切换贴纸时，调用STMobileStickerNative的changeSticker函数，传入贴纸路径(参考setShowSticker函数的使用)
                     * 2.切换贴纸后，使用STMobileStickerNative的getTriggerAction函数获取当前贴纸支持的手势和前后背景等信息，返回值为int类型
                     * 3.根据getTriggerAction函数返回值，重新配置humanActionDetect函数的config参数，使detect更高效
                     *
                     * 例：只检测人脸信息和当前贴纸支持的手势等信息时，使用如下配置：
                     * mDetectConfig = mSTMobileStickerNative.getTriggerAction()|STMobileHumanActionNative.ST_MOBILE_FACE_DETECT;
                    */
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

                //画点
                if(mNeedFaceExtraInfo && humanAction != null && humanAction.faceCount > 0){
                    for(int i = 0; i < humanAction.faceCount; i++){
                        float[] points = STUtils.getExtraPoints(humanAction, i, mImageWidth, mImageHeight);
                        mGLRender.onDrawPoints(textureId, points);
                    }

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                }

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

            LogUtils.i(TAG, "frame cost time total: %d", System.currentTimeMillis() - mStartTime + mRotateCost + mObjectCost + mFaceAttributeCost/20);
        }


        if(mNeedSave) {
            savePicture(textureId);
            mNeedSave = false;
        }

        //video capturing
        if(mVideoEncoder != null){
            GLES20.glFinish();
        }

        mVideoEncoderTexture[0] = textureId;
        mSurfaceTexture.getTransformMatrix(mStMatrix);
        processStMatrix(mStMatrix, mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT);

        synchronized (this) {
            if (mVideoEncoder != null) {
                if(mNeedResetEglContext){
                    mVideoEncoder.setEglContext(EGL14.eglGetCurrentContext(), mVideoEncoderTexture[0]);
                    mNeedResetEglContext = false;
                }
                mVideoEncoder.frameAvailableSoon(mStMatrix);

            }
        }

        mFrameCost = (int)(System.currentTimeMillis() - mStartTime + mRotateCost + mObjectCost + mFaceAttributeCost/20);

        long timer  = System.currentTimeMillis();
        mCount++;
        if(mIsFirstCount){
            mCurrentTime = timer;
            mIsFirstCount = false;
        }else{
            int cost = (int)(timer - mCurrentTime);
            if(cost >= 1000){
                mCurrentTime = timer;
                mFps = (((float)mCount *1000)/cost);
                mCount = 0;
            }
        }

        LogUtils.i(TAG, "render fps: %f", mFps);

        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);

        mGLRender.onDrawFrame(textureId);
    }

    private void savePicture(int textureId) {
        ByteBuffer mTmpBuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
        mGLRender.saveTextureToFrameBuffer(textureId, mTmpBuffer);

        mTmpBuffer.position(0);
        Message msg = Message.obtain(mHandler);
        msg.what = CameraActivity.MSG_SAVING_IMG;
        msg.obj = mTmpBuffer;
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

//    private OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {
//
//        @Override
//        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//            if (!mCameraChanging) {
//                mGlSurfaceView.requestRender();
//            }
//        }
//    };

    /**
     * camera设备startPreview
     */
    private void setUpCamera() {
        // 初始化Camera设备预览需要的显示区域(mSurfaceTexture)
        if (mTextureId == OpenGLUtils.NO_TEXTURE) {
            mTextureId = OpenGLUtils.getExternalOESTextureID();
            mSurfaceTexture = new SurfaceTexture(mTextureId);
//            mSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);
        }

        String size = mSupportedPreviewSizes.get(mCurrentPreview);
        int index = size.indexOf('x');
        mImageHeight = Integer.parseInt(size.substring(0, index));
        mImageWidth = Integer.parseInt(size.substring(index + 1));

        if(mIsPaused)
            return;

        while(!mSetPreViewSizeSucceed){
            try{
                mCameraProxy.setPreviewSize(mImageHeight, mImageWidth);
                mSetPreViewSizeSucceed = true;
            }catch (Exception e){
                mSetPreViewSizeSucceed = false;
            }

            try{
                Thread.sleep(10);
            }catch (Exception e){

            }
        }

        boolean flipHorizontal = mCameraProxy.isFlipHorizontal();
        mGLRender.adjustTextureBuffer(mCameraProxy.getOrientation(), flipHorizontal);

        if(mIsPaused)
            return;
        mCameraProxy.startPreview(mSurfaceTexture, mPreviewCallback);
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

    public long getStickerTriggerAction(){
        return mStStickerNative.getTriggerAction();
    }

    public void onResume() {
        LogUtils.i(TAG, "onResume");

        if (mCameraProxy.getCamera() == null) {
            if (mCameraProxy.getNumberOfCameras() == 1) {
                mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            mCameraProxy.openCamera(mCameraID);
            mSupportedPreviewSizes = mCameraProxy.getSupportedPreviewSize(new String[]{"640x480", "1280x720"});
        }
        mIsPaused = false;
        mSetPreViewSizeSucceed = false;

        mSTHumanActionNative.reset();

        mGLRender = new STGLRender();

        mGlSurfaceView.onResume();
        mGlSurfaceView.forceLayout();
        //mGlSurfaceView.requestRender();
    }

    public void onPause() {
        LogUtils.i(TAG, "onPause");
        mSetPreViewSizeSucceed = false;
        //mCurrentSticker = null;
        mIsPaused = true;
        mImageData = null;
        mCameraProxy.releaseCamera();
        LogUtils.d(TAG, "Release camera");

        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mStBeautifyNative.destroyBeautify();
                mStStickerNative.destroyInstance();
                mSTMobileStreamFilterNative.destroyInstance();
                mRGBABuffer = null;
                mNv21ImageData = null;
                deleteTextures();
                if(mSurfaceTexture != null){
                    mSurfaceTexture.release();
                }
                mGLRender.destroyFrameBuffers();
            }
        });

        mGlSurfaceView.onPause();
    }

    public void onDestroy() {
        //必须释放非opengGL句柄资源,负责内存泄漏
        synchronized (mHumanActionHandleLock) {
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

        if(mVideoEncoderTexture != null){
            GLES20.glDeleteTextures(1, mVideoEncoderTexture, 0);
            mVideoEncoderTexture = null;
        }
    }

    public void switchCamera() {
        if (Camera.getNumberOfCameras() == 1
                || mCameraChanging) {
            return;
        }

        if(mCameraProxy.cameraOpenFailed()){
            return;
        }

        final int cameraID = 1 - mCameraID;
        mCameraChanging = true;
        mCameraProxy.openCamera(cameraID);

        mSetPreViewSizeSucceed = false;

        if(mNeedObject){
            resetIndexRect();
        }else{
            Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
            mHandler.sendMessage(msg);
        }

        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                deleteTextures();
                if (mCameraProxy.getCamera() != null) {
                    setUpCamera();
                }
                mCameraChanging = false;
                mCameraID = cameraID;
            }
        });
        //fix 双输入camera changing时，贴纸和画点mirrow显示
        //mGlSurfaceView.requestRender();
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
        mSetPreViewSizeSucceed = false;
        mIsChangingPreviewSize = true;

        mCameraChanging = true;
        mCameraProxy.stopPreview();
        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mRGBABuffer != null) {
                    mRGBABuffer.clear();
                }
                mRGBABuffer = null;

                deleteTextures();
                if (mCameraProxy.getCamera() != null) {
                    setUpCamera();
                }

                mGLRender.init(mImageWidth, mImageHeight);
                if(mNeedFaceExtraInfo){
                    mGLRender.initDrawPoints();
                }

                if(mNeedObject){
                    resetIndexRect();
                }

                mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mImageWidth, mImageHeight);
                if (mListener != null) {
                    mListener.onChangePreviewSize(mImageHeight, mImageWidth);
                }

                mCameraChanging = false;
                mIsChangingPreviewSize = false;
                //mGlSurfaceView.requestRender();
                LogUtils.d(TAG, "exit  change Preview size queue event");
            }
        });


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

    private int checkFlag(long action, long flag) {
        long res = action & flag;
        return res == 0 ? 0 : 1;
    }

    private int getRotateOrientation(){
        //相机预览buffer的旋转角度。由于Camera获取的buffer为横向图像，将buffer旋转为竖向（即正向竖屏使用手机时，人脸方向朝上）
        int rotateOrientation = STRotateType.ST_CLOCKWISE_ROTATE_270;

        if(mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT){
            rotateOrientation  = STRotateType.ST_CLOCKWISE_ROTATE_270;
        }else if(mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK && mCameraProxy.getOrientation() == 90){
            rotateOrientation  = STRotateType.ST_CLOCKWISE_ROTATE_90;
        }else if(mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK && mCameraProxy.getOrientation() == 270){
            rotateOrientation  = STRotateType.ST_CLOCKWISE_ROTATE_270;
        }

        return rotateOrientation;
    }

    private int getHumanActionOrientation(){
        //用于humanActionDetect接口。根据传感器方向计算出在不同设备朝向时，人脸在图片中的朝向。
        int humanOrientation = getCurrentOrientation();

        if(mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT){
            //前置摄像头nv21 buffer未做mirror，当设备朝向为90或270时，人脸朝向与设备方向相反，做humanActionDetect时需输入相反方向
            if(humanOrientation == STRotateType.ST_CLOCKWISE_ROTATE_90){
                humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_270;
            }else if(humanOrientation == STRotateType.ST_CLOCKWISE_ROTATE_270){
                humanOrientation = STRotateType.ST_CLOCKWISE_ROTATE_90;
            }
        }

        return humanOrientation;
    }

    public int getPreviewWidth(){
        return mImageWidth;
    }

    public int getPreviewHeight(){
        return mImageHeight;
    }

    public void setVideoEncoder(final MediaVideoEncoder encoder) {

        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (encoder != null && mVideoEncoderTexture != null) {
                        encoder.setEglContext(EGL14.eglGetCurrentContext(), mVideoEncoderTexture[0]);
                    }
                    mVideoEncoder = encoder;
                }
            }
        });
    }

    private void processStMatrix(float[] matrix, boolean needMirror){
        if(needMirror && matrix != null && matrix.length == 16){
            for(int i = 0; i < 3; i++){
                matrix[4 * i] = -matrix[4 * i];
            }

            if(matrix[4 * 3] == 0){
                matrix[4 * 3] = 1.0f;
            }else if(matrix[4 *3] == 1.0f){
                matrix[4 *3] = 0f;
            }
        }

        return;
    }

    public int getFrameCost(){
        return mFrameCost;
    }

    public boolean isChangingPreviewSize(){
        return mIsChangingPreviewSize;
    }

}
