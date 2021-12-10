package com.example.detecthands.sticker;

public enum StickerType {

    TYPE_STICKER_NEW("最新", "ff81fc70f6c111e899f602f2be7c2171"),
    TYPE_STICKER_2D("2D贴纸", "3cd2dae0f6c211e8877702f2beb67403"),
    TYPE_STICKER_3D("3D贴纸", "4e869010f6c211e888ea020d88863a42"),
    TYPE_STICKER_HANDLE("手势贴纸", "5aea6840f6c211e899f602f2be7c2171"),
    TYPE_STICKER_BG("背景分割", "65365cf0f6c211e8877702f2beb67403"),
    TYPE_STICKER_FACE("脸部变形", "6d036ef0f6c211e899f602f2be7c2171"),
    TYPE_STICKER_AVATAR("Avatar", "46028a20f6c211e888ea020d88863a42"),
    TYPE_STICKER_BEAUTY("美妆贴纸", "73bffb50f6c211e899f602f2be7c2171"),
    TYPE_STICKER_PARTICLE("粒子贴纸", "7c6089f0f6c211e8877702f2beb67403"),
    TYPE_STICKER_CAT("猫脸", "f101913d44fb42f2ad279a9b383062c8"),
    TYPE_STICKER_CARTOON("GAN", "2723987dcea34e2dafd6f83d9ff83e45"),
    TYPE_STICKER_BUCKLE("抠脸", "caa93c1160e2440eb8dbb4c9e42fa961"),
    TYPE_STICKER_PLAY("特效玩法", "837faa0485a7462b982d9709aa124b4f"),
    TYPE_STICKER_SHADOW("影分身", "1ae998ea4dc8489da76346df0daff8ca"),
    TYPE_STICKER_BIG_HEAD("大头特效", "36d0ec16b7684703a82bb59b0b0f7f4e");

    public String desc;
    public String groupId;

    StickerType(String desc,String groupId){
        this.desc = desc;
        this.groupId = groupId;
    }
}
