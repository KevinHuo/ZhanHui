package com.example.detecthands.activity;

import android.Manifest;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.detecthands.R;
import com.example.detecthands.sticker.StickerDataUtils;
import com.example.detecthands.utils.GesturesDetector;
import com.example.detecthands.utils.TextureProcessor;
import com.example.detecthands.utils.ToastUtils;
import com.example.detecthands.utils.UploadImageUtil;
import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLCameraParamSelectListener;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLShortVideoRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.pili.droid.shortvideo.PLWatermarkSetting;
import com.qiniu.sensetimeplugin.QNDetectResultListener;
import com.qiniu.sensetimeplugin.QNSenseTimePlugin;
import com.sensetime.stmobile.STMobileHumanActionNative;
import com.sensetime.stmobile.model.STAnimalFace;
import com.sensetime.stmobile.model.STHumanAction;
import com.sensetime.stmobile.model.STPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int TIME_REDUCE = 100;

    private PLShortVideoRecorder mShortVideoRecorder;
    private GesturesDetector mGesturesDetector;

    private QNSenseTimePlugin mSenseTimePlugin;
    private TextureProcessor mMirrorProcessor;

    private TextView mTvFPS;
    private int mFPS;
    private long mLastUpdateFpsTime;
    private TextView mTvDetectNum;
    private int mDetectNum;
    private boolean mNeedDetect = true;
    private TextView mTvNumber;
    private Animation mNumberAnimation;
    private int mCurrentNumber = 3;
    private SoundPool mSoundPool;
    private int mTimeReduceSoundId;
    private int mShutterSoundId;
    private int mSwitchStickerSoundId;

    private int mCurrentStickerIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 777);
        }

        initEffect();
        initShortVideo();
        StickerDataUtils.getStickerPathList();

        mTvFPS = findViewById(R.id.tv_fps);
        mTvDetectNum = findViewById(R.id.tv_detect_num);
        mTvNumber = findViewById(R.id.tv_number);
        mNumberAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_alpha);
        mSoundPool = new SoundPool(3, AudioManager.STREAM_SYSTEM, 5);
        mTimeReduceSoundId = mSoundPool.load(this, R.raw.di, 1);
        mShutterSoundId = mSoundPool.load(this, R.raw.shutter, 1);
        mSwitchStickerSoundId = mSoundPool.load(this, R.raw.switch_sticker, 1);
        Log.e("飞", "onCreate: log 有效");
    }

    private void initEffect() {
        mSenseTimePlugin = new QNSenseTimePlugin.Builder(this)
                .setLicenseAssetPath("SenseME.lic")
                .setModelActionAssetPath("M_SenseME_Face_Video_5.3.3.model")
                .build();
        boolean isAuthorized = mSenseTimePlugin.checkLicense();
        if (!isAuthorized) {
            ToastUtils.showShortToast("鉴权失败，请检查授权文件");
        }

        // 由于短视频 SDK 纹理回调的方向为竖直镜像的，所以需要一个竖直镜像操作将其转正
        mSenseTimePlugin.updateDirection(0, false, true);
        mMirrorProcessor = new TextureProcessor();

        mSenseTimePlugin.setFilterStrength(0.8f);

        mGesturesDetector = new GesturesDetector();
        mGesturesDetector.setGesturesListener(mGesturesListener);
        mSenseTimePlugin.setDetectResultListener(new QNDetectResultListener() {
            @Override
            public void onDetectedHumanAction(STHumanAction stHumanAction) {
                if (stHumanAction.getHandInfos() != null) {
                    mDetectNum++;
                    if (!mNeedDetect) {
                        return;
                    }

                    if (stHumanAction.getHandInfos()[0].getHandAction() == STMobileHumanActionNative.ST_MOBILE_HAND_SCISSOR) {
                        mNeedDetect = false;
                        mHandler.postDelayed(() -> mNeedDetect = true, 5000);
                        mHandler.sendEmptyMessage(TIME_REDUCE);
                        return;
                    }
                    STPoint[] stPoints = stHumanAction.getHandInfos()[0].getExtra2dKeyPoints();
                    if (stPoints != null) {
                        mGesturesDetector.detectGesture(stPoints);
                    }
                }
            }

            @Override
            public void onDetectedAnimalFace(STAnimalFace[] stAnimalFaces) {

            }
        });
    }

    private void initShortVideo() {
        GLSurfaceView glSurfaceView = findViewById(R.id.surfaceView);

        mShortVideoRecorder = new PLShortVideoRecorder();

        PLCameraSetting cameraSetting = new PLCameraSetting();
        cameraSetting.setCameraId(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT);

        PLMicrophoneSetting microphoneSetting = new PLMicrophoneSetting();

        PLVideoEncodeSetting videoEncodeSetting = new PLVideoEncodeSetting(this);
        videoEncodeSetting.setEncodingSizeLevel(PLVideoEncodeSetting.VIDEO_ENCODING_SIZE_LEVEL.VIDEO_ENCODING_SIZE_LEVEL_1088P_1);

        PLAudioEncodeSetting audioEncodeSetting = new PLAudioEncodeSetting();

        PLRecordSetting recordSetting = new PLRecordSetting();

        mShortVideoRecorder.prepare(glSurfaceView, cameraSetting, microphoneSetting, videoEncodeSetting,
                audioEncodeSetting, null, recordSetting);

        mShortVideoRecorder.setCameraParamSelectListener(mCameraParamSelectListener);
        mShortVideoRecorder.setVideoFilterListener(mVideoFilterListener);

        PLWatermarkSetting watermarkSetting = new PLWatermarkSetting();
        watermarkSetting.setSize(1.0f, 1.0f);
        watermarkSetting.setResourceId(R.drawable.water_mark);
        mShortVideoRecorder.setWatermark(watermarkSetting);
    }

    private void captureFrame() {
        mShortVideoRecorder.captureFrame(capturedFrame -> {
            if (capturedFrame == null) {
                Log.e("飞", "截帧失败！");
                return;
            }

            Log.i("飞", "captured frame width: " + capturedFrame.getWidth() + " height: " + capturedFrame.getHeight() + " timestamp: " + capturedFrame.getTimestampMs());
            try {
                String savePath = getExternalFilesDir(Environment.DIRECTORY_MOVIES) + "/"+System.currentTimeMillis()+".jpg";
                FileOutputStream fos = new FileOutputStream(savePath);
                capturedFrame.toBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
                ToastUtils.showShortToast("截帧已保存到 " + savePath);
                new Thread(() -> {
                    String result = UploadImageUtil.uploadImage(savePath);
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        String message = jsonObject.getString("message");
                        ToastUtils.showShortToast("上传结果：" + message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TIME_REDUCE:
                    if (mCurrentNumber == 0) {
                        mTvNumber.setVisibility(View.GONE);
                        mCurrentNumber = 3;
                        mSoundPool.play(mShutterSoundId, 1, 1, 0, 0, 1);
                        captureFrame();
                    } else {
                        mTvNumber.setVisibility(View.VISIBLE);
                        mTvNumber.setText(mCurrentNumber + "");
                        mHandler.sendEmptyMessageDelayed(TIME_REDUCE, 1000);
                        mNumberAnimation.reset();
                        mTvNumber.startAnimation(mNumberAnimation);
                        mSoundPool.play(mTimeReduceSoundId, 1, 1, 0, 0, 1);
                        mCurrentNumber--;
                    }
                    break;
            }
        }
    };

    private final PLCameraParamSelectListener mCameraParamSelectListener = new PLCameraParamSelectListener() {
        @Override
        public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
            return list.get(list.size() - 1);
        }

        @Override
        public int[] onPreviewFpsSelected(List<int[]> list) {
            return list.get(list.size() - 1);
        }

        @Override
        public String onFocusModeSelected(List<String> list) {
            return list.get(list.size() - 1);
        }
    };

    private final PLVideoFilterListener mVideoFilterListener = new PLVideoFilterListener() {

        @Override
        public void onSurfaceCreated() {
            mSenseTimePlugin.init();
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Face_Extra_5.23.0.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Iris_2.0.0.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Hand_5.4.0.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Attribute_1.0.1.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Segment_4.10.8.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseAR_Segment_MouthOcclusion_FastV1_1.1.1.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Hand_Skeleton_2d3d_1.0.0.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Hand_Dynamic_Gesture_1.0.0.model");
            mSenseTimePlugin.recoverEffects();

            mMirrorProcessor.release();
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            mMirrorProcessor.setViewportSize(width, height);
        }

        @Override
        public void onSurfaceDestroy() {
            mSenseTimePlugin.destroy();
        }

        @Override
        public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastUpdateFpsTime > 1000) {
                runOnUiThread(() -> {
                    mGesturesDetector.setDetectNum(mFPS);
                    mTvFPS.setText(mFPS + "");
                    mFPS = 0;
                    mTvDetectNum.setText(mDetectNum + "");
                    mDetectNum = 0;
                });
                mLastUpdateFpsTime = currentTime;
            }
            mFPS++;
//            int newTexId = mMirrorProcessor.draw(texId);
            int newTexId = texId;
            newTexId = mSenseTimePlugin.processTexture(newTexId, texWidth, texHeight);
            return newTexId;
        }
    };

    private final GesturesDetector.GesturesListener mGesturesListener = new GesturesDetector.GesturesListener() {
        @Override
        public void onDetect(GesturesDetector.Gestures gestures) {
            Log.e("飞", gestures.name());
            ToastUtils.showShortToast(gestures.name());
            switch (gestures) {
                case RIGHT_WAVE:
                case LEFT:
                    mCurrentStickerIndex--;
                    if (mCurrentStickerIndex < 0) {
                        mCurrentStickerIndex = StickerDataUtils.selected_sticker_paths.size() - 1;
                    }
                    mSoundPool.play(mSwitchStickerSoundId, 1, 1, 0, 0, 1);
                    mSenseTimePlugin.setSticker(StickerDataUtils.selected_sticker_paths.get(mCurrentStickerIndex % StickerDataUtils.selected_sticker_paths.size()));
                    String tip = StickerDataUtils.sticker_trigger_actions.get(mCurrentStickerIndex % StickerDataUtils.selected_sticker_paths.size());
                    if (!tip.equals("")) {
                        ToastUtils.showShortToast(tip);
                    }
                    break;
                case LEFT_WAVE:
                case RIGHT:
                    mCurrentStickerIndex++;
                    mSoundPool.play(mSwitchStickerSoundId, 1, 1, 0, 0, 1);
                    mSenseTimePlugin.setSticker(StickerDataUtils.selected_sticker_paths.get(mCurrentStickerIndex % StickerDataUtils.selected_sticker_paths.size()));
                    tip = StickerDataUtils.sticker_trigger_actions.get(mCurrentStickerIndex % StickerDataUtils.selected_sticker_paths.size());
                    if (!tip.equals("")) {
                        ToastUtils.showShortToast(tip);
                    }
                    break;
                default:
                    break;
            }
            Log.e("飞", "当前贴纸地址下标: " + mCurrentStickerIndex);
            Log.e("飞", "当前贴纸: " + StickerDataUtils.selected_sticker_paths.get(mCurrentStickerIndex % StickerDataUtils.selected_sticker_paths.size()));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mShortVideoRecorder.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mShortVideoRecorder.pause();
    }
}
