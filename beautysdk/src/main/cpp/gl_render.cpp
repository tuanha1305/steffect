#include "gl_render.h"
#include "string.h"

JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawZuichun(JNIEnv* env, jobject obj, jobjectArray stPoint, jfloatArray downmousecolors)
{
    int arr_len = env->GetArrayLength(stPoint);
    STPoint *points = new STPoint[arr_len];
    memset(points, 0, sizeof(STPoint) * arr_len);
//    points[0] = env->GetObjectArrayElement(stPoint, 0);

    float sum = 0.0f;
    int arrf_len = env->GetArrayLength(downmousecolors);
    float *colors = new float[arrf_len];
    memset(colors, 0, sizeof(float) * arrf_len);
    env->GetFloatArrayRegion(downmousecolors, 0, arrf_len, colors);
    for( int i = 0; i < arrf_len; ++i ){
        sum += colors[i];
    }
    free(colors);

}