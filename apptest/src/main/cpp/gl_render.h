#ifndef __GL_RENDER_H__
#define __GL_RENDER_H__

#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeInitWH(JNIEnv* env, jobject obj, jint w, int h);
JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeInitMousePrograme(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_com_facebeauty_com_beautysdk_display_STGLRender_nativeDrawZuichun(JNIEnv* env, jobject obj, jobjectArray stPoint, jfloatArray downmousecolors);

#ifdef __cplusplus
}
#endif

#endif

