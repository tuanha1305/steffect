package com.sensetime.stmobile.model;

public class STMobileFaceInfo {
    public STMobile106 face106;           /// 人脸信息，包含矩形、106点、pose信息等

    public STPoint[] extraFacePoints;
    public int extraFacePointsCount;

    public STPoint[] eyeballCenter;
    public int eyeballCenterPointsCount;

    public STPoint[] eyeballContour;
    public int eyeballContourPointsCount;

    public long faceAction;

    public STMobile106 getFace() {
        return face106;
    }

    public void setFace(STMobile106 face) {
        this.face106 = face;
    }

    public long getFaceAction() {
        return faceAction;
    }

    public void setFaceAction(int face_action) {
        this.faceAction = face_action;
    }

    public int getExtraFacePointsCount(){
        return extraFacePointsCount;
    }

    public STPoint[] getExtraFacePoints(){
        return extraFacePoints;
    }

    public int getEyeballCenterPointsCount(){
        return eyeballCenterPointsCount;
    }

    public STPoint[] getEyeballCenter(){
        return eyeballCenter;
    }

    public int getEyeballContourPointsCount(){
        return eyeballContourPointsCount;
    }

    public STPoint[] getEyeballContour(){
        return eyeballContour;
    }
}
