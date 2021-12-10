package com.example.detecthands.utils;

import android.os.Handler;
import android.util.Pair;

import com.sensetime.stmobile.model.STPoint;

import java.util.concurrent.LinkedBlockingQueue;

public class GesturesDetector {

    private int mDetectNum = 7;
    private final LinkedBlockingQueue<Pair<Float, Float>> mHandMarkQueue = new LinkedBlockingQueue<>();
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

    public void detectWaveHand(float x, float y) {
        if (!mNeedDetect) {
            return;
        }
        if ((System.currentTimeMillis() - mLastDetectTime) > 1000) {
            mHandMarkQueue.clear();
        }
        mLastDetectTime = System.currentTimeMillis();

        mHandMarkQueue.offer(new Pair<>(x, y));
        if (mHandMarkQueue.size() < mDetectNum) {
            return;
        }
        while (mHandMarkQueue.size() > mDetectNum) {
            mHandMarkQueue.poll();
        }

        float moveX = 0;
        float moveY = 0;
        Pair<Float, Float> last;
        mHandMarkQueue.size();
        last = mHandMarkQueue.poll();
        for (Pair<Float, Float> pair : mHandMarkQueue) {
            moveX += last.first - pair.first;
            moveY += last.second - pair.second;
            last = pair;
        }

        if (moveY > 700) {
            mHandMarkQueue.clear();
            return;
        }
        if (moveX > 400) {
            mGesturesListener.onDetect(Gestures.RIGHT_WAVE);
            repeatDetect();
            return;
        } else if (moveX < -400) {
            mGesturesListener.onDetect(Gestures.LEFT_WAVE);
            repeatDetect();
            return;
        }

        // 轨迹检测
        moveX = 0;
        boolean direction = true;
        for (Pair<Float, Float> pair : mHandMarkQueue) {
            float cha = last.first - pair.first;
            if (cha > 0) {
                if (!direction) {
                    direction = true;
                    moveX = 0;
                }
                moveX += last.first - pair.first;
            } else {
                if (direction) {
                    direction = false;
                    moveX = 0;
                }
                moveX += last.first - pair.first;
            }

            if (moveX > 400) {
                mGesturesListener.onDetect(Gestures.RIGHT_WAVE);
                repeatDetect();
                break;
            } else if (moveX < -400) {
                mGesturesListener.onDetect(Gestures.LEFT_WAVE);
                repeatDetect();
                break;
            }

            last = pair;
        }
    }

    public void detectGesture(STPoint[] stPoints) {
        if ((System.currentTimeMillis() - mLastDetectTime) < 2000) {
            return;
        }

        if (Math.abs(stPoints[7].getY() - stPoints[4].getY()) < 50) {
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
        mHandler.postDelayed(() -> mNeedDetect = true, 1000);
    }

    public interface GesturesListener {
        void onDetect(Gestures gestures);
    }

    public enum Gestures {
        LEFT_WAVE,
        RIGHT_WAVE,
        LEFT,
        RIGHT
    }

}
