package sensetime.senseme.com.effects.display;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

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

import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import sensetime.senseme.com.effects.CameraActivity;
import sensetime.senseme.com.effects.R;
import sensetime.senseme.com.effects.camera.CameraProxy;
import sensetime.senseme.com.effects.glutils.OpenGLUtils;
import sensetime.senseme.com.effects.glutils.STUtils;
import sensetime.senseme.com.effects.glutils.TextureRotationUtil;
import sensetime.senseme.com.effects.glutils.Utils;
import sensetime.senseme.com.effects.utils.Accelerometer;
import sensetime.senseme.com.effects.utils.FileUtils;
import sensetime.senseme.com.effects.utils.LogUtils;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glVertexAttribPointer;

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
    private int mProgram;
    private int mBitmapSize=0;
    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    " \n" +
                    "varying vec2 textureCoordinate;\n" +
                    " \n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_Position = position;\n" +
                    "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "}";

    private final String fragmentShaderCode =  "varying highp vec2 textureCoordinate;\n" +//
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{ \n" +
             "  vec4 texColor = texture2D(inputImageTexture, textureCoordinate);" +
//            "   texColor.r = 1.0f;" +
//            "   texColor.b = 1.0f;" +
//            "   texColor.g = 1.0f;" +
            "if (texColor.a <0.01f){" +
            "discard;" +
            "}" +
            "     gl_FragColor = texColor;\n" +
//            "     gl_FragColor = vec4(texColor.r,texColor.g,texColor.b,texColor.a);\n" +
//            "     gl_FragColor = vec4(texColor.rgb,texColor.a);\n" +
            "}";
//texture2D(inputImageTexture, textureCoordinate)

    private final String vertexShaderCode2 = "uniform mat4 projection;\n" +
            "uniform mat4 modelView;\n" +
            "attribute vec4 vPosition;\n" +
            "attribute vec4 SourceColor; // color of vertex\n" +
            "varying vec4 DestinationColor; // will pass out to fragment shader\n" +
            "\n" +
            "void main(void)\n" +
            "{\n" +
            "    DestinationColor = SourceColor;\n" +
            "    gl_Position =  vPosition;\n" +
            "}\n";
    private final String fragmentShaderCode2 ="precision mediump float;\n" +
            "uniform highp float color_selector;\n" +
            "varying lowp vec4 DestinationColor;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "        gl_FragColor = DestinationColor;\n" +
            "    \n" +
            "}\n";
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
//    private int[] mBeautifyTextureId;
//    private int[] mTextureOutId;
//    private int[] mFilterTextureOutId;

    private int leftmeimaoTextureId=OpenGLUtils.NO_TEXTURE;
    private boolean mCameraChanging = false;
    private int mCurrentPreview = 0;
    private ArrayList<String> mSupportedPreviewSizes;

    private FpsChangeListener mFpsListener;
    private FaceAttributeChangeListener mFaceAttributeChangeListener;
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
    private float[] mBeautifyParams = new float[6];

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
    private int mDetectConfig = STMobileHumanActionNative.ST_MOBILE_FACE_240_DETECT;
    private boolean mIsCreateHumanActionHandleSucceeded = false;
    private Object mHumanActionHandleLock = new Object();

    private boolean mNeedShowRect = true;
    private int mScreenIndexRectWidth = 0;
    private int mIndexRectWidthSmall = 128;//640X480
    private int mIndexRectWidthLarge = 256;//1280X720

    private Rect mTargetRect = new Rect();
    private Rect mIndexRect = new Rect();
    private boolean mNeedSetObjectTarget = false;
    private boolean mIsObjectTracking = false;
    int textureMId = OpenGLUtils.NO_TEXTURE;
    int textSiaHongId = OpenGLUtils.NO_TEXTURE;
    int textJiemao = OpenGLUtils.NO_TEXTURE;
    //face extra info swicth
    private boolean mNeedFaceExtraInfo = true;
    private int id=0;
    private int mHumanActionCreateConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_VIDEO;
    private int mGLProgId,mGLAttribPosition,mGLUniformTexture,mGLAttribTextureCoordinate;//贴图
    private int mGLMouseId, mGLAttribMousePos,mGLUniformTexture2, mGLAttribMouseColor;//绘图

//    private int mHumanActionCreateConfig = STCommon.ST_MOBILE_ENABLE_FACE_240_DETECT;

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
//        glSurfaceView.setZOrderOnTop(true);
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mProgram = GLES20.glCreateProgram(); // create empty OpenGL Program

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
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//        GLES20.glEnable( GLES20.GL_BLEND );
//        GLES20.glBlendFunc( GLES20.GL_SRC_ALPHA , GLES20.GL_ONE_MINUS_SRC_ALPHA );
         mGLProgId = OpenGLUtils.loadProgram(vertexShaderCode, fragmentShaderCode);
        mGLMouseId =OpenGLUtils.loadProgram(vertexShaderCode2, fragmentShaderCode2);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,"inputTextureCoordinate");


        mGLAttribMousePos = GLES20.glGetAttribLocation(mGLMouseId, "vPosition");
        mGLAttribMouseColor = GLES20.glGetUniformLocation(mGLMouseId,"SourceColor");
        if (mCameraProxy.getCamera() != null) {
            setUpCamera();
        }
        //上面是纹理贴图的取样方式，包括拉伸方式，取临近值和线性值
        InputStream in = null;
        try {
            in =mContext.getAssets().open("browleft.png");
        } catch (Exception e) {
        }


