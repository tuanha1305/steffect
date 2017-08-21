#include <GLES2/gl2.h>
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
}


JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawZuichun(JNIEnv* env, jobject obj, jobjectArray stPoint, jfloatArray downmousecolors)
{
    int objlen = env->GetArrayLength(stPoint);
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
//    for(int xx = 0; xx < objlen; ++xx){
//        jobject pointObj = env->GetObjectArrayElement(stPoint, xx);
//        float x = env->GetFloatField(pointObj, id_x);
//        float y = env->GetFloatField(pointObj, id_y);
//        env->GetFloatField(env->GetObjectArrayElement(stPoint, xx),id_x);
//    }

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
//        p0 = changeToGLPointT(pointMouseList.get(i).getX());
//        p1 =  changeToGLPointR(pointMouseList.get(i).getY());
//        p2 = changeToGLPointT(pointDownList.get(i).getX());
//        p3 =  changeToGLPointR(pointDownList.get(i).getY());
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
//        p0 =changeToGLPointT(pointMouseList.get(i).getX());
//        p1 =changeToGLPointR(pointMouseList.get(i).getY());
//        p2 = changeToGLPointT(pointDownList.get(i).getX());
//        p3 = changeToGLPointR(pointDownList.get(i).getY());
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
//        p0 =changeToGLPointT(pointMouseList.get(i).getX());
//        p1 =changeToGLPointR(pointMouseList.get(i).getY());
//        p2 = changeToGLPointT(pointDownList.get(i).getX());
//        p3 = changeToGLPointR(pointDownList.get(i).getY());
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