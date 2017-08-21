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

    private final String vertexShaderCode =
            "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_Position = position;\n" +
                    "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "}";
    private final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "varying highp vec2 textureCoordinate;\n" +//
                    " \n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    " \n" +
                    "void main()\n" +
                    "{ \n" +
                    "  vec4 texColor = texture2D(inputImageTexture, textureCoordinate);" +
//            "   texColor.r = 1.0f;" +
//            "   texColor.b = 1.0f;" +
//            "   texColor.g = 1.0f;" +
                    "if (texColor.a <0.01){" +
                    "discard;" +
                    "}" +
//                    "     gl_FragColor = vec4(0.698,0.07, 0.01, 0.6 );\n" +
                    "       gl_FragColor = texColor;\n" +
//            "     gl_FragColor = vec4(texColor.r,texColor.g,texColor.b,texColor.a);\n" +
//            "     gl_FragColor = vec4(texColor.rgb.textColor.a,texColor.a);\n" +
                    "}";
    //texture2D(inputImageTexture, textureCoordinate)
    private final String vertexShaderCode2 =
            "uniform mat4 projection;\n" +
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
    private final String fragmentShaderCode2 =
            "precision mediump float;\n" +
                    "uniform highp float color_selector;\n" +
                    "varying lowp vec4 DestinationColor;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