//                                final Bitmap bitmap = BitmapFactory.decodeStream(in);

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

    private void initBeauty() {
        // 初始化beautify,preview的宽高
        int result = mStBeautifyNative.createInstance(mImageHeight, mImageWidth);
        LogUtils.i(TAG, "the result is for initBeautify " + result);
        if (result == 0) {
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH, 0.36f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, 0.74f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH, 0.30f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO, 0.13f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO, 0.11f);
            mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO, 0.10f);
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
            mDetectConfig = config |STMobileHumanActionNative.ST_MOBILE_FACE_240_DETECT;
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

//        if (mBeautifyTextureId == null) {
//            mBeautifyTextureId = new int[1];
            //GlUtil.initEffectTexture(mImageWidth, mImageHeight, mBeautifyTextureId, GLES20.GL_TEXTURE_2D);
//        }
//
//        if (mTextureOutId == null) {
//            mTextureOutId = new int[1];
//            GlUtil.initEffectTexture(mImageWidth, mImageHeight, mTextureOutId, GLES20.GL_TEXTURE_2D);
//        }

        if(mSurfaceTexture != null){
            mSurfaceTexture.updateTexImage();
        }else{
            return;
        }

        mStartTime = System.currentTimeMillis();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mRGBABuffer.rewind();

        long preProcessCostTime = System.currentTimeMillis();
        int textureId = mGLRender.preProcess(mTextureId, mRGBABuffer);
        LogUtils.i(TAG, "preprocess cost time: %d", System.currentTimeMillis() - preProcessCostTime);

        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        mGLRender.onDrawFrame(textureId);

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
                if(!mNeedFaceExtraInfo || !(mNeedBeautify || mNeedSticker || mNeedFaceAttribute)){
                    Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
                    mHandler.sendMessage(msg);
                }
            }

            if(mNeedBeautify || mNeedSticker || mNeedFaceAttribute && mIsCreateHumanActionHandleSucceeded) {
                STMobile106[] arrayFaces = null, arrayOutFaces = null;
                int orientation = getCurrentOrientation();
                long humanActionCostTime = System.currentTimeMillis();
                STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(mRGBABuffer.array(),STCommon.ST_PIX_FMT_RGBA8888,
                        mDetectConfig, orientation, mImageWidth, mImageHeight);
                LogUtils.i(TAG, "human action cost time: %d", System.currentTimeMillis() - humanActionCostTime);
                if(humanAction != null){
                    LogUtils.i(TAG, "human action background result: %d", humanAction.backGroundRet);

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

                if(mNeedFaceExtraInfo && humanAction != null && !mNeedObject){
                    if(humanAction.faceExtraInfo != null){
                        arrayFaces = humanAction.getMobileFaces();
                        if(arrayFaces!=null){
//                            //在屏幕画点
                            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.biaozhunmei);
                            Bitmap bitmapSaihong = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.saihong);
                            Bitmap bitmapjiemao = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ziranjiemao);

//                                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//                                ByteBuffer buffer = SaveMyBitmap(bitmap);
//                                PNGInfoHandle.getPNGHandle(mContext.getAssets(), "browleft.png").
//                                        glPngTexImage2D(GLES20.GL_TEXTURE_2D, 0);
                            id = OpenGLUtils.loadTexture(bitmap,textureMId,true);
                            textSiaHongId = OpenGLUtils.loadTexture(bitmapSaihong,textSiaHongId, true);
                            textJiemao = OpenGLUtils.loadTexture(bitmapjiemao,textJiemao,true);
                            for(int i =0;i<arrayFaces.length;i++){
                                STPoint[] stPoints = arrayFaces[i].getPoints_array();
                                STPoint[] stPoint240 = new STPoint[240];
                                STPoint[] points = humanAction.faceExtraInfo.getAllPoints();
                                STPoint[] pointsBrowLeft = humanAction.faceExtraInfo.getEyebrowLeftPoints(0);
                                STPoint[] pointsBrowRight = humanAction.faceExtraInfo.getEyebrowRightPoints(0);
                                STPoint[] pointsEyeLeft = humanAction.faceExtraInfo.getEyeLeftPoints(0);
                                STPoint[] pointsEyeRight = humanAction.faceExtraInfo.getEyeRightPoints(0);
                                STPoint[] pointsLips = humanAction.faceExtraInfo.getLipsPoints(0);
                                //106+左眼+右眼+做眉毛+右眉毛+嘴
                                for(int j = 0;j<106;j++){
                                    stPoint240[j] = stPoints[j];
                                }
                                //左眼
                                for(int j = 0 ;j<22;j++){
                                    stPoint240[106+j] =pointsEyeLeft[j];
                                }
                                //右眼
                                for(int j = 0 ;j<22;j++){
                                    stPoint240[106+22+j] =pointsEyeRight[j];
                                }
                                //左眉毛
                                for(int j = 0 ;j<13;j++){
                                    stPoint240[106+22+22+j] =pointsBrowLeft[j];
                                }
                                //右眉毛
                                for(int j = 0 ;j<13;j++){
                                    stPoint240[106+22*2+13+j] =pointsEyeRight[j];
                                }
                                //嘴
                                for(int j = 0 ;j<64;j++){
                                    stPoint240[106+22*2+13*2+j] =pointsLips[j];
                                }
                                drawLeft(pointsBrowLeft, id);
                                drawRight(pointsBrowRight,id);
                                drawSaiHong(stPoint240,textSiaHongId);
                                drawRightJiemao(stPoint240,textJiemao);
                                drawZuichun(stPoint240);
//                        }
                        }
                        }
                    }

                    if(humanAction.faceExtraInfo != null && humanAction.faceExtraInfo.eyebrowCount == 0 &&
                            humanAction.faceExtraInfo.eyeCount == 0 && humanAction.faceExtraInfo.lipsCount == 0){
                        Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
                        mHandler.sendMessage(msg);
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
//                    long beautyStartTime = System.currentTimeMillis();
//                    result = mStBeautifyNative.processTexture(textureId, mImageWidth, mImageHeight, arrayFaces, mBeautifyTextureId[0], arrayOutFaces);
//                    long beautyEndTime = System.currentTimeMillis();
//                    LogUtils.i(TAG, "beautify cost time: %d", beautyEndTime-beautyStartTime);
//                    if (result == 0) {
//                        textureId = mBeautifyTextureId[0];
//                    }
//
//                    if (arrayOutFaces != null && arrayOutFaces.length != 0 && humanAction != null && result == 0) {
//                        boolean replace = humanAction.replaceMobile106(arrayOutFaces);
//                        LogUtils.i(TAG, "replace enlarge eye and shrink face action: " + replace);
//                    }
                }


                //调用贴纸API绘制贴纸
