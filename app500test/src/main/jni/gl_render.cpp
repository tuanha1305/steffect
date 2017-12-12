#include <GLES2/gl2.h>
#include "math.h"
#include "gl_render.h"
#include "string.h"
#include "esUtil.h"
#include "vector"
#include "shaders.h"


GLint mGLProgId,mGLAttribPosition,mGLUniformTexture,mGLAttribTextureCoordinate;
GLint mGLMouseId, mGLAttribMousePos,mGLUniformTexture2, mGLAttribMouseColor;
int mViewPortWidth;
int mViewPortHeight;

GLuint _faceandjawProgram, facejawAttPos, facejawCoord, uinputImageTexture;

GLuint faceLianpuProgram, faceLianpuAttPos, faceLianpuCoord, faceLianpuImageTexture, faceLianpuTexture;


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

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeInitWH(JNIEnv* env, jobject obj, jint w, int h)
{
    mViewPortWidth = w;
    mViewPortHeight = h;
}

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeInitMousePrograme(JNIEnv* env, jobject obj)
{
    mGLMouseId = esLoadProgram(g_mouseVerShader, g_mouseFraShader);
    mGLAttribMousePos = glGetAttribLocation(mGLMouseId, "vPosition");
    mGLAttribMouseColor = glGetAttribLocation(mGLMouseId,"SourceColor");

    mGLProgId = esLoadProgram(vertexShaderCode, fragmentShaderCode);
    mGLAttribPosition = glGetAttribLocation(mGLProgId, "position");
    mGLUniformTexture = glGetUniformLocation(mGLProgId, "inputImageTexture");
    mGLAttribTextureCoordinate = glGetAttribLocation(mGLProgId,"inputTextureCoordinate");

    _faceandjawProgram = esLoadProgram(ChangeFaceAndJawV, ChangeFaceAndJawF);
    facejawAttPos = glGetAttribLocation(_faceandjawProgram, "position");
    facejawCoord = glGetAttribLocation(_faceandjawProgram, "inputTextureCoordinate");
    uinputImageTexture = glGetUniformLocation(_faceandjawProgram, "inputImageTexture");

    int ilval = 25 % 2;

    faceLianpuProgram = esLoadProgram(FaceLianpuV, FaceLianpuF);
    faceLianpuAttPos = glGetAttribLocation(faceLianpuProgram, "position");
    faceLianpuCoord = glGetAttribLocation(faceLianpuProgram, "inputTextureCoordinate");
    faceLianpuImageTexture = glGetUniformLocation(faceLianpuProgram, "inputImageTexture");
    faceLianpuTexture = glGetUniformLocation(faceLianpuProgram, "lianpuTexture" );
}