//                    "          gl_FragColor = vec4(0.698,0.07, 0.01, 0.3 );\n" +
                    "        gl_FragColor =  DestinationColor * DestinationColor.a;\n" +
                    "    \n" +
                    "}\n";
    private int mGLProgId,mGLAttribPosition,mGLUniformTexture,mGLAttribTextureCoordinate;//贴图
    private int mGLMouseId, mGLAttribMousePos,mGLUniformTexture2, mGLAttribMouseColor;//绘图
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

    public static native void nativeDrawZuichun(STPoint[] stPoint240, float downmousecolors[]);

    public void drawMeizhuang(STPoint[] stPoint240, int texture_left_meimao, int texture_right_meimao, int texture_jiemao, int texture_yanxian, int texture_yanying, int texture_saihong, float _upmousecolors[], float _downmousecolors[] )
    {
//        if( stPoint240 != null) {
//            drawLeftMeiMao(stPoint240, texture_left_meimao);
////            float _mousecolors[] = {178/255f,18/255f,32/255f,0.6f};
//
//            drawUPMouSe(stPoint240,_upmousecolors);
//            drawZuichun(stPoint240,_downmousecolors);
//            drawRightJiemao(stPoint240,texture_jiemao);
//            drawSaiHong(stPoint240,texture_saihong);
//            GlUtil.checkGlError("test");
//        }
        if( stPoint240 != null) {
            drawLeftMeiMao(stPoint240, texture_left_meimao);
            drawRightMeiMao(stPoint240,texture_right_meimao);
//            float _mousecolors[] = {178/255f,18/255f,32/255f,0.6f};
            drawUPMouSe(stPoint240,_upmousecolors);
            drawZuichun(stPoint240,_downmousecolors);
            nativeDrawZuichun(stPoint240, _downmousecolors);
            drawRightJiemao(stPoint240,texture_jiemao);
            drawRightJiemao(stPoint240,texture_yanxian);
            drawRightJiemao(stPoint240,texture_yanying);
            drawSaiHong(stPoint240,texture_saihong);
            GlUtil.checkGlError("test");
        }

        GLES20.glUseProgram(0);
        GLES20.glFinish();

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
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                mGLTextureBuffer);
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

    public int bindFrameBuffer(int textureId){
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mViewPortWidth, mViewPortHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[2]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mFrameBuffers[2];
    }


    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
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
        mGLProgId = OpenGLUtils.loadProgram(vertexShaderCode, fragmentShaderCode);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,"inputTextureCoordinate");
        mGLMouseId =OpenGLUtils.loadProgram(vertexShaderCode2, fragmentShaderCode2);
        mGLAttribMousePos = GLES20.glGetAttribLocation(mGLMouseId, "vPosition");
        mGLAttribMouseColor = GLES20.glGetAttribLocation(mGLMouseId,"SourceColor");
    }


    /**
     * 睫毛
     * @param points
     * @param textureId
     */
    public void drawRightJiemao(STPoint[] points, int textureId){
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
        x1 = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1+30);
        x2 = changeToGLPointT(x2);
        y2 = changeToGLPointR(y2+30);
        x3 = changeToGLPointT(x3);
        y3 = changeToGLPointR(y3+30);
        x4 = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4+30);
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
        GLES20.glUseProgram(mGLProgId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices1));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        //左上
        x0 =points[52].getX();
        y0 = points[52].getY();
        x = points[55].getX();
        y =  points[55].getY();
        x1 = (float) (x0 - (x - x0)*0.8);
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
        x1 = changeToGLPointT(x1);
        y1 = changeToGLPointR(y1+30);
        x2 = changeToGLPointT(x2);
        y2 = changeToGLPointR(y2+30);
        x3 = changeToGLPointT(x3);
        y3 = changeToGLPointR(y3+30);
        x4 = changeToGLPointT(x4);
        y4 = changeToGLPointR(y4+30);
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
        GLES20.glUseProgram(mGLProgId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices3));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20. glDisable( GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    public float changeToGLPointT(float x){
        float tempX = (x - mViewPortWidth/2) / (mViewPortWidth/2);
        return tempX;
    };
    public float changeToGLPointR(float y){
        float tempY = (y-mViewPortHeight/2) / (mViewPortHeight/2);
        return tempY;
    };

    /**
     * 下嘴唇
     * @param points
     */
    public  void drawZuichun(STPoint[] points, float mousecolors[]){
        //绘制下嘴唇 10个点 84,85,97,86,98,87,99,88,90,89
        float[] squareVertices = new float[200];
        float[] squareVertices2 = new float[400];
        List<STPoint> pointMouseList = new ArrayList<>();
        List<STPoint> pointDownList = new ArrayList<>();
//        float[] _mousecolors = new {};
        STPoint fitPoint;
        float p0,p1,p2,p3;
        pointMouseList.add(points[176]);
        for(int i =210;i<=224;i++) {
            pointMouseList.add(points[i]);
        }
        pointMouseList.add(points[192]);
        pointDownList.add(points[176]);
        for(int i=225;i<=239;i++) {
            pointDownList.add(points[i]);
        }
        pointDownList.add(points[192]);
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
            squareVertices2[i*8]   = mousecolors[0];
            squareVertices2[i*8+1] = mousecolors[1];
            squareVertices2[i*8+2] = mousecolors[2];
            squareVertices2[i*8+3] = mousecolors[3]/6.0f;
            squareVertices2[i*8+4] = mousecolors[0];
            squareVertices2[i*8+5] = mousecolors[1];
            squareVertices2[i*8+6] = mousecolors[2];
            if(i==0 || i == pointMouseList.size()-1)
                k0=5.0;
            else
                k0=1.0;
            squareVertices2[i*8+7] = (float) (mousecolors[3]/k0);
        }
        GLES20.glUseProgram(mGLMouseId);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
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
            p3 = changeToGLPointR(pointDownList.get(i).getY());
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
            squareVertices2[i*8+3] = (float)(mousecolors[3]/k0);
            squareVertices2[i*8+7] = mousecolors[3];
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
            p3 = changeToGLPointR(pointDownList.get(i).getY());
            squareVertices[i*4]   = (float) (p0/5.0 + p2*4.0/5.0);
            squareVertices[i*4+1] = (float) (p1/5.0 + p3*4.0/5.0);
            squareVertices[i*4+2] = (float) (p2*1.1-p0*0.1);
            squareVertices[i*4+3] = (float) (p3*1.1-p1*0.1);
        }
        for(int i = 0; i<pointMouseList.size(); i++) {
            squareVertices2[i*8+7] = 0.01f;
        }
        GLES20.glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribMousePos);
        GLES20.glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
        GLES20.glDisableVertexAttribArray(mGLAttribMousePos);
        GLES20.glDisableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDisable( GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
     * 上嘴唇
     * @param points
     */
    public void drawUPMouSe(STPoint[] points, float mousecolors[]) {
        //绘制上嘴唇 10个点 84,85,97,86,98,87,99,88,90,89
        float fitPoint;
        float p0, p1, p2, p3;
        //绘制下嘴唇 10个点 84,85,97,86,98,87,99,88,90,89
        float[] squareVertices = new float[200];
        float[] squareVertices2 = new float[400];
        List<STPoint> pointMouseList = new ArrayList<>();
        List<STPoint> pointDownList = new ArrayList<>();
        for (int i = 176; i <= 192; i++) {
            pointMouseList.add(points[i]);
        }
        for (int i = 193; i <= 209; i++) {
            pointDownList.add(points[i]);
        }
        //上段
        for (int i = 0; i <pointMouseList.size(); i++){
            p0 = changeToGLPointT(pointMouseList.get(i).getX());
            p1 =  changeToGLPointR(pointMouseList.get(i).getY());
            p2 = changeToGLPointT(pointDownList.get(i).getX());
            p3 =  changeToGLPointR(pointDownList.get(i).getY());
            squareVertices[ i * 4] = (float) (p0 * 1.1 - p2 * 0.1);
            squareVertices[ i * 4 + 1] = (float) (p1 * 1.1 - p3 * 0.1);
            squareVertices[ i* 4 + 2] = (float) (p0 * 4.0 / 5.0 + p2 / 5.0);
            squareVertices[ i * 4 + 3] = (float) (p1 * 4.0 / 5.0 + p3 / 5.0);
        }
        for (int i = 0; i <pointMouseList.size();
             i++){
            squareVertices2[i * 8] = mousecolors[0];;
            squareVertices2[i * 8 + 1] = mousecolors[1];
            squareVertices2[i * 8 + 2] = mousecolors[2];
            squareVertices2[i * 8 + 3] = 0.01f;
            squareVertices2[i * 8 + 4] = mousecolors[0];
            squareVertices2[i * 8 + 5] = mousecolors[1];
            squareVertices2[i * 8 + 6] = mousecolors[2];
            squareVertices2[i * 8 + 7] = mousecolors[3];
        }
        GLES20.glUseProgram(mGLMouseId);
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glVertexAttribPointer(mGLAttribMousePos, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribMousePos);
        GLES20.glVertexAttribPointer(mGLAttribMouseColor, 4, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
        //中段
        for (int i = 0; i <pointMouseList.size(); i++){
            p0 = changeToGLPointT(pointMouseList.get(i).getX());
            p1 =  changeToGLPointR(pointMouseList.get(i).getY());
            p2 = changeToGLPointT(pointDownList.get(i).getX());
            p3 =  changeToGLPointR(pointDownList.get(i).getY());
            squareVertices[i * 4] = (float) (p0 * 4.0 / 5.0 + p2 / 5.0);
            squareVertices[i * 4 + 1] = (float) (p1 * 4.0 / 5.0 + p3 / 5.0);
            squareVertices[i * 4 + 2] = (float) (p0 / 5.0 + p2 * 4.0 / 5.0);
            squareVertices[i * 4 + 3] = (float) (p1 / 5.0 + p3 * 4.0 / 5.0);
        }
        for (int i = 0; i <pointMouseList.size();
             i++){
            squareVertices2[i * 8 + 3] = mousecolors[3];
            squareVertices2[i * 8 + 7] = mousecolors[3];
        }
        //下段
        GLES20.glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribMousePos);
        GLES20.glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0,  Utils.getFloatBuffer(squareVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
        //下段
        for (int i = 0; i <pointMouseList.size();
             i++){
            p0 = changeToGLPointT(pointMouseList.get(i).getX());
            p1 =  changeToGLPointR(pointMouseList.get(i).getY());
            p2 = changeToGLPointT(pointDownList.get(i).getX());
            p3 =  changeToGLPointR(pointDownList.get(i).getY());
            squareVertices[i * 4] = (float) (p0 / 5.0 + p2 * 4.0 / 5.0);
            squareVertices[i * 4 + 1] = (float) (p1 / 5.0 + p3 * 4.0 / 5.0);
            squareVertices[i * 4 + 2] = (float) (p2 * 1.1 - p0 * 0.1);
            squareVertices[i * 4 + 3] = (float) (p3 * 1.1 - p1 * 0.1);
        }
        for (int i = 0; i <pointMouseList.size();
             i++){
            squareVertices2[i * 8 + 3] = mousecolors[3];
            squareVertices2[i * 8 + 7] = (float) (mousecolors[3] / 6.0);
        }
        GLES20.glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribMousePos);
        GLES20.glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribMouseColor);
        GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
        GLES20.glDisableVertexAttribArray(mGLAttribMousePos);
        GLES20.glDisableVertexAttribArray(mGLAttribMouseColor);
        GLES20. glDisable( GLES20.GL_BLEND);
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
     * 眉毛
     * @param points
     * @param textureId
     */
    public void drawLeftMeiMao(STPoint[] points, int textureId){
        float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4,k;
        double d,theta;
        x0 = points[150].getX();
        y0 = points[150].getY();
        x = points[162].getX();
        y =  points[162].getY();
        x1 = (float) (x0 - (x - x0)*0.2);
        y1 = (float) (y0 - (y - y0)*0.2);
        x2 = (float) (x + (x - x0)*0.124);
        y2 = (float) (y + (y - y0)*0.124);
        x0 = points[156].getX();
        y0 =  points[156].getY();
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
        y1  = changeToGLPointR(y1);
        x2  = changeToGLPointT(x2);
        y2  = changeToGLPointR(y2);
        x3  = changeToGLPointT(x3);
        y3  = changeToGLPointR(y3);
        x4  = changeToGLPointT(x4);
        y4  = changeToGLPointR(y4);
        float squareVertices[] = {
                x1,y1,
                x2,y2,
                x3,y3,
                x4,y4,
        };
        float textureVertices2[] = {
                1.0f, 1.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,



        };
        GLES20.glUseProgram(mGLProgId);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_CONSTANT_COLOR,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20. glDisable( GLES20.GL_BLEND);

    }
    /**
     * 眉毛
     * @param points
     * @param textureId
     */
    public void drawRightMeiMao(STPoint[] points, int textureId){
        float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4,k;
        double d,theta;
        x0 = points[175].getX();
        y0 = points[175].getY();
        x = points[163].getX();
        y =  points[163].getY();
        x1 = (float) (x0 - (x - x0)*0.124);
        y1 = (float) (y0 - (y - y0)*0.124);
        x2 = (float) (x + (x - x0)*0.2);
        y2 = (float) (y + (y - y0)*0.2);
        x0 = points[169].getX();
        y0 =  points[169].getY();
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
        y1  = changeToGLPointR(y1);
        x2  = changeToGLPointT(x2);
        y2  = changeToGLPointR(y2);
        x3  = changeToGLPointT(x3);
        y3  = changeToGLPointR(y3);
        x4  = changeToGLPointT(x4);
        y4  = changeToGLPointR(y4);
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
        GLES20.glUseProgram(mGLProgId);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_CONSTANT_COLOR,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(squareVertices));
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, Utils.getFloatBuffer(textureVertices2));
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20. glDisable( GLES20.GL_BLEND);

    }
    /**
     * 腮红
     * @param points
     * @param textureId
     */
    public void drawSaiHong(STPoint[] points, int textureId){
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
        GLES20. glDisable( GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }


    public int getFrameBufferId(){
        return  mFrameBuffers[0];
    };

}
