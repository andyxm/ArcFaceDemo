package com.face.lib;

import com.arcsoft.facerecognition.AFR_FSDKFace;

import java.io.Serializable;

/**
 * 创建日期: 2018/6/7.
 * 创 建 人: xm
 * 内    容:
 */
public class BaseFaceMode implements Serializable{
    private String FaceCard;
    private double Score;

    public double getScore() {
        return Score;
    }

    public void setScore(double score) {
        Score = score;
    }

    public String getFaceCard() {
        return FaceCard;
    }
    public void setFaceCard(String faceCard) {
        FaceCard = faceCard;
    }
    private AFR_FSDKFace FaceFeature;
    public AFR_FSDKFace getFaceFeature() {
        return FaceFeature;
    }
    public void setFaceFeature(AFR_FSDKFace faceFeature) {
        FaceFeature = faceFeature;
    }
}
