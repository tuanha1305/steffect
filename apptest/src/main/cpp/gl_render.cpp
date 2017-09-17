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
            gl_FragColor =  vec4(DestinationColor.rgb, DestinationColor.a * 0.2);
        }
);

const char *const ChangeFaceSizeVsh = SHADER_STRING
(
        attribute vec4 position;
        attribute vec4 inputTextureCoordinate;
        varying highp vec2 textureCoordinate;

        void main(void)
        {
            gl_Position =  position;
            textureCoordinate = inputTextureCoordinate.xy;
        }


);

const char *const ChangeFaceSizeFsh = SHADER_STRING
(
// 瘦脸
        precision highp float;

        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;

        uniform highp float radius;

        uniform highp float aspectRatio;

        uniform float leftContourPoints[9*2];
        uniform float rightContourPoints[9*2];
        uniform float deltaArray[9];
        uniform int arraySize;

        highp vec2 warpPositionToUse(vec2 currentPoint, vec2 contourPointA,  vec2 contourPointB, float radius, float delta, float aspectRatio)
{
    vec2 positionToUse = currentPoint;

//    vec2 currentPointToUse = vec2(currentPoint.x, currentPoint.y * aspectRatio + 0.5 - 0.5 * aspectRatio);
//    vec2 contourPointAToUse = vec2(contourPointA.x, contourPointA.y * aspectRatio + 0.5 - 0.5 * aspectRatio);

    vec2 currentPointToUse = currentPoint;
    vec2 contourPointAToUse = contourPointA;

    float r = distance(currentPointToUse, contourPointAToUse);
    if(r < radius)
    {
        vec2 dir = normalize(contourPointB - contourPointA);
        float dist = radius * radius - r * r;
        float alpha = dist / (dist + (r-delta) * (r-delta));
        alpha = alpha * alpha;

        positionToUse = positionToUse - alpha * delta * dir;

    }

    return positionToUse;
}

        void main()
        {
            vec2 positionToUse = textureCoordinate;

            for(int i = 0; i < 9; i++)
            {
                positionToUse = warpPositionToUse(positionToUse, vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), radius, deltaArray[i], aspectRatio);
                positionToUse = warpPositionToUse(positionToUse, vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), radius, deltaArray[i], aspectRatio);
            }


            gl_FragColor = texture2D(inputImageTexture, positionToUse);

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


const char *const ChangeFaceAndJawV = SHADER_STRING
(
//贴图渲染

        attribute vec4 position;
        attribute vec4 inputTextureCoordinate;
        varying highp vec2 textureCoordinate;

        void main(void)
        {
            gl_Position =  position;
            textureCoordinate = inputTextureCoordinate.xy;
            //    textureCoordinate = vec2(inputTextureCoordinate.x*2.0-1.0, inputTextureCoordinate.y*2.0-1.0);
        }


);

const char *const ChangeFaceAndJawF = SHADER_STRING
(
// 瘦脸
        precision highp float;

        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;

        uniform highp float radius;

        uniform highp float aspectRatio;

        uniform float leftContourPoints[9*2];
        uniform float rightContourPoints[9*2];
        uniform float deltaFaceArray[9];

        uniform float jawContourPoints[7*2];
        uniform float jawDownPoints[7*2];
        uniform float deltaJawArray[7];

        highp vec2 warpPositionToUse(vec2 currentPoint, vec2 contourPointA, vec2 contourPointB,float radius, float delta, float aspectRatio)
{
    vec2 positionToUse = currentPoint;

    //    vec2 currentPointToUse = vec2(currentPoint.x, currentPoint.y * aspectRatio + 0.5 - 0.5 * aspectRatio);
    //    vec2 contourPointAToUse = vec2(contourPointA.x, contourPointA.y * aspectRatio + 0.5 - 0.5 * aspectRatio);

    vec2 currentPointToUse = currentPoint;
    vec2 contourPointAToUse = contourPointA;

    float r = distance(currentPointToUse, contourPointAToUse);
    if(r < radius)
    {
        vec2 dir = normalize(contourPointB - contourPointA);
        float dist = radius * radius - r * r;
        float alpha = dist / (dist + (r-delta) * (r-delta));
        alpha = alpha * alpha;

        positionToUse = positionToUse - alpha * delta * dir;
    }

    return positionToUse;
}

        void main()
        {
            vec2 positionToUse = textureCoordinate;

            for(int i = 0; i < 9; i++)
            {
                positionToUse = warpPositionToUse(positionToUse, vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), radius, deltaFaceArray[i], aspectRatio);
                positionToUse = warpPositionToUse(positionToUse, vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), radius, deltaFaceArray[i], aspectRatio);
            }

            for(int i = 0; i < 7; i++)
            {
                positionToUse = warpPositionToUse(positionToUse,
                                                  vec2(jawContourPoints[i * 2], jawContourPoints[i * 2 + 1]),
                                                  vec2(jawDownPoints[i * 2], jawDownPoints[i * 2 + 1]),
                                                  radius, deltaJawArray[i], aspectRatio);
            }

            gl_FragColor = texture2D(inputImageTexture, positionToUse);

        }

);