//                if(mNeedSticker){
//                    boolean needOutputBuffer = false; //如果需要输出buffer推流或其他，设置该开关为true
//                    long stickerStartTime = System.currentTimeMillis();
//                    if (!needOutputBuffer) {
//                        result = mStStickerNative.processTexture(textureId, humanAction, orientation, mImageWidth, mImageHeight,
//                                false, mTextureOutId[0]);
//                    } else {  //如果需要输出buffer用作推流等
//                        byte[] imageOut = new byte[mImageWidth * mImageHeight * 4];
//                        result = mStStickerNative.processTextureAndOutputBuffer(textureId, humanAction, orientation, mImageWidth,
//                                mImageHeight, false, mTextureOutId[0], STCommon.ST_PIX_FMT_RGBA8888, imageOut);
//                    }
//
//                    LogUtils.i(TAG, "processTexture result: %d", result);
//                    LogUtils.i(TAG, "sticker cost time: %d", System.currentTimeMillis() - stickerStartTime);
//
//                    if (result == 0) {
//                        textureId = mTextureOutId[0];
//                    }
//                }

            }

            if(mCurrentFilterStyle != mFilterStyle){
                mCurrentFilterStyle = mFilterStyle;
                mSTMobileStreamFilterNative.setStyle(mCurrentFilterStyle);
            }
            if(mCurrentFilterStrength != mFilterStrength){
                mCurrentFilterStrength = mFilterStrength;
                mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mCurrentFilterStrength);
            }

