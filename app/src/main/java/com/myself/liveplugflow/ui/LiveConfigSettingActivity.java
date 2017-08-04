package com.myself.liveplugflow.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import com.alibaba.livecloud.live.AlivcMediaFormat;
import com.myself.liveplugflow.R;
import com.myself.liveplugflow.builder.PlugFlowConfigBuilder;

public class LiveConfigSettingActivity extends AppCompatActivity {

    private CheckBox frontCameraMirror;
    private RadioButton resolution240button;
    private RadioButton resolution360button;
    private RadioButton resolution480button;
    private RadioButton resolution540button;
    private RadioButton resolution720button;
    private RadioButton resolution1080button;
    private RadioButton screenOrientation1;
    private EditText urlET;
    private EditText mEtMaxBitrate;
    private EditText mEtMinBitrate;
    private EditText mEtBestBitrate;
    private EditText mEtInitialBitrate;
    private EditText mEtFrameRate;
    private EditText watermarkET;
    private EditText dxET;
    private EditText dyET;
    private EditText siteET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_config_setting);

        // 获取推流参数设置界面上的所有带有参数信息的控件
        Button connectBT = (Button) findViewById(R.id.connectBT);
        urlET = (EditText) findViewById(R.id.rtmpUrl);

        resolution240button = (RadioButton) findViewById(R.id.radiobutton0);
        resolution360button = (RadioButton) findViewById(R.id.radiobutton1);
        resolution480button = (RadioButton) findViewById(R.id.radiobutton2);
        resolution540button = (RadioButton) findViewById(R.id.radiobutton3);
        resolution720button = (RadioButton) findViewById(R.id.radiobutton4);
        resolution1080button = (RadioButton) findViewById(R.id.radiobutton5);
        screenOrientation1 = (RadioButton) findViewById(R.id.screenOrientation1);
        frontCameraMirror = (CheckBox) findViewById(R.id.front_camera_mirror);
        mEtBestBitrate = (EditText) findViewById(R.id.et_best_bitrate);
        mEtMaxBitrate = (EditText) findViewById(R.id.et_max_bitrate);
        mEtMinBitrate = (EditText) findViewById(R.id.et_min_bitrate);
        mEtInitialBitrate = (EditText) findViewById(R.id.et_init_bitrate);
        mEtFrameRate = (EditText) findViewById(R.id.et_frame_rate);
        watermarkET = (EditText) findViewById(R.id.watermark_path);
        dxET = (EditText) findViewById(R.id.dx);
        dyET = (EditText) findViewById(R.id.dy);
        siteET = (EditText) findViewById(R.id.site);

//        RadioGroup rotationGroup = (RadioGroup) findViewById(R.id.rotation_group);
//        RadioGroup resolutionCB = (RadioGroup) findViewById(R.id.resolution_group);
//        RadioButton screenOrientation2 = (RadioButton) findViewById(R.id.screenOrientation2);


        // 开始推流直播的按钮点击事件
        connectBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 分辨率,通过AlivcMediaFormat类的静态变量来设置
                int videoResolution = 0;
                if (resolution240button.isChecked()) {
                    videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_240P;
                } else if (resolution360button.isChecked()) {
                    videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_360P;
                } else if (resolution480button.isChecked()) {
                    videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_480P;
                } else if (resolution540button.isChecked()) {
                    videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_540P;
                } else if (resolution720button.isChecked()) {
                    videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_720P;
                } else if (resolution1080button.isChecked()) {
                    videoResolution = AlivcMediaFormat.OUTPUT_RESOLUTION_1080P;
                }

                // 摄像头的选择,通过AlivcMediaFormat类的静态变量来设置
                int cameraFrontFacing;
                if (frontCameraMirror.isChecked()) {
                    cameraFrontFacing = AlivcMediaFormat.CAMERA_FACING_FRONT;
                } else {
                    cameraFrontFacing = AlivcMediaFormat.CAMERA_FACING_BACK;
                }

                // 横竖屏选择，screenOrientation1控件是横屏的单选，所以screenOrientation为true则是横屏，screenOrientation为false则为竖屏
                boolean screenOrientation = screenOrientation1.isChecked();

                // 判断推流url是否为空
                if (TextUtils.isEmpty(urlET.getText())) {
                    Toast.makeText(view.getContext(), "Push url is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 水印图片在assets的位置
                String watermark = watermarkET.getText().toString();

                // 水印显示的位置参数，dx dy是坐标，site是上右,上左,下右,下左
                int dx = TextUtils.isEmpty(dxET.getText().toString()) ? 14 : Integer.parseInt(dxET.getText().toString());
                int dy = TextUtils.isEmpty(dyET.getText().toString()) ? 14 : Integer.parseInt(dyET.getText().toString());
                int site = TextUtils.isEmpty(siteET.getText().toString()) ? 1 : Integer.parseInt(siteET.getText().toString());

                // 帧率
                int frameRate = TextUtils.isEmpty(mEtFrameRate.getText().toString()) ?
                        30 : Integer.parseInt(mEtFrameRate.getText().toString());

                // 比特率
                int minBitrate = 500;
                int maxBitrate = 800;
                int bestBitrate = 600;
                int initBitrate = 600;
                try {
                    minBitrate = Integer.parseInt(mEtMinBitrate.getText().toString());
                } catch (NumberFormatException ignored) {
                }
                try {
                    maxBitrate = Integer.parseInt(mEtMaxBitrate.getText().toString());
                } catch (NumberFormatException ignored) {
                }
                try {
                    bestBitrate = Integer.parseInt(mEtBestBitrate.getText().toString());
                } catch (NumberFormatException ignored) {
                }
                try {
                    initBitrate = Integer.parseInt(mEtInitialBitrate.getText().toString());
                } catch (NumberFormatException ignored) {
                }

                Intent intent = new PlugFlowConfigBuilder()
                        .frameRate(frameRate)
                        .cameraFacing(cameraFrontFacing)
                        .dx(dx)
                        .dy(dy)
                        .site(site)
                        .rtmpUrl(urlET.getText().toString())
                        .videoResolution(videoResolution)
                        .portrait(screenOrientation)
                        .watermarkUrl(watermark)
                        .minBitrate(minBitrate)
                        .maxBitrate(maxBitrate)
                        .bestBitrate(bestBitrate)
                        .initBitrate(initBitrate).toIntent();

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);

            }
        });
    }
}
