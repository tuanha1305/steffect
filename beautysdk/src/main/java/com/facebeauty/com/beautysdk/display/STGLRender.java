package com.facebeauty.com.beautysdk.display;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.facebeauty.com.beautysdk.glutils.GlUtil;
import com.facebeauty.com.beautysdk.glutils.OpenGLUtils;
import com.facebeauty.com.beautysdk.glutils.TextureRotationUtil;
import com.facebeauty.com.beautysdk.glutils.Utils;
import com.facebeauty.com.beautysdk.utils.LogUtils;
import com.sensetime.stmobile.model.STPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static android.opengl.GLES20.GL_FLOAT;

/**
 * Created by  on 16-11-16.
 */

public class STGLRender {

    static
    {
        System.loadLibrary("beautysdk");
    }
    private final static String TAG = "STGLRender";
    private static final String CAMERA_INPUT_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	textureCoordinate = inputTextureCoordinate.xy;\n" +
            "	gl_Position = position;\n" +
            "}";

    private static final String CAMERA_INPUT_FRAGMENT_SHADER_OES = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    public static final String CAMERA_INPUT_FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";


//    private int mGLProgId,mGLAttribPosition,mGLUniformTexture,mGLAttribTextureCoordinate;//贴图
    private final static String PROGRAM_ID = "program";
    private final static String POSITION_COORDINATE = "position";
    private final static String TEXTURE_UNIFORM = "inputImageTexture";
    private final static String TEXTURE_COORDINATE = "inputTextureCoordinate";
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final FloatBuffer mGLSaveTextureBuffer;
    private FloatBuffer mTextureBuffer;
    private FloatBuffer mVertexBuffer;

    private boolean mIsInitialized;
    private ArrayList<HashMap<String, Integer>> mArrayPrograms = new ArrayList<HashMap<String, Integer>>(2) {
        {
            for (int i = 0; i < 2; ++i) {
                HashMap<String, Integer> hashMap = new HashMap<>();
                hashMap.put(PROGRAM_ID, 0);
                hashMap.put(POSITION_COORDINATE, -1);
                hashMap.put(TEXTURE_UNIFORM, -1);
                hashMap.put(TEXTURE_COORDINATE, -1);
                add(hashMap);
            }
        }
    };
    private int mViewPortWidth;
    private int mViewPortHeight;
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;


    public STGLRender() {
        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);

        mGLSaveTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLSaveTextureBuffer.put(TextureRotationUtil.getRotation(0, false, true)).position(0);
    }

    public void init(int width, int height) {
        if (mViewPortWidth == width && mViewPortHeight == height) {
            return ;
        }
        initProgram(CAMERA_INPUT_FRAGMENT_SHADER_OES, mArrayPrograms.get(0));
        initProgram(CAMERA_INPUT_FRAGMENT_SHADER, mArrayPrograms.get(1));
        InitPrograme();
        mViewPortWidth = width;
        mViewPortHeight = height;
        nativeInitWH(width, height);
        initFrameBuffers(width, height);
        mIsInitialized = true;
    }

    private void initProgram(String fragment, HashMap<String, Integer> programInfo) {
        int proID = programInfo.get(PROGRAM_ID);
        if (proID == 0) {
            proID = OpenGLUtils.loadProgram(CAMERA_INPUT_VERTEX_SHADER, fragment);
            programInfo.put(PROGRAM_ID, proID);
            programInfo.put(POSITION_COORDINATE, GLES20.glGetAttribLocation(proID, POSITION_COORDINATE));
            programInfo.put(TEXTURE_UNIFORM, GLES20.glGetUniformLocation(proID, TEXTURE_UNIFORM));
            programInfo.put(TEXTURE_COORDINATE, GLES20.glGetAttribLocation(proID, TEXTURE_COORDINATE));
        }
    }

    public void adjustTextureBuffer(int orientation, boolean flipVertical) {
        float[] textureCords = TextureRotationUtil.getRotation(orientation, true, flipVertical);
        LogUtils.d(TAG, "==========rotation: " + orientation + " flipVertical: " + flipVertical
                + " texturePos: " + Arrays.toString(textureCords));
        if (mTextureBuffer == null) {
            mTextureBuffer = ByteBuffer.allocateDirect(textureCords.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCords).position(0);
    }

    /**
     * 用来计算贴纸渲染的纹理最终需要的顶点坐标
     */
    public void calculateVertexBuffer(int displayW, int displayH, int imageW, int imageH) {
        int outputHeight = displayH;
        int outputWidth = displayW;

        float ratio1 = (float) outputWidth / imageW;
        float ratio2 = (float) outputHeight / imageH;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageW * ratioMax);
        int imageHeightNew = Math.round(imageH * ratioMax);

        float ratioWidth = imageWidthNew / (float) outputWidth;
        float ratioHeight = imageHeightNew / (float) outputHeight;

        float[] cube = new float[]{
                TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
                TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
                TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
                TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
        };

        if (mVertexBuffer == null) {
            mVertexBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }

    /**
     * 此函数有三个功能
     * 1. 将OES的纹理转换为标准的GL_TEXTURE_2D格式
     * 2. 将纹理宽高对换，即将wxh的纹理转换为了hxw的纹理，并且如果是前置摄像头，则需要有水平的翻转
     * 3. 读取上面两个步骤后纹理的内容到cpu内存，存储为RGBA格式的buffer
     * @param textureId 输入的OES的纹理id
     * @param buffer 输出的RGBA的buffer
     * @return 转换后的GL_TEXTURE_2D的纹理id
     */
    public int preProcess(int textureId, ByteBuffer buffer) {
        if (mFrameBuffers == null
                || !mIsInitialized)
            return -2;

        GLES20.glUseProgram(mArrayPrograms.get(0).get(PROGRAM_ID));
        GlUtil.checkGlError("glUseProgram");

        mGLCubeBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(0).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(0).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);

        if (textureId != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mArrayPrograms.get(0).get(TEXTURE_UNIFORM), 0);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GlUtil.checkGlError("glBindFramebuffer");
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (buffer != null) {
            GLES20.glReadPixels(0, 0, mViewPortWidth, mViewPortHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        }

        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUseProgram(0);

        return mFrameBufferTextures[0];
    }

    public void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(3, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(3, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    public void makeup(STPoint[] stPoint240, int texture_left_meimao, int texture_right_meimao, int texture_jiemao, int texture_yanxian, int texture_yanying, int texture_saihong,int texture_fendi,float _upmousecolors[], float _downmousecolors[],float jiemaobgcolors[],float meimaobgcolors[],float saihongbgcolors[],float yanyingbgcolors[],float yanxianbgcolors[],float fendibgcolors[])
    {
        if( stPoint240 != null) {
            nativeDrawLeftMeiMao(stPoint240, texture_left_meimao,meimaobgcolors);
            nativeDrawRightMeiMao(stPoint240,texture_right_meimao,meimaobgcolors);
//            float _mousecolors[] = {178/255f,18/255f,32/255f,0.6f};
            nativeDrawUPMouse(stPoint240, _downmousecolors);
            nativeDrawZuichun(stPoint240, _downmousecolors);
            nativeDrawRightJiemao(stPoint240,texture_jiemao,jiemaobgcolors);
            nativeDrawRightJiemao(stPoint240,texture_yanxian,yanxianbgcolors);
            nativeDrawRightJiemao(stPoint240,texture_yanying,yanyingbgcolors);
            nativeDrawSaiHong(stPoint240,texture_saihong,saihongbgcolors);
            nativeDrawFenDi(stPoint240,texture_fendi,fendibgcolors);
            GlUtil.checkGlError("test");
        }
        GLES20.glUseProgram(0);
        GLES20.glFinish();
    }

    /**
     * 双眼皮
     * @param stPoint240
     * @param texture_eyelidso
     * @param jiemaobgcolors
     */
    public void makeup(STPoint[] stPoint240, int texture_eyelidso,float jiemaobgcolors[])
    {
        if( stPoint240 != null) {
            nativeDrawRightJiemao(stPoint240,texture_eyelidso,null);
            GlUtil.checkGlError("test");
        }
        GLES20.glUseProgram(0);
        GLES20.glFinish();
    }

    public int onDrawFrame(STPoint[] points, int texid,float faceValue,float jawValue)
    {
         nativeChangeFaceAndJaw(points, texid,faceValue, jawValue);
        return OpenGLUtils.ON_DRAWN;
    }

    public int onDrawFrame(final int textureId) {

        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        GLES20.glUseProgram(mArrayPrograms.get(1).get(PROGRAM_ID));
        mVertexBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(1).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mGLTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(1).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);

        if (textureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mArrayPrograms.get(1).get(TEXTURE_UNIFORM), 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);


        return OpenGLUtils.ON_DRAWN;
    }

    public int saveTextureToFrameBuffer(int textureOutId, ByteBuffer buffer) {
        if(mFrameBuffers == null) {
            return OpenGLUtils.NO_TEXTURE;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);

        GLES20.glUseProgram(mArrayPrograms.get(1).get(PROGRAM_ID));

        if(!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        mGLCubeBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(1).get(POSITION_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);

        mGLSaveTextureBuffer.position(0);
        int glAttribTextureCoordinate = mArrayPrograms.get(1).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLSaveTextureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);

        if(textureOutId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureOutId);
            GLES20.glUniform1i(mArrayPrograms.get(1).get(TEXTURE_UNIFORM), 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if(buffer != null) {
            GLES20.glReadPixels(0, 0, mViewPortWidth, mViewPortHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        }

        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return mFrameBufferTextures[1];
    }

    private void initFrameBuffers(int width, int height) {
        destroyFrameBuffers();

        if (mFrameBuffers == null) {
            mFrameBuffers = new int[3];
            mFrameBufferTextures = new int[3];

            GLES20.glGenFramebuffers(3, mFrameBuffers, 0);
            GLES20.glGenTextures(3, mFrameBufferTextures, 0);
            bindFrameBuffer(mFrameBufferTextures[0], mFrameBuffers[0], width, height);
            bindFrameBuffer(mFrameBufferTextures[1], mFrameBuffers[1], width, height);
            bindFrameBuffer(mFrameBufferTextures[2], mFrameBuffers[2], width, height);

        }
    }

    public int genTexture()
    {
        int texid[] = new int[1];
        GLES20.glGenTextures(1, texid, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texid[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mViewPortWidth, mViewPortHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return texid[0];
    }

    public int bindFrameBuffer(){
        int[] textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mViewPortWidth, mViewPortHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[2]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,textureId[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mViewPortWidth, mViewPortHeight);
        return textureId[0];
    }

    public int bindFrameBuffer(int textureId){
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mViewPortWidth, mViewPortHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[2]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mFrameBuffers[2];
    }

    public int bindFrameByImaageBuffer(int textureId){
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mViewPortWidth, mViewPortHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mFrameBuffers[0];
    }



    public void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,textureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public final void destroy() {
        mIsInitialized = false;
        destroyFrameBuffers();
        GLES20.glDeleteProgram(mArrayPrograms.get(0).get(PROGRAM_ID));
        GLES20.glDeleteProgram(mArrayPrograms.get(1).get(PROGRAM_ID));
    }

    /**
     * 实例
     */
    public void InitPrograme()
    {
        nativeInitMousePrograme();
        GlUtil.checkGlError("test");
    }

    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mArrayPrograms.get(1).get(PROGRAM_ID));
//        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        cubeBuffer.position(0);
        int glAttribPosition = mArrayPrograms.get(0).get(POSITION_COORDINATE);
        int glAttribTextureCoordinate = mArrayPrograms.get(0).get(TEXTURE_COORDINATE);
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(glAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);
        if (textureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mArrayPrograms.get(1).get(TEXTURE_UNIFORM), 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(glAttribPosition);
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return OpenGLUtils.ON_DRAWN;
    }


    public int getFrameBufferId(){
        return  mFrameBuffers[2];
    };
    public int getFrameBufferByImageId(){
        return  mFrameBuffers[1];
    };

    public native void nativeDrawRightJiemao(STPoint[] points, int textureId, float bgcolors[]);
    public native void nativeDrawLeftMeiMao(STPoint[] points, int textureId, float bgcolors[]);
    public native void nativeDrawRightMeiMao(STPoint[] points, int textureId, float bgcolors[]);
    public native void nativeDrawSaiHong(STPoint[] points, int textureId, float bgcolors[]);
    public native void nativeDrawZuichun(STPoint[] stPoint240, float downmousecolors[]);
    public native void nativeDrawUPMouse(STPoint[] stPoint240, float downmousecolors[]);
    public native void nativeInitMousePrograme();
    public native void nativeInitWH(int w, int h);
    public native void nativeChangeFaceAndJaw(STPoint[] points, int texid, float scale, float jawsale);
    public native void nativeDrawFenDi(STPoint[] points, int textureId, float bgcolors[]);

}