//            if(mFilterTextureOutId == null){
//                mFilterTextureOutId = new int[1];
//                GlUtil.initEffectTexture(mImageWidth, mImageHeight, mFilterTextureOutId, GLES20.GL_TEXTURE_2D);
//            }
//
//            //滤镜
//            if(mNeedFilter){
//                long filterStartTime = System.currentTimeMillis();
//                int ret = mSTMobileStreamFilterNative.processTexture(textureId, mImageWidth, mImageHeight, mFilterTextureOutId[0]);
//                LogUtils.i(TAG, "filter cost time: %d", System.currentTimeMillis() - filterStartTime);
//                if(ret == 0){
//                    textureId = mFilterTextureOutId[0];
//                }
//            }

            LogUtils.i(TAG, "frame cost time total: %d", System.currentTimeMillis() - mStartTime);
        }else{
            Message msg = mHandler.obtainMessage(CameraActivity.MSG_CLEAR_OBJECT);
            mHandler.sendMessage(msg);

            resetObjectTrack();
        }

        if(mNeedSave) {
            savePicture(textureId);
            mNeedSave = false;
        }

        long dt = System.currentTimeMillis() - mStartTime;
        if(mFpsListener != null) {
            mFpsListener.onFpsChanged((int) dt);
        }

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

        String size = mSupportedPreviewSizes.get(mCurrentPreview);
        int index = size.indexOf('x');
        mImageHeight = Integer.parseInt(size.substring(0, index));
        mImageWidth = Integer.parseInt(size.substring(index + 1));

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
            }else if(mSupportedPreviewSizes.contains("1280x720")){
                mCurrentPreview = mSupportedPreviewSizes.indexOf("1280x720");
            }
        }

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
//        deleteInternalTextures();
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

//    private void deleteInternalTextures() {
//        if (mBeautifyTextureId != null) {
//            GLES20.glDeleteTextures(1, mBeautifyTextureId, 0);
//            mBeautifyTextureId = null;
//        }
//
//        if (mTextureOutId != null) {
//            GLES20.glDeleteTextures(1, mTextureOutId, 0);
//            mTextureOutId = null;
//        }
//
//        if(mFilterTextureOutId != null){
//            GLES20.glDeleteTextures(1, mFilterTextureOutId, 0);
//            mFilterTextureOutId = null;
//        }
//    }

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

