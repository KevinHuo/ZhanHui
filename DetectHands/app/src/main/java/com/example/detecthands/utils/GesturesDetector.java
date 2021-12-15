package com.example.detecthands.utils;

import static java.lang.Math.abs;

import android.os.Handler;
import android.util.Pair;

import com.google.mediapipe.formats.proto.ClassificationProto;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.HandsResult;
import com.sensetime.stmobile.model.STPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class GesturesDetector {

    private int mDetectNum = 7;
    private final LinkedBlockingQueue<Pair<Float, Float>> mHandMarkQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<Pair<Float, Float>> mLeftHandMarkQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<Pair<Float, Float>> mRightHandMarkQueue = new LinkedBlockingQueue<>();
    private boolean mNeedDetect = true;

    private Handler mHandler = new Handler();
    private GesturesListener mGesturesListener;
    private long mLastDetectTime;

    public void setDetectNum(int num) {
        mDetectNum = num / 3;
    }

    public void setGesturesListener(GesturesListener listener) {
        mGesturesListener = listener;
    }

    /**
     * 这些检测方式同时只能使用一种，别用混了
     */

//    public void detectWaveHand(float x, float y) {
//        if (!mNeedDetect) {
//            return;
//        }
//        if ((System.currentTimeMillis() - mLastDetectTime) > 1000) {
//            mHandMarkQueue.clear();
//        }
//        mLastDetectTime = System.currentTimeMillis();
//
//        mHandMarkQueue.offer(new Pair<>(x, y));
//        if (mHandMarkQueue.size() < mDetectNum) {
//            return;
//        }
//        while (mHandMarkQueue.size() > mDetectNum) {
//            mHandMarkQueue.poll();
//        }
//
//        float moveX = 0;
//        float moveY = 0;
//        Pair<Float, Float> last;
//        mHandMarkQueue.size();
//        last = mHandMarkQueue.poll();
//        for (Pair<Float, Float> pair : mHandMarkQueue) {
//            moveX += last.first - pair.first;
//            moveY += last.second - pair.second;
//            last = pair;
//        }
//
//        if (moveY > 700) {
//            mHandMarkQueue.clear();
//            return;
//        }
//        if (moveX > 400) {
//            mGesturesListener.onDetect(Gestures.RIGHT_WAVE);
//            repeatDetect();
//            return;
//        } else if (moveX < -400) {
//            mGesturesListener.onDetect(Gestures.LEFT_WAVE);
//            repeatDetect();
//            return;
//        }
//
//        // 轨迹检测
//        moveX = 0;
//        boolean direction = true;
//        for (Pair<Float, Float> pair : mHandMarkQueue) {
//            float cha = last.first - pair.first;
//            if (cha > 0) {
//                if (!direction) {
//                    direction = true;
//                    moveX = 0;
//                }
//                moveX += last.first - pair.first;
//            } else {
//                if (direction) {
//                    direction = false;
//                    moveX = 0;
//                }
//                moveX += last.first - pair.first;
//            }
//
//            if (moveX > 400) {
//                mGesturesListener.onDetect(Gestures.RIGHT_WAVE);
//                repeatDetect();
//                break;
//            } else if (moveX < -400) {
//                mGesturesListener.onDetect(Gestures.LEFT_WAVE);
//                repeatDetect();
//                break;
//            }
//
//            last = pair;
//        }
//    }
    public void detectWaveHand(HandsResult handsResult) {
        if (!mNeedDetect) {
            return;
        }
        // 如果两次检测到的时间差距大于 1s，重新记录点位
        if ((System.currentTimeMillis() - mLastDetectTime) > 1000) {
            mLeftHandMarkQueue.clear();
            mRightHandMarkQueue.clear();
        }
        mLastDetectTime = System.currentTimeMillis();

        // 对 HandLandMarks 列表根据对应的 Classification 进行排序
        ArrayList<LandmarkProto.NormalizedLandmarkList> handLandMarks = new ArrayList<>(handsResult.multiHandLandmarks());
        List<ClassificationProto.Classification> classifications = new ArrayList<>(handsResult.multiHandedness());
        for (int i = 0; i < handLandMarks.size(); i++) {
            for (int j = 1; j < handLandMarks.size() - i; j++) {
                if (classifications.get(j - 1).getScore() > classifications.get(j).getScore()) {
                    ClassificationProto.Classification classificationTemp;
                    classificationTemp = classifications.get(j - 1);
                    classifications.set(j - 1, classifications.get(j));
                    classifications.set(j, classificationTemp);

                    LandmarkProto.NormalizedLandmarkList landmarkListTemp;
                    landmarkListTemp = handLandMarks.get(j - 1);
                    handLandMarks.set(j - 1, handLandMarks.get(j));
                    handLandMarks.set(j, landmarkListTemp);
                }
            }
        }

        // 填充左右手的点位，只记录两只手
        int handSize = handLandMarks.size();
        for (int i = 0; i < (Math.min(handSize, 2)); i++) {
            LandmarkProto.NormalizedLandmark landmark = handLandMarks.get(i).getLandmarkList().get(HandLandmark.MIDDLE_FINGER_TIP);
            if (classifications.get(i).getLabel().equalsIgnoreCase("left")) {
                mLeftHandMarkQueue.offer(new Pair<>(landmark.getX(), landmark.getY()));
            } else {
                mRightHandMarkQueue.offer(new Pair<>(landmark.getX(), landmark.getY()));
            }
        }

        // 手势检测
        if (mLeftHandMarkQueue.size() >= mDetectNum) {
            while (mLeftHandMarkQueue.size() > mDetectNum) {
                mLeftHandMarkQueue.poll();
            }

            float moveX = 0;
            float moveY = 0;
            Pair<Float, Float> last;
            last = mLeftHandMarkQueue.poll();
            for (Pair<Float, Float> pair : mLeftHandMarkQueue) {
                moveX += last.first - pair.first;
                moveY += last.second - pair.second;
                last = pair;
            }

            if (abs(moveY) < 0.15f) {
                if (moveX > 0.3f) {
                    mGesturesListener.onDetect(Gestures.LEFT_HAND_LEFT_WAVE);
                    repeatDetect();
                    return;
                } else if (moveX < -0.3f) {
                    mGesturesListener.onDetect(Gestures.LEFT_HAND_RIGHT_WAVE);
                    repeatDetect();
                    return;
                }
            }
        }

        if (mRightHandMarkQueue.size() >= mDetectNum) {
            while (mRightHandMarkQueue.size() > mDetectNum) {
                mRightHandMarkQueue.poll();
            }

            float moveX = 0;
            float moveY = 0;
            Pair<Float, Float> last = mRightHandMarkQueue.poll();
            for (Pair<Float, Float> pair : mRightHandMarkQueue) {
                moveX += last.first - pair.first;
                moveY += last.second - pair.second;
                last = pair;
            }

            if (abs(moveY) < 0.15f) {
                if (moveX > 0.3f) {
                    mGesturesListener.onDetect(Gestures.RIGHT_HAND_LEFT_WAVE);
                    repeatDetect();
                    return;
                } else if (moveX < -0.3f) {
                    mGesturesListener.onDetect(Gestures.RIGHT_HAND_RIGHT_WAVE);
                    repeatDetect();
                    return;
                }
            }
        }
    }

    public void trajectoryTracking(HandsResult handsResult) {
        if (!mNeedDetect) {
            return;
        }
        // 如果两次检测到的时间差距大于 1s，重新记录点位
        if ((System.currentTimeMillis() - mLastDetectTime) > 1000) {
            mHandMarkQueue.clear();
        }
        mLastDetectTime = System.currentTimeMillis();

        // 对 HandLandMarks 列表根据对应的 Classification 进行排序
        ArrayList<LandmarkProto.NormalizedLandmarkList> handLandMarks = new ArrayList<>(handsResult.multiHandLandmarks());
        List<ClassificationProto.Classification> classifications = new ArrayList<>(handsResult.multiHandedness());
        for (int i = 0; i < handLandMarks.size(); i++) {
            for (int j = 1; j < handLandMarks.size() - i; j++) {
                if (classifications.get(j - 1).getScore() < classifications.get(j).getScore()) {
                    ClassificationProto.Classification classificationTemp;
                    classificationTemp = classifications.get(j - 1);
                    classifications.set(j - 1, classifications.get(j));
                    classifications.set(j, classificationTemp);

                    LandmarkProto.NormalizedLandmarkList landmarkListTemp;
                    landmarkListTemp = handLandMarks.get(j - 1);
                    handLandMarks.set(j - 1, handLandMarks.get(j));
                    handLandMarks.set(j, landmarkListTemp);
                }
            }
        }

        int handSize = handLandMarks.size();
        for (int i = 0; i < (Math.min(handSize, 2)); i++) {
            LandmarkProto.NormalizedLandmark landmark = handLandMarks.get(i).getLandmarkList().get(HandLandmark.MIDDLE_FINGER_TIP);
            if (classifications.get(i).getLabel().equalsIgnoreCase("right")) {
                mHandMarkQueue.offer(new Pair<>(landmark.getX(), landmark.getY()));
                break;
            }
        }

        if (mHandMarkQueue.size() < mDetectNum) {
            return;
        }
        while (mHandMarkQueue.size() > mDetectNum) {
            mHandMarkQueue.poll();
        }

        float moveX = 0;
        float moveY = 0;
        Pair<Float, Float> last;
        last = mHandMarkQueue.poll();
        boolean direction = true;
        for (Pair<Float, Float> pair : mHandMarkQueue) {
            float cha = last.first - pair.first;
            if (cha > 0) {
                if (!direction) {
                    direction = true;
                    moveX = 0;
                    moveY = 0;
                }
                moveX += last.first - pair.first;
            } else {
                if (direction) {
                    direction = false;
                    moveX = 0;
                    moveY = 0;
                }
                moveX += last.first - pair.first;
            }
            moveY += last.second - pair.second;

            if (abs(moveY) < 0.15) {
                if (moveX > 0.3) {
                    mGesturesListener.onDetect(Gestures.RIGHT_WAVE);
                    repeatDetect();
                    break;
                } else if (moveX < -0.3) {
                    mGesturesListener.onDetect(Gestures.LEFT_WAVE);
                    repeatDetect();
                    break;
                }
            }
            last = pair;
        }
    }

    public void detectGesture(STPoint[] stPoints) {
        if ((System.currentTimeMillis() - mLastDetectTime) < 2000) {
            return;
        }

        if (abs(stPoints[7].getY() - stPoints[4].getY()) < 50) {
            // 食指上的各个点在一个水平面上
            if ((stPoints[7].getX() - stPoints[4].getX()) < 0) {
                // 指向左边
                if ((stPoints[7].getX() - stPoints[6].getX()) < 0 && (stPoints[6].getX() - stPoints[5].getX()) < 0 && (stPoints[5].getX() - stPoints[4].getX()) < 0) {
                    // 说明食指是展开的
                    if ((stPoints[7].getX() - stPoints[11].getX()) < 0 && (stPoints[6].getX() - stPoints[15].getX()) < 0 && (stPoints[6].getX() - stPoints[19].getX()) < 0) {
                        // 食指之间的 x 坐标小于中指，无名指，小拇指，说明另外三个手指是缩起来的
                        mGesturesListener.onDetect(Gestures.LEFT);
                        mLastDetectTime = System.currentTimeMillis();
                    }
                }
            } else {
                // 指向右边
                if ((stPoints[7].getX() - stPoints[6].getX()) > 0 && (stPoints[6].getX() - stPoints[5].getX()) > 0 && (stPoints[5].getX() - stPoints[4].getX()) > 0) {
                    // 说明食指是展开的
                    if ((stPoints[7].getX() - stPoints[11].getX()) > 0 && (stPoints[6].getX() - stPoints[15].getX()) > 0 && (stPoints[6].getX() - stPoints[19].getX()) > 0) {
                        // 食指之间的 x 坐标大于中指，无名指，小拇指，说明另外三个手指是缩起来的
                        mGesturesListener.onDetect(Gestures.RIGHT);
                        mLastDetectTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void repeatDetect() {
        mNeedDetect = false;
        mHandMarkQueue.clear();
        mLeftHandMarkQueue.clear();
        mRightHandMarkQueue.clear();
        mHandler.postDelayed(() -> mNeedDetect = true, 1000);
    }

    public interface GesturesListener {
        void onDetect(Gestures gestures);
    }

    public enum Gestures {
        LEFT_HAND_LEFT_WAVE,
        LEFT_HAND_RIGHT_WAVE,
        RIGHT_HAND_LEFT_WAVE,
        RIGHT_HAND_RIGHT_WAVE,
        LEFT_WAVE,
        RIGHT_WAVE,
        LEFT,
        RIGHT
    }

}
