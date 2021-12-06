package com.example.detecthands.activity;

import android.Manifest;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.detecthands.R;
import com.example.detecthands.utils.FileUtils;
import com.example.detecthands.utils.GesturesDetector;
import com.example.detecthands.utils.SharedPreferencesUtils;
import com.example.detecthands.utils.TextureProcessor;
import com.example.detecthands.utils.ToastUtils;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.qiniu.sensetimeplugin.QNSenseTimePlugin;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class USBCameraActivity extends AppCompatActivity {

    private UVCCameraHelper mCameraHelper;
    private UVCCameraTextureView mUVCCameraView;
    private Hands mHands;
    private GesturesDetector mGesturesDetector;

    private QNSenseTimePlugin mSenseTimePlugin;
    private TextureProcessor mMirrorProcessor;

    private boolean isRequest;
    private boolean isPreview;

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
        setContentView(R.layout.activity_usb);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        ToastUtils.init(this);

        mTvFPS = findViewById(R.id.textView);

        initHandDetect();
        initUSBCamera();
        initEffect();
        initEffectResource();
        Log.e("飞", "onCreate: log 有效");
    }

    private void initEffectResource() {
        if (!SharedPreferencesUtils.resourceReady(this)) {
            new Thread(() -> {
                Log.e("飞", "开始拷贝滤镜资源");
                FileUtils.copyFilterModelFiles(USBCameraActivity.this);
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

        mSenseTimePlugin.updateDirection(0, false, false);
        mMirrorProcessor = new TextureProcessor();

        mSenseTimePlugin.setFilterStrength(0.8f);
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

    private void initUSBCamera() {
        mUVCCameraView = findViewById(R.id.camera_view);
        mUVCCameraView.setCallback(mCallback);
        mUVCCameraView.setEGLContext(mHands.getGlContext());
//        mUVCCameraView.setAspectRatio(1080 * 1.0f / 2259);

        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultPreviewSize(2560, 1440);
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }


    private CameraViewInterface.Callback mCallback = new CameraViewInterface.Callback() {
        @Override
        public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
            if (!isPreview && mCameraHelper.isCameraOpened()) {
                mCameraHelper.startPreview(mUVCCameraView);
                isPreview = true;
            }

            mSenseTimePlugin.init();
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Face_Extra_5.23.0.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Iris_2.0.0.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Hand_5.4.0.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseME_Segment_4.10.8.model");
            mSenseTimePlugin.addSubModelFromAssetsFile("M_SenseAR_Segment_MouthOcclusion_FastV1_1.1.1.model");
            mSenseTimePlugin.recoverEffects();

            mSenseTimePlugin.setStickerFromAssetsFile("newEngine/joker.zip");

            mMirrorProcessor.release();
        }

        @Override
        public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

        }

        @Override
        public int onDraw(int texId, int texWidth, int texHeight) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastUpdateFpsTime > 1000) {
                runOnUiThread(() -> {
                    mTvFPS.setText(mFPS + "");
                    mFPS = 0;
                });
                mLastUpdateFpsTime = currentTime;
            }
            mFPS++;
//            if (!mMirrorProcessor.isSetup()) {
//                mMirrorProcessor.setViewportSize(texWidth, texHeight);
//            }
//            int newTexId = mMirrorProcessor.draw(texId);
            int newTexId = texId;
//            mHands.send(new TextureFrameImpl(newTexId, texWidth, texHeight, System.currentTimeMillis()));
            return mSenseTimePlugin.processTexture(newTexId, 4016, 2259,true);
//            return texId;
//            return newTexId;
        }

        @Override
        public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
            if (isPreview && mCameraHelper.isCameraOpened()) {
                mCameraHelper.stopPreview();
                isPreview = false;
            }
            mSenseTimePlugin.destroy();
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

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            Log.e("飞", "onAttachDev " + device.getDeviceName());
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            Log.e("飞", "onDettachDev " + device.getDeviceName());
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                ToastUtils.showShortToast(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            Log.e("飞", "onConnectDev " + device.getDeviceName());
            if (!isConnected) {
                ToastUtils.showShortToast("连接失败，检查下分辨率参数");
                isPreview = false;
            } else {
                isPreview = true;
                ToastUtils.showShortToast("正在连接");
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            ToastUtils.showShortToast("disconnecting");
        }
    };

}
