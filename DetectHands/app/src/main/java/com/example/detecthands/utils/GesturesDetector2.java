package com.example.detecthands.utils;

import android.os.Handler;
import android.util.Pair;

import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.HandsResult;

import java.util.concurrent.LinkedBlockingQueue;

public class GesturesDetector2 {

    private static final int DETECT_NUMS = 10;
    private final LinkedBlockingQueue<Pair<Float, Float>> mLeftHandMarkQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<Pair<Float, Float>> mRightHandMarkQueue = new LinkedBlockingQueue<>();
    private boolean mNeedDetect = true;

    private Handler mHandler = new Handler();
    private GesturesListener mGesturesListener;

    public void setGesturesListener(GesturesListener listener) {
        mGesturesListener = listener;
    }

    public void dealHandDetectResult(HandsResult handsResult) {
        if (!mNeedDetect || handsResult.multiHandLandmarks().isEmpty()) {
            return;
        }

        LandmarkProto.NormalizedLandmark landmark =
                handsResult.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.MIDDLE_FINGER_TIP);

        if (handsResult.multiHandedness().get(0).getLabel().equalsIgnoreCase("left")) {
            mLeftHandMarkQueue.offer(new Pair<>(landmark.getX(), landmark.getY()));
        } else {
            mRightHandMarkQueue.offer(new Pair<>(landmark.getX(), landmark.getY()));
        }

        if (mLeftHandMarkQueue.size() < DETECT_NUMS && mRightHandMarkQueue.size() < DETECT_NUMS) {
            return;
        }

        float moveX = 0;
        float moveY = 0;
        Pair<Float, Float> last;
        if (mLeftHandMarkQueue.size() > 0) {
            last = mLeftHandMarkQueue.poll();
            for (Pair<Float, Float> pair : mLeftHandMarkQueue) {
                moveX += last.first - pair.first;
                moveY += last.second - pair.second;
                last = pair;
            }
            if (Math.abs(moveY) < 0.2) {
                if (moveX > 0.3) {
                    mGesturesListener.onDetect(Gestures.LEFT_HAND_RIGHT_WAVE);
                    repeatDetect();
                } else if (moveX < -0.3) {
                    mGesturesListener.onDetect(Gestures.LEFT_HAND_LEFT_WAVE);
                    repeatDetect();
                }
            }
        }

        if (mRightHandMarkQueue.size() > 0) {
            moveX = 0;
            moveY = 0;
            last = mRightHandMarkQueue.poll();
            for (Pair<Float, Float> pair : mRightHandMarkQueue) {
                moveX += last.first - pair.first;
                moveY += last.second - pair.second;
                last = pair;
            }
            if (Math.abs(moveY) < 0.2) {
                if (moveX > 0.3) {
                    mGesturesListener.onDetect(Gestures.RIGHT_HAND_RIGHT_WAVE);
                    repeatDetect();
                } else if (moveX < -0.3) {
                    mGesturesListener.onDetect(Gestures.RIGHT_HAND_LEFT_WAVE);
                    repeatDetect();
                }
            }
        }
    }

    private void repeatDetect() {
        mNeedDetect = false;
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
        RIGHT_HAND_RIGHT_WAVE
    }

}
