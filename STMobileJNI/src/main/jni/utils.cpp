#include "utils.h"
#include "st_mobile_common.h"

#define  LOG_TAG    "utils"

long getCurrentTime()
{
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

int getImageStride(const st_pixel_format &pixel_format, const int &outputWidth) {
    int stride = 0;
    switch(pixel_format)
    {
        case ST_PIX_FMT_YUV420P:
        case ST_PIX_FMT_NV12:
        case ST_PIX_FMT_NV21:
            stride = outputWidth;
            break;
        case ST_PIX_FMT_BGRA8888:
        case ST_PIX_FMT_RGBA8888:
            stride = outputWidth * 4;
            break;
        case ST_PIX_FMT_BGR888:
            stride = outputWidth * 3;
            break;
        default:
            break;
    }

    return stride;
}

jobject convert2STRect(JNIEnv *env, const st_rect_t &object_rect){
    jclass STRectClass = env->FindClass("com/sensetime/stmobile/model/STRect");

    if (STRectClass == NULL) {
        return NULL;
    }

    jobject rectObject = env->AllocObject(STRectClass);

    jfieldID rect_left = env->GetFieldID(STRectClass, "left", "I");
    jfieldID rect_top = env->GetFieldID(STRectClass, "top", "I");
    jfieldID rect_right = env->GetFieldID(STRectClass, "right", "I");
    jfieldID rect_bottom = env->GetFieldID(STRectClass, "bottom", "I");

    env->SetIntField(rectObject, rect_left, object_rect.left);
    env->SetIntField(rectObject, rect_right, object_rect.right);
    env->SetIntField(rectObject, rect_top, object_rect.top);
    env->SetIntField(rectObject, rect_bottom, object_rect.bottom);

    if(STRectClass != NULL){
        env->DeleteLocalRef(STRectClass);
    }

    return rectObject;
}

jobject convert2HumanAction(JNIEnv *env, const st_mobile_human_action_t &human_action)
{
    jclass humanActionCls = env->FindClass("com/sensetime/stmobile/STHumanAction");
    if (humanActionCls == NULL) {
        LOGE("STHumanAction class not found!");
        return NULL;
    }

    jobject humanActionObject = env->AllocObject(humanActionCls);

    jfieldID fieldFaceCount = env->GetFieldID(humanActionCls, "faceCount", "I");
    jfieldID fieldHumanAction = env->GetFieldID(humanActionCls, "faces", "[Lcom/sensetime/stmobile/model/STMobileFaceAction;");
    jfieldID fieldHandCount = env->GetFieldID(humanActionCls, "handCount", "I");
    jfieldID fieldHandAction = env->GetFieldID(humanActionCls, "hands", "[Lcom/sensetime/stmobile/model/STMobileHandAction;");
    jfieldID fieldBackGroundRet = env->GetFieldID(humanActionCls, "backGroundRet", "I");
    jfieldID fieldImage = env->GetFieldID(humanActionCls, "image", "Lcom/sensetime/stmobile/model/STImage;");
    jfieldID fieldFaceExtraInfo = env->GetFieldID(humanActionCls, "faceExtraInfo", "Lcom/sensetime/stmobile/model/STFaceExtraInfo;");

    // human action face info
    jclass humanActionFaceCls = env->FindClass("com/sensetime/stmobile/model/STMobileFaceAction");
    if (humanActionFaceCls == NULL) {
        LOGE("HumanActionFace class not found!");
        return NULL;
    }
    int face_count = human_action.face_count;
    jobjectArray humanActionFaces_array = env->NewObjectArray(face_count, humanActionFaceCls, 0);

    jfieldID fieldFace106 = env->GetFieldID(humanActionFaceCls, "face", "Lcom/sensetime/stmobile/model/STMobile106;");
    jfieldID fieldFaceAction = env->GetFieldID(humanActionFaceCls, "face_action", "I");

    // 106 point
    jclass st_mobile_106_class = env->FindClass("com/sensetime/stmobile/model/STMobile106");
    if (st_mobile_106_class == NULL) {
        LOGE("STFace106 class not found!");
        return NULL;
    }
    jfieldID frect = env->GetFieldID(st_mobile_106_class, "rect", "Lcom/sensetime/stmobile/model/STRect;");
    jfieldID fscore = env->GetFieldID(st_mobile_106_class, "score", "F");
    jfieldID fpoints_array = env->GetFieldID(st_mobile_106_class, "points_array", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fvisibility_array = env->GetFieldID(st_mobile_106_class, "visibility_array", "[F");
    jfieldID fyaw = env->GetFieldID(st_mobile_106_class, "yaw", "F");
    jfieldID fpitch = env->GetFieldID(st_mobile_106_class, "pitch", "F");
    jfieldID froll = env->GetFieldID(st_mobile_106_class, "roll", "F");
    jfieldID feye_dist = env->GetFieldID(st_mobile_106_class, "eye_dist", "F");
    jfieldID fID = env->GetFieldID(st_mobile_106_class, "ID", "I");

    jclass st_mobile_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(st_mobile_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(st_mobile_point_class, "y", "F");

    jclass st_face_rect_class = env->FindClass("com/sensetime/stmobile/model/STRect");
    jfieldID frect_left = env->GetFieldID(st_face_rect_class, "left", "I");
    jfieldID frect_top = env->GetFieldID(st_face_rect_class, "top", "I");
    jfieldID frect_right = env->GetFieldID(st_face_rect_class, "right", "I");
    jfieldID frect_bottom = env->GetFieldID(st_face_rect_class, "bottom", "I");


    for (int i = 0; i < face_count; ++i)
    {
        jobject humanFace = env->AllocObject(humanActionFaceCls);

        struct st_mobile_106_t face = human_action.faces[i].face;
        jobject st_106_object = env->AllocObject(st_mobile_106_class);

        //继续获取rect
        jobject face_rect = env->AllocObject(st_face_rect_class);

        env->SetIntField(face_rect, frect_left, face.rect.left);
        env->SetIntField(face_rect, frect_right, face.rect.right);
        env->SetIntField(face_rect, frect_top, face.rect.top);
        env->SetIntField(face_rect, frect_bottom, face.rect.bottom);

        //继续获取points_array
        jobjectArray face_point_array = env->NewObjectArray(106, st_mobile_point_class, 0);
        jfloatArray face_visibility_array = env->NewFloatArray(106);
        jfloat visibility_array[106];

        for(int j=0; j<106; j++) {
            jobject st_point_object = env->AllocObject(st_mobile_point_class);

            env->SetFloatField(st_point_object, fpoint_x, face.points_array[j].x);
            env->SetFloatField(st_point_object, fpoint_y, face.points_array[j].y);

            env->SetObjectArrayElement(face_point_array, j, st_point_object);
            env->DeleteLocalRef(st_point_object);

            visibility_array[j] = face.visibility_array[j];
        }
        env->SetFloatArrayRegion(face_visibility_array, 0, 106, visibility_array);
        env->SetObjectField(st_106_object, frect, face_rect);
        env->SetFloatField(st_106_object, fscore, face.score);
        env->SetObjectField(st_106_object, fpoints_array, face_point_array);
        env->SetObjectField(st_106_object, fvisibility_array, face_visibility_array);
        env->SetFloatField(st_106_object, fyaw, face.yaw);
        env->SetFloatField(st_106_object, fpitch, face.pitch);
        env->SetFloatField(st_106_object, froll, face.roll);
        env->SetFloatField(st_106_object, feye_dist, face.eye_dist);
        env->SetIntField(st_106_object, fID, face.ID);


        env->SetIntField(humanFace, fieldFaceAction, human_action.faces[i].face_action);
        env->SetObjectField(humanFace, fieldFace106, st_106_object);
        env->SetObjectArrayElement(humanActionFaces_array, i, humanFace);

        env->DeleteLocalRef(st_106_object);
        env->DeleteLocalRef(face_rect);
        env->DeleteLocalRef(face_point_array);
        env->DeleteLocalRef(face_visibility_array);
        env->DeleteLocalRef(humanFace);
    }

    env->SetIntField(humanActionObject, fieldFaceCount, face_count);
    env->SetObjectField(humanActionObject, fieldHumanAction, humanActionFaces_array);

    env->DeleteLocalRef(humanActionFaces_array);

    //hand info
    jclass handActionClass = env->FindClass("com/sensetime/stmobile/model/STMobileHandAction");
    int hand_count = human_action.hand_count;
    jobjectArray hand_action_obj_array = env->NewObjectArray(hand_count, handActionClass, 0);

    for(int i = 0; i< human_action.hand_count; i++){//hand count 最大值为1
        jobject hand_action_object = env->AllocObject(handActionClass);
        hand_action_object = convert2HandAction(env, human_action.hands[0]);

        env->SetObjectArrayElement(hand_action_obj_array, 0, hand_action_object);
        env->DeleteLocalRef(hand_action_object);
    }
    env->SetObjectField(humanActionObject, fieldHandAction, hand_action_obj_array);
    env->SetIntField(humanActionObject, fieldHandCount, human_action.hand_count);

    env->DeleteLocalRef(hand_action_obj_array);
    //image
    jclass imageClass = env->FindClass("com/sensetime/stmobile/model/STImage");
    jobject image_object = env->AllocObject(imageClass);
    image_object = convert2Image(env,human_action.background);

    env->SetObjectField(humanActionObject, fieldImage, image_object);
    env->SetIntField(humanActionObject, fieldBackGroundRet, human_action.background_result);

    //face extra info
    if(human_action.p_face_extra_info != NULL){
        jclass faceExtraInfoClass = env->FindClass("com/sensetime/stmobile/model/STFaceExtraInfo");
        jobject face_extra_info_object = env->AllocObject(faceExtraInfoClass);
        face_extra_info_object = convert2FaceExtraInfo(env, human_action.p_face_extra_info);

        env->SetObjectField(humanActionObject, fieldFaceExtraInfo, face_extra_info_object);

        if(faceExtraInfoClass != NULL){
            env->DeleteLocalRef(faceExtraInfoClass);
        }
    }

    //env->DeleteLocalRef(image_object); //Must comment out, or it will crash
    if (imageClass != NULL) {
            env->DeleteLocalRef(imageClass);
     }

    if (handActionClass != NULL) {
        env->DeleteLocalRef(handActionClass);
    }

    if (st_mobile_point_class != NULL) {
        env->DeleteLocalRef(st_mobile_point_class);
    }

    if (st_face_rect_class != NULL) {
        env->DeleteLocalRef(st_face_rect_class);
    }

    if (st_mobile_106_class != NULL) {
        env->DeleteLocalRef(st_mobile_106_class);
    }

    if (humanActionFaceCls != NULL) {
        env->DeleteLocalRef(humanActionFaceCls);
    }

    if (humanActionCls != NULL) {
        env->DeleteLocalRef(humanActionCls);
    }

    return humanActionObject;
}
jobject convert2MobileFace106(JNIEnv *env, const st_mobile_106_t &mobile_106)
{
    jclass st_mobile_106_class = env->FindClass("com/sensetime/stmobile/model/STMobile106");
    jfieldID frect = env->GetFieldID(st_mobile_106_class, "rect", "Lcom/sensetime/stmobile/model/STRect;");
    jfieldID fscore = env->GetFieldID(st_mobile_106_class, "score", "F");
    jfieldID fpoints_array = env->GetFieldID(st_mobile_106_class, "points_array", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fvisibility_array = env->GetFieldID(st_mobile_106_class, "visibility_array", "[F");
    jfieldID fyaw = env->GetFieldID(st_mobile_106_class, "yaw", "F");
    jfieldID fpitch = env->GetFieldID(st_mobile_106_class, "pitch", "F");
    jfieldID froll = env->GetFieldID(st_mobile_106_class, "roll", "F");
    jfieldID feye_dist = env->GetFieldID(st_mobile_106_class, "eye_dist", "F");
    jfieldID fID = env->GetFieldID(st_mobile_106_class, "ID", "I");

    jclass st_mobile_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(st_mobile_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(st_mobile_point_class, "y", "F");

    jclass st_face_rect_class = env->FindClass("com/sensetime/stmobile/model/STRect");
    jfieldID frect_left = env->GetFieldID(st_face_rect_class, "left", "I");
    jfieldID frect_top = env->GetFieldID(st_face_rect_class, "top", "I");
    jfieldID frect_right = env->GetFieldID(st_face_rect_class, "right", "I");
    jfieldID frect_bottom = env->GetFieldID(st_face_rect_class, "bottom", "I");

    jobject st_106_object = env->AllocObject(st_mobile_106_class);

    //继续获取rect
    jobject face_rect = env->AllocObject(st_face_rect_class);

    env->SetIntField(face_rect, frect_left, mobile_106.rect.left);
    env->SetIntField(face_rect, frect_right, mobile_106.rect.right);
    env->SetIntField(face_rect, frect_top, mobile_106.rect.top);
    env->SetIntField(face_rect, frect_bottom, mobile_106.rect.bottom);

    //继续获取points_array
    jobjectArray face_point_array = env->NewObjectArray(106, st_mobile_point_class, 0);
    jfloatArray face_visibility_array = env->NewFloatArray(106);
    jfloat visibility_array[106];

    for(int j=0; j<106; j++) {
        jobject st_point_object = env->AllocObject(st_mobile_point_class);

        env->SetFloatField(st_point_object, fpoint_x, mobile_106.points_array[j].x);
        env->SetFloatField(st_point_object, fpoint_y, mobile_106.points_array[j].y);

        env->SetObjectArrayElement(face_point_array, j, st_point_object);
        env->DeleteLocalRef(st_point_object);

        visibility_array[j] = mobile_106.visibility_array[j];
    }
    env->SetFloatArrayRegion(face_visibility_array, 0, 106, visibility_array);
    env->SetObjectField(st_106_object, frect, face_rect);
    env->SetFloatField(st_106_object, fscore, mobile_106.score);
    env->SetObjectField(st_106_object, fpoints_array, face_point_array);
    env->SetObjectField(st_106_object, fvisibility_array, face_visibility_array);
    env->SetFloatField(st_106_object, fyaw, mobile_106.yaw);
    env->SetFloatField(st_106_object, fpitch, mobile_106.pitch);
    env->SetFloatField(st_106_object, froll, mobile_106.roll);
    env->SetFloatField(st_106_object, feye_dist, mobile_106.eye_dist);
    env->SetIntField(st_106_object, fID, mobile_106.ID);

    env->DeleteLocalRef(face_rect);
    env->DeleteLocalRef(face_point_array);
    env->DeleteLocalRef(face_visibility_array);

    if (st_mobile_point_class != NULL) {
        env->DeleteLocalRef(st_mobile_point_class);
    }

    if (st_face_rect_class != NULL) {
        env->DeleteLocalRef(st_face_rect_class);
    }

    if (st_mobile_106_class != NULL) {
        env->DeleteLocalRef(st_mobile_106_class);
    }

    return st_106_object;
}

jobject convert2FaceAttribute(JNIEnv *env, const st_mobile_attribute_t &faceAttribute)
{
    jclass face_attribute_cls = env->FindClass("com/sensetime/stmobile/STFaceAttribute");

    jfieldID fieldAttribute_count = env->GetFieldID(face_attribute_cls, "attribute_count", "I");
    jfieldID fieldAttribute = env->GetFieldID(face_attribute_cls, "arrayAttribute", "[Lcom/sensetime/stmobile/STFaceAttribute$Attribute;");

    jobject faceAttributeObj = env->AllocObject(face_attribute_cls);

    env->SetIntField(faceAttributeObj, fieldAttribute_count, faceAttribute.attribute_count);

    jclass attribute_cls = env->FindClass("com/sensetime/stmobile/STFaceAttribute$Attribute");
    jfieldID fieldCategory = env->GetFieldID(attribute_cls, "category", "Ljava/lang/String;");
    jfieldID fieldLabel = env->GetFieldID(attribute_cls, "label", "Ljava/lang/String;");
    jfieldID fieldScore = env->GetFieldID(attribute_cls, "score", "F");

    if (faceAttribute.attribute_count > 0) {
        jobjectArray arrayAttrObj = env->NewObjectArray(faceAttribute.attribute_count, attribute_cls, 0);
        for (int i = 0; i < faceAttribute.attribute_count; ++i) {
            st_mobile_attribute_one one = faceAttribute.attributes[i];
            jobject attrObj = env->AllocObject(attribute_cls);
            jstring cateObj = env->NewStringUTF(one.category);
            jstring labelObj = env->NewStringUTF(one.label);
            env->SetObjectField(attrObj, fieldCategory, cateObj);
            env->SetObjectField(attrObj, fieldLabel, labelObj);
            env->SetFloatField(attrObj, fieldScore, one.score);

            env->SetObjectArrayElement(arrayAttrObj, i, attrObj);
            env->DeleteLocalRef(cateObj);
            env->DeleteLocalRef(labelObj);
            env->DeleteLocalRef(attrObj);
        }

        env->SetObjectField(faceAttributeObj, fieldAttribute, arrayAttrObj);
        env->DeleteLocalRef(arrayAttrObj);
    }

    env->DeleteLocalRef(attribute_cls);
    env->DeleteLocalRef(face_attribute_cls);
    return faceAttributeObj;
}

jobject convert2Image(JNIEnv *env, const st_image_t &image){
    jclass image_cls = env->FindClass("com/sensetime/stmobile/model/STImage");

    jfieldID fieldImageData = env->GetFieldID(image_cls, "imageData", "[B");
    jfieldID fieldPixelFormat = env->GetFieldID(image_cls, "pixelFormat", "I");
    jfieldID fieldWidth = env->GetFieldID(image_cls, "width", "I");
    jfieldID fieldHeight = env->GetFieldID(image_cls, "height", "I");
    jfieldID fieldStride = env->GetFieldID(image_cls, "stride", "I");
    jfieldID fieldTime = env->GetFieldID(image_cls, "timeStamp", "Lcom/sensetime/stmobile/model/STImage$STTime;");

    jobject imageObj = env->AllocObject(image_cls);

    jbyteArray arrayImageData;
    arrayImageData = (env)->NewByteArray(image.width*image.height);
    jbyte* data = (jbyte*)(image.data);
    env->SetByteArrayRegion(arrayImageData, 0, image.width*image.height, data);
    env->SetObjectField(imageObj, fieldImageData, arrayImageData);

    env->SetIntField(imageObj, fieldPixelFormat, image.pixel_format);
    env->SetIntField(imageObj, fieldWidth, image.width);
    env->SetIntField(imageObj, fieldHeight, image.height);
    env->SetIntField(imageObj, fieldStride, image.stride);

    jclass time_cls = env->FindClass("com/sensetime/stmobile/model/STImage$STTime");
    jfieldID fieldSecond = env->GetFieldID(time_cls, "second", "J");
    jfieldID fieldMicroSeconds = env->GetFieldID(time_cls, "microSeconds", "J");

    jobject timeObj = env->AllocObject(time_cls);
    env->SetLongField(timeObj, fieldSecond, image.time_stamp.tv_sec);
    env->SetLongField(timeObj, fieldMicroSeconds, image.time_stamp.tv_usec);

    env->SetObjectField(imageObj, fieldTime, timeObj);

    env->DeleteLocalRef(arrayImageData);
    env->DeleteLocalRef(image_cls);
    env->DeleteLocalRef(timeObj);
    env->DeleteLocalRef(time_cls);

    return imageObj;
}

jobject convert2HandAction(JNIEnv *env, const st_mobile_hand_action_t &hand_action){
    jclass hand_action_cls = env->FindClass("com/sensetime/stmobile/model/STMobileHandAction");

    jfieldID fieldHandRect = env->GetFieldID(hand_action_cls, "handRect", "Lcom/sensetime/stmobile/model/STRect;");
    jfieldID fieldKeyAction = env->GetFieldID(hand_action_cls, "keyAction", "Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldHandScore = env->GetFieldID(hand_action_cls, "handScore", "F");
    jfieldID fieldHandAction = env->GetFieldID(hand_action_cls, "handAction", "I");
    jfieldID fieldHandActionScore = env->GetFieldID(hand_action_cls, "handActionScore", "F");

    jobject handActionObj = env->AllocObject(hand_action_cls);

    jclass hand_rect_class = env->FindClass("com/sensetime/stmobile/model/STRect");
    jfieldID hrect_left = env->GetFieldID(hand_rect_class, "left", "I");
    jfieldID hrect_top = env->GetFieldID(hand_rect_class, "top", "I");
    jfieldID hrect_right = env->GetFieldID(hand_rect_class, "right", "I");
    jfieldID hrect_bottom = env->GetFieldID(hand_rect_class, "bottom", "I");

    jobject handRectObj = env->AllocObject(hand_rect_class);
    env->SetIntField(handRectObj, hrect_left, hand_action.hand.left);
    env->SetIntField(handRectObj, hrect_top, hand_action.hand.top);
    env->SetIntField(handRectObj, hrect_right, hand_action.hand.right);
    env->SetIntField(handRectObj, hrect_bottom, hand_action.hand.bottom);

    env->SetObjectField(handActionObj, fieldHandRect, handRectObj);

    jclass key_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(key_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(key_point_class, "y", "F");

    jobject keyPointObj = env->AllocObject(key_point_class);
    env->SetFloatField(keyPointObj, fpoint_x, hand_action.key_point.x);
    env->SetFloatField(keyPointObj, fpoint_y, hand_action.key_point.y);

    env->SetObjectField(handActionObj, fieldKeyAction, keyPointObj);

    env->SetFloatField(handActionObj, fieldHandScore, hand_action.hand_score);
    env->SetIntField(handActionObj, fieldHandAction, hand_action.hand_action);
    env->SetFloatField(handActionObj, fieldHandActionScore, hand_action.hand_action_score);

    env->DeleteLocalRef(hand_action_cls);
    env->DeleteLocalRef(hand_rect_class);
    env->DeleteLocalRef(handRectObj);
    env->DeleteLocalRef(key_point_class);
    env->DeleteLocalRef(keyPointObj);

    return handActionObj;
}

jobject convert2FaceExtraInfo(JNIEnv *env, const st_mobile_face_extra_info_t *face_extra_info){

    jclass face_extra_info_cls = env->FindClass("com/sensetime/stmobile/model/STFaceExtraInfo");

    jfieldID fieldEyeCount = env->GetFieldID(face_extra_info_cls, "eyeCount", "I");
    jfieldID fieldEyebrowCount = env->GetFieldID(face_extra_info_cls, "eyebrowCount", "I");
    jfieldID fieldLipsCount = env->GetFieldID(face_extra_info_cls, "lipsCount", "I");

    jfieldID fieldEyeLeft = env->GetFieldID(face_extra_info_cls, "eyeLeft", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldEyeRight = env->GetFieldID(face_extra_info_cls, "eyeRight", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldEyebrowLeft = env->GetFieldID(face_extra_info_cls, "eyebrowLeft", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldEyebrowRight = env->GetFieldID(face_extra_info_cls, "eyebrowRight", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldLips = env->GetFieldID(face_extra_info_cls, "lips", "[Lcom/sensetime/stmobile/model/STPoint;");

    jclass st_mobile_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(st_mobile_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(st_mobile_point_class, "y", "F");

    jobject faceExtraInfoObj = env->AllocObject(face_extra_info_cls);

    env->SetIntField(faceExtraInfoObj, fieldEyeCount, face_extra_info->eye_count);
    env->SetIntField(faceExtraInfoObj, fieldEyebrowCount, face_extra_info->eyebrow_count);
    env->SetIntField(faceExtraInfoObj, fieldLipsCount, face_extra_info->lips_count);

    jobjectArray eyeLeftArray = env->NewObjectArray(22 * ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT, st_mobile_point_class, 0);
    jobjectArray eyeRightArray = env->NewObjectArray(22 * ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT, st_mobile_point_class, 0);

    jobjectArray eyebrowLeftArray = env->NewObjectArray(13 * ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT, st_mobile_point_class, 0);
    jobjectArray eyebrowRightArray = env->NewObjectArray(13 * ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT, st_mobile_point_class, 0);

    jobjectArray lipsArray = env->NewObjectArray(64 *ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT, st_mobile_point_class, 0);

    if(face_extra_info->eye_count <= ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT){
        for(int i = 0; i < face_extra_info->eye_count; i++){
            for(int j = 0; j < 22; j++){
                jobject point = env->AllocObject(st_mobile_point_class);

                env->SetFloatField(point, fpoint_x, face_extra_info->eye_left[i][j].x);
                env->SetFloatField(point, fpoint_y, face_extra_info->eye_left[i][j].y);
                env->SetObjectArrayElement(eyeLeftArray, i * 22 +j, point);
                env->DeleteLocalRef(point);
            }

            for(int j = 0; j < 22; j++){
                jobject point = env->AllocObject(st_mobile_point_class);

                env->SetFloatField(point, fpoint_x, face_extra_info->eye_right[i][j].x);
                env->SetFloatField(point, fpoint_y, face_extra_info->eye_right[i][j].y);
                env->SetObjectArrayElement(eyeRightArray, i * 22 + j, point);
                env->DeleteLocalRef(point);
            }
        }
    }

    if(face_extra_info->eyebrow_count <= ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT){
        for(int i = 0; i < face_extra_info->eyebrow_count; i++){
            for(int j = 0; j < 13; j++){
                jobject point = env->AllocObject(st_mobile_point_class);

                env->SetFloatField(point, fpoint_x, face_extra_info->eyebrow_left[i][j].x);
                env->SetFloatField(point, fpoint_y, face_extra_info->eyebrow_left[i][j].y);
                env->SetObjectArrayElement(eyebrowLeftArray, i * 13 + j, point);
                env->DeleteLocalRef(point);
            }

            for(int j = 0; j < 13; j++){
                jobject point = env->AllocObject(st_mobile_point_class);

                env->SetFloatField(point, fpoint_x, face_extra_info->eyebrow_right[i][j].x);
                env->SetFloatField(point, fpoint_y, face_extra_info->eyebrow_right[i][j].y);
                env->SetObjectArrayElement(eyebrowRightArray, i * 13 + j, point);
                env->DeleteLocalRef(point);
            }
        }
    }

    if(face_extra_info->lips_count <= ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT){
        for(int i = 0; i < face_extra_info->lips_count; i++){
            for(int j = 0; j < 64; j++){
                jobject point = env->AllocObject(st_mobile_point_class);

                env->SetFloatField(point, fpoint_x, face_extra_info->lips[i][j].x);
                env->SetFloatField(point, fpoint_y, face_extra_info->lips[i][j].y);
                env->SetObjectArrayElement(lipsArray, i * 64 + j, point);
                env->DeleteLocalRef(point);
            }
        }
    }

    env->SetObjectField(faceExtraInfoObj, fieldEyeLeft, eyeLeftArray);
    env->SetObjectField(faceExtraInfoObj, fieldEyeRight, eyeRightArray);

    env->SetObjectField(faceExtraInfoObj, fieldEyebrowLeft, eyebrowLeftArray);
    env->SetObjectField(faceExtraInfoObj, fieldEyebrowRight, eyebrowRightArray);

    env->SetObjectField(faceExtraInfoObj, fieldLips, lipsArray);

    if (st_mobile_point_class != NULL) {
        env->DeleteLocalRef(st_mobile_point_class);
    }

    if (face_extra_info_cls != NULL) {
        env->DeleteLocalRef(face_extra_info_cls);
    }

    return faceExtraInfoObj;
}

bool convert2st_mobile_face_extra_info_t(JNIEnv *env, jobject faceExtraInfoObj, st_mobile_face_extra_info_t &face_extra_info){
    if(faceExtraInfoObj == NULL){
        return false;
    }

    jclass face_extra_info_cls = env->GetObjectClass(faceExtraInfoObj);

    if (face_extra_info_cls == NULL) {
        return false;
    }

    jfieldID fieldEyeCount = env->GetFieldID(face_extra_info_cls, "eyeCount", "I");
    jfieldID fieldEyebrowCount = env->GetFieldID(face_extra_info_cls, "eyebrowCount", "I");
    jfieldID fieldLipsCount = env->GetFieldID(face_extra_info_cls, "lipsCount", "I");

    jfieldID fieldEyeLeft = env->GetFieldID(face_extra_info_cls, "eyeLeft", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldEyeRight = env->GetFieldID(face_extra_info_cls, "eyeRight", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldEyebrowLeft = env->GetFieldID(face_extra_info_cls, "eyebrowLeft", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldEyebrowRight = env->GetFieldID(face_extra_info_cls, "eyebrowRight", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldLips = env->GetFieldID(face_extra_info_cls, "lips", "[Lcom/sensetime/stmobile/model/STPoint;");

    face_extra_info.eye_count = env->GetIntField(faceExtraInfoObj, fieldEyeCount);
    face_extra_info.eyebrow_count = env->GetIntField(faceExtraInfoObj, fieldEyebrowCount);
    face_extra_info.lips_count = env->GetIntField(faceExtraInfoObj, fieldLipsCount);

    jclass st_mobile_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(st_mobile_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(st_mobile_point_class, "y", "F");

    jobjectArray eye_left_points_array = (jobjectArray)env->GetObjectField(faceExtraInfoObj, fieldEyeLeft);
    jobjectArray eye_right_points_array = (jobjectArray)env->GetObjectField(faceExtraInfoObj, fieldEyeRight);
    for (int i = 0; i < face_extra_info.eye_count; ++i){
        for(int j = 0; j < 22; ++j){
            jobject point = env->GetObjectArrayElement(eye_left_points_array, i * 22 + j);

            face_extra_info.eye_left[i][j].x = env->GetFloatField(point, fpoint_x);
            face_extra_info.eye_left[i][j].y = env->GetFloatField(point, fpoint_y);
            env->DeleteLocalRef(point);
        }

        for(int j = 0; j < 22; ++j){
            jobject point = env->GetObjectArrayElement(eye_right_points_array, i * 22 + j);

            face_extra_info.eye_right[i][j].x = env->GetFloatField(point, fpoint_x);
            face_extra_info.eye_right[i][j].y = env->GetFloatField(point, fpoint_y);
            env->DeleteLocalRef(point);
        }
    }

    jobjectArray eyebrow_left_points_array = (jobjectArray)env->GetObjectField(faceExtraInfoObj, fieldEyebrowLeft);
    jobjectArray eyebrow_right_points_array = (jobjectArray)env->GetObjectField(faceExtraInfoObj, fieldEyebrowRight);
    for (int i = 0; i < face_extra_info.eyebrow_count; ++i){
        for(int j = 0; j < 13; ++j){
            jobject point = env->GetObjectArrayElement(eyebrow_left_points_array, i * 13 + j);

            face_extra_info.eyebrow_left[i][j].x = env->GetFloatField(point, fpoint_x);
            face_extra_info.eyebrow_left[i][j].y = env->GetFloatField(point, fpoint_y);
            env->DeleteLocalRef(point);
        }

        for(int j = 0; j < 13; ++j){
            jobject point = env->GetObjectArrayElement(eyebrow_right_points_array, i * 13 + j);

            face_extra_info.eyebrow_right[i][j].x = env->GetFloatField(point, fpoint_x);
            face_extra_info.eyebrow_right[i][j].y = env->GetFloatField(point, fpoint_y);
            env->DeleteLocalRef(point);
        }
    }

    jobjectArray lips_points_array = (jobjectArray)env->GetObjectField(faceExtraInfoObj, fieldLips);
    for (int i = 0; i < face_extra_info.lips_count; ++i){
        for(int j = 0; j < 64; ++j){
            jobject point = env->GetObjectArrayElement(lips_points_array, i * 64 + j);

            face_extra_info.lips[i][j].x = env->GetFloatField(point, fpoint_x);
            face_extra_info.lips[i][j].y = env->GetFloatField(point, fpoint_y);
            env->DeleteLocalRef(point);
        }
    }

    env->DeleteLocalRef(face_extra_info_cls);
    env->DeleteLocalRef(st_mobile_point_class);
    env->DeleteLocalRef(eye_left_points_array);
    env->DeleteLocalRef(eye_right_points_array);
    env->DeleteLocalRef(eyebrow_left_points_array);
    env->DeleteLocalRef(eyebrow_right_points_array);
    env->DeleteLocalRef(lips_points_array);

    return true;
}

bool convert2st_rect_t(JNIEnv *env, jobject rectObject, st_rect_t &rect){
    if(rectObject == NULL){
        return false;
    }

    jclass STRectClass = env->GetObjectClass(rectObject);

    if (STRectClass == NULL) {
        return false;
    }

    jfieldID rect_left = env->GetFieldID(STRectClass, "left", "I");
    jfieldID rect_top = env->GetFieldID(STRectClass, "top", "I");
    jfieldID rect_right = env->GetFieldID(STRectClass, "right", "I");
    jfieldID rect_bottom = env->GetFieldID(STRectClass, "bottom", "I");

    rect.left = env->GetIntField(rectObject, rect_left);
    rect.top = env->GetIntField(rectObject, rect_top);
    rect.right = env->GetIntField(rectObject, rect_right);
    rect.bottom = env->GetIntField(rectObject, rect_bottom);

    if(STRectClass != NULL){
        env->DeleteLocalRef(STRectClass);
    }

    return true;
}

bool convert2human_action(JNIEnv *env, jobject humanActionObject, st_mobile_human_action_t &human_action)
{
    if (humanActionObject == NULL) {
        return false;
    }
    jclass humanActionCls = env->GetObjectClass(humanActionObject);

    jfieldID fieldFaceCount = env->GetFieldID(humanActionCls, "faceCount", "I");
    jfieldID fieldHumanAction = env->GetFieldID(humanActionCls, "faces", "[Lcom/sensetime/stmobile/model/STMobileFaceAction;");
    jfieldID fieldHandCount = env->GetFieldID(humanActionCls, "handCount", "I");
    jfieldID fieldHandAction = env->GetFieldID(humanActionCls, "hands", "[Lcom/sensetime/stmobile/model/STMobileHandAction;");
    jfieldID fieldBackGroundRet = env->GetFieldID(humanActionCls, "backGroundRet", "I");
    jfieldID fieldImage = env->GetFieldID(humanActionCls, "image", "Lcom/sensetime/stmobile/model/STImage;");
    jfieldID fieldFaceExtraInfo = env->GetFieldID(humanActionCls, "faceExtraInfo", "Lcom/sensetime/stmobile/model/STFaceExtraInfo;");

    human_action.face_count = env->GetIntField(humanActionObject, fieldFaceCount);
    jobjectArray humanActionArray = (jobjectArray)env->GetObjectField(humanActionObject, fieldHumanAction);

    // human action face info
    jclass humanActionFaceCls = env->FindClass("com/sensetime/stmobile/model/STMobileFaceAction");
    if (humanActionFaceCls == NULL) {
        LOGE("HumanActionFace class not found!");
        return false;
    }

    jfieldID fieldFace106 = env->GetFieldID(humanActionFaceCls, "face", "Lcom/sensetime/stmobile/model/STMobile106;");
    jfieldID fieldFaceAction = env->GetFieldID(humanActionFaceCls, "face_action", "I");

    // 106 point
    jclass st_mobile_106_class = env->FindClass("com/sensetime/stmobile/model/STMobile106");
    jfieldID frect = env->GetFieldID(st_mobile_106_class, "rect", "Lcom/sensetime/stmobile/model/STRect;");
    jfieldID fscore = env->GetFieldID(st_mobile_106_class, "score", "F");
    jfieldID fpoints_array = env->GetFieldID(st_mobile_106_class, "points_array", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fvisibility_array = env->GetFieldID(st_mobile_106_class, "visibility_array", "[F");
    jfieldID fyaw = env->GetFieldID(st_mobile_106_class, "yaw", "F");
    jfieldID fpitch = env->GetFieldID(st_mobile_106_class, "pitch", "F");
    jfieldID froll = env->GetFieldID(st_mobile_106_class, "roll", "F");
    jfieldID feye_dist = env->GetFieldID(st_mobile_106_class, "eye_dist", "F");
    jfieldID fID = env->GetFieldID(st_mobile_106_class, "ID", "I");

    jclass st_mobile_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(st_mobile_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(st_mobile_point_class, "y", "F");

    jclass st_face_rect_class = env->FindClass("com/sensetime/stmobile/model/STRect");
    jfieldID frect_left = env->GetFieldID(st_face_rect_class, "left", "I");
    jfieldID frect_top = env->GetFieldID(st_face_rect_class, "top", "I");
    jfieldID frect_right = env->GetFieldID(st_face_rect_class, "right", "I");
    jfieldID frect_bottom = env->GetFieldID(st_face_rect_class, "bottom", "I");

    for (int i = 0; i < human_action.face_count; ++i)
    {
        jobject humanActionFace = env->GetObjectArrayElement(humanActionArray, i);

        human_action.faces[i].face_action = env->GetIntField(humanActionFace, fieldFaceAction);

        jobject face106 = env->GetObjectField(humanActionFace, fieldFace106);
        human_action.faces[i].face.score = env->GetFloatField(face106, fscore);
        human_action.faces[i].face.yaw = env->GetFloatField(face106, fyaw);
        human_action.faces[i].face.pitch = env->GetFloatField(face106, fpitch);
        human_action.faces[i].face.roll = env->GetFloatField(face106, froll);
        human_action.faces[i].face.eye_dist = env->GetFloatField(face106, feye_dist);
        human_action.faces[i].face.ID = env->GetIntField(face106, fID);

        jobject faceRect = env->GetObjectField(face106, frect);
        human_action.faces[i].face.rect.left = env->GetIntField(faceRect, frect_left);
        human_action.faces[i].face.rect.right = env->GetIntField(faceRect, frect_right);
        human_action.faces[i].face.rect.top = env->GetIntField(faceRect, frect_top);
        human_action.faces[i].face.rect.bottom = env->GetIntField(faceRect, frect_bottom);

        jobjectArray points_array = (jobjectArray)env->GetObjectField(face106, fpoints_array);
        jfloatArray face_visibility_array = (jfloatArray)env->GetObjectField(face106, fvisibility_array);
        jfloat* visibility_array = env->GetFloatArrayElements(face_visibility_array, 0);

        for (int j = 0; j < 106; ++j)
        {
            jobject point = env->GetObjectArrayElement(points_array, j);

            human_action.faces[i].face.points_array[j].x = env->GetFloatField(point, fpoint_x);
            human_action.faces[i].face.points_array[j].y = env->GetFloatField(point, fpoint_y);
            env->DeleteLocalRef(point);

            human_action.faces[i].face.visibility_array[j] = visibility_array[j];
        }

        env->ReleaseFloatArrayElements(face_visibility_array, visibility_array, JNI_FALSE);
        env->DeleteLocalRef(humanActionFace);
        env->DeleteLocalRef(face106);
        env->DeleteLocalRef(faceRect);
        env->DeleteLocalRef(points_array);
        env->DeleteLocalRef(face_visibility_array);
    }

    //hand action
    human_action.hand_count = env->GetIntField(humanActionObject, fieldHandCount);

    if(human_action.hand_count > 0){
        jobjectArray handActionArray = (jobjectArray)env->GetObjectField(humanActionObject, fieldHandAction);
        jobject handActionObj = env->GetObjectArrayElement(handActionArray, 0);
        convert2HandAction(env,handActionObj, human_action.hands[0]);

        env->DeleteLocalRef(handActionArray);
        env->DeleteLocalRef(handActionObj);
    }

    //image
    human_action.background_result = env->GetIntField(humanActionObject, fieldBackGroundRet);

    jobject imageObj = env->GetObjectField(humanActionObject, fieldImage);
    convert2Image(env, imageObj, human_action.background);
    env->DeleteLocalRef(imageObj);

    //face extra info
    jobject faceExtraInfoObj = env->GetObjectField(humanActionObject, fieldFaceExtraInfo);
    if(faceExtraInfoObj != NULL){
       if (!convert2st_mobile_face_extra_info_t(env, faceExtraInfoObj, *human_action.p_face_extra_info)) {
            memset(human_action.p_face_extra_info, 0, sizeof(st_mobile_face_extra_info_t));
        }
        env->DeleteLocalRef(faceExtraInfoObj);
    }

    env->DeleteLocalRef(st_mobile_106_class);
    env->DeleteLocalRef(st_face_rect_class);
    env->DeleteLocalRef(st_mobile_point_class);
    env->DeleteLocalRef(humanActionArray);
    env->DeleteLocalRef(humanActionObject);

    if (humanActionCls != NULL) {
        env->DeleteLocalRef(humanActionCls);
    }

    return true;
}

bool convert2mobile_106(JNIEnv *env, jobject face106, st_mobile_106_t &mobile_106)
{
    if (face106 == NULL) {
        return false;
    }

    jclass st_mobile_106_class = env->FindClass("com/sensetime/stmobile/model/STMobile106");
    jfieldID frect = env->GetFieldID(st_mobile_106_class, "rect", "Lcom/sensetime/stmobile/model/STRect;");
    jfieldID fscore = env->GetFieldID(st_mobile_106_class, "score", "F");
    jfieldID fpoints_array = env->GetFieldID(st_mobile_106_class, "points_array", "[Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fvisibility_array = env->GetFieldID(st_mobile_106_class, "visibility_array", "[F");
    jfieldID fyaw = env->GetFieldID(st_mobile_106_class, "yaw", "F");
    jfieldID fpitch = env->GetFieldID(st_mobile_106_class, "pitch", "F");
    jfieldID froll = env->GetFieldID(st_mobile_106_class, "roll", "F");
    jfieldID feye_dist = env->GetFieldID(st_mobile_106_class, "eye_dist", "F");
    jfieldID fID = env->GetFieldID(st_mobile_106_class, "ID", "I");

    jclass st_mobile_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(st_mobile_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(st_mobile_point_class, "y", "F");

    jclass st_face_rect_class = env->FindClass("com/sensetime/stmobile/model/STRect");
    jfieldID frect_left = env->GetFieldID(st_face_rect_class, "left", "I");
    jfieldID frect_top = env->GetFieldID(st_face_rect_class, "top", "I");
    jfieldID frect_right = env->GetFieldID(st_face_rect_class, "right", "I");
    jfieldID frect_bottom = env->GetFieldID(st_face_rect_class, "bottom", "I");

    mobile_106.score = env->GetFloatField(face106, fscore);
    mobile_106.yaw = env->GetFloatField(face106, fyaw);
    mobile_106.pitch = env->GetFloatField(face106, fpitch);
    mobile_106.roll = env->GetFloatField(face106, froll);
    mobile_106.eye_dist = env->GetFloatField(face106, feye_dist);
    mobile_106.ID = env->GetIntField(face106, fID);

    jobject faceRect = env->GetObjectField(face106, frect);
    mobile_106.rect.left = env->GetIntField(faceRect, frect_left);
    mobile_106.rect.right = env->GetIntField(faceRect, frect_right);
    mobile_106.rect.top = env->GetIntField(faceRect, frect_top);
    mobile_106.rect.bottom = env->GetIntField(faceRect, frect_bottom);

    jobjectArray points_array = (jobjectArray)env->GetObjectField(face106, fpoints_array);
    jfloatArray face_visibility_array = (jfloatArray)env->GetObjectField(face106, fvisibility_array);
    jfloat* visibility_array = env->GetFloatArrayElements(face_visibility_array, 0);

    for (int j = 0; j < 106; ++j)
    {
        jobject point = env->GetObjectArrayElement(points_array, j);

        mobile_106.points_array[j].x = env->GetFloatField(point, fpoint_x);
        mobile_106.points_array[j].y = env->GetFloatField(point, fpoint_y);
        env->DeleteLocalRef(point);

        mobile_106.visibility_array[j] = visibility_array[j];
    }

    env->ReleaseFloatArrayElements(face_visibility_array, visibility_array, JNI_FALSE);
    env->DeleteLocalRef(face_visibility_array);
    env->DeleteLocalRef(points_array);
    env->DeleteLocalRef(faceRect);
    env->DeleteLocalRef(st_mobile_106_class);
    env->DeleteLocalRef(st_face_rect_class);
    env->DeleteLocalRef(st_mobile_point_class);

    return true;
}

bool convert2Image(JNIEnv *env, jobject image, st_image_t &background){
    if (image == NULL) {
        return false;
    }

    jclass image_cls = env->FindClass("com/sensetime/stmobile/model/STImage");

    jfieldID fieldImageData = env->GetFieldID(image_cls, "imageData", "[B");
    jfieldID fieldPixelFormat = env->GetFieldID(image_cls, "pixelFormat", "I");
    jfieldID fieldWidth = env->GetFieldID(image_cls, "width", "I");
    jfieldID fieldHeight = env->GetFieldID(image_cls, "height", "I");
    jfieldID fieldStride = env->GetFieldID(image_cls, "stride", "I");
    jfieldID fieldTime = env->GetFieldID(image_cls, "timeStamp", "Lcom/sensetime/stmobile/model/STImage$STTime;");

    jobject imageData = env->GetObjectField(image, fieldImageData);
    jbyteArray *arr = reinterpret_cast<jbyteArray*>(&imageData);
    jbyte* data = env->GetByteArrayElements(*arr, NULL);
    background.data = (unsigned char*)data;

    background.pixel_format = env->GetIntField(image, fieldPixelFormat);
    background.width = env->GetIntField(image, fieldWidth);
    background.height = env->GetIntField(image, fieldHeight);
    background.stride = env->GetIntField(image, fieldStride);


    jclass time_cls = env->FindClass("com/sensetime/stmobile/model/STImage$STTime");
    jfieldID fieldSecond = env->GetFieldID(time_cls, "second", "J");
    jfieldID fieldMicroSeconds = env->GetFieldID(time_cls, "microSeconds", "J");
    jobject timeObj = env->GetObjectField(image, fieldTime);

    background.time_stamp.tv_sec = env-> GetLongField (timeObj, fieldSecond);
    background.time_stamp.tv_usec = env-> GetLongField (timeObj, fieldMicroSeconds);

    env->ReleaseByteArrayElements(*arr, data, JNI_FALSE);
    env->DeleteLocalRef(imageData);
    env->DeleteLocalRef(image_cls);
    env->DeleteLocalRef(time_cls);
    env->DeleteLocalRef(timeObj);

    //test for jni memory leak
    //jclass vm_class = env->FindClass("dalvik/system/VMDebug");
    //jmethodID dump_mid = env->GetStaticMethodID( vm_class, "dumpReferenceTables", "()V" );
    //env->CallStaticVoidMethod( vm_class, dump_mid );

    return true;
}

bool convert2HandAction(JNIEnv *env, jobject handActionObject, st_mobile_hand_action_t &hand_action){
    if (handActionObject == NULL) {
        return false;
    }

    jclass hand_action_cls = env->FindClass("com/sensetime/stmobile/model/STMobileHandAction");

    jfieldID fieldHandRect = env->GetFieldID(hand_action_cls, "handRect", "Lcom/sensetime/stmobile/model/STRect;");
    jfieldID fieldKeyAction = env->GetFieldID(hand_action_cls, "keyAction", "Lcom/sensetime/stmobile/model/STPoint;");
    jfieldID fieldHandScore = env->GetFieldID(hand_action_cls, "handScore", "F");
    jfieldID fieldHandAction = env->GetFieldID(hand_action_cls, "handAction", "I");
    jfieldID fieldHandActionScore = env->GetFieldID(hand_action_cls, "handActionScore", "F");

    jclass hand_rect_class = env->FindClass("com/sensetime/stmobile/model/STRect");
    jfieldID hrect_left = env->GetFieldID(hand_rect_class, "left", "I");
    jfieldID hrect_top = env->GetFieldID(hand_rect_class, "top", "I");
    jfieldID hrect_right = env->GetFieldID(hand_rect_class, "right", "I");
    jfieldID hrect_bottom = env->GetFieldID(hand_rect_class, "bottom", "I");

    jobject handRectObj = env->GetObjectField(handActionObject, fieldHandRect);
    hand_action.hand.left = env->GetIntField(handRectObj, hrect_left);
    hand_action.hand.top = env->GetIntField(handRectObj, hrect_top);
    hand_action.hand.right = env->GetIntField(handRectObj, hrect_right);
    hand_action.hand.bottom = env->GetIntField(handRectObj, hrect_bottom);

    jclass key_point_class = env->FindClass("com/sensetime/stmobile/model/STPoint");
    jfieldID fpoint_x = env->GetFieldID(key_point_class, "x", "F");
    jfieldID fpoint_y = env->GetFieldID(key_point_class, "y", "F");

    jobject keyPointObj = env->GetObjectField(handActionObject, fieldKeyAction);
    hand_action.key_point.x = env->GetFloatField(keyPointObj, fpoint_x);
    hand_action.key_point.y = env->GetFloatField(keyPointObj, fpoint_y);

    hand_action.hand_score = env->GetFloatField(handActionObject, fieldHandScore);
    hand_action.hand_action = env->GetIntField(handActionObject, fieldHandAction);
    hand_action.hand_action_score = env->GetFloatField(handActionObject, fieldHandActionScore);

    env->DeleteLocalRef(hand_action_cls);
    env->DeleteLocalRef(hand_rect_class);
    env->DeleteLocalRef(handRectObj);
    env->DeleteLocalRef(key_point_class);
    env->DeleteLocalRef(keyPointObj);

    return true;
}
