#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "utils.h"

#define  LOG_TAG    "STCommonNative"

extern "C" {
    JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STCommon_stColorConvert(JNIEnv * env, jobject obj, jbyteArray imagesrc,
                                            jbyteArray imagedst, jint imageWidth, jint imageHeight, jint type);
    JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STCommon_stImageRotate(JNIEnv * env, jobject obj, jbyteArray imagesrc, jbyteArray imagedst, jint imageWidth, jint imageHeight, jint format, jint rotation);
};

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STCommon_stColorConvert(JNIEnv * env, jobject obj, jbyteArray imagesrc, jbyteArray imagedst, jint imageWidth, jint imageHeight, jint type)
{
    jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(imagesrc, 0));
    jbyte *dstdata = (jbyte*) env->GetPrimitiveArrayCritical(imagedst, 0);

    int result = (int)st_mobile_color_convert((unsigned char *)srcdata,(unsigned char *)dstdata,imageWidth,imageHeight,(st_color_convert_type)type);

    env->ReleasePrimitiveArrayCritical(imagesrc, srcdata, 0);
    env->ReleasePrimitiveArrayCritical(imagedst, dstdata, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STCommon_stImageRotate(JNIEnv * env, jobject obj, jbyteArray imagesrc, jbyteArray imagedst, jint imageWidth, jint imageHeight, jint format, jint rotation)
{
    jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(imagesrc, 0));
    jbyte *dstdata = (jbyte*) env->GetPrimitiveArrayCritical(imagedst, 0);

    st_pixel_format pixel_format = (st_pixel_format)format;
    int stride = getImageStride(pixel_format, imageWidth);

    long startTime = getCurrentTime();
    int result = (int)st_mobile_image_rotate((unsigned char *)srcdata,(unsigned char *)dstdata,imageWidth,imageHeight,stride, pixel_format, (st_rotate_type)rotation);
    long afterdetectTime = getCurrentTime();

    env->ReleasePrimitiveArrayCritical(imagesrc, srcdata, 0);
    env->ReleasePrimitiveArrayCritical(imagedst, dstdata, 0);
   return result;
}