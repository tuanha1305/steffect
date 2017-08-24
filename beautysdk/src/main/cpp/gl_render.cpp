#include <GLES2/gl2.h>
#include "math.h"
#include "gl_render.h"
#include "string.h"
#include "esUtil.h"
#include "vector"

#define SHADER_STRING(...) #__VA_ARGS__
const char* const g_mouseVerShader = SHADER_STRING
(
        uniform mat4 projection;
        uniform mat4 modelView;
        attribute vec4 vPosition;
        attribute vec4 SourceColor;
        varying vec4 DestinationColor;
        void main(void)
        {
            DestinationColor = SourceColor;
            gl_Position =  vPosition;
        }
);

const char* const g_mouseFraShader = SHADER_STRING
(
        precision mediump float;
        uniform highp float color_selector;
        varying lowp vec4 DestinationColor;
        void main()
        {
            gl_FragColor =  DestinationColor * DestinationColor.a;
        }
);

const char* const vertexShaderCode =SHADER_STRING(
        attribute vec4 position;
        attribute vec4 inputTextureCoordinate;
        varying vec2 textureCoordinate;
        void main()
        {
            gl_Position = position;
            textureCoordinate = inputTextureCoordinate.xy;
        }
);

const char* const fragmentShaderCode =SHADER_STRING(
        precision mediump float;
        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        void main()
        {
            vec4 texColor = texture2D(inputImageTexture, textureCoordinate);
            if(texColor.a <0.01){
                discard;
            }
            gl_FragColor = texColor;
        }
);

GLint mGLProgId,mGLAttribPosition,mGLUniformTexture,mGLAttribTextureCoordinate;
GLint mGLMouseId, mGLAttribMousePos,mGLUniformTexture2, mGLAttribMouseColor;
int mViewPortWidth;
int mViewPortHeight;

float changeToGLPointT(float x){
    float tempX = (float)(x - mViewPortWidth/2) / (mViewPortWidth/2);
    return tempX;
};
float changeToGLPointR(float y){
    float tempY = (float)(y-mViewPortHeight/2) / (mViewPortHeight/2);
    return tempY;
};

float changeToGLPointR2(float y){
    float tempY = (float)(mViewPortHeight/2-y) / (mViewPortHeight/2);
    return tempY;
};

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeInitWH(JNIEnv* env, jobject obj, jint w, int h)
{
    mViewPortWidth = w;
    mViewPortHeight = h;
}

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeInitMousePrograme(JNIEnv* env, jobject obj)
{
    mGLMouseId = esLoadProgram(g_mouseVerShader, g_mouseFraShader);
    mGLAttribMousePos = glGetAttribLocation(mGLMouseId, "vPosition");
    mGLAttribMouseColor = glGetAttribLocation(mGLMouseId,"SourceColor");
    mGLProgId = esLoadProgram(vertexShaderCode, fragmentShaderCode);
    mGLAttribPosition = glGetAttribLocation(mGLProgId, "position");
    mGLUniformTexture = glGetUniformLocation(mGLProgId, "inputImageTexture");
    mGLAttribTextureCoordinate = glGetAttribLocation(mGLProgId,"inputTextureCoordinate");
}


JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawZuichun(JNIEnv* env, jobject obj, jobjectArray stPoint, jfloatArray downmousecolors)
{
    int objlen = env->GetArrayLength(stPoint);
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");

    float squareVertices[200];
    float squareVertices2[400];
    std::vector<int> pointMouseList;
    std::vector<int> pointDownList;
    float p0,p1,p2,p3;
    pointMouseList.push_back(176);
    for(int i =210;i<=224;i++) {
        pointMouseList.push_back(i);
    }
    pointMouseList.push_back(192);

    pointDownList.push_back(176);
    for(int i=225;i<=239;i++) {
        pointDownList.push_back(i);
    }
    pointDownList.push_back(192);
    //上段
    for(int i = 0; i<pointMouseList.size(); i++) {
        p0 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_x));
        p1 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_y));
        p2 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_x));
        p3 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_y));
        squareVertices[i*4]   = (float) (p0*1.1-p2*0.1);
        squareVertices[i*4+1] = (float) (p1*1.1-p3*0.1);
        squareVertices[i*4+2] = (float) (p0*4.0/5.0 + p2/5.0);
        squareVertices[i*4+3] = (float) (p1*4.0/5.0 + p3/5.0);
    }
    double k0=1.0;
    float mousecolors[4];
    memset(mousecolors, 0, sizeof(float) * 4);
    env->GetFloatArrayRegion(downmousecolors, 0, 4, mousecolors);
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
    glUseProgram(mGLMouseId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribMousePos);
    glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0, squareVertices2);
    glEnableVertexAttribArray(mGLAttribMouseColor);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);

    //中段
    for(int i  = 0; i<pointMouseList.size(); i++) {
        p0 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_x));
        p1 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_y));
        p2 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_x));
        p3 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_y));
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
    glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribMousePos);
    glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0,  squareVertices2);
    glEnableVertexAttribArray(mGLAttribMouseColor);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
    for(int i = 0; i<pointMouseList.size(); i++) {
        p0 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_x));
        p1 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_y));
        p2 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_x));
        p3 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_y));
        squareVertices[i*4]   = (float) (p0/5.0 + p2*4.0/5.0);
        squareVertices[i*4+1] = (float) (p1/5.0 + p3*4.0/5.0);
        squareVertices[i*4+2] = (float) (p2*1.1-p0*0.1);
        squareVertices[i*4+3] = (float) (p3*1.1-p1*0.1);
    }
    for(int i = 0; i<pointMouseList.size(); i++) {
        squareVertices2[i*8+7] = 0.01f;
    }
    glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribMousePos);
    glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0, squareVertices2);
    glEnableVertexAttribArray(mGLAttribMouseColor);
    glDrawArrays( GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
    glDisableVertexAttribArray(mGLAttribMousePos);
    glDisableVertexAttribArray(mGLAttribMouseColor);
    glDisable( GL_BLEND);
    glEnable(GL_DEPTH_TEST);

    ///////////
}

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawUPMouse(JNIEnv* env, jobject obj, jobjectArray stPoint, jfloatArray downmousecolors)
{
    int objlen = env->GetArrayLength(stPoint);
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
    float mousecolors[4];
    memset(mousecolors, 0, sizeof(float) * 4);
    env->GetFloatArrayRegion(downmousecolors, 0, 4, mousecolors);

    float squareVertices[200];
    float squareVertices2[400];
    std::vector<int> pointMouseList;
    std::vector<int> pointDownList;
    float p0,p1,p2,p3;
    for (int i = 176; i <= 192; i++) {
        pointMouseList.push_back(i);
    }
    for (int i = 193; i <= 209; i++) {
        pointDownList.push_back(i);
    }
    //上段
    for (int i = 0; i <pointMouseList.size(); i++){
        p0 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_x));
        p1 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_y));
        p2 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_x));
        p3 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_y));
        squareVertices[ i * 4] = (float) (p0 * 1.1 - p2 * 0.1);
        squareVertices[ i * 4 + 1] = (float) (p1 * 1.1 - p3 * 0.1);
        squareVertices[ i* 4 + 2] = (float) (p0 * 4.0 / 5.0 + p2 / 5.0);
        squareVertices[ i * 4 + 3] = (float) (p1 * 4.0 / 5.0 + p3 / 5.0);
    }
    for (int i = 0; i <pointMouseList.size(); i++){
        squareVertices2[i * 8] = mousecolors[0];;
        squareVertices2[i * 8 + 1] = mousecolors[1];
        squareVertices2[i * 8 + 2] = mousecolors[2];
        squareVertices2[i * 8 + 3] = 0.01f;
        squareVertices2[i * 8 + 4] = mousecolors[0];
        squareVertices2[i * 8 + 5] = mousecolors[1];
        squareVertices2[i * 8 + 6] = mousecolors[2];
        squareVertices2[i * 8 + 7] = mousecolors[3];
    }
    glUseProgram(mGLMouseId);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribMousePos);
    glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0, squareVertices2);
    glEnableVertexAttribArray(mGLAttribMouseColor);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);

    //中段
    for (int i = 0; i <pointMouseList.size(); i++){
        p0 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_x));
        p1 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_y));
        p2 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_x));
        p3 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_y));
        squareVertices[i * 4] = (float) (p0 * 4.0 / 5.0 + p2 / 5.0);
        squareVertices[i * 4 + 1] = (float) (p1 * 4.0 / 5.0 + p3 / 5.0);
        squareVertices[i * 4 + 2] = (float) (p0 / 5.0 + p2 * 4.0 / 5.0);
        squareVertices[i * 4 + 3] = (float) (p1 / 5.0 + p3 * 4.0 / 5.0);
    }
    for (int i = 0; i <pointMouseList.size(); i++){
        squareVertices2[i * 8 + 3] = mousecolors[3];
        squareVertices2[i * 8 + 7] = mousecolors[3];
    }
    //下段
    glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribMousePos);
    glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0,  squareVertices2);
    glEnableVertexAttribArray(mGLAttribMouseColor);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
    //下段
    for (int i = 0; i <pointMouseList.size(); i++){
        p0 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_x));
        p1 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointMouseList[i]),id_y));
        p2 = changeToGLPointT(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_x));
        p3 =  changeToGLPointR(env->GetFloatField(env->GetObjectArrayElement(stPoint, pointDownList[i]),id_y));
        squareVertices[i * 4] = (float) (p0 / 5.0 + p2 * 4.0 / 5.0);
        squareVertices[i * 4 + 1] = (float) (p1 / 5.0 + p3 * 4.0 / 5.0);
        squareVertices[i * 4 + 2] = (float) (p2 * 1.1 - p0 * 0.1);
        squareVertices[i * 4 + 3] = (float) (p3 * 1.1 - p1 * 0.1);
    }
    for (int i = 0; i <pointMouseList.size(); i++){
        squareVertices2[i * 8 + 3] = mousecolors[3];
        squareVertices2[i * 8 + 7] = (float) (mousecolors[3] / 6.0);
    }
    glVertexAttribPointer(mGLAttribMousePos, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribMousePos);
    glVertexAttribPointer(mGLAttribMouseColor, 4, GL_FLOAT, false, 0, squareVertices2);
    glEnableVertexAttribArray(mGLAttribMouseColor);
    glDrawArrays( GL_TRIANGLE_STRIP, 0, pointMouseList.size()*2);
    glDisableVertexAttribArray(mGLAttribMousePos);
    glDisableVertexAttribArray(mGLAttribMouseColor);
    glDisable( GL_BLEND);
    return;
}

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawRightJiemao(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId ,jfloatArray bgcolors)
{
    int objlen = env->GetArrayLength(stPoint);
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
    float x0,y0,x,y,x1,y1,x2,y2,x3,y3,x4,y4,k,d,b;
    double theta,theta0;
    //画睫毛/眼影/眼线
    jobject jobj = env->GetObjectArrayElement(stPoint, 58);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);
    //右上
     jobj = env->GetObjectArrayElement(stPoint, 61);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    x1 = x - (x0 - x)*0.2;
    y1 = y - (y0 - y)*0.2;

    x2 = x0 + (x0 - x)*0.8;
    y2 = y0 + (y0 - y)*0.8;
    jobj = env->GetObjectArrayElement(stPoint, 75);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    k = (y2-y1)/(x2-x1);

    theta = atanf(k);

    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*2.1;

    x3 = x1 + d * sinf(theta);
    y3 = y1 - d * cosf(theta);

    x4 = x2 + d * sinf(theta);
    y4 = y2 - d * cosf(theta);

    x1 = changeToGLPointT(x1);
    y1 = changeToGLPointR(y1);
    x2 = changeToGLPointT(x2);
    y2 = changeToGLPointR(y2);
    x3 = changeToGLPointT(x3);
    y3 = changeToGLPointR(y3);
    x4 = changeToGLPointT(x4);
    y4 = changeToGLPointR(y4);
    GLfloat squareVertices[] = {
            x3,y3,
            x4,y4,
            x1,y1,
            x2,y2,


    };

    static const GLfloat textureVertices1[] = {
            0.0f,  0.0f,
            1.0f,  0.0f,
            0.0f, 0.668f,
            1.0f, 0.668f,
    };
    glUseProgram(mGLProgId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendColor(1.0f, 0.0f,0.0f, 1.0f);
    glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices1);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    //右下
    jobj = env->GetObjectArrayElement(stPoint, 58);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);
    jobj = env->GetObjectArrayElement(stPoint, 61);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    x3 = x - (x0 - x)*0.2;
    y3 = y - (y0 - y)*0.2;

    x4 = x0 + (x0 - x)*0.8;
    y4 = y0 + (y0 - y)*0.8;

    jobj = env->GetObjectArrayElement(stPoint, 76);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    k = (y4-y3)/(x4-x3);
    theta = atanf(k);

    d = fabsf(k*x0-y0+y3-k*x3)/sqrtf(k*k+1)*2.2;//106
    //    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*2.1;//240

    x1 = x3 - d * sinf(theta);
    y1 = y3 + d * cosf(theta);

    x2 = x4 - d * sinf(theta);
    y2 = y4 + d * cosf(theta);

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

    static const GLfloat textureVertices2[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f,  0.668f,
            1.0f,  0.668f,
    };

    glUseProgram(mGLProgId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendColor(1.0f, 0.0f,0.0f, 1.0f);
    glBlendFunc(GL_CONSTANT_COLOR, GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices2);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    //左上
    jobj = env->GetObjectArrayElement(stPoint, 52);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    jobj = env->GetObjectArrayElement(stPoint, 55);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);


    x1 = x0 - (x - x0)*0.8;
    y1 = y0 - (y - y0)*0.8;

    x2 = x + (x - x0)*0.2;
    y2 = y + (y - y0)*0.2;
    jobj = env->GetObjectArrayElement(stPoint, 72);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    k = (y2-y1)/(x2-x1);

    theta = atanf(k);

    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*2.1;

    x3 = x1 + d * sinf(theta);
    y3 = y1 - d * cosf(theta);

    x4 = x2 + d * sinf(theta);
    y4 = y2 - d * cosf(theta);

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

    static const GLfloat textureVertices3[] = {
            1.0f,  0.668f,
            0.0f, 0.668f,
            1.0f,  0.0f,
            0.0f,  0.0f,
    };


    glUseProgram(mGLProgId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices3);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);


    //左下
    jobj = env->GetObjectArrayElement(stPoint, 52);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    jobj = env->GetObjectArrayElement(stPoint, 55);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);

    x3 = x0 - (x - x0)*0.8;
    y3 = y0 - (y - y0)*0.8;

    x4 = x + (x - x0)*0.2;
    y4 = y + (y - y0)*0.2;

    jobj = env->GetObjectArrayElement(stPoint, 73);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    k = (y4-y3)/(x4-x3);
    theta = atanf(k);

    d = fabsf(k*x0-y0+y3-k*x3)/sqrtf(k*k+1)*2.2;
    //    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*2.1;

    x1 = x3 - d * sinf(theta);
    y1 = y3 + d * cosf(theta);

    x2 = x4 - d * sinf(theta);
    y2 = y4 + d * cosf(theta);

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

    static const GLfloat textureVertices4[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f,  0.668f,
            0.0f,  0.668f,

    };

    glUseProgram(mGLProgId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices4);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisable( GL_BLEND);
    glEnable(GL_DEPTH_TEST);
    glDisable(GL_BLEND);
    return;
}

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawLeftMeiMao(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId,jfloatArray bgcolors )
{
    int objlen = env->GetArrayLength(stPoint);
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
    float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4,k;
    double d,theta;
    jobject jobj = env->GetObjectArrayElement(stPoint, 150);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    jobj = env->GetObjectArrayElement(stPoint, 162);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);
    x1 = (float) (x0 - (x - x0)*0.2);
    y1 = (float) (y0 - (y - y0)*0.2);
    x2 = (float) (x + (x - x0)*0.124);
    y2 = (float) (y + (y - y0)*0.124);
    jobj = env->GetObjectArrayElement(stPoint, 156);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    k = (y2-y1)/(x2-x1);
    theta = atan(k);
    d = abs(k*x0-y0+y1-k*x1)/sqrt(k*k+1)*2.232;
    x3 = (float) (x1 + d * sin(theta));
    y3 = (float) (y1 - d * cos(theta));
    x4 = (float) (x2 + d * sin(theta));
    y4 = (float) (y2 - d * cos(theta));
    x1 = (float) (x1 - d * sin(theta));
    y1 = (float) (y1 + d * cos(theta));
    x2 = (float) (x2 - d * sin(theta));
    y2 = (float) (y2 + d * cos(theta));
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
    glUseProgram(mGLProgId);
    glEnable(GL_BLEND);
    glBlendFunc(GL_CONSTANT_COLOR,GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices2);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
     glDisable( GL_BLEND);
    return;
}

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawRightMeiMao(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId ,jfloatArray bgcolors)
{
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
    float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4,k;
    double d,theta;
    jobject jobj = env->GetObjectArrayElement(stPoint, 175);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    jobj = env->GetObjectArrayElement(stPoint, 163);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);

    x1 = (float) (x0 - (x - x0)*0.124);
    y1 = (float) (y0 - (y - y0)*0.124);
    x2 = (float) (x + (x - x0)*0.2);
    y2 = (float) (y + (y - y0)*0.2);
    jobj = env->GetObjectArrayElement(stPoint, 169);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    k = (y2-y1)/(x2-x1);
    theta = atan(k);
    d = abs(k*x0-y0+y1-k*x1)/sqrt(k*k+1)*2.232;
    x3 = (float) (x1 + d * sin(theta));
    y3 = (float) (y1 - d * cos(theta));
    x4 = (float) (x2 + d * sin(theta));
    y4 = (float) (y2 - d * cos(theta));
    x1 = (float) (x1 - d * sin(theta));
    y1 = (float) (y1 + d * cos(theta));
    x2 = (float) (x2 - d * sin(theta));
    y2 = (float) (y2 + d * cos(theta));
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
    glUseProgram(mGLProgId);
    glEnable(GL_BLEND);
    glBlendFunc(GL_CONSTANT_COLOR,GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices2);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray(mGLAttribPosition);
    glDisableVertexAttribArray(mGLAttribTextureCoordinate);
     glDisable( GL_BLEND);
}

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawSaiHong(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId,jfloatArray bgcolors )
{
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
    float x,y,x0,y0,x1,y1,x2,y2,x3,y3,x4,y4;
    double theta,d,k;
    //画腮红
    jobject jobj = env->GetObjectArrayElement(stPoint, 82);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    jobj = env->GetObjectArrayElement(stPoint, 83);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);

    x1 = (float) (x0 - (x - x0)*1.463);
    y1 = (float) (y0 - (y - y0)*1.463);
    x2 = (float) (x + (x - x0)*1.463);
    y2 = (float) (y + (y - y0)*1.463);
    jobj = env->GetObjectArrayElement(stPoint, 45);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    k = (y2-y1)/(x2-x1);
    theta = atan(k);
    d = abs(k*x0-y0+y1-k*x1)/sqrt(k*k+1)*2;
//    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*1.5;
    x3 =(float)(x1 + d * sin(theta));
    y3 =(float)(y1 - d * cos(theta));
    x4 = (float)(x2 + d * sin(theta));
    y4 = (float)(y2 - d * cos(theta));
    d = abs(k*x0-y0+y1-k*x1)/sqrt(k*k+1)*1.456;
    x1 = (float)(x1 - d * sin(theta));
    y1 = (float)(y1 + d * cos(theta));
    x2 = (float)(x2 - d * sin(theta));
    y2 = (float)(y2 + d * cos(theta));
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
    glUseProgram(mGLProgId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices1);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisable( GL_BLEND);
    return;
}
