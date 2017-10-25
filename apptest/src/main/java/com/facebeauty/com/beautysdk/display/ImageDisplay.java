package com.facebeauty.com.beautysdk.display;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.facebeauty.com.beautysdk.domain.FileSave;
import com.facebeauty.com.beautysdk.glutils.GlUtil;
import com.facebeauty.com.beautysdk.glutils.OpenGLUtils;
import com.facebeauty.com.beautysdk.glutils.STUtils;
import com.facebeauty.com.beautysdk.glutils.TextureRotationUtil;
import com.facebeauty.com.beautysdk.utils.FileUtils;
import com.facebeauty.com.beautysdk.utils.LogUtils;
import com.facebeauty.com.beautysdk.view.ImageCamerView;
import com.sensetime.stmobile.STBeautifyNative;
import com.sensetime.stmobile.STBeautyParamsType;
import com.sensetime.stmobile.STCommon;
import com.sensetime.stmobile.STFaceAttribute;
import com.sensetime.stmobile.STFilterParamsType;
import com.sensetime.stmobile.STHumanAction;
import com.sensetime.stmobile.STMobileFaceAttributeNative;
import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.STMobileStickerNative;
import com.sensetime.stmobile.STMobileStreamFilterNative;
import com.sensetime.stmobile.model.STMobile106;
import com.sensetime.stmobile.model.STPoint;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageDisplay implements Renderer {

    private Bitmap mOriginBitmap;
    private String TAG = "ImageDisplay";
    private int flag = 0;//0-视频 1-图片
	private int mImageWidth;
	private int mImageHeight;
	private GLSurfaceView mGlSurfaceView;
	private int mDisplayWidth;
	private int mDisplayHeight;
	public static final int MSG_SAVING_IMG = 1;
	protected Context mContext;
	protected final FloatBuffer mVertexBuffer;
	protected final FloatBuffer mTextureBuffer;
	private STGLRender mImageInputRender;
	private boolean mInitialized = false;
	private long mFrameCostTime = 0;
	private Bitmap mProcessedImage;
	private boolean mNeedSave = false;
	private Handler mHandler;

	private STMobileStickerNative mStStickerNative = new STMobileStickerNative();
	private STBeautifyNative mStBeautifyNative = new STBeautifyNative();
	private STMobileHumanActionNative mSTHumanActionNative = new STMobileHumanActionNative();
	private STMobileStreamFilterNative mSTMobileStreamFilterNative = new STMobileStreamFilterNative();
	private STMobileFaceAttributeNative mSTFaceAttributeNative = new STMobileFaceAttributeNative();

	private CostChangeListener mCostListener;

	private String mCurrentSticker;
	private String mCurrentFilterStyle;
	private float mCurrentFilterStrength = 0.5f;
	private float mFilterStrength = 0.5f;
	private String mFilterStyle;
	private float[] mBeautifyParams = new float[6];

	private boolean mNeedBeautify = false;
	private boolean mNeedFaceAttribute = true;
	private boolean mNeedSticker = false;
	private boolean mNeedFilter = false;
	private String mFaceAttribute = " ";
	private int[] mBeautifyTextureId;
	private int[] mTextureOutId;
	private int[] mFilterTextureOutId;
	private int[] mFaceJAw;
	private int textSiaHongId = OpenGLUtils.NO_TEXTURE;
	int textFaceId = OpenGLUtils.NO_TEXTURE;

	STPoint[] points;
	public static int[] beautyTypes = {
			STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH,
			STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH,
			STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH,
			STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO,
			STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO,
			STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO
	};

	private boolean mShowOriginal = false;
	private  STPoint[] pointsBrowLeft;//左眉毛
	private File file;
	private STPoint[] pointsBrowRight;//右眉毛
	private STPoint[] stPoints;//106个点
	private STPoint[] pointsEyeLeft;
	private STPoint[] pointsEyeRight;
	private STPoint[] pointsLips;
	float[] yanxianbgcolors = {0.0f, 0.0f, 0.0f, 0.0f};
	private int mHumanActionCreateConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_IMAGE;
	private int mHumanActionDetectConfig = STMobileHumanActionNative.ST_MOBILE_HUMAN_ACTION_DEFAULT_CONFIG_DETECT;
	private boolean mNeedFaceExtraInfo = true;
	private float faceValue=0,jawValue=0;
	private Bitmap bitmap;
	/**
	 * SurfaceTexureid
	 */
	protected int mTextureId = OpenGLUtils.NO_TEXTURE;
	STHumanAction humanAction;

    public ImageDisplay(Context context, GLSurfaceView glSurfaceView){
    	mImageInputRender = new STGLRender();
    	mGlSurfaceView = glSurfaceView;
    	glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(this);
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    	mContext = context;

//		 final float CUBE[] = {
//				-1.0f, -1.0f,
//				1.0f, -1.0f,
//				-1.0f, 1.0f,
//				1.0f, 1.0f,
//		};
		final float CUBE[] = {
//				0.0f, 0.0f,
//				1.0f, 0.0f,
//				0.0f, 1.0f,
//				1.0f, 1.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				0.0f, 0.0f,
				1.0f, 0.0f,

		};

		mVertexBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(CUBE).position(0);

		final float TEXTURE_ROTATED_180[] = {
				0.0f, 1.0f,
				1.0f, 1.0f,
				0.0f, 0.0f,
				1.0f, 0.0f,
		};
        mTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_ROTATED_180.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TEXTURE_ROTATED_180).position(0);

        if(mNeedFaceExtraInfo){
			mHumanActionCreateConfig = mHumanActionCreateConfig | STCommon.ST_MOBILE_ENABLE_FACE_240_DETECT;
		}
	}

	private void initFaceAttribute() {
		int result = mSTFaceAttributeNative.createInstance(FileUtils.getFaceAttributeModelPath(mContext));
		LogUtils.i(TAG, "the result for createInstance for faceAttribute is %d", result);
	}

	private void initHumanAction() {
		int result = mSTHumanActionNative.createInstance(FileUtils.getTrackModelPath(mContext), mHumanActionCreateConfig);
		LogUtils.i(TAG, "the result for createInstance for human action is %d", result);
	}

	private void initSticker() {
		int result = mStStickerNative.createInstance(null);
		LogUtils.i(TAG, "the result for createInstance for sticker is %d", result);

		result = mStStickerNative.setWaitingMaterialLoaded(true);
		LogUtils.i(TAG, "the result for createInstance for setWaitingMaterialLoaded is %d", result);
	}

	private void initBeauty() {
		// 初始化beautify,preview的宽高
		int result = mStBeautifyNative.createInstance(mImageHeight, mImageWidth);
		LogUtils.i(TAG, "the result is for initBeautify " + result);
		if (result == 0) {
			mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_REDDEN_STRENGTH, 0.3f);
			mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, 0.5f);
			mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_WHITEN_STRENGTH, 0.02f);
			mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_ENLARGE_EYE_RATIO, 0.0f);
			mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_FACE_RATIO, 0.0f);
			mStBeautifyNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SHRINK_JAW_RATIO, 0.0f);
		}
	}

	private void initFilter(){
		mSTMobileStreamFilterNative.createInstance();

		mFilterStyle = null;
		mSTMobileStreamFilterNative.setStyle(mFilterStyle);

		mFilterStrength = 0.5f;
		mSTMobileStreamFilterNative.setParam(STFilterParamsType.ST_FILTER_STRENGTH, mFilterStrength);
	}

	public void enableBeautify(boolean needBeautify) {
		mNeedBeautify = needBeautify;
	}

	public void enableFaceAttribute(boolean needFaceAttribute) {
		mNeedFaceAttribute = needFaceAttribute;
		refreshDisplay();
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
		if(!needSticker){
			refreshDisplay();
		}
	}

	public void enableFilter(boolean needFilter){
		mNeedFilter = needFilter;
		if(!needFilter){
			refreshDisplay();
		}
	}

	public long getCostTime(){
		return mFrameCostTime;
	}

	public String getFaceAttributeString() {
		return mFaceAttribute;
	}

	public void setShowSticker(String sticker) {
		mCurrentSticker = sticker;
		mStStickerNative.changeSticker(mCurrentSticker);
		refreshDisplay();
	}

	public void setFilterStyle(String modelPath) {
		mFilterStyle = modelPath;
		refreshDisplay();
	}

	public void setFilterStrength(float strength){
		mFilterStrength = strength;
		refreshDisplay();
	}

	public void setBeautyParam(int index, float value) {
		if(mBeautifyParams[index] != value){
			mStBeautifyNative.setParam(beautyTypes[index], value);
			mBeautifyParams[index] = value;
			refreshDisplay();
		}

	}

	public float[] getBeautyParams(){
		float[] values = new float[6];
		for(int i = 0; i< mBeautifyParams.length; i++){
			values[i] = mBeautifyParams[i];
		}

		return values;
	}

	public void enableSave(boolean save,File file){
		this.file = file;
		mNeedSave = save;
		refreshDisplay();
	}

	public void setHandler(Handler handler){
		mHandler = handler;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0,0,0,0);
        GLES20.glDisable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		mDisplayWidth = width;
		mDisplayHeight = height;
		adjustImageDisplaySize();
		mInitialized = true;
		mImageInputRender.init(mImageWidth,mImageHeight);
	}


	@Override
	public void onDrawFrame(GL10 gl) {
		long frameStartTime = System.currentTimeMillis();
		int mTemTextureId = 0;
		if(!mInitialized)
			return;
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		int textureId = OpenGLUtils.NO_TEXTURE;

		if(mOriginBitmap != null && mTextureId == OpenGLUtils.NO_TEXTURE){
			mTextureId = OpenGLUtils.loadTexture(mOriginBitmap, OpenGLUtils.NO_TEXTURE);
			textureId = mTextureId;
		}else if(mTextureId != OpenGLUtils.NO_TEXTURE){
			textureId = mTextureId;
		}else{
			return;
		}
		mTemTextureId = textureId;
		if (mBeautifyTextureId == null) {
			mBeautifyTextureId = new int[1];
			GlUtil.initEffectTexture(mImageWidth, mImageHeight, mBeautifyTextureId, GLES20.GL_TEXTURE_2D);
		}

		if (mTextureOutId == null) {
			mTextureOutId = new int[1];
			GlUtil.initEffectTexture(mImageWidth, mImageHeight, mTextureOutId, GLES20.GL_TEXTURE_2D);
		}
		if(mFaceJAw ==null){
			mFaceJAw = new int[1];
			GlUtil.initEffectTexture(mImageWidth, mImageHeight, mFaceJAw, GLES20.GL_TEXTURE_2D);
		}

//		if (mTextureOutId == null) {
//			mTextureOutId = new int[1];
//			GlUtil.initEffectTexture(mImageWidth, mImageHeight, mTextureOutId, GLES20.GL_TEXTURE_2D);
//		}

		byte[] mTmpBuffer = null;
		if(mOriginBitmap != null) {
			if(mTmpBuffer == null){
				mTmpBuffer = STUtils.getBGRFromBitmap(mOriginBitmap);
			}

			if(!mShowOriginal){
				if(mNeedBeautify || mCurrentSticker != null || mNeedFaceAttribute) {
					STMobile106[] arrayFaces = null, arrayOutFaces = null;
					int orientation = 0;
					long humanActionCostTime = System.currentTimeMillis();
					STHumanAction humanAction = mSTHumanActionNative.humanActionDetect(mTmpBuffer, STCommon.ST_PIX_FMT_BGR888,
							mHumanActionDetectConfig, orientation,
							mImageWidth, mImageHeight);
					LogUtils.i(TAG, "human action cost time: %d", System.currentTimeMillis() - humanActionCostTime);
					this.humanAction = humanAction;
					if(mNeedFaceExtraInfo && humanAction != null){
							arrayFaces = humanAction.getMobileFaces();
							if(arrayFaces!=null) {
								for (int i = 0; i < arrayFaces.length; i++) {
									stPoints = arrayFaces[i].getPoints_array();
									points = new STPoint[106];
									for (int j = 0; j < 106; j++) {
										points[j] = stPoints[j];
									}
							}
						}

					}

					if(arrayFaces != null && arrayFaces.length != 0){
						if (mNeedFaceAttribute && arrayFaces != null && arrayFaces.length != 0) { // face attribute
							STFaceAttribute[] arrayFaceAttribute = new STFaceAttribute[arrayFaces.length];
							long attributeCostTime = System.currentTimeMillis();
							int result = mSTFaceAttributeNative.detect(mTmpBuffer, STCommon.ST_PIX_FMT_BGR888, mImageWidth, mImageHeight, arrayFaces, arrayFaceAttribute);
							LogUtils.i(TAG, "attribute cost time: %d", System.currentTimeMillis() - attributeCostTime);
							if (result == 0) {
								if (arrayFaceAttribute[0].attribute_count > 0) {
									mFaceAttribute = genFaceAttributeString(arrayFaceAttribute[0]);
									mNeedFaceAttribute = false;
								} else {
									mFaceAttribute = "null";
								}
							}
						}
					}

					if (mNeedBeautify) {// do beautify
						long beautyStartTime = System.currentTimeMillis();
						int result = mStBeautifyNative.processTexture(textureId, mImageWidth, mImageHeight, arrayFaces, mBeautifyTextureId[0], arrayOutFaces);
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

					if(mNeedSticker){
						long stickerStartTime = System.currentTimeMillis();
						int result = mStStickerNative.processTexture(textureId, humanAction, orientation, mImageWidth, mImageHeight,
								false, mTextureOutId[0]);
						LogUtils.i(TAG, "sticker cost time: %d", System.currentTimeMillis()-stickerStartTime);
						if (result == 0) {
							textureId = mTextureOutId[0];
						}
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

				if (mFilterTextureOutId == null) {
					mFilterTextureOutId = new int[1];
					GlUtil.initEffectTexture(mImageWidth, mImageHeight, mFilterTextureOutId, GLES20.GL_TEXTURE_2D);
				}
				//滤镜
				if(mNeedFilter){
					long filterStartTime = System.currentTimeMillis();
					int ret = mSTMobileStreamFilterNative.processTexture(textureId, mImageWidth, mImageHeight, mFilterTextureOutId[0]);
					if(ret == 0){textureId = mFilterTextureOutId[0];}
				}
				int frameBuffer = mImageInputRender.getFrameBufferId();
				int texid = 0;
				if(mTemTextureId != textureId)
				{
					texid = mImageInputRender.bindFrameBuffer();
				}

				GlUtil.checkGlError("glUseProgram");
				if(points != null) {
					GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
					mImageInputRender.onDrawFrame(points, textureId,faceValue,jawValue);
					if( bitmap != null){
						textSiaHongId = OpenGLUtils.loadTexture(bitmap, textSiaHongId, false);
						mImageInputRender.makeup(stPoints,textSiaHongId,yanxianbgcolors);
					}
					GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
				}
//				else
				{
					GLES20.glViewport(0, 0, mDisplayWidth, mDisplayHeight);
					mImageInputRender.onDrawFrame(texid,mVertexBuffer,mTextureBuffer);
				}
				textureId = texid;
				GlUtil.checkGlError("glUseProgram");
			}
		}
		mFrameCostTime = System.currentTimeMillis() - frameStartTime;
		if (mCostListener != null) {
			mCostListener.onCostChanged((int)mFrameCostTime);
		}

		if(mNeedSave){
			textureToBitmap(textureId);
			mNeedSave =false;

		}
	}

	public void setImageBitmap(Bitmap bitmap) {
		if (bitmap == null || bitmap.isRecycled())
			return;
		mImageWidth = bitmap.getWidth();
		mImageHeight = bitmap.getHeight();
		mOriginBitmap = bitmap;
		adjustImageDisplaySize();
		refreshDisplay();
	}

	public void setShowOriginal(boolean isShow)
	{
		mShowOriginal = isShow;
		refreshDisplay();
	}

	private void refreshDisplay(){
//		deleteTextures();
		mGlSurfaceView.requestRender();
	}

	public void onResume(){

		initBeauty();
		initSticker();
		initHumanAction();
		initFaceAttribute();
		initFilter();
//		initMakeup();
		mGlSurfaceView.onResume();

		if(mNeedSticker || mNeedFilter){
			mStStickerNative.changeSticker(mCurrentSticker);
			mFilterStyle = mCurrentFilterStyle;
			mFilterStrength = mCurrentFilterStrength;
			mCurrentFilterStyle = null;
			mCurrentFilterStrength = 0;
		}
	}

	public void onPause(){
		//mCurrentSticker = null;

		mSTHumanActionNative.destroyInstance();
		mSTFaceAttributeNative.destroyInstance();
		mGlSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
				mStStickerNative.destroyInstance();
				mStBeautifyNative.destroyBeautify();
				mSTMobileStreamFilterNative.destroyInstance();

				deleteTextures();
			}
		});

		mGlSurfaceView.onPause();
	}

	public void onDestroy(){

	}

	private void adjustImageDisplaySize() {
		float ratio1 = (float)mDisplayWidth / mImageWidth;
        float ratio2 = (float)mDisplayHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / (float)mDisplayWidth;
        float ratioHeight = imageHeightNew / (float)mDisplayHeight;

		final float CUBE[] = {
				-1.0f, -1.0f,
				1.0f, -1.0f,
				-1.0f, 1.0f,
				1.0f, 1.0f,
		};

        float[] cube = new float[]{
        		CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
        		CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
        		CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
        		CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
        };
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }

	private void textureToBitmap(int textureId){
		FileSave fileSave = new FileSave();
		ByteBuffer mTmpBuffer = ByteBuffer.allocate(mImageHeight * mImageWidth * 4);
		int[] mFrameBuffers = new int[1];
		if(textureId != OpenGLUtils.NO_TEXTURE) {
			GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,textureId, 0);
		}
		GLES20.glReadPixels(0, 0, mImageWidth, mImageHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mTmpBuffer);

		mProcessedImage = STUtils.getBitmapFromRGBA(mTmpBuffer.array(),mImageWidth,mImageHeight);
		//mProcessedImage = Bitmap.createBitmap(imageData, mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
		Message msg = Message.obtain(mHandler);
		fileSave.setBitmap(mTmpBuffer);
		fileSave.setBitFile(mProcessedImage);
		fileSave.setFile(file);
		msg.obj = fileSave;
		msg.what = ImageCamerView.MSG_SAVING_IMG;
		msg.sendToTarget();
	}

	public Bitmap getBitmap(){
		return mProcessedImage;
	}

	protected void deleteTextures() {
		if(mTextureId != OpenGLUtils.NO_TEXTURE)
			mGlSurfaceView.queueEvent(new Runnable() {

				@Override
				public void run() {
	                GLES20.glDeleteTextures(1, new int[]{
	                        mTextureId
	                }, 0);
	                mTextureId = OpenGLUtils.NO_TEXTURE;

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
	        });
    }

	public interface CostChangeListener {
		void onCostChanged(int value);
	}

	public void setCostChangeListener(ImageDisplay.CostChangeListener listener) {
		mCostListener = listener;
	}

	public void setJawandFace(float faceValue,float jawValue){
		this.faceValue = faceValue;
		this.jawValue = jawValue;
		refreshDisplay();
	}

	public void setEyeLips(Bitmap bitmap,float[] foundationColor){
		this.bitmap = bitmap;
		refreshDisplay();
	}
}