//bool ptInPolygon(float  pt[2], float facept[212])
//{
//    int ncross = 0;
//    for( int i = 0; i < 105; ++ i)
//    {
//        vec2 p1 = vec2(facept[i * 2], facept[ i * 2 + 1]);
//        int index =  i + 1;
//        vec2 p2 = vec2(facept[index * 2], facept[ index * 2 + 1] );
//        if( p1.y == p2.y )
//            continue;
//        if( pt[1] < min(p1.y, p2.y))
//            continue;
//        if( pt[1] >= max(p1.y, p2.y))
//            continue;
//        float x = (float)(pt[1] - p1.y) * (float)(p2.x - p1.x) / (float)(p2.y - p1.y) + p1.x;
//        if( x > pt[0])
//            ++ncross;
//    }
//    return true;
//////            return (mod(ncross , 2) == 1);
//}

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawZuichun(JNIEnv* env, jobject obj, jobjectArray stPoint, jfloatArray downmousecolors)
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

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawUPMouse(JNIEnv* env, jobject obj, jobjectArray stPoint, jfloatArray downmousecolors)
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

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawRightJiemao(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId ,jfloatArray bgcolors)
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
    glBlendColor(0.0f, 0.0f, 0.0f, 0.0f);
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
    //glBlendColor(0.0f, 0.0f, 0.0f, 0.0f);
    //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
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
    //glBlendColor(0.0f, 0.0f, 0.0f, 0.0f);
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
    //glBlendColor(0.0f, 0.0f, 0.0f, 0.0f);
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

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawLeftMeiMao(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId,jfloatArray bgcolors )
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

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawRightMeiMao(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId ,jfloatArray bgcolors)
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

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawFenDi(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId ,jfloatArray bgcolors)
{
    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
    float x0,y0,x,y,x1,y1,x2,y2,x3,y3,x4,y4,k,d;
    float theta;

    //右粉底
    jobject jobj = env->GetObjectArrayElement(stPoint, 82);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    jobj = env->GetObjectArrayElement(stPoint, 83);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);

    //x1 = face.points_array[43].x*0.5 + face.points_array[93].x*0.5;
    //y1 = face.points_array[43].y*0.5 + face.points_array[93].y*0.5;

    x1 = x0 - (x - x0)*1.25035;
    y1 = y0 - (y - y0)*1.25035;

    x2 = x + (x - x0)*1.25035;
    y2 = y + (y - y0)*1.25035;

    jobj = env->GetObjectArrayElement(stPoint, 43);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);
    k = (y2-y1)/(x2-x1);

    theta = atanf(k);

    d = fabsf(k*x0-y0+y1-k*x1)/sqrtf(k*k+1)*2.598;

    x3 = x1 + d * sinf(theta);
    y3 = y1 - d * cosf(theta);

    x4 = x2 + d * sinf(theta);
    y4 = y2 - d * cosf(theta);

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

    static const float textureVertices1[] = {
            0.0f,  0.5747f,
            1.0f,  0.5747f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    glUseProgram(mGLProgId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendColor(1,0,0,1);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices1);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    //下粉底
    jobj = env->GetObjectArrayElement(stPoint, 82);
    x = env->GetFloatField(jobj,id_x);
    y = env->GetFloatField(jobj,id_y);

    jobj = env->GetObjectArrayElement(stPoint, 83);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    x3 = x - (x0 - x)*1.25035;
    y3 = y - (y0 - y)*1.25035;

    x4 = x0 + (x0 - x)*1.25035;
    y4 = y0 + (y0 - y)*1.25035;

    jobj = env->GetObjectArrayElement(stPoint, 93);
    x0 = env->GetFloatField(jobj,id_x);
    y0 = env->GetFloatField(jobj,id_y);

    k = (y4-y3)/(x4-x3);
    theta = atanf(k);

    d = fabsf(k*x0-y0+y3-k*x3)/sqrtf(k*k+1)*1.972;

    x1 = x3 - d * sinf(theta);
    y1 = y3 + d * cosf(theta);

    x2 = x4 - d * sinf(theta);
    y2 = y4 + d * cosf(theta);

    x1  = changeToGLPointT(x1);
    y1  = changeToGLPointR(y1);
    x2  = changeToGLPointT(x2);
    y2  = changeToGLPointR(y2);
    x3  = changeToGLPointT(x3);
    y3  = changeToGLPointR(y3);
    x4  = changeToGLPointT(x4);
    y4  = changeToGLPointR(y4);

    squareVertices[0] = x1;
    squareVertices[1] = y1;
    squareVertices[2] = x2;
    squareVertices[3] = y2;
    squareVertices[4] = x3;
    squareVertices[5] = y3;
    squareVertices[6] = x4;
    squareVertices[7] = y4;

    static const float textureVertices2[] = {
            0.0f,  1.0f,
            1.0f,  1.0f,
            0.0f, 0.5747f,
            1.0f, 0.5747f
    };
    glUseProgram(mGLProgId);
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
    glVertexAttribPointer(mGLAttribPosition, 2, GL_FLOAT, false, 0, squareVertices);
    glEnableVertexAttribArray(mGLAttribPosition);
    glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GL_FLOAT, false, 0, textureVertices2);
    glEnableVertexAttribArray(mGLAttribTextureCoordinate);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, textureId);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisable( GL_BLEND);
}

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawSaiHong(JNIEnv* env, jobject obj, jobjectArray stPoint, int textureId,jfloatArray bgcolors )
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

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeChangeFaceAndJaw(JNIEnv* env, jobject obj, jobjectArray stPoint, int texture,  float scale, float jawscale)
{
    GLuint resultTexture = 0;

    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");

    GLfloat x,y,x0,y0,radius,theta0;

    static const int iFaceArrSize = 9;
    static const int iJawArrSize = 7;

    glUseProgram(_faceandjawProgram);

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

    jobj = env->GetObjectArrayElement(stPoint, 15);
    float x15 = env->GetFloatField(jobj, id_x);
    float y15 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 16);
    float x16 = env->GetFloatField(jobj, id_x);
    float y16 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 17);
    float x17 = env->GetFloatField(jobj, id_x);
    float y17 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 18);
    float x18 = env->GetFloatField(jobj, id_x);
    float y18 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 19);
    float x19 = env->GetFloatField(jobj, id_x);
    float y19 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 20);
    float x20 = env->GetFloatField(jobj, id_x);
    float y20 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 21);
    float x21 = env->GetFloatField(jobj, id_x);
    float y21 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 22);
    float x22 = env->GetFloatField(jobj, id_x);
    float y22 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 23);
    float x23 = env->GetFloatField(jobj, id_x);
    float y23 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 24);
    float x24 = env->GetFloatField(jobj, id_x);
    float y24 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 25);
    float x25 = env->GetFloatField(jobj, id_x);
    float y25 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 26);
    float x26 = env->GetFloatField(jobj, id_x);
    float y26 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 43);
    float x43 = env->GetFloatField(jobj, id_x);
    float y43 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 55);
    float x55 = env->GetFloatField(jobj, id_x);
    float y55 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 56);
    float x56 = env->GetFloatField(jobj, id_x);
    float y56 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 58);
    float x58 = env->GetFloatField(jobj, id_x);
    float y58 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 63);
    float x63 = env->GetFloatField(jobj, id_x);
    float y63 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 78);
    float x78 = env->GetFloatField(jobj, id_x);
    float y78 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 79);
    float x79 = env->GetFloatField(jobj, id_x);
    float y79 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 93);
    float x93 = env->GetFloatField(jobj, id_x);
    float y93 = env->GetFloatField(jobj, id_y);

    float arrleft[iFaceArrSize*2] = {
            x6/mViewPortWidth, y6/mViewPortHeight,
            x7/mViewPortWidth,y7/mViewPortHeight,
            x8/mViewPortWidth,y8/mViewPortHeight,
            x9/mViewPortWidth,y9/mViewPortHeight,
            x10/mViewPortWidth,y10/mViewPortHeight,
            x11/mViewPortWidth,y11/mViewPortHeight,
            x12/mViewPortWidth,y12/mViewPortHeight,
            x13/mViewPortWidth,y13/mViewPortHeight,
            x14/mViewPortWidth,y14/mViewPortHeight,

    };

    GLuint leftFaceCenterPosition = glGetUniformLocation(_faceandjawProgram, "leftContourPoints");
    glUniform1fv(leftFaceCenterPosition, iFaceArrSize*2, arrleft);

    // 右脸控制点
    float arrright[iFaceArrSize*2] = {
            x26/mViewPortWidth,y26/mViewPortHeight,
            x25/mViewPortWidth,y25/mViewPortHeight,
            x24/mViewPortWidth,y24/mViewPortHeight,
            x23/mViewPortWidth,y23/mViewPortHeight,
            x22/mViewPortWidth,y22/mViewPortHeight,
            x21/mViewPortWidth,y21/mViewPortHeight,
            x20/mViewPortWidth,y20/mViewPortHeight,
            x19/mViewPortWidth,y19/mViewPortHeight,
            x18/mViewPortWidth,y18/mViewPortHeight,
    };

    static GLuint GrightFaceCenterPosition = glGetUniformLocation(_faceandjawProgram, "rightContourPoints");
    glUniform1fv(GrightFaceCenterPosition, iFaceArrSize*2, arrright);

    float arrdeltaface[9] = {
            static_cast<float>(scale*0.0045),
            static_cast<float>(scale*0.006),
            static_cast<float>(scale*0.009),
            static_cast<float>(0.012*scale),
            static_cast<float>(0.002*scale),
            static_cast<float>(0.002*scale),
            static_cast<float>(0.004*scale),
            static_cast<float>(0.002*scale),
            static_cast<float>(scale*0.001)
    };

    static GLuint deltafaceArray = glGetUniformLocation(_faceandjawProgram, "deltaFaceArray");
    glUniform1fv(deltafaceArray, iFaceArrSize, arrdeltaface);

    //调整下巴长度
    // 缩放算法的作用域半径
    x0 = x16;
    y0 = y16;
    x = x93;
    y = y93;

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
            x13/mViewPortWidth,y13/mViewPortHeight,
            x14/mViewPortWidth,y14/mViewPortHeight,
            x15/mViewPortWidth,y15/mViewPortHeight,
            x16/mViewPortWidth,y16/mViewPortHeight,
            x17/mViewPortWidth,y17/mViewPortHeight,
            x18/mViewPortWidth,y18/mViewPortHeight,
            x19/mViewPortWidth,y19/mViewPortHeight,
    };

    float arrjawdownpoints[iJawArrSize*2] = {
            static_cast<float>(x13/mViewPortWidth *1.2 - x56/mViewPortWidth *0.2),
            static_cast<float>(y13/mViewPortHeight*1.2 - y56/mViewPortHeight*0.2),
            static_cast<float>(x14/mViewPortWidth *1.2 - x55/mViewPortWidth *0.2),
            static_cast<float>(y14/mViewPortHeight*1.2 - y55/mViewPortHeight*0.2),
            static_cast<float>(x15/mViewPortWidth *1.2 - x78/mViewPortWidth *0.2),
            static_cast<float>(y15/mViewPortHeight*1.2 - y78/mViewPortHeight*0.2),
            static_cast<float>(x16/mViewPortWidth *1.2 - x43/mViewPortWidth *0.2),
            static_cast<float>(y16/mViewPortHeight*1.2 - y43/mViewPortHeight*0.2),
            static_cast<float>(x17/mViewPortWidth *1.2 - x79/mViewPortWidth *0.2),
            static_cast<float>(y17/mViewPortHeight*1.2 - y79/mViewPortHeight*0.2),
            static_cast<float>(x18/mViewPortWidth *1.2 - x58/mViewPortWidth *0.2),
            static_cast<float>(y18/mViewPortHeight*1.2 - y58/mViewPortHeight*0.2),
            static_cast<float>(x19/mViewPortWidth *1.2 - x63/mViewPortWidth *0.2),
            static_cast<float>(y19/mViewPortHeight*1.2 - y63/mViewPortHeight*0.2),
    };

    // 下巴控制点
    GLuint jawCenterPosition = glGetUniformLocation(_faceandjawProgram, "jawContourPoints");
    glUniform1fv(jawCenterPosition, iJawArrSize*2, arrjawpoints);

    GLuint jawDownPoints = glGetUniformLocation(_faceandjawProgram, "jawDownPoints");
    glUniform1fv(jawDownPoints, iJawArrSize*2, arrjawdownpoints);

    float deltajawarr[iJawArrSize] = {

            static_cast<float>(jawscale*0.005),
            static_cast<float>(jawscale*0.008),
            static_cast<float>(0.011*jawscale),
            static_cast<float>(0.016*jawscale),
            static_cast<float>(0.011*jawscale),
            static_cast<float>(0.008*jawscale),
            static_cast<float>(0.005*jawscale)

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
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };

//    static const GLfloat textureVertices1[] = {
//            0.0f,  1.0f,
//            1.0f,  1.0f,
//            0.0f, 0.0f,
//            1.0f, 0.0f,
//
//    };

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);
    glUniform1i(uinputImageTexture, 0);
    glVertexAttribPointer(facejawAttPos, 2, GL_FLOAT, 0, 0, textureVertices1);
    glEnableVertexAttribArray(facejawAttPos);
    glVertexAttribPointer(facejawCoord, 2, GL_FLOAT, 0, 0, squareVertices1);
    glEnableVertexAttribArray(facejawCoord);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    resultTexture = texture;
    return ;
}


JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_STGLRender_nativeDrawLianpu(JNIEnv* env, jobject obj, jfloatArray  stPoint,
                                                                                              int texture, int lianpuid, float scale, float jawscale)
{
    GLuint resultTexture = 0;
    jfloat* stPoints = env->GetFloatArrayElements( stPoint,0);
    static const int iFaceArrSize = 9;
    static const int iJawArrSize = 7;

    glUseProgram(faceLianpuProgram);
    float fwidth = 800.0f;
    float fheight = 1067.0f;  // 中心点 400， 505
    float lianpuCoord[33 * 2] = {
            108,  450,
            143,  481,
            146,  515,
            150,  552,
            156,  578,
            163,  612,
            173,  644,
            180,  675,
            192,  706,
            204,  738,
            222,  765,
            243,  790,
            269,  811,
            291,  832,
            316,  848,
            348,  359,
            380,  865,
            413,  862,
            445,  851,
            475,  839,
            502,  818,
            526,  798,
            550,  773,
            570,  747,
            586,  717,
            600,  688,
            609,  655,
            617,  621,
            622,  589,
            628,  555,
            632,  522,
            637,  489,
            640,  456,
    };

    for( int i = 0; i < 33; ++i )
    {
        lianpuCoord[i * 2] = lianpuCoord[i * 2] / fwidth;
        lianpuCoord[i * 2 + 1] = lianpuCoord[i * 2 + 1] / fheight;
    }

    float leftEyes[16];
    float rightEyes[16];
    float mousePoints[16];

    for( int k = 0; k < 6; ++k )
    {
        leftEyes[k * 2] =    stPoints[(52 + k) * 2];
        leftEyes[k * 2 + 1] =  stPoints[(52 + k) * 2 + 1];

        rightEyes[k * 2] =     stPoints[(58 + k) * 2];
        rightEyes[k * 2 + 1] =  stPoints[(58 + k) * 2 + 1];
    }
    for( int k = 6; k < 8; ++k)
    {
        leftEyes[k * 2 ] =     stPoints[(72 + k - 6 )* 2];
        leftEyes[k * 2 + 1] =  stPoints[(72 + k - 6) * 2 + 1];

        rightEyes[k * 2] =     stPoints[(75 + k - 6) * 2];
        rightEyes[k * 2 + 1] = stPoints[(75 + k - 6) * 2 + 1];
    }

    for( int k = 0;  k < 8; ++k )
    {
        mousePoints[k * 2 ] =     stPoints[(96 + k)* 2];
        mousePoints[k * 2 + 1] =  stPoints[(96 + k)* 2 + 1];
    }

    static GLuint ufacePoints = glGetUniformLocation( faceLianpuProgram, "facePoints");
    glUniform1fv(ufacePoints, 212, stPoints);

    static GLuint uleftEyes = glGetUniformLocation( faceLianpuProgram, "leftEyes");
    glUniform1fv(uleftEyes, 16, leftEyes);

    static GLuint uRightEyes = glGetUniformLocation( faceLianpuProgram, "rightEyes");
    glUniform1fv(uRightEyes, 16, rightEyes);

    static GLuint uMousePoints = glGetUniformLocation(faceLianpuProgram, "mousePoints");
    glUniform1fv(uMousePoints, 16, mousePoints);

    static const GLfloat squareVertices1[] = {
            0.0f,  0.0f,
            1.0f,  0.0f,
            0.0f,   1.0f,
            1.0f,   1.0f,
    };
    static const GLfloat textureVertices1[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);
    glUniform1i(faceLianpuImageTexture, 0);

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, lianpuid );
    glUniform1i(faceLianpuTexture, 1);

    glVertexAttribPointer(faceLianpuAttPos, 2, GL_FLOAT, 0, 0, textureVertices1);
    glEnableVertexAttribArray(faceLianpuAttPos);
    glVertexAttribPointer(faceLianpuCoord, 2, GL_FLOAT, 0, 0, squareVertices1);
    glEnableVertexAttribArray(faceLianpuCoord);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    resultTexture = texture;
    return ;
}

