package com.example.detecthands.activity;

import android.Manifest;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.detecthands.R;
import com.example.detecthands.model.TextureFrameImpl;
import com.example.detecthands.utils.FileUtils;
import com.example.detecthands.utils.GesturesDetector;
import com.example.detecthands.utils.SharedPreferencesUtils;
import com.example.detecthands.utils.TextureProcessor;
import com.example.detecthands.utils.ToastUtils;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;
import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLCameraParamSelectListener;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLShortVideoEnv;
import com.qiniu.pili.droid.shortvideo.PLShortVideoRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.sensetimeplugin.QNSenseTimePlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView mGlSurfaceView;
    private PLShortVideoRecorder mShortVideoRecorder;
    private Hands mHands;
    private GesturesDetector mGesturesDetector;

    private QNSenseTimePlugin mSenseTimePlugin;
    private TextureProcessor mMirrorProcessor;

    private TextView mTvFPS;
    private int mFPS;
    private long mLastUpdateFpsTime;

    private int mCurrentFilterIndex = 50;
    private final ArrayList<String> filterPaths = new ArrayList<>(200);
    private int mCurrentStickerIndex = 5;
    private final ArrayList<String> stickerPaths = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 777);
        }
        ToastUtils.init(this);

        initHandDetect();
        initEffect();
        initShortVideo();
        initEffectResource();

        mTvFPS = findViewById(R.id.textView);
    }

    private void initEffectResource() {
        if (!SharedPreferencesUtils.resourceReady(this)) {
            new Thread(() -> {
                Log.e("飞", "开始拷贝滤镜资源");
                FileUtils.copyFilterModelFiles(MainActivity.this);
                SharedPreferencesUtils.setResourceReady(this,true);
                initFilterPaths();
                Log.e("飞", "滤镜资源拷贝完毕！");
            }).start();
        } else {
            initFilterPaths();
        }

        String[] filePath = new String[0];
        try {
            filePath = getAssets().list("newEngine");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String stickerName : filePath) {
            stickerPaths.add("newEngine/" + stickerName);
        }
    }

    private void initFilterPaths() {
        File file = new File(getExternalFilesDir(null).getAbsolutePath() + File.separator + "filters");
        File[] subFiles = file.listFiles();
        for (int i = 0; i < subFiles.length; i++) {
            // 判断是否为文件夹
            if (!subFiles[i].isDirectory()) {
                String filename = subFiles[i].getAbsolutePath();
                // 判断是否为model结尾
                filterPaths.add(filename);
            }
        }
    }

    private void initHandDetect() {
        mHands = new Hands(this,
                HandsOptions.builder()
                        .setStaticImageMode(false)
                        .setMaxNumHands(2)
                        .setRunOnGpu(true)
                        .build());

        mHands.setErrorListener((message, e) -> Log.e("飞", "MediaPipe Hands error:" + message));

        mGesturesDetector = new GesturesDetector();
        mGesturesDetector.setGesturesListener(mGesturesListener);
        mHands.setResultListener(result -> mGesturesDetector.dealHandDetectResult(result));
    }

    private void initEffect() {
        mSenseTimePlugin = new QNSenseTimePlugin.Builder(this)
                .setLicenseAssetPath("SenseME.lic")
                .setModelActionAssetPath("M_SenseME_Face_Video_5.3.3.model")
                .setCatFaceModelAssetPath("M_SenseME_CatFace_3.0.0.model")
                .setDogFaceModelAssetPath("M_SenseME_DogFace_2.0.0.model")
                .build();
        boolean isAuthorized = mSenseTimePlugin.checkLicense();
        if (!isAuthorized) {
            ToastUtils.showShortToast("鉴权失败，请检查授权文件");
        }

        // 由于短视频 SDK 纹理回调的方向为竖直镜像的，所以需要一个竖直镜像操作将其转正
        mSenseTimePlugin.updateDirection(0, false, true);
        mMirrorProcessor = new TextureProcessor();

        mSenseTimePlugin.setFilterStrength(0.8f);
    }

    private void initShortVideo() {
        PLShortVideoEnv.init(this);

        mGlSurfaceView = findViewById(R.id.surfaceView);
        mGlSurfaceView.setEGLContextFactory(new GLSurfaceView.EGLContextFactory() {
            @Override
            public EGLContext createContext(EGL10 egl10, EGLDisplay eglDisplay, EGLConfig eglConfig) {
                int[] contextAttrs = new int[]{12440, mHands.getGlMajorVersion(), 12344};
                return egl10.eglCreateContext(eglDisplay, eglConfig, mHands.getGlContext(), contextAttrs);
            }

            @Override
            public void destroyContext(EGL10 egl10, EGLDisplay eglDisplay, EGLContext eglContext) {
                egl10.eglDestroyContext(eglDisplay, eglContext);
            }
        });

        mShortVideoRecorder = new PLShortVideoRecorder();

        PLCameraSetting cameraSetting = new PLCameraSetting();
        cameraSetting.setCameraId(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT);

        PLMicrophoneSetting microphoneSetting = new PLMicrophoneSetting();

        PLVideoEncodeSetting videoEncodeSetting = new PLVideoEncodeSetting(this);
        videoEncodeSetting.setEncodingSizeLevel(PLVideoEncodeSetting.VIDEO_ENCODING_SIZE_LEVEL.VIDEO_ENCODING_SIZE_LEVEL_1088P_1);

        PLAudioEncodeSetting audioEncodeSetting = new PLAudioEncodeSetting();

        PLRecordSetting recordSetting = new PLRecordSetting();

        mShortVideoRecorder.prepare(mGlSurfaceView, cameraSetting, microphoneSetting, videoEncodeSetting,
                audioEncodeSetting, null, recordSetting);

        mShortVideoRecorder.setCameraParamSelectListener(mCameraParamSelectListener);
        mShortVideoRecorder.setVideoFilterListener(mVideoFilterListener);
    }

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
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Segment_4.10.8.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseAR_Segment_MouthOcclusion_FastV1_1.1.1.model");
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
                    mTvFPS.setText(mFPS + "");
                    mFPS = 0;
                });
                mLastUpdateFpsTime = currentTime;
            }
            mFPS++;
