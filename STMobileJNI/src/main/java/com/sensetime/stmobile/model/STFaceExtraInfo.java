package com.sensetime.stmobile.model;

import com.sensetime.stmobile.STCommon;

/**
 * Created by sensetime on 21/06/2017.
 */

public class STFaceExtraInfo {

    public int eyeCount;
    public int eyebrowCount;
    public int lipsCount;

    public STPoint[] eyeLeft = new STPoint[22 * STCommon.ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT];
    public STPoint[] eyeRight = new STPoint[22 * STCommon.ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT];

    public STPoint[] eyebrowLeft = new STPoint[13 * STCommon.ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT];
    public STPoint[] eyebrowRight = new STPoint[13 * STCommon.ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT];

    public STPoint[] lips = new STPoint[64 * STCommon.ST_MOBILE_HUMAN_ACTION_MAX_FACE_COUNT];

    public STPoint[] getEyeLeftPoints(int index){
        if(index > eyeCount - 1 || index < 0){
            return null;
        }

        STPoint[] points = new STPoint[22];
        for(int i = 0; i < 22; i++){
            points[i] = eyeLeft[22 *index + i];
        }
        return points;
    }

    public STPoint[] getEyeRightPoints(int index){
        if(index > eyeCount - 1 || index < 0){
            return null;
        }

        STPoint[] points = new STPoint[22];
        for(int i = 0; i < 22; i++){
            points[i] = eyeRight[22 *index + i];
        }
        return points;
    }

    public STPoint[] getEyebrowLeftPoints(int index){
        if(index > eyebrowCount - 1 || index < 0){
            return null;
        }

        STPoint[] points = new STPoint[13];
        for(int i = 0; i < 13; i++){
            points[i] = eyebrowLeft[13 *index + i];
        }
        return points;
    }

    public STPoint[] getEyebrowRightPoints(int index){
        if(index > eyebrowCount - 1 || index < 0){
            return null;
        }

        STPoint[] points = new STPoint[13];
        for(int i = 0; i < 13; i++){
            points[i] = eyebrowRight[13 *index + i];
        }
        return points;
    }

    public STPoint[] getLipsPoints(int index){
        if(index > lipsCount - 1 || index < 0){
            return null;
        }

        STPoint[] points = new STPoint[64];
        for(int i = 0; i < 64; i++){
            points[i] = lips[64 *index + i];
        }
        return points;
    }

    //for test
    public STPoint[] getAllPoints(){
        int count = 22 * 2 * eyeCount + 13 * 2 * eyebrowCount + 64 *lipsCount;
        STPoint[] points = new STPoint[count];

        for(int i = 0; i < eyeCount; i++){
            for(int j = 0; j < 22; j++){
                points[i * 22 + j] = eyeLeft[i * 22 +j];
                points[22 * eyeCount + i * 22 + j] = eyeRight[i * 22 +j];
            }
        }

        for(int i = 0; i < eyebrowCount; i++){
            for(int j = 0; j < 13; j++){
                points[22 * 2 * eyeCount + i * 13 + j] = eyebrowLeft[i * 13 +j];
                points[22 * 2 * eyeCount + 13 * eyebrowCount + i * 13 + j] = eyebrowRight[i * 13 +j];
            }
        }

        for(int i = 0; i < lipsCount; i++){
            for(int j = 0; j < 64; j++){
                points[22 * 2 * eyeCount + 13 * 2 * eyebrowCount + i * 64 + j] = lips[i * 64 +j];
            }
        }

        return points;
    }

}