JNIEXPORT void JNICALL Java_sensetime_senseme_com_effects_display_ImageInputRender_nativeChangeFaceAndJaw(JNIEnv* env, jobject obj, jobjectArray stPoint, int texture,  float scale, float jawscale) {

    // TODO
    GLuint resultTexture = 0;

    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
    jfieldID id_y = env->GetFieldID(objClass, "y", "F");

    GLfloat x,y,x0,y0,radius,theta0;

    static const int iFaceArrSize = 9;
    static const int iJawArrSize = 7;

    glUseProgram(_faceandjawProgram);

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

    jobj = env->GetObjectArrayElement(stPoint, 15);
    float x15 = env->GetFloatField(jobj, id_x);
    float y15 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 16);
    float x16 = env->GetFloatField(jobj, id_x);
    float y16 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 17);
    float x17 = env->GetFloatField(jobj, id_x);
    float y17 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 18);
    float x18 = env->GetFloatField(jobj, id_x);
    float y18 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 19);
    float x19 = env->GetFloatField(jobj, id_x);
    float y19 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 20);
    float x20 = env->GetFloatField(jobj, id_x);
    float y20 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 21);
    float x21 = env->GetFloatField(jobj, id_x);
    float y21 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 22);
    float x22 = env->GetFloatField(jobj, id_x);
    float y22 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 23);
    float x23 = env->GetFloatField(jobj, id_x);
    float y23 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 24);
    float x24 = env->GetFloatField(jobj, id_x);
    float y24 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 25);
    float x25 = env->GetFloatField(jobj, id_x);
    float y25 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 26);
    float x26 = env->GetFloatField(jobj, id_x);
    float y26 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 43);
    float x43 = env->GetFloatField(jobj, id_x);
    float y43 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 55);
    float x55 = env->GetFloatField(jobj, id_x);
    float y55 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 56);
    float x56 = env->GetFloatField(jobj, id_x);
    float y56 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 58);
    float x58 = env->GetFloatField(jobj, id_x);
    float y58 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 63);
    float x63 = env->GetFloatField(jobj, id_x);
    float y63 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 78);
    float x78 = env->GetFloatField(jobj, id_x);
    float y78 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 79);
    float x79 = env->GetFloatField(jobj, id_x);
    float y79 = env->GetFloatField(jobj, id_y);

    jobj = env->GetObjectArrayElement(stPoint, 93);
    float x93 = env->GetFloatField(jobj, id_x);
    float y93 = env->GetFloatField(jobj, id_y);

    float arrleft[iFaceArrSize*2] = {
            x6/mViewPortWidth, y6/mViewPortHeight,
            x7/mViewPortWidth,y7/mViewPortHeight,
            x8/mViewPortWidth,y8/mViewPortHeight,
            x9/mViewPortWidth,y9/mViewPortHeight,
            x10/mViewPortWidth,y10/mViewPortHeight,
            x11/mViewPortWidth,y11/mViewPortHeight,
            x12/mViewPortWidth,y12/mViewPortHeight,
            x13/mViewPortWidth,y13/mViewPortHeight,
            x14/mViewPortWidth,y14/mViewPortHeight,

    };

    GLuint leftFaceCenterPosition = glGetUniformLocation(_faceandjawProgram, "leftContourPoints");
    glUniform1fv(leftFaceCenterPosition, iFaceArrSize*2, arrleft);

    // 右脸控制点
    float arrright[iFaceArrSize*2] = {
            x26/mViewPortWidth,y26/mViewPortHeight,
            x25/mViewPortWidth,y25/mViewPortHeight,
            x24/mViewPortWidth,y24/mViewPortHeight,
            x23/mViewPortWidth,y23/mViewPortHeight,
            x22/mViewPortWidth,y22/mViewPortHeight,
            x21/mViewPortWidth,y21/mViewPortHeight,
            x20/mViewPortWidth,y20/mViewPortHeight,
            x19/mViewPortWidth,y19/mViewPortHeight,
            x18/mViewPortWidth,y18/mViewPortHeight,
    };

    static GLuint GrightFaceCenterPosition = glGetUniformLocation(_faceandjawProgram, "rightContourPoints");
    glUniform1fv(GrightFaceCenterPosition, iFaceArrSize*2, arrright);

    float arrdeltaface[9] = {
            static_cast<float>(scale*0.0045),
            static_cast<float>(scale*0.006),
            static_cast<float>(scale*0.009),
            static_cast<float>(0.012*scale),
            static_cast<float>(0.002*scale),
            static_cast<float>(0.002*scale),
            static_cast<float>(0.004*scale),
            static_cast<float>(0.002*scale),
            static_cast<float>(scale*0.001)
    };

    static GLuint deltafaceArray = glGetUniformLocation(_faceandjawProgram, "deltaFaceArray");
    glUniform1fv(deltafaceArray, iFaceArrSize, arrdeltaface);

    //调整下巴长度
    // 缩放算法的作用域半径
    x0 = x16;
    y0 = y16;
    x = x93;
    y = y93;

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
            x13/mViewPortWidth,y13/mViewPortHeight,
            x14/mViewPortWidth,y14/mViewPortHeight,
            x15/mViewPortWidth,y15/mViewPortHeight,
            x16/mViewPortWidth,y16/mViewPortHeight,
            x17/mViewPortWidth,y17/mViewPortHeight,
            x18/mViewPortWidth,y18/mViewPortHeight,
            x19/mViewPortWidth,y19/mViewPortHeight,
    };

    float arrjawdownpoints[iJawArrSize*2] = {
            static_cast<float>(x13/mViewPortWidth *1.2 - x56/mViewPortWidth *0.2),
            static_cast<float>(y13/mViewPortHeight*1.2 - y56/mViewPortHeight*0.2),
            static_cast<float>(x14/mViewPortWidth *1.2 - x55/mViewPortWidth *0.2),
            static_cast<float>(y14/mViewPortHeight*1.2 - y55/mViewPortHeight*0.2),
            static_cast<float>(x15/mViewPortWidth *1.2 - x78/mViewPortWidth *0.2),
            static_cast<float>(y15/mViewPortHeight*1.2 - y78/mViewPortHeight*0.2),
            static_cast<float>(x16/mViewPortWidth *1.2 - x43/mViewPortWidth *0.2),
            static_cast<float>(y16/mViewPortHeight*1.2 - y43/mViewPortHeight*0.2),
            static_cast<float>(x17/mViewPortWidth *1.2 - x79/mViewPortWidth *0.2),
            static_cast<float>(y17/mViewPortHeight*1.2 - y79/mViewPortHeight*0.2),
            static_cast<float>(x18/mViewPortWidth *1.2 - x58/mViewPortWidth *0.2),
            static_cast<float>(y18/mViewPortHeight*1.2 - y58/mViewPortHeight*0.2),
            static_cast<float>(x19/mViewPortWidth *1.2 - x63/mViewPortWidth *0.2),
            static_cast<float>(y19/mViewPortHeight*1.2 - y63/mViewPortHeight*0.2),
    };

    // 下巴控制点
    GLuint jawCenterPosition = glGetUniformLocation(_faceandjawProgram, "jawContourPoints");
    glUniform1fv(jawCenterPosition, iJawArrSize*2, arrjawpoints);

    GLuint jawDownPoints = glGetUniformLocation(_faceandjawProgram, "jawDownPoints");
    glUniform1fv(jawDownPoints, iJawArrSize*2, arrjawdownpoints);

    float deltajawarr[iJawArrSize] = {

            static_cast<float>(jawscale*0.005),
            static_cast<float>(jawscale*0.008),
            static_cast<float>(0.011*jawscale),
            static_cast<float>(0.016*jawscale),
            static_cast<float>(0.011*jawscale),
            static_cast<float>(0.008*jawscale),
            static_cast<float>(0.005*jawscale)

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
    glUniform1i(uinputImageTexture, 0);
    glVertexAttribPointer(facejawAttPos, 2, GL_FLOAT, 0, 0, textureVertices1);
    glEnableVertexAttribArray(facejawAttPos);
    glVertexAttribPointer(facejawCoord, 2, GL_FLOAT, 0, 0, squareVertices1);
    glEnableVertexAttribArray(facejawCoord);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    resultTexture = texture;
    return;
}

