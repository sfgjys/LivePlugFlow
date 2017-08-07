package com.myself.liveplugflow.builder;

import android.content.Intent;

import com.myself.liveplugflow.ui.LiveCameraActivity;
import com.myself.liveplugflow.MyApplication;
import com.myself.liveplugflow.StringFlag;

public class PlugFlowConfigBuilder {

    private String rtmpUrl;
    private int videoResolution;
    private boolean isPortrait;
    private int cameraFacing;
    private String watermarkUrl;
    private int dx;
    private int dy;
    private int site;
    private int bestBitrate;
    private int minBitrate;
    private int maxBitrate;
    private int initBitrate;
    private int frameRate;

    public PlugFlowConfigBuilder rtmpUrl(String url) {
        this.rtmpUrl = url;
        return this;
    }

    public PlugFlowConfigBuilder videoResolution(int resolution) {
        this.videoResolution = resolution;
        return this;
    }

    public PlugFlowConfigBuilder portrait(boolean isPortrait) {
        this.isPortrait = isPortrait;
        return this;
    }

    public PlugFlowConfigBuilder cameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
        return this;
    }

    public PlugFlowConfigBuilder watermarkUrl(String url) {
        this.watermarkUrl = url;
        return this;
    }

    public PlugFlowConfigBuilder dx(int dx) {
        this.dx = dx;
        return this;
    }

    public PlugFlowConfigBuilder dy(int dy) {
        this.dy = dy;
        return this;
    }

    public PlugFlowConfigBuilder site(int site) {
        this.site = site;
        return this;
    }

    public PlugFlowConfigBuilder bestBitrate(int bestBitrate) {
        this.bestBitrate = bestBitrate;
        return this;
    }

    public PlugFlowConfigBuilder minBitrate(int minBitrate) {
        this.minBitrate = minBitrate;
        return this;
    }

    public PlugFlowConfigBuilder maxBitrate(int maxBitrate) {
        this.maxBitrate = maxBitrate;
        return this;
    }

    public PlugFlowConfigBuilder initBitrate(int initBitrate) {
        this.initBitrate = initBitrate;
        return this;
    }

    public PlugFlowConfigBuilder frameRate(int frameRate) {
        this.frameRate = frameRate;
        return this;
    }

    public Intent toIntent() {

        Intent intent = new Intent(MyApplication.getContext(), LiveCameraActivity.class);
        intent.putExtra(StringFlag.URL, rtmpUrl);
        intent.putExtra(StringFlag.VIDEO_RESOLUTION, videoResolution);
        intent.putExtra(StringFlag.SCREENORIENTATION, isPortrait);
        intent.putExtra(StringFlag.FRONT_CAMERA_FACING, cameraFacing);
        intent.putExtra(StringFlag.WATERMARK_PATH, watermarkUrl);
        intent.putExtra(StringFlag.WATERMARK_DX, dx);
        intent.putExtra(StringFlag.WATERMARK_DY, dy);
        intent.putExtra(StringFlag.WATERMARK_SITE, site);
        intent.putExtra(StringFlag.BEST_BITRATE, bestBitrate);
        intent.putExtra(StringFlag.MIN_BITRATE, minBitrate);
        intent.putExtra(StringFlag.MAX_BITRATE, maxBitrate);
        intent.putExtra(StringFlag.INIT_BITRATE, initBitrate);
        intent.putExtra(StringFlag.FRAME_RATE, frameRate);

        return intent;
    }
}
