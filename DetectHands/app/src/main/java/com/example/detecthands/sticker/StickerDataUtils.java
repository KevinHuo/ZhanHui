package com.example.detecthands.sticker;

import static com.example.detecthands.sticker.Constants.APPID;
import static com.example.detecthands.sticker.Constants.APPKEY;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_2D;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_3D;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_AVATAR;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_BEAUTY;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_BG;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_BIG_HEAD;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_BUCKLE;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_CARTOON;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_CAT;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_FACE;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_HANDLE;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_NEW;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_PARTICLE;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_PLAY;
import static com.example.detecthands.sticker.StickerType.TYPE_STICKER_SHADOW;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.example.detecthands.R;
import com.example.detecthands.utils.ContextHolder;
import com.example.detecthands.utils.ImageUtil;
import com.example.detecthands.utils.ToastUtils;
import com.sensetime.sensearsourcemanager.SenseArMaterial;
import com.sensetime.sensearsourcemanager.SenseArMaterialService;
import com.sensetime.sensearsourcemanager.SenseArMaterialType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StickerDataUtils {

    private static final List<StickerType> allStickerTypes = Arrays.asList(TYPE_STICKER_NEW, TYPE_STICKER_2D,
            TYPE_STICKER_3D, TYPE_STICKER_HANDLE,
            TYPE_STICKER_BG, TYPE_STICKER_FACE,
            TYPE_STICKER_AVATAR, TYPE_STICKER_BEAUTY,
            TYPE_STICKER_PARTICLE,
            TYPE_STICKER_CAT, TYPE_STICKER_BUCKLE,
            TYPE_STICKER_PLAY, TYPE_STICKER_SHADOW,
            TYPE_STICKER_BIG_HEAD, TYPE_STICKER_CARTOON);

    private static final List<String> selected_stickers = Arrays.asList(
            "20190128153619370478447", // 手势 ok
            "20190128153544436897273", // 手势 大拇哥
            "20190128214915096994874", // 手势 控雨
            "20201025223709561192827", // 2D 彩色猫耳
            "20201119103030628573263", // 2D 粉色脸颊
            "20161201192236340556464", // 2D 粉色猪猪
            "20200403165617680148039", // 3D 墨镜
            "20210401223823619551940", // 3D 金牛
            "20190128153524759899401", // 背景分割 爱心
            "20210914120231452344419", // 2D 猫趴头上
            "20210809171628514924985", // 2D 卡通恐龙
            "20201119103532637992896", // 2D 头上长草
            "20211207161028932438789", // 3D 卖萌
            "20190128153643480640749", // 手势 顶篮球
            "20210309142349632299619", // 3D 皇冠
            "20201119144738507426868"  // 背景分割 凤冠
    );

    public static final List<String> selected_sticker_paths = new ArrayList<>();
    public static final List<String> sticker_trigger_actions = new ArrayList<>();

    /**
     * 获取各贴纸类型下贴纸的图标下载路径和 id ,按贴纸类型写入到不同 json 文件中
     */
    public static void getStickerInfo() {
        SenseArMaterialService.shareInstance().authorizeWithAppId(ContextHolder.getContext(), APPID, APPKEY, new SenseArMaterialService.OnAuthorizedListener() {
            @Override
            public void onSuccess() {
                for (StickerType stickerType : allStickerTypes) {
                    SenseArMaterialService.shareInstance().fetchMaterialsFromGroupId("", stickerType.groupId, SenseArMaterialType.Effect, new SenseArMaterialService.FetchMaterialListener() {
                        @Override
                        public void onSuccess(final List<SenseArMaterial> materials) {
                            if (materials == null || materials.size() <= 0) {
                                return;
                            }
                            final ArrayList<StickerItem> stickerList = new ArrayList<>();
                            ArrayList<JSONObject> jsonArray = new ArrayList<>();
                            for (SenseArMaterial sarm : materials) {
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("id", sarm.id);
                                    jsonObject.put("icon", sarm.thumbnail);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                jsonArray.add(jsonObject);

                                Bitmap bitmap = null;
                                try {
                                    bitmap = ImageUtil.getImageSync(sarm.thumbnail, ContextHolder.getContext());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (bitmap == null) {
                                    bitmap = BitmapFactory.decodeResource(ContextHolder.getContext().getResources(), R.drawable.none);
                                }
                                String path = "";
                                //如果已经下载则传入路径地址
                                if (SenseArMaterialService.shareInstance().isMaterialDownloaded(ContextHolder.getContext(), sarm)) {
                                    path = SenseArMaterialService.shareInstance().getMaterialCachedPath(ContextHolder.getContext(), sarm);
                                }
                                StickerItem stickerItem = new StickerItem(sarm.name, bitmap, path);

                                //如果素材还未下载
                                if (stickerItem.state == StickerState.NORMAL_STATE) {
                                    stickerItem.state = StickerState.LOADING_STATE;
                                    SenseArMaterialService.shareInstance().downloadMaterial(ContextHolder.getContext(), sarm, new SenseArMaterialService.DownloadMaterialListener() {
                                        @Override
                                        public void onSuccess(final SenseArMaterial material) {
                                            Log.e("飞", String.format(Locale.getDefault(), "素材下载成功:%s,cached path is %s", material.materials, material.cachedPath));
                                            stickerItem.state = StickerState.DONE_STATE;
                                            stickerItem.path = material.cachedPath;
                                            stickerList.add(stickerItem);
                                        }

                                        @Override
                                        public void onFailure(SenseArMaterial material, final int code, String message) {
                                            Log.e("飞", String.format(Locale.getDefault(), "素材下载失败:%s", material.materials));
                                        }

                                        @Override
                                        public void onProgress(SenseArMaterial material, float progress, int size) {

                                        }
                                    });
                                } else if (stickerItem.state == StickerState.DONE_STATE) {
                                    stickerList.add(stickerItem);
                                    Log.e("飞", "onSuccess: " + stickerItem);
                                }
                            }

                            String savePath = ContextHolder.getContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + stickerType.name() + ".json";
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(savePath);
                                fileOutputStream.write(jsonArray.toString().getBytes());
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int code, String message) {
                            ToastUtils.showShortToast(String.format(Locale.getDefault(), "下载素材信息失败！%d, %s", code, TextUtils.isEmpty(message) ? "" : message));
                            Log.e("飞", "下载素材信息失败 onFailure: " + message);
                        }
                    });
                }
            }

            @Override
            public void onFailure(SenseArMaterialService.AuthorizeErrorCode errorCode, String errorMsg) {
                ToastUtils.showShortToast(String.format(Locale.getDefault(), "鉴权失败！%d, %s", errorCode, errorMsg));
                Log.e("飞", "鉴权失败 onFailure: " + errorMsg);
            }
        });
    }

    /**
     * 填充挑选贴纸 id 对应的地址列表
     */
    public static void getStickerPathList() {
        SenseArMaterialService.shareInstance().authorizeWithAppId(ContextHolder.getContext(), APPID, APPKEY, new SenseArMaterialService.OnAuthorizedListener() {
            @Override
            public void onSuccess() {
                for (StickerType stickerType : allStickerTypes) {
                    SenseArMaterialService.shareInstance().fetchMaterialsFromGroupId("", stickerType.groupId, SenseArMaterialType.Effect, new SenseArMaterialService.FetchMaterialListener() {
                        @Override
                        public void onSuccess(final List<SenseArMaterial> materials) {
                            if (materials == null || materials.size() <= 0) {
                                return;
                            }
                            for (SenseArMaterial sarm : materials) {
                                if (!selected_stickers.contains(sarm.id)) {
                                    continue;
                                }

                                String path = "";
                                //如果已经下载则传入路径地址
                                if (SenseArMaterialService.shareInstance().isMaterialDownloaded(ContextHolder.getContext(), sarm)) {
                                    path = SenseArMaterialService.shareInstance().getMaterialCachedPath(ContextHolder.getContext(), sarm);
                                    if (!selected_sticker_paths.contains(path)) {
                                        selected_sticker_paths.add(path);
                                        if (sarm.triggerActions != null) {
                                            sticker_trigger_actions.add(sarm.triggerActions[0].actionTip);
                                        } else {
                                            sticker_trigger_actions.add("");
                                        }
                                    }
                                    continue;
                                }

                                //如果素材还未下载
                                if (path.equals("")) {
                                    SenseArMaterialService.shareInstance().downloadMaterial(ContextHolder.getContext(), sarm, new SenseArMaterialService.DownloadMaterialListener() {
                                        @Override
                                        public void onSuccess(final SenseArMaterial material) {
                                            Log.e("飞", String.format(Locale.getDefault(), "素材下载成功:%s,cached path is %s", material.materials, material.cachedPath));
                                            if (!selected_sticker_paths.contains(material.cachedPath)) {
                                                selected_sticker_paths.add(material.cachedPath);
                                                if (sarm.triggerActions != null) {
                                                    sticker_trigger_actions.add(sarm.triggerActions[0].actionTip);
                                                } else {
                                                    sticker_trigger_actions.add("");
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailure(SenseArMaterial material, final int code, String message) {
                                            Log.e("飞", String.format(Locale.getDefault(), "素材下载失败:%s", material.materials));
                                        }

                                        @Override
                                        public void onProgress(SenseArMaterial material, float progress, int size) {

                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onFailure(int code, String message) {
                            ToastUtils.showShortToast(String.format(Locale.getDefault(), "下载素材信息失败！%d, %s", code, TextUtils.isEmpty(message) ? "" : message));
                            Log.e("飞", "下载素材信息失败 onFailure: " + message);
                        }
                    });
                }
            }

            @Override
            public void onFailure(SenseArMaterialService.AuthorizeErrorCode errorCode, String errorMsg) {
                ToastUtils.showShortToast(String.format(Locale.getDefault(), "鉴权失败！%d, %s", errorCode, errorMsg));
                Log.e("飞", "鉴权失败 onFailure: " + errorMsg);
            }
        });
    }
}
