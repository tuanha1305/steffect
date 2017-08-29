package com.sensetime.stmobile;

/**
 * Created by sensetime on 17-3-14.
 */

public class STHumanActionParamsType {
    //background结果中长边的长度[10,长边长度](默认长边240,短边=长边/原始图像长边*原始图像短边).值越大,背景分割的耗时越长,边缘部分效果越好.
    public final static int ST_HUMAN_ACTION_BACKGROUND_MAX_SIZE = 1;
    // 背景分割羽化程度[0,1](默认值0.35),0 完全不羽化,1羽化程度最高,在strenth较小时,羽化程度基本不变.值越大,前景与背景之间的过度边缘部分越宽.
    public final static int ST_HUMAN_ACTION_BACKGROUND_BLUR_STRENGTH = 2;
    // 设置检测到的最大人脸数目N(默认值10),持续track已检测到的N个人脸直到人脸数小于N再继续做detect.值越大,检测到的人脸数目越多,但相应耗时越长.
    public final static int ST_HUMAN_ACTION_FACELIMIT = 3;
    //  设置tracker每多少帧进行一次detect(默认值有人脸时30,无人脸时30/3=10). 值越大,cpu占用率越低, 但检测出新人脸的时间越长.
    public final static int ST_HUMAN_ACTION_DETECT_INTERVAL = 4;
    // 设置106点平滑的阈值[0.0,1.0](默认值0.5), 值越大, 点越稳定,但相应点会有滞后.
    public final static int ST_HUMAN_ACTION_SMOOTH_THRESHOLD = 5;
    // 设置head_pose去抖动的阈值[0.0,1.0](默认值0.5),值越大, pose信息的值越稳定,但相应值会有滞后.
    public final static int ST_HUMAN_ACTION_HEADPOSE_THRESHOLD = 6;
}