//void AddPointsToArr(JNIEnv* env, jobject obj,jobjectArray stPoint, int iCount,jobjectArray returnPoint) {
//    int objlen = env->GetArrayLength(stPoint);
//    jclass objClass = env->FindClass("com/sensetime/stmobile/model/STPoint");
//    jfieldID id_x = env->GetFieldID(objClass, "x", "F");
//    jfieldID id_y = env->GetFieldID(objClass, "y", "F");
//
//    int iniCount = env->GetArrayLength(stPoint);;
//    if(iniCount<=2)
//        return;
//    float a,b,c,x0,x1,x2 = 0.0,y0,y1,y2 = 0.0,k,x,y;
//    int num=(int)roundf((float)(iCount-iniCount)/(iniCount-1)),i,j;
//    jobjectArray fitPoint;
//    for(i=0; i<iniCount/2;i++) {
//        x1 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i),id_x);
//        y1 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i),id_y);
//        x0 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i+1),id_x);
//        y0 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i+1),id_y);
//        x2 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i+2),id_x);
//        y2 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i+2),id_y);
//        a = ((y0-y1)*(x0-x2)-(y0-y2)*(x0-x1))/((x0-x1)*(x0-x2)*(x1-x2));
//        b = (y0-y1-a*(x0*x0-x1*x1))/(x0-x1);
//        c = y0 - a*x0*x0 - b*x0;
//        x = x1;
//        y = y1;
//         env->SetObjectField(fitPoint,id_x,x);
//        [fitPoints addObject:[NSValue valueWithCGPoint:fitPoint]];
//
//        for(j=num;j>=1;j--) {
//            k = (float)j/(num+1);
//            x = x1*k+x0*(1-k);
//            y = a*x*x + b*x + c;
//            if(y>fmax(y0,y1) | y<fmin(y0,y1)) {
//                y = y1*k + y0*(1-k);
//            }
//            Se
//            fitPoint = CGPointMake(x, y);
//            [fitPoints addObject:[NSValue valueWithCGPoint:fitPoint]];
//        }
//    }
//    for(i=iniCount/2; i<iniCount-1;i++) {
//        x1 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i-1),id_x);
//        y1 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i-1),id_y);
//        x0 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i),id_x);
//        y0 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i),id_y);
//        x2 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i+1),id_x);
//        y2 =  env->GetFloatField(env->GetObjectArrayElement(stPoint,i+1),id_y);
//        a = ((y0-y1)*(x0-x2)-(y0-y2)*(x0-x1))/((x0-x1)*(x0-x2)*(x1-x2));
//        b = (y0-y1-a*(x0*x0-x1*x1))/(x0-x1);
//        c = y0 - a*x0*x0 - b*x0;
//
//        x = x0;
//        y = y0;
//        fitPoint = CGPointMake(x, y);
//        [fitPoints addObject:[NSValue valueWithCGPoint:fitPoint]];
//
//        if(i == iniCount-2) {
//            num = iCount - iniCount - num*(iniCount-2);
//        }
//
//        for(j=num;j>=1;j--) {
//            k = (float)j/(num+1);
//            x = x0*k+x2*(1-k);
//            y = a*x*x + b*x + c;
//            if(y>fmax(y0,y2) | y<fmin(y0,y2)) {
//                y = y0*k + y2*(1-k);
//            }
//            fitPoint = CGPointMake(x, y);
//            [fitPoints addObject:[NSValue valueWithCGPoint:fitPoint]];
//        }
//    }
//
//    x = x2;
//    y = y2;
//    fitPoint = CGPointMake(x, y);
//    [fitPoints addObject:[NSValue valueWithCGPoint:fitPoint]];
//
//}