//                deleteInternalTextures();
                if (mCameraProxy.getCamera() != null) {
                    setUpCamera();
                }

                mGLRender.init(mImageWidth, mImageHeight);

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

    public void drawLeftMeiMao( STPoint[] points,int textureId){
        float x1,y1,x2,y2,x3,y3,x4,y4,d;
        x2 = points[12].getX();
        y2 = points[12].getY();
        x4 =points[6].getX();
        y4 = y2 + 2*(points[6].getY()-y2);
        x1 = points[0].getX();
        y1 = points[0].getY();
        x3 = x1;
        y3 = y1+(y4-y2);
        d = (y3-y1)/3;
        y1 = y1-d;
        y3 = y3-d;
        x1  = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1);
        x2  = changeToGLPointT(x2);
        y2 = changeToGLPointR(y2);
        x3  = changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);
        x4  = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        float squareVertices2[] = {

                x1,y1,
                x2,y2,
                x3,y3,
                x4,y4,

        };

        float textureVertices2[] = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f,  0.0f,
                1.0f,  0.0f,
        };

        int mGLProgId = OpenGLUtils.loadProgram(vertexShaderCode, fragmentShaderCode);
        int mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        int mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        int mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,"inputTextureCoordinate");
        GLES20.glUniform1i(mGLUniformTexture, 0);
        GLES20.glUseProgram(mGLProgId);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendColor(0,0,0,0);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR,GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);

    }
    public float changeToGLPointT(float x){
        float tempX = (x - mSurfaceWidth/2) / (mSurfaceWidth/2);
        return tempX;
    };
    public float changeToGLPointR(float y){
        float tempY = (mSurfaceHeight/2-y) / (mSurfaceHeight/2);
        return tempY;
    };

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

    public void drawBrow(STPoint[] points,int textureId){
        float x,y,x0,y0,x3,y3,x4,y4;
        float x1,y1,x2,y2,k,theta,d;
       //画右眉毛
        x0 = points[12].getX();
        y0 = points[12].getY();
        x = points[6].getX();
        y = points[6].getY();

        x1 = (float) (x0 - (x - x0)*0.124);
        y1 = (float) (y0 - (y - y0)*0.124);
        x2 =(float) (x + (x - x0)*0.2);
        y2 =(float) (y + (y - y0)*0.2);

        x0 =  points[0].getX();
        y0 =  points[0].getX();

        k = (y2-y1)/(x2-x1);
        theta =(float) ( Math.atan(k));

        d = (float) (Math.abs(k*x0-y0+y1-k*x1)/ Math.sqrt(k*k+1)*2.232);

        x3 = (float) (x1 + d * Math.sin(theta));
        y3 = (float) (y1 - d * Math.cos(theta));

        x4 = (float) (x2 + d * Math.sin(theta));
        y4 = (float) (y2 - d * Math.cos(theta));

        x1 =(float)  (x1 - d * Math.sin(theta));
        y1 =(float)  (y1 + d * Math.cos(theta));

        x2 = (float) (x2 - d * Math.sin(theta));
        y2 = (float) (y2 + d * Math.cos(theta));

        x1  = changeToGLPointT((float) x1);
        y1 = changeToGLPointR((float)y1);

        x2  =changeToGLPointT((float) x2);
        y2 =  changeToGLPointR((float)y2);

        x3  = changeToGLPointT((float) x3);
        y3 = changeToGLPointR((float)y3);

        x4  =changeToGLPointT((float) x4);
        y4 =  changeToGLPointR((float)y4);

        float squareVertices[] = {

                x1,y1,
                x2,y2,
                x3,y3,
                x4,y4,

        };

        float textureVertices1[] = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,

        };
        int mGLProgId = OpenGLUtils.loadProgram(vertexShaderCode, fragmentShaderCode);
        int mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        int mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        int mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,"inputTextureCoordinate");
        GLES20.glUniform1i(mGLUniformTexture, 0);
        GLES20.glUseProgram(mGLProgId);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR,GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices1));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
    }


    public ByteBuffer SaveMyBitmap(Bitmap bitmap)
    {
        int width,height;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        mBitmapSize=width*height*4;
        ByteBuffer mBuf = ByteBuffer.allocate(mBitmapSize);
        return mBuf;

    };

    private Buffer createFloatBuffer(float[] floats) {
        return ByteBuffer
                .allocateDirect(floats.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(floats)
                .rewind();
    }



    public void drawRight(STPoint[] points,int textureId){
        float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4,k;
        double d,theta;
        //画右眉毛
        x0 = points[6].getX();
        y0 = points[6].getY();

        x = points[0].getX();
        y = points[0].getY();

        x1 = x0 - (x - x0)*0.124f;
        y1 = y0 - (y - y0)*0.124f;

        x2 = x + (x - x0)*0.2f;
        y2 = y + (y - y0)*0.2f;

        x0 = points[12].getX();
        y0 = points[12].getY();

        k = (y2-y1)/(x2-x1);

        theta = Math.atan(k);

        d = Math.abs(k*x0-y0+y1-k*x1)/ Math.sqrt(k*k+1)*2.232;

        x3 = (float) (x1 + d * Math.sin(theta));
        y3 = (float) (y1 - d * Math.cos(theta));

        x4 =(float) ( x2 + d * Math.sin(theta));
        y4 = (float) (y2 - d * Math.cos(theta));

        x1 = (float) (x1 - d * Math.sin(theta));
        y1 = (float) (y1 + d * Math.cos(theta));

        x2 = (float) (x2 - d * Math.sin(theta));
        y2 = (float) (y2 + d * Math.cos(theta));

        x1  = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1);
        x2  = changeToGLPointT(x2);
        y2 =changeToGLPointR(y2);
        x3  = changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);
        x4  = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        float squareVertices[] = {
                x1,y1,
                x2,y2,
                x3,y3,
                x4,y4,
        };

        float textureVertices1[] = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
        };

        GLES20.glUniform1i(mGLUniformTexture, 0);
        GLES20.glUseProgram(mGLProgId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices1));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    public void drawLeft(STPoint[] points,int textureId){
        float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4,k;
        double d,theta;
        x0 = points[6].getX();
        y0 = points[6].getY();
        x = points[0].getX();
        y =  points[0].getY();

        x1 = (float) (x0 - (x - x0)*0.2);
        y1 = (float) (y0 - (y - y0)*0.2);

        x2 = (float) (x + (x - x0)*0.124);
        y2 = (float) (y + (y - y0)*0.124);

        x0 = points[12].getX();
        y0 =  points[12].getY();

        k = (y2-y1)/(x2-x1);

        theta = Math.atan(k);

        d = Math.abs(k*x0-y0+y1-k*x1)/Math.sqrt(k*k+1)*2.232;

        x3 = (float) (x1 + d * Math.sin(theta));
        y3 = (float) (y1 - d * Math.cos(theta));

        x4 = (float) (x2 + d * Math.sin(theta));
        y4 = (float) (y2 - d * Math.cos(theta));

        x1 = (float) (x1 - d * Math.sin(theta));
        y1 = (float) (y1 + d * Math.cos(theta));

        x2 = (float) (x2 - d * Math.sin(theta));
        y2 = (float) (y2 + d * Math.cos(theta));

        x1  = changeToGLPointT(x1);
        y1 =changeToGLPointR(y1);

        x2  =changeToGLPointT(x2);
        y2 = changeToGLPointR(y2);

        x3  =changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);

        x4  =changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        float squareVertices[] = {
                x1,y1,
                x2,y2,
                x3,y3,
                x4,y4,
        };

        float textureVertices2[] = {

                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,


        };


        GLES20.glUniform1i(mGLUniformTexture, 0);
        GLES20.glUseProgram(mGLProgId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    public void drawMouse(STPoint[] points,int textureId){
        Point point;
        float p0,p1,p2,p3;

    }

    public void drawRightJiemao(STPoint[] points,int textureId){
        float x0,y0,x,y,x1,y1,x2,y2,x3,y3,x4,y4,k,d,b;
        double theta,theta0;
        //右上
        x = points[58].getX();
        y = points[58].getY();
        x0 = points[61].getX();
        y0 = points[61].getY();

        x1 = (float)(x - (x0 - x)*0.2);
        y1 = (float)(y - (y0 - y)*0.2);

        x2 = (float)(x0 + (x0 - x)*0.8);
        y2 = (float)(y0 + (y0 - y)*0.8);

        x0 = points[75].getX();
        y0 = points[75].getY();

        k = (y2-y1)/(x2-x1);

        theta = Math.atan(k);

        d = (float) (Math.abs(k*x0-y0+y1-k*x1)/Math.sqrt(k*k+1)*2.1);

        x3 = (float) (x1 + d * Math.sin(theta));
        y3 = (float) (y1 - d * Math.cos(theta));

        x4 =(float) ( x2 + d * Math.sin(theta));
        y4 = (float) (y2 - d * Math.cos(theta));

        x1  = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1);

        x2  = changeToGLPointT(x2);
        y2 = changeToGLPointR(y2);

        x3  = changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);

        x4  = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        float squareVertices[] = {
                x1,y1,
                x2,y2,
                x3,y3,
                x4,y4,
        };

        float textureVertices1[] = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f,  0.332f,
                1.0f,  0.332f,
        };
        GLES20.glUniform1i(mGLUniformTexture, 0);
        GLES20.glUseProgram(mGLProgId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices1));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        //右下
        x =points[58].getX();
        y = points[58].getY();
        x0 = points[61].getX();
        y0 = points[61].getY();

        x3 = (float) (x - (x0 - x)*0.2);
        y3 = (float) (y - (y0 - y)*0.2);

        x4 = (float) (x0 + (x0 - x)*0.8);
        y4 = (float) (y0 + (y0 - y)*0.8);

        x0 = points[76].getX();
        y0 = points[76].getY();

        k = (y4-y3)/(x4-x3);
        theta = Math.atan(k);

        d = (float) (Math.abs(k*x0-y0+y3-k*x3)/Math.sqrt(k*k+1)*2.2);//106
        //    (Math.abs(k*x0-y0+y1-k*x1)/Math.sqrt(k*k+1)*2.1);

        x1 =(float)(x3 - d * Math.sin(theta));
        y1 = (float) (y3 + d * Math.cos(theta));

        x2 = (float) (x4 - d * Math.sin(theta));
        y2 = (float) (y4 + d * Math.cos(theta));

        x1 = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1);

        x2 = changeToGLPointT(x2);
        y2 = changeToGLPointR(y2);

        x3 = changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);

        x4 = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        squareVertices[0] = x1;
        squareVertices[1] = y1;
        squareVertices[2] = x2;
        squareVertices[3] = y2;
        squareVertices[4] = x3;
        squareVertices[5] = y3;
        squareVertices[6] = x4;
        squareVertices[7] = y4;

        float textureVertices2[] = {
                0.0f,  0.0f,
                1.0f,  0.0f,
                0.0f, 0.332f,
                1.0f, 0.332f,

        };


        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        //左上
        x0 =points[52].getX();
        y0 = points[52].getY();
        x = points[55].getX();
        y =  points[55].getY();

        x1 =(float) (x0 - (x - x0)*0.8);
        y1 = (float) (y0 - (y - y0)*0.8);

        x2 = (float) (x + (x - x0)*0.2);
        y2 = (float) (y + (y - y0)*0.2);

        x0 = points[72].getX();
        y0 = points[72].getY();

        k = (y2-y1)/(x2-x1);

        theta = Math.atan(k);

        d = (float) (Math.abs(k*x0-y0+y1-k*x1)/Math.sqrt(k*k+1)*2.1);

        x3 = (float) (x1 + d * Math.sin(theta));
        y3 = (float) (y1 - d * Math.cos(theta));

        x4 = (float) (x2 + d * Math.sin(theta));
        y4 = (float) (y2 - d * Math.cos(theta));


        x1  = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1);
        x2  = changeToGLPointT(x2);
        y2 =changeToGLPointR(y2);
        x3  = changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);
        x4  = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        squareVertices[0] = x1;
        squareVertices[1] = y1;
        squareVertices[2] = x2;
        squareVertices[3] = y2;
        squareVertices[4] = x3;
        squareVertices[5] = y3;
        squareVertices[6] = x4;
        squareVertices[7] = y4;

        float textureVertices3[] = {
                1.0f, 1.0f,
                0.0f, 1.0f,
                1.0f,  0.332f,
                0.0f,  0.332f,
        };
        GLES20.glUniform1i(mGLUniformTexture, 0);
        GLES20.glUseProgram(mGLProgId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices3));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        //左下
        x0 = points[52].getX();
        y0 = points[52].getY();
        x = points[55].getX();
        y = points[55].getY();

        x3 = (float)(x0 - (x - x0)*0.8);
        y3 = (float)(y0 - (y - y0)*0.8);

        x4 = (float) (x + (x - x0)*0.2);
        y4 = (float) (y + (y - y0)*0.2);

        x0 = points[73].getX();
        y0 = points[73].getY();

        k = (y4-y3)/(x4-x3);
        theta = Math.atan(k);

        d = (float) (Math.abs(k*x0-y0+y3-k*x3)/Math.sqrt(k*k+1)*2.2);
        //    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*2.1;

        x1 = (float) (x3 - d * Math.sin(theta));
        y1 = (float) (y3 + d * Math.cos(theta));

        x2 = (float) (x4 - d * Math.sin(theta));
        y2 = (float) (y4 + d * Math.cos(theta));

        x1  = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1);

        x2  =  changeToGLPointT(x2);
        y2 = changeToGLPointR(y2);

        x3  =  changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);

        x4  = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        squareVertices[0] = x1;
        squareVertices[1] = y1;
        squareVertices[2] = x2;
        squareVertices[3] = y2;
        squareVertices[4] = x3;
        squareVertices[5] = y3;
        squareVertices[6] = x4;
        squareVertices[7] = y4;

        float textureVertices4[] = {
                1.0f,  0.0f,
                0.0f,  0.0f,
                1.0f, 0.332f,
                0.0f, 0.332f,
        };

        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices4));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);


    }

    //腮红
    public void drawSaiHong(STPoint[] points,int textureId){
        float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4;
        double theta,d,k;
        //画腮红
        x0 = points[82].getX();
        y0 = points[82].getY();
        x = points[83].getX();
        y = points[83].getY();

        x1 = (float) (x0 - (x - x0)*1.463);
        y1 = (float) (y0 - (y - y0)*1.463);

        x2 = (float) (x + (x - x0)*1.463);
        y2 = (float) (y + (y - y0)*1.463);

        x0 = points[45].getX();
        y0 = points[45].getY();

        k = (y2-y1)/(x2-x1);
        theta = Math.atan(k);
        d = Math.abs(k*x0-y0+y1-k*x1)/Math.sqrt(k*k+1)*2;
//    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*1.5;

        x3 =(float)(x1 + d * Math.sin(theta));
        y3 =(float)(y1 - d * Math.cos(theta));

        x4 = (float)(x2 + d * Math.sin(theta));
        y4 = (float)(y2 - d * Math.cos(theta));

        d = Math.abs(k*x0-y0+y1-k*x1)/Math.sqrt(k*k+1)*1.456;

        x1 = (float)(x1 - d * Math.sin(theta));
        y1 = (float)(y1 + d * Math.cos(theta));

        x2 = (float)(x2 - d * Math.sin(theta));
        y2 = (float)(y2 + d * Math.cos(theta));

        x1 = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1);

        x2 = changeToGLPointT(x2);
        y2 = changeToGLPointR(y2);

        x3  =  changeToGLPointT(x3);
        y3 = changeToGLPointR(y3);

        x4  = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4);

        float squareVertices[] = {

                x1,y1,
                x2,y2,
                x3,y3,
                x4,y4,

        };

        float textureVertices1[] = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,

        };
        GLES20.glUniform1i(mGLUniformTexture, 0);
        GLES20.glUseProgram(mGLProgId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices1));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }


    public  void drawZuichun(STPoint[] points){
        //绘制下嘴唇 10个点 84,85,97,86,98,87,99,88,90,89
        float[] squareVertices = new float[200];
        float[] squareVertices2 = new float[400];

        List<STPoint> pointMouseList = new ArrayList<>();
        List<STPoint> pointDownList = new ArrayList<>();

//        float[] _mousecolors = new {};

        STPoint fitPoint;
        float p0,p1,p2,p3;

        pointMouseList.add(points[176]);
        for(int i =1;i<=15;i++) {
            pointMouseList.add(points[209+i]);
        }
        pointMouseList.add(points[192]);
        pointDownList.add(points[176]);

        for(int i=0;i<=13;i++) {
            pointMouseList.add(points[225+i]);
        }

        pointDownList.add(points[192]);
        for(int  i= 0;i<pointMouseList.size(); i++) {
            pointDownList.add(points[192]);
        }
//
//    [self AddPointsToArr:upMouseBeAdd resultPointsNum:[upMouseBeAdd count]*2 fitPoint:upMouse];
//    [self AddPointsToArr:downMouseBeAdd resultPointsNum:[upMouseBeAdd count]*2+1 fitPoint:downMouse];
        //上段
        for(int i = 0; i<pointMouseList.size(); i++) {

            p0 = changeToGLPointT(pointMouseList.get(i).getX());
            p1 =  changeToGLPointR(pointMouseList.get(i).getY());
            p2 = changeToGLPointT(pointDownList.get(i).getX());
            p3 =  changeToGLPointR(pointDownList.get(i).getY());

            squareVertices[i*4]   = (float) (p0*1.1-p2*0.1);
            squareVertices[i*4+1] = (float) (p1*1.1-p3*0.1);
            squareVertices[i*4+2] = (float) (p0*4.0/5.0 + p2/5.0);
            squareVertices[i*4+3] = (float) (p1*4.0/5.0 + p3/5.0);
        }

        double k0=1.0;

        for(int i = 0; i<pointMouseList.size(); i++) {
            squareVertices2[i*8] = 0.5f;
            squareVertices2[i*8+1] = 0.6f;
            squareVertices2[i*8+2] = 0.7f;
//        squareVertices2[_index*8+3] = _mousecolors[3]/2.0;
            squareVertices2[i*8+3] = 0.9f/6.0f;

            squareVertices2[i*8+4] = 0.5f;
            squareVertices2[i*8+5] = 0.4f;
            squareVertices2[i*8+6] = 0.3f;

            if(i==0 || i == pointMouseList.size()-1)
            k0=5.0;
        else
            k0=1.0;
            squareVertices2[i*8+7] = (float) (0.5f/k0);
        }

        GLES20.glUseProgram(mGLMouseId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
//    glEnable(GL_LINE_SMOOTH_HINT);
//    glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        GLES20. glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glVertexAttribPointer(mGLAttribMousePos, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribMousePos);
        GLES20.glVertexAttribPointer(mGLAttribMouseColor, 4, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
        //中段
        for(int i  = 0; i<pointMouseList.size(); i++) {
            p0 =changeToGLPointT(pointMouseList.get(i).getX());
            p1 =changeToGLPointR(pointMouseList.get(i).getY());
            p2 = changeToGLPointT(pointDownList.get(i).getX());
            p3 = changeToGLPointT(pointDownList.get(i).getY());

            squareVertices[i*4]   = (float) (p0*4.0/5.0 + p2/5.0);
            squareVertices[i*4+1] = (float) (p1*4.0/5.0 + p3/5.0);
            squareVertices[i*4+2] = (float) (p0/5.0 + p2*4.0/5.0);
            squareVertices[i*4+3] = (float) (p1/5.0 + p3*4.0/5.0);
        }

        for(int i = 0; i<pointMouseList.size(); i++) {
            if(i==0 || i == pointMouseList.size()-1)
            k0=5.0;
        else
            k0=1.0;
            squareVertices2[i*8+3] = (float)(1.0f/k0);
            squareVertices2[i*8+7] = 1.0f;
        }

        //下段
        GLES20.glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribMousePos);
        GLES20.glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0,  Utils.getFloatBuffer(squareVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);

        for(int i = 0; i<pointMouseList.size(); i++) {
            p0 =changeToGLPointT(pointMouseList.get(i).getX());
            p1 =changeToGLPointR(pointMouseList.get(i).getY());
            p2 = changeToGLPointT(pointDownList.get(i).getX());
            p3 = changeToGLPointT(pointDownList.get(i).getY());

            squareVertices[i*4]   = (float) (p0/5.0 + p2*4.0/5.0);
            squareVertices[i*4+1] = (float) (p1/5.0 + p3*4.0/5.0);
            squareVertices[i*4+2] = (float) (p2*1.1-p0*0.1);
            squareVertices[i*4+3] =(float) (p3*1.1-p1*0.1);
        }

        for(int i = 0; i<pointMouseList.size(); i++) {
            squareVertices2[i*8+7] = 0.01f;
        }

        glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        glEnableVertexAttribArray(mGLAttribMousePos);
        glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices2));
        glEnableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);

        GLES20. glDisable( GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
//    [upMouse removeAllObjects];upMouse = nil;
//    [downMouse removeAllObjects];downMouse = nil;
//    [upMouseBeAdd removeAllObjects];upMouse = nil;
//    [downMouseBeAdd removeAllObjects];downMouse = nil;
    }

}