//            int newTexId = mMirrorProcessor.draw(texId);
            int newTexId = texId;
            mHands.send(new TextureFrameImpl(newTexId, texWidth, texHeight, timestampNs));
            return mSenseTimePlugin.processTexture(newTexId, texWidth, texHeight);
        }
    };

    private final GesturesDetector.GesturesListener mGesturesListener = new GesturesDetector.GesturesListener() {
        @Override
        public void onDetect(GesturesDetector.Gestures gestures) {
            Log.e("飞", gestures.name());
            ToastUtils.showShortToast(gestures.name());
            switch (gestures) {
                case LEFT_HAND_RIGHT_WAVE:
                    mCurrentFilterIndex--;
                    if (mCurrentFilterIndex<0){
                        mCurrentFilterIndex = filterPaths.size()-1;
                    }
                    mSenseTimePlugin.setFilter(filterPaths.get(mCurrentFilterIndex));
                    break;
                case LEFT_HAND_LEFT_WAVE:
                    mCurrentFilterIndex++;
                    mSenseTimePlugin.setFilter(filterPaths.get(mCurrentFilterIndex % filterPaths.size()));
                    break;
                case RIGHT_HAND_LEFT_WAVE:
                    mCurrentStickerIndex--;
                    if (mCurrentStickerIndex<0){
                        mCurrentStickerIndex = stickerPaths.size()-1;
                    }
                    mSenseTimePlugin.setStickerFromAssetsFile(stickerPaths.get(mCurrentStickerIndex));
                    break;
                case RIGHT_HAND_RIGHT_WAVE:
                    mCurrentStickerIndex++;
                    mSenseTimePlugin.setStickerFromAssetsFile(stickerPaths.get(mCurrentStickerIndex % stickerPaths.size()));
                    break;
                default:
                    break;
            }
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