GLint mGLProgId,mGLAttribPosition,mGLUniformTexture,mGLAttribTextureCoordinate;
GLint mGLMouseId, mGLAttribMousePos,mGLUniformTexture2, mGLAttribMouseColor;
int mViewPortWidth;
int mViewPortHeight;

GLuint _faceandjawProgram;

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

    _faceandjawProgram = esLoadProgram(ChangeFaceAndJawV, ChangeFaceAndJawF);

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
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
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
    float r,g,b,a;
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

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeChangeFaceAndJaw(JNIEnv* env, jobject obj, jobjectArray stPoint, int texture,  float scale, float jawsale)
{
    GLuint resultTexture = 0;

    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");

    GLfloat x,y,x0,y0,radius,theta0;

    static const int iFaceArrSize = 9;
    static const int iJawArrSize = 7;

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, texture);
    glUseProgram(_faceandjawProgram);
//    glVertexAttribPointer(0, 2, GL_FLOAT, 0, 0, squareVertices);
//    glEnableVertexAttribArray(0);
//    glVertexAttribPointer(1, 2, GL_FLOAT, 0, 0, textureVertices);
//    glEnableVertexAttribArray(1);

    // 左脸控制点

    jobject jobj = env->GetObjectArrayElement(stPoint, 6);
    float x6 = env->GetFloatField(jobj, id_x);
    float y6 = env->GetFloatField(jobj, id_y);
    jobj = env->GetObjectArrayElement(stPoint, 7);
    float x7 = env->GetFloatField(jobj, id_x);
    float y7 = env->GetFloatField(jobj, id_y);
    jobj = env->GetObjectArrayElement(stPoint, 8);
    float x8 = env->GetFloatField(jobj, id_x);
    float y8 = env->GetFloatField(jobj, id_y);
    jobj = env->GetObjectArrayElement(stPoint, 9);
    float x9 = env->GetFloatField(jobj, id_x);
    float y9 = env->GetFloatField(jobj, id_y);
    jobj = env->GetObjectArrayElement(stPoint, 10);
    float x10 = env->GetFloatField(jobj, id_x);
    float y10 = env->GetFloatField(jobj, id_y);
    jobj = env->GetObjectArrayElement(stPoint, 11);
    float x11 = env->GetFloatField(jobj, id_x);
    float y11 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 12);
    float x12 = env->GetFloatField(jobj, id_x);
    float y12 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 13);
    float x13 = env->GetFloatField(jobj, id_x);
    float y13 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 14);
    float x14 = env->GetFloatField(jobj, id_x);
    float y14 = env->GetFloatField(jobj, id_y);

    float arrleft[iFaceArrSize*2] = {
            face.points_array[6].x/mViewPortWidth,face.points_array[6].y/mViewPortHeight,
            face.points_array[7].x/mViewPortWidth,face.points_array[7].y/mViewPortHeight,
            face.points_array[8].x/mViewPortWidth,face.points_array[8].y/mViewPortHeight,
            face.points_array[9].x/mViewPortWidth,face.points_array[9].y/mViewPortHeight,
            face.points_array[10].x/mViewPortWidth,face.points_array[10].y/mViewPortHeight,
            face.points_array[11].x/mViewPortWidth,face.points_array[11].y/mViewPortHeight,
            face.points_array[12].x/mViewPortWidth,face.points_array[12].y/mViewPortHeight,
            face.points_array[13].x/mViewPortWidth,face.points_array[13].y/mViewPortHeight,
            face.points_array[14].x/mViewPortWidth,face.points_array[14].y/mViewPortHeight,

    };

    GLuint leftFaceCenterPosition = glGetUniformLocation(_faceandjawProgram, "leftContourPoints");
    glUniform1fv(leftFaceCenterPosition, iFaceArrSize*2, arrleft);

    // 右脸控制点
    float arrright[iFaceArrSize*2] = {
            face.points_array[26].x/mViewPortWidth,face.points_array[26].y/mViewPortHeight,
            face.points_array[25].x/mViewPortWidth,face.points_array[25].y/mViewPortHeight,
            face.points_array[24].x/mViewPortWidth,face.points_array[24].y/mViewPortHeight,
            face.points_array[23].x/mViewPortWidth,face.points_array[23].y/mViewPortHeight,
            face.points_array[22].x/mViewPortWidth,face.points_array[22].y/mViewPortHeight,
            face.points_array[21].x/mViewPortWidth,face.points_array[21].y/mViewPortHeight,
            face.points_array[20].x/mViewPortWidth,face.points_array[20].y/mViewPortHeight,
            face.points_array[19].x/mViewPortWidth,face.points_array[19].y/mViewPortHeight,
            face.points_array[18].x/mViewPortWidth,face.points_array[18].y/mViewPortHeight,
    };

    static GLuint GrightFaceCenterPosition = glGetUniformLocation(_faceandjawProgram, "rightContourPoints");
    glUniform1fv(GrightFaceCenterPosition, iFaceArrSize*2, arrright);

    float arrdeltaface[9] = {
            static_cast<float>(ffacescale*0.0045), static_cast<float>(ffacescale*0.006), static_cast<float>(ffacescale*0.009), static_cast<float>(0.012*ffacescale), static_cast<float>(0.002*ffacescale), static_cast<float>(0.002*ffacescale), static_cast<float>(0.004*ffacescale), static_cast<float>(0.002*ffacescale), static_cast<float>(ffacescale*0.001)
    };

    static GLuint deltafaceArray = glGetUniformLocation(_faceandjawProgram, "deltaFaceArray");
    glUniform1fv(deltafaceArray, iFaceArrSize, arrdeltaface);

    //调整下巴长度
    // 缩放算法的作用域半径
    x0 = face.points_array[16].x;
    y0 = face.points_array[16].y;
    x = face.points_array[93].x;
    y = face.points_array[93].y;

    x = x/mViewPortWidth;
    y = y /mViewPortHeight;
    x0 = x0/mViewPortWidth;
    y0 = y0/mViewPortHeight;

    radius = sqrtf((x-x0)*(x-x0)+(y-y0)*(y-y0));
    theta0 = atanf((y-y0)/(x-x0));

    //radius = 0.5;

    // 缩放系数，0无缩放，大于0则放大
    static GLuint Gtheta0 = glGetUniformLocation(_faceandjawProgram, "theta0");
    glUniform1f(Gtheta0,theta0);

    static GLuint GscaleRatio = glGetUniformLocation(_faceandjawProgram, "scaleRatio");
    glUniform1f(GscaleRatio,2.0);

    static GLuint Gradius = glGetUniformLocation(_faceandjawProgram, "radius");
    glUniform1f(Gradius,radius);

    float arrjawpoints[iJawArrSize*2] = {
            face.points_array[13].x/mViewPortWidth,face.points_array[13].y/mViewPortHeight,
            face.points_array[14].x/mViewPortWidth,face.points_array[14].y/mViewPortHeight,
            face.points_array[15].x/mViewPortWidth,face.points_array[15].y/mViewPortHeight,
            face.points_array[16].x/mViewPortWidth,face.points_array[16].y/mViewPortHeight,
            face.points_array[17].x/mViewPortWidth,face.points_array[17].y/mViewPortHeight,
            face.points_array[18].x/mViewPortWidth,face.points_array[18].y/mViewPortHeight,
            face.points_array[19].x/mViewPortWidth,face.points_array[19].y/mViewPortHeight,
    };

    float arrjawdownpoints[iJawArrSize*2] = {
            static_cast<float>(face.points_array[13].x/mViewPortWidth *1.2 - face.points_array[56].x/mViewPortWidth *0.2),
            static_cast<float>(face.points_array[13].y/mViewPortHeight*1.2 - face.points_array[56].y/mViewPortHeight*0.2),
            static_cast<float>(face.points_array[14].x/mViewPortWidth *1.2 - face.points_array[55].x/mViewPortWidth *0.2),
            static_cast<float>(face.points_array[14].y/mViewPortHeight*1.2 - face.points_array[55].y/mViewPortHeight*0.2),
            static_cast<float>(face.points_array[15].x/mViewPortWidth *1.2 - face.points_array[78].x/mViewPortWidth *0.2),
            static_cast<float>(face.points_array[15].y/mViewPortHeight*1.2 - face.points_array[78].y/mViewPortHeight*0.2),
            static_cast<float>(face.points_array[16].x/mViewPortWidth *1.2 - face.points_array[43].x/mViewPortWidth *0.2),
            static_cast<float>(face.points_array[16].y/mViewPortHeight*1.2 - face.points_array[43].y/mViewPortHeight*0.2),
            static_cast<float>(face.points_array[17].x/mViewPortWidth *1.2 - face.points_array[79].x/mViewPortWidth *0.2),
            static_cast<float>(face.points_array[17].y/mViewPortHeight*1.2 - face.points_array[79].y/mViewPortHeight*0.2),
            static_cast<float>(face.points_array[18].x/mViewPortWidth *1.2 - face.points_array[58].x/mViewPortWidth *0.2),
            static_cast<float>(face.points_array[18].y/mViewPortHeight*1.2 - face.points_array[58].y/mViewPortHeight*0.2),
            static_cast<float>(face.points_array[19].x/mViewPortWidth *1.2 - face.points_array[63].x/mViewPortWidth *0.2),
            static_cast<float>(face.points_array[19].y/mViewPortHeight*1.2 - face.points_array[63].y/mViewPortHeight*0.2),
    };

    // 下巴控制点
    GLuint jawCenterPosition = glGetUniformLocation(_faceandjawProgram, "jawContourPoints");
    glUniform1fv(jawCenterPosition, iJawArrSize*2, arrjawpoints);

    GLuint jawDownPoints = glGetUniformLocation(_faceandjawProgram, "jawDownPoints");
    glUniform1fv(jawDownPoints, iJawArrSize*2, arrjawdownpoints);

    float deltajawarr[iJawArrSize] = {

            static_cast<float>(fjawscale*0.005), static_cast<float>(fjawscale*0.008),
            static_cast<float>(0.011*fjawscale),static_cast<float>(0.016*fjawscale),
            static_cast<float>(0.011*fjawscale), static_cast<float>(0.008*fjawscale),
            static_cast<float>(0.005*fjawscale)

    };

    static GLuint deltajawArray = glGetUniformLocation(_faceandjawProgram, "deltaJawArray");
    glUniform1fv(deltajawArray, iJawArrSize, deltajawarr);

    GLfloat aspectratio = mViewPortWidth/mViewPortHeight;
    static GLuint GaspectRatio = glGetUniformLocation(_faceandjawProgram, "aspectRatio");
    glUniform1f(GaspectRatio,aspectratio);

    static const GLfloat squareVertices1[] = {
            0.0f,  0.0f,
            1.0f,  0.0f,
            0.0f,   1.0f,
            1.0f,   1.0f,
    };
    static const GLfloat textureVertices1[] = {
            -1.0f,  1.0f,
            1.0f,  1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,

    };

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);
    glUseProgram(_faceandjawProgram);
    glVertexAttribPointer(0, 2, GL_FLOAT, 0, 0, squareVertices1);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, 0, 0, textureVertices1);
    glEnableVertexAttribArray(1);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    resultTexture = texture;
    return resultTexture;


    return;

}
