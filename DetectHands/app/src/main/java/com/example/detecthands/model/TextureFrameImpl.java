package com.example.detecthands.model;

import com.google.mediapipe.framework.GlSyncToken;
import com.google.mediapipe.framework.TextureFrame;

public class TextureFrameImpl implements TextureFrame {
    private int mTextureId;
    private int mTexWidth;
    private int mTexHeight;
    private long mTimestamp;

    public TextureFrameImpl(int textureId, int texWidth, int texHeight, long timestamp) {
        mTextureId = textureId;
        mTexWidth = texWidth;
        mTexHeight = texHeight;
        mTimestamp = timestamp;
    }

    @Override
    public int getTextureName() {
        return mTextureId;
    }

    @Override
    public int getWidth() {
        return mTexWidth;
    }

    @Override
    public int getHeight() {
        return mTexHeight;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public void release() {

    }

    @Override
    public void release(GlSyncToken syncToken) {

    }
}
