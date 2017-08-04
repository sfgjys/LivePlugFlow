package com.myself.liveplugflow.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.alibaba.livecloud.event.AlivcEvent;
import com.alibaba.livecloud.event.AlivcEventResponse;
import com.alibaba.livecloud.event.AlivcEventSubscriber;
import com.alibaba.livecloud.live.AlivcMediaFormat;
import com.alibaba.livecloud.live.AlivcMediaRecorder;
import com.alibaba.livecloud.live.AlivcMediaRecorderFactory;
import com.alibaba.livecloud.live.AlivcRecordReporter;
import com.alibaba.livecloud.live.AlivcStatusCode;
import com.alibaba.livecloud.live.OnLiveRecordErrorListener;
import com.alibaba.livecloud.live.OnNetworkStatusListener;
import com.alibaba.livecloud.live.OnRecordStatusListener;
import com.alibaba.livecloud.model.AlivcWatermark;
import com.myself.liveplugflow.DataStatistics;
import com.myself.liveplugflow.R;
import com.myself.liveplugflow.StringFlag;
import com.myself.liveplugflow.dialog.AdvancedSettingDialog;

import java.util.HashMap;
import java.util.Map;

public class LiveCameraActivity extends FragmentActivity {

    private static final String TAG = "LiveCameraActivity";

    private boolean mHasPermission = false;// 是否获取了权限

    private AdvancedSettingDialog mSettingDialog;// 美颜选择对话框

    private AlivcMediaRecorder mMediaRecorder;// 推流器接口类AlivcMediaRecorder，提供推流控制

    private boolean isRecording = false; // 是否正在推流

    // 推流器可配参数类AlivcMediaFormat  在开始推流前需要调用prepare(Map params, Surface surface);其中的map参数的Key值指定即为AlivcMediaFormat的字段
    private Map<String, Object> mConfigure = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_live_camera);

        // 权限设置，最终结果通过mHasPermission参数来表现，为true说明所有权限通过，为false说明有权限没有通过
        if (Build.VERSION.SDK_INT >= 23) {// 版本是否大于等于6.0
            permissionCheck();
        } else {
            mHasPermission = true;
        }

        // 获取Intent中的参数
        getExtraIntentData();

        // 初始化控件
        initView();

        //  设置控件属性???????????????????????????????????????????????????????????????
        setViewAttribute();

        // 根据传递过来的screenOrientation参数来设定屏幕横竖
        setRequestedOrientation(screenOrientation ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 视频采集控件设置??????????????????????????????????????????????????????????????
        setSurfaceViewAttribute();

        // 创建推流器接口类AlivcMediaRecorder，提供推流控制
        mMediaRecorder = AlivcMediaRecorderFactory.createMediaRecorder();
        mMediaRecorder.init(this);

        mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);// 开启美颜

        // 设置日志的显示
        setReporterShow();

        // 设置mMediaRecorder推流器的各种回调监听
        setMediaRecorderListener();

        // 推流器可配参数类AlivcMediaFormat  在开始推流前需要调用prepare(Map params, Surface surface);其中的map参数的Key值指定即为AlivcMediaFormat的字段
        mConfigure.put(AlivcMediaFormat.KEY_CAMERA_FACING, cameraFrontFacing);
        mConfigure.put(AlivcMediaFormat.KEY_MAX_ZOOM_LEVEL, 3);// 最大缩放级别
        mConfigure.put(AlivcMediaFormat.KEY_OUTPUT_RESOLUTION, resolution);
        mConfigure.put(AlivcMediaFormat.KEY_MAX_VIDEO_BITRATE, maxBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_BEST_VIDEO_BITRATE, bestBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_MIN_VIDEO_BITRATE, minBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_INITIAL_VIDEO_BITRATE, initBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_DISPLAY_ROTATION, screenOrientation ? AlivcMediaFormat.DISPLAY_ROTATION_90 : AlivcMediaFormat.DISPLAY_ROTATION_0);
        mConfigure.put(AlivcMediaFormat.KEY_EXPOSURE_COMPENSATION, -1);//曝光度 设定范围：[0, 100] -1或者不设表示自动曝光
        mConfigure.put(AlivcMediaFormat.KEY_WATERMARK, mWatermark);
        mConfigure.put(AlivcMediaFormat.KEY_FRAME_RATE, frameRate);

        btn_switch_beauty.setChecked(false);// 关闭美颜
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
//        pushUrl = FileUtil.getSDCardPath()+ File.separator+sdf.format(new Date(System.currentTimeMillis()))+".flv";

        // 美颜级别对话框设置
        mSettingDialog = new AdvancedSettingDialog();
        // 设置对话框回调接口
        mSettingDialog.setAdvancedSettingListener(new AdvancedSettingDialog.AdvancedSettingListener() {
            @Override
            public void onAdvancedSettingChange(int event, Bundle bundle) {
                switch (event) {
                    case AdvancedSettingDialog.EVENT_BEAUTY_LEVEL_CHANGED:
                        //  选择的美颜级别
                        int beautyLevel = bundle.getInt(AdvancedSettingDialog.KEY_BEAUTY_LEVEL);
                        mMediaRecorder.setBeautyLevel(beautyLevel);
                        break;
                }
            }
        });
    }

    // **********************************************************进行权限请求***************************************************************

    // 需要进行权限请求的权限
    private static final String[] permissionManifest = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    // 权限申请后，回调时onRequestPermissionsResult作为一个限制参数
    private static final int PERMISSION_REQUEST_CODE = 1;

    // PERMISSION_GRANTED代表权限已经申请 PERMISSION_DENIED代表没有权限
    private void permissionCheck() {
        int isHavePermissionNotSuccess = PackageManager.PERMISSION_GRANTED;
        // 检查所有权限是否都申请成功
        for (String permission : permissionManifest) {
            if (PermissionChecker.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                isHavePermissionNotSuccess = PackageManager.PERMISSION_DENIED;
            }
        }

        if (isHavePermissionNotSuccess != PackageManager.PERMISSION_GRANTED) {
            // permissionManifest中有权限没有通过，所以要申请
            ActivityCompat.requestPermissions(this, permissionManifest, PERMISSION_REQUEST_CODE);
        } else {
            mHasPermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                boolean isApplyPermissionSuccess = true;
                // 遍历权限申请结果
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        int toastTip = 0;
                        if (Manifest.permission.CAMERA.equals(permissions[i])) {
                            toastTip = R.string.no_camera_permission;
                        } else if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {
                            toastTip = R.string.no_record_audio_permission;
                        }
                        if (toastTip != 0) {
                            Toast.makeText(this, toastTip, Toast.LENGTH_SHORT).show();
                            isApplyPermissionSuccess = false;
                        }
                    }
                }// 只要有一个权限没申请成功，isApplyPermissionSuccess就为false
                mHasPermission = isApplyPermissionSuccess;
                break;
        }
    }
    // ---------------------------------------------------------------------------------------------------------------------------------------

    // *************************************************获取上一个Activity传递过来的参数*************************************************************
    private String pushUrl;                   // 推流地址URL
    private int resolution;                  // 输出分辨率: AlivcMediaFormat.OUTPUT_RESOLUTION_240P; AlivcMediaFormat.OUTPUT_RESOLUTION_360P; ...........
    private boolean screenOrientation;      // 横屏；竖屏  还有一种解释：旋转角度值  AlivcMediaFormat.DISPLAY_ROTATION_0(竖屏)；AlivcMediaFormat.DISPLAY_ROTATION_90(横屏) ；AlivcMediaFormat.DISPLAY_ROTATION_180(倒竖屏)；AlivcMediaFormat.DISPLAY_ROTATION_270(倒横屏)
    private int cameraFrontFacing;          // 摄像头方向:前置或者后置
    private AlivcWatermark mWatermark;       // 水印信息对像
    private int bestBitrate;                // 最优码率？？？？？？？？？？？？？？？？？？？？？？？
    private int minBitrate;                 // 最小码率
    private int maxBitrate;                 // 最大码率
    private int initBitrate;                // 初始码率
    private int frameRate;                  // 帧率？？？？？？？？？？？？？？？？？？？？？？？

    private void getExtraIntentData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            pushUrl = bundle.getString(StringFlag.URL);
            resolution = bundle.getInt(StringFlag.VIDEO_RESOLUTION);
            screenOrientation = bundle.getBoolean(StringFlag.SCREENORIENTATION);
            cameraFrontFacing = bundle.getInt(StringFlag.FRONT_CAMERA_FACING);
            mWatermark = new AlivcWatermark.Builder()
                    .watermarkUrl(bundle.getString(StringFlag.WATERMARK_PATH))//水印图片地址
                    .paddingX(bundle.getInt(StringFlag.WATERMARK_DX)) //水印图片在x轴的偏移
                    .paddingY(bundle.getInt(StringFlag.WATERMARK_DY)) //水印图片在y轴的偏移
                    .site(bundle.getInt(StringFlag.WATERMARK_SITE)) //水印图片在屏幕上的位置
                    .build();
            minBitrate = bundle.getInt(StringFlag.MIN_BITRATE);
            maxBitrate = bundle.getInt(StringFlag.MAX_BITRATE);
            bestBitrate = bundle.getInt(StringFlag.BEST_BITRATE);
            initBitrate = bundle.getInt(StringFlag.INIT_BITRATE);
            frameRate = bundle.getInt(StringFlag.FRAME_RATE);
        }
    }
    // ------------------------------------------------------------------------------------------------------------

    // *******************************************************获取并初始化本界面的控件并********************************************************
    private ToggleButton btn_live_push;
    private ToggleButton toggle_camera;
    private ToggleButton btn_switch_beauty;
    private ToggleButton toggle_flash_light;
    private ToggleButton mTbtnMute;

    private TextView tv_video_capture_fps;
    private TextView tv_audio_encoder_fps;
    private TextView tv_video_encoder_fps;
    private TextView tv_output_bitrate;
    private TextView tv_av_output_diff;
    private TextView tv_audio_out_fps;
    private TextView tv_video_output_fps;
    private TextView tv_video_delay_duration;
    private TextView tv_audio_delay_duration;
    private TextView tv_video_cache_frame_cnt;
    private TextView tv_audio_cache_frame_cnt;
    private TextView tv_video_cache_byte_size;
    private TextView tv_audio_cache_byte_size;
    private TextView tv_video_frame_discard_cnt;
    private TextView tv_audio_frame_discard_cnt;
    private TextView tv_cur_video_bueaty_duration;
    private TextView tv_cur_video_encoder_duration;
    private TextView tv_cur_video_encode_birate;
    private TextView tv_video_output_frame_count;
    private TextView tv_video_data;
    private TextView tv_video_buffer_count;
    private TextView tv_audio_data;

    private Button mBtnAdavanceSetting;

    private void initView() {
        btn_live_push = findViewById(R.id.toggle_live_push);
        toggle_camera = findViewById(R.id.toggle_camera);
        mTbtnMute = findViewById(R.id.btn_mute);
        btn_switch_beauty = findViewById(R.id.btn_switch_beauty);
        toggle_flash_light = findViewById(R.id.toggle_flash_light);

        tv_video_capture_fps = findViewById(R.id.tv_video_capture_fps);
        tv_audio_encoder_fps = findViewById(R.id.tv_audio_encoder_fps);
        tv_video_encoder_fps = findViewById(R.id.tv_video_encoder_fps);
        tv_output_bitrate = findViewById(R.id.tv_output_bitrate);
        tv_av_output_diff = findViewById(R.id.tv_av_output_diff);
        tv_audio_out_fps = findViewById(R.id.tv_audio_out_fps);
        tv_video_output_fps = findViewById(R.id.tv_video_output_fps);
//        tv_stream_publish_time = (TextView) findViewById(R.id.tv_video_capture_fps);
//        tv_stream_server_ip = (TextView) findViewById(R.id.tv_video_capture_fps);
        tv_video_delay_duration = findViewById(R.id.tv_video_delay_duration);
        tv_audio_delay_duration = findViewById(R.id.tv_audio_delay_duration);
        tv_video_cache_frame_cnt = findViewById(R.id.tv_video_cache_frame_cnt);
        tv_audio_cache_frame_cnt = findViewById(R.id.tv_audio_cache_frame_cnt);
        tv_video_cache_byte_size = findViewById(R.id.tv_video_cache_byte_size);
        tv_audio_cache_byte_size = findViewById(R.id.tv_audio_cache_byte_size);
        tv_video_frame_discard_cnt = findViewById(R.id.tv_video_frame_discard_cnt);
        tv_audio_frame_discard_cnt = findViewById(R.id.tv_audio_frame_discard_cnt);
        tv_cur_video_bueaty_duration = findViewById(R.id.tv_cur_video_bueaty_duration);
        tv_cur_video_encoder_duration = findViewById(R.id.tv_cur_video_encoder_duration);
        tv_cur_video_encode_birate = findViewById(R.id.tv_video_encode_bitrate);
        tv_video_output_frame_count = findViewById(R.id.tv_video_output_frame_count);
        tv_video_data = findViewById(R.id.tv_video_data);
        tv_video_buffer_count = findViewById(R.id.tv_video_buffer_count);
        tv_audio_data = findViewById(R.id.tv_audio_data);

        mBtnAdavanceSetting = findViewById(R.id.btn_advance);
    }
    // --------------------------------------------------------------------------------------------------------------------------------------

    // ************************************************设置初始化的控件属性***************************************************

    // 是否开启闪光灯
    private final CompoundButton.OnCheckedChangeListener _SwitchFlashLightOnCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_FLASH_MODE_ON);// 开启闪光
            } else {
                mMediaRecorder.removeFlag(AlivcMediaFormat.FLAG_FLASH_MODE_ON);// 关闭
            }
        }
    };

    // 是否开启美颜
    private final CompoundButton.OnCheckedChangeListener _SwitchBeautyOnCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);// 开启美颜
            } else {
                mMediaRecorder.removeFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);// 关闭美颜
            }
        }
    };

    // 切换前后摄像头
    private final CompoundButton.OnCheckedChangeListener _CameraOnCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int currFacing = mMediaRecorder.switchCamera();// 调用switchCamera方法直接切换摄像头，前变后，后变前
            if (currFacing == AlivcMediaFormat.CAMERA_FACING_FRONT) {// 如果是前置就开启美颜
                mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
            }
            // 将前置的参数放入mConfigure中
            mConfigure.put(AlivcMediaFormat.KEY_CAMERA_FACING, currFacing);
        }
    };

    // 静音开启或者关闭
    private final CompoundButton.OnCheckedChangeListener mMuteCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_MUTE_ON);// 静音开启
            } else {
                mMediaRecorder.removeFlag(AlivcMediaFormat.FLAG_MUTE_ON);// 静音关闭
            }
        }
    };

    //  开始或者结束推流
    private final CompoundButton.OnCheckedChangeListener _PushOnCheckedChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                try {
                    // 在prepare完成之后调用startRecord进行推流。可以先进行是否prepare完成
                    mMediaRecorder.startRecord(pushUrl);
                } catch (Exception ignored) {
                }
                isRecording = true;// 正在推流
            } else {
                // 结束推流
                mMediaRecorder.stopRecord();
                isRecording = false;// 不在推流
            }
        }
    };

    private void setViewAttribute() {
        // 设置推流摄像头美颜闪关灯静音的开关闭接口
        btn_live_push.setOnCheckedChangeListener(_PushOnCheckedChange);
        toggle_camera.setOnCheckedChangeListener(_CameraOnCheckedChange);
        mTbtnMute.setOnCheckedChangeListener(mMuteCheckedChange);
        btn_switch_beauty.setOnCheckedChangeListener(_SwitchBeautyOnCheckedChange);
        toggle_flash_light.setOnCheckedChangeListener(_SwitchFlashLightOnCheckedChange);

        // 展示高级设置的美颜等级修改对话框
        mBtnAdavanceSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingDialog.show(getSupportFragmentManager(), "Settings");
            }
        });
    }
    // ---------------------------------------------------------------------------------------------------------------------------

    // *******************************************************视频采集控件设置******************************************************

    private SurfaceView _CameraSurface;
    private Surface mPreviewSurface;// 预览时需要

    private GestureDetector mDetector;
    private ScaleGestureDetector mScaleDetector;

    private int mPreviewWidth = 0;// 预览时的宽
    private int mPreviewHeight = 0;// 预览时的高

    private final SurfaceHolder.Callback _CameraSurfaceCallback = new SurfaceHolder.Callback() {
        // Surface创建时让屏幕保持开启，并开始预览
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            holder.setKeepScreenOn(true);// 让屏幕保持开启状态
            mPreviewSurface = holder.getSurface();// 获取对应的Surface
            startPreview();
        }

        // Surface改变时，重新设置推流器宽高
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mMediaRecorder.setPreviewSize(width, height);// 设置预览大小
            mPreviewWidth = width;
            mPreviewHeight = height;
        }

        // Surface 销毁时释放资源
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mPreviewSurface = null;

            //下面2句话取消就可以实现后台推流 但是部分手机不支持
            //mMediaRecorder.stopRecord();// 停止推流
            // mMediaRecorder.reset();// 释放资源
        }
    };

    // 在触屏Surface时拦截onTouch方法，让对焦和缩放来处理
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mDetector.onTouchEvent(motionEvent);
            mScaleDetector.onTouchEvent(motionEvent);
            return true;
        }
    };

    // 进行对焦处理
    private GestureDetector.OnGestureListener mGestureDetector = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override // 轻击一下屏幕，立刻抬起来，才会有这个触发
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            if (mPreviewWidth > 0 && mPreviewHeight > 0) {
                float x = motionEvent.getX() / mPreviewWidth;
                float y = motionEvent.getY() / mPreviewHeight;
                mMediaRecorder.focusing(x, y);// 对焦 focusing是废弃的方法
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }
    };

    private ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override // 缩放时
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mMediaRecorder.setZoom(scaleGestureDetector.getScaleFactor());// 进行缩放
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    };

    private void setSurfaceViewAttribute() {
        // 采集SurfaceView控件
        _CameraSurface = findViewById(R.id.camera_surface);
        _CameraSurface.getHolder().addCallback(_CameraSurfaceCallback);
        _CameraSurface.setOnTouchListener(mOnTouchListener);

        //对焦，缩放
        mDetector = new GestureDetector(_CameraSurface.getContext(), mGestureDetector);
        mScaleDetector = new ScaleGestureDetector(_CameraSurface.getContext(), mScaleGestureListener);
    }
    // --------------------------------------------------------------------------------------------------------------------------

    // **************************************************设置mMediaRecorder推流器的各种回调监听*******************************************

    // 推流的状态回调监听
    private OnRecordStatusListener mRecordStatusListener = new OnRecordStatusListener() {
        @Override // 摄像头打开成功
        public void onDeviceAttach() {
//            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_AUTO_FOCUS_ON);// 开启自动对焦
        }

        @Override // 摄像头打开失败
        public void onDeviceAttachFailed(int facing) {
        }

        @Override // 开启预览成功
        public void onSessionAttach() {
            // 此时isRecording为false，否则就是预览成功后就自动开启推流
            if (isRecording && !TextUtils.isEmpty(pushUrl)) {
                mMediaRecorder.startRecord(pushUrl);
            }
            mMediaRecorder.focusing(0.5f, 0.5f);// 手动对焦
        }

        @Override  // 停止预览
        public void onSessionDetach() {

        }

        @Override // 关闭摄像头
        public void onDeviceDetach() {

        }

        @Override // 非法输出分辨率
        public void onIllegalOutputResolution() {
            Log.d(TAG, "selected illegal output resolution");
            Toast.makeText(LiveCameraActivity.this, R.string.illegal_output_resolution, Toast.LENGTH_LONG).show();
        }
    };

    // 网络状态的回调监听
    private OnNetworkStatusListener mOnNetworkStatusListener = new OnNetworkStatusListener() {
        @Override // 网络较差时的回调，此时推流buffer为满的状态，会执行丢包，此时数据流不能正常推送
        public void onNetworkBusy() {
            Log.d("network_status", "==== on network busy ====");
            Toast.makeText(LiveCameraActivity.this, "当前网络状态极差，已无法正常流畅直播，确认要继续直播吗？", Toast.LENGTH_LONG).show();
        }

        @Override // 网络空闲状态，此时本地推流buffer不满，数据流可正常发送
        public void onNetworkFree() {
            Toast.makeText(LiveCameraActivity.this, "network free", Toast.LENGTH_LONG).show();
            Log.d("network_status", "===== on network free ====");
        }

        @Override // 链接状态改变
        public void onConnectionStatusChange(int status) {
            Log.d(TAG, "ffmpeg Live stream connection status-->" + status);

            switch (status) {
                case AlivcStatusCode.STATUS_CONNECTION_START:// 开始直播链接
                    Toast.makeText(LiveCameraActivity.this, "Start live stream connection!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Start live stream connection!");
                    break;
                case AlivcStatusCode.STATUS_CONNECTION_ESTABLISHED:// 直播链接建立成功
                    Log.d(TAG, "Live stream connection is established!");
//                    showIllegalArgumentDialog("链接成功");
                    Toast.makeText(LiveCameraActivity.this, "Live stream connection is established!", Toast.LENGTH_LONG).show();
                    break;
                case AlivcStatusCode.STATUS_CONNECTION_CLOSED:// 关闭直播链接
                    Log.d(TAG, "Live stream connection is closed!");
                    Toast.makeText(LiveCameraActivity.this, "Live stream connection is closed!", Toast.LENGTH_LONG).show();
//                    mLiveRecorder.stop();
//                    mLiveRecorder.release();
//                    mLiveRecorder = null;
//                    mMediaRecorder.stopRecord();
                    break;
            }
        }

        /**
         * 重连失败
         * @return false:停止重连 true:继续重连
         * 说明: ｓｄｋ检测到检测到需要重连时将会自动执行重连，直到重连成功或者重连尝试超时，
         * 超时时间可以通过{@link AlivcMediaFormat#KEY_RECONNECT_TIMEOUT}来设置，
         * 默认为５ｓ，超时后将触发此回调，若返回true表示继续开始新的一轮尝试，返回false，
         * 表示不再尝试
         */
        @Override
        public boolean onNetworkReconnectFailed() {
            Log.d(TAG, "Reconnect timeout, not adapt to living");
            Toast.makeText(LiveCameraActivity.this, "长时间重连失败，已不适合直播，请退出", Toast.LENGTH_LONG).show();
            mMediaRecorder.stopRecord();// 停止推流
            return false;
        }
    };

    // 推流错误的回调接口
    private OnLiveRecordErrorListener mOnErrorListener = new OnLiveRecordErrorListener() {
        @Override
        public void onError(int errorCode) {
            String hanYi;
            String keNengInformation;
            switch (errorCode) {
                case AlivcStatusCode.ERROR_BROKEN_PIPE:
                    hanYi = "管道中断";
                    keNengInformation = "推流时进行了违法操作，比如同时推流同一个地址，或者重复推流，服务器端会主动关闭socket，引起broken pipe";
                    break;
                case AlivcStatusCode.ERORR_OUT_OF_MEMORY:
                    hanYi = "内存不足";
                    keNengInformation = "手机内存不足时导致底层某些内存开辟失败引起该错误";
                    break;
                case AlivcStatusCode.ERROR_IO:
                    hanYi = "I/O错误";
                    keNengInformation = "导致该错误的情况比较多，比如网络环境较差或者推流域名错误等导致DNS解析失败等";
                    break;
                case AlivcStatusCode.ERROR_ILLEGAL_ARGUMENT:
                    hanYi = "参数非法";
                    keNengInformation = "该错误通常发生在帧数据错误的情况下";
                    break;
                case AlivcStatusCode.ERROR_NETWORK_UNREACHABLE:
                    hanYi = "网络不可达";
                    keNengInformation = "该错误通常发生在网络无法传输数据的情况，或者推流过程网络中断等情况";
                    break;
                case AlivcStatusCode.ERROR_SERVER_CLOSED_CONNECTION:
                    hanYi = "服务器关闭链接";
                    keNengInformation = "发生违法操作时，服务器会主动断开链接";
                    break;
                case AlivcStatusCode.ERROR_CONNECTION_TIMEOUT:
                    hanYi = "网络链接超时";
                    keNengInformation = "网络较差时导致链接超时或者数据发送超时";
                    break;
                case AlivcStatusCode.ERROR_AUTH_FAILED:
                    hanYi = "鉴权失败";
                    keNengInformation = "推流地址开启鉴权时，鉴权失败";
                    break;
                case AlivcStatusCode.ERROR_OPERATION_NOT_PERMITTED:
                    hanYi = "操作不允许";
                    keNengInformation = "发生违法操作时，服务器端主动断开链接导致";
                    break;
                case AlivcStatusCode.ERROR_CONNECTION_REFUSED:
                    hanYi = "服务器拒绝链接";
                    keNengInformation = "域名解析错误，或者其他异常导致无法链接服务器时";
                    break;
                default:
                    hanYi = "errorCode没有对应的";
                    keNengInformation = "errorCode没有对应的";
                    break;
            }
            Log.d(TAG, "直播链接错误代码-->" + errorCode + " 含义: " + hanYi + " 可能出现的情况: " + keNengInformation);
        }
    };

    private void setMediaRecorderListener() {
        mMediaRecorder.setOnRecordStatusListener(mRecordStatusListener);// 设置推流的状态回调监听
        mMediaRecorder.setOnNetworkStatusListener(mOnNetworkStatusListener);// 设置网络状态的回调监听
        mMediaRecorder.setOnRecordErrorListener(mOnErrorListener);// 设置推流错误回调
    }
    // ------------------------------------------------------------------------------------------------------------------------------------

    // *******************************************************开始预览********************************************************
    private void startPreview() {
        if (!mHasPermission) {
            // 刚进本界面的时候，还在申请权限，但是Surface已经创建并走到startPreview方法了，所以需要postDelayed延迟下
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startPreview();
                }
            }, 100);
            return;
        }
        // 权限通过，开始预览
        mMediaRecorder.prepare(mConfigure, mPreviewSurface);
        // 设置预览的大小（SurfaceView的大小）
        mMediaRecorder.setPreviewSize(_CameraSurface.getMeasuredWidth(), _CameraSurface.getMeasuredHeight());
        if ((int) mConfigure.get(AlivcMediaFormat.KEY_CAMERA_FACING) == AlivcMediaFormat.CAMERA_FACING_FRONT) {
            // 是前置摄像头
            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
        }
    }
    // ----------------------------------------------------------------------------------------------------------------------------------

    // ***********************************************展示性能日志******************************************************

    private AlivcRecordReporter mRecordReporter;// 获取性能日志的对象

    private DataStatistics mDataStatistics = new DataStatistics(1000);// 进行日志统计的Runnable对象

    DataStatistics.ReportListener mReportListener = new DataStatistics.ReportListener() {
        @Override
        public void onInfoReport() {
            // 在主线程中更新UI(显示性能日志)
            runOnUiThread(mLoggerReportRunnable);
        }
    };

    private Runnable mLoggerReportRunnable = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            if (mRecordReporter != null) {
                tv_video_capture_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_CAPTURE_FPS) + "fps");
                tv_audio_encoder_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_ENCODER_FPS) + "fps");
                tv_video_encoder_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_ENCODER_FPS) + "fps");

                // OUTPUT_BITRATE的单位是byte / s，所以转换成bps需要要乘8
                tv_output_bitrate.setText(mRecordReporter.getLong(AlivcRecordReporter.OUTPUT_BITRATE) * 8 + "bps");

                tv_av_output_diff.setText(mRecordReporter.getLong(AlivcRecordReporter.AV_OUTPUT_DIFF) + "microseconds");
                tv_audio_out_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_OUTPUT_FPS) + "fps");
                tv_video_output_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_OUTPUT_FPS) + "fps");
//                tv_stream_publish_time = (TextView) findViewById(R.id.tv_video_capture_fps);
//                tv_stream_server_ip = (TextView) findViewById(R.id.tv_video_capture_fps);
                tv_video_delay_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.VIDEO_DELAY_DURATION) + "microseconds");
                tv_audio_delay_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.AUDIO_DELAY_DURATION) + "microseconds");
                tv_video_cache_frame_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_CACHE_FRAME_CNT) + "");
                tv_audio_cache_frame_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_CACHE_FRAME_CNT) + "");
                tv_video_cache_byte_size.setText(mRecordReporter.getLong(AlivcRecordReporter.VIDEO_CACHE_BYTE_SIZE) + "byte");
                tv_audio_cache_byte_size.setText(mRecordReporter.getLong(AlivcRecordReporter.AUDIO_CACHE_BYTE_SIZE) + "byte");
                tv_video_frame_discard_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_FRAME_DISCARD_CNT) + "");
                tv_audio_frame_discard_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_FRAME_DISCARD_CNT) + "");
                tv_cur_video_bueaty_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.CUR_VIDEO_BEAUTY_DURATION) + "ms");
                tv_cur_video_encoder_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.CUR_VIDEO_ENCODER_DURATION) + "ms");
                tv_cur_video_encode_birate.setText(mRecordReporter.getInt(AlivcRecordReporter.CUR_VIDEO_ENCODE_BITRATE) * 8 + "bps");

                tv_video_output_frame_count.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_OUTPUT_FRAME_COUNT) + "");
                tv_video_data.setText(mRecordReporter.getLong(AlivcRecordReporter.VIDEO_OUTPUT_DATA_SIZE) + "");
                tv_video_buffer_count.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_BUFFER_COUNT) + "");
                tv_audio_data.setText(mRecordReporter.getLong(AlivcRecordReporter.AUDIO_OUTPUT_DATA_SIZE) + "");
            }
        }
    };

    private void setReporterShow() {
        mDataStatistics.setReportListener(mReportListener);// 性能日志统计

        // 只有经过mMediaRecorder.init(),才能调用getRecordReporter方法
        mRecordReporter = mMediaRecorder.getRecordReporter();// 日志

        mDataStatistics.start();// 开启循环的Runnable
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    // 屏幕改变后，如果在清单文件中设置了 android:configChanges 则会调用次方法
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override // 得
    protected void onResume() {
        super.onResume();
        if (mPreviewSurface != null) {
            mMediaRecorder.prepare(mConfigure, mPreviewSurface);
            Log.d("AlivcMediaRecorder", " onResume==== isRecording =" + isRecording + "=====");
        }
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_BITRATE_DOWN, mBitrateDownRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_BITRATE_RAISE, mBitrateUpRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_SUCC, mAudioCaptureSuccRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_DATA_DISCARD, mDataDiscardRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_INIT_DONE, mInitDoneRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_SUCC, mVideoEncoderSuccRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_FAILED, mVideoEncoderFailedRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_VIDEO_ENCODED_FRAMES_FAILED, mVideoEncodeFrameFailedRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_AUDIO_ENCODED_FRAMES_FAILED, mAudioEncodeFrameFailedRes));
        mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_FAILED, mAudioCaptureOpenFailedRes));
    }

    @Override // 失
    protected void onPause() {
      /*  if (isRecording) {
            mMediaRecorder.stopRecord();
        }*/
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_BITRATE_DOWN);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_BITRATE_RAISE);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_SUCC);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_DATA_DISCARD);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_INIT_DONE);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_SUCC);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_FAILED);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_VIDEO_ENCODED_FRAMES_FAILED);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_AUDIO_ENCODED_FRAMES_FAILED);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_FAILED);
        //  如果要调用stopRecord和reset()方法，则stopRecord（）必须在reset之前调用，否则将会抛出IllegalStateException
        //  mMediaRecorder.reset();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        RecordLoggerManager.closeLoggerFile();
        mDataStatistics.stop();
        mMediaRecorder.release();
    }

    // 码率上升调整事件
    private AlivcEventResponse mBitrateUpRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Bundle bundle = event.getBundle();
            int preBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_PRE_BITRATE);
            int currBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_CURR_BITRATE);
            Log.d(TAG, "event->up bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate);
            Toast.makeText(LiveCameraActivity.this, "event->up bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate, Toast.LENGTH_SHORT).show();
        }
    };
    // 码率下降调整事件
    private AlivcEventResponse mBitrateDownRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Bundle bundle = event.getBundle();
            int preBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_PRE_BITRATE);
            int currBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_CURR_BITRATE);
            Log.d(TAG, "event->down bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate);
            Toast.makeText(LiveCameraActivity.this, "event->down bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate, Toast.LENGTH_SHORT).show();
        }
    };
    // 音频设备打开成功
    private AlivcEventResponse mAudioCaptureSuccRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->audio recorder start success");
            Toast.makeText(LiveCameraActivity.this, "event->audio recorder start success", Toast.LENGTH_SHORT).show();
        }
    };
    // 视频编码设备打开成功
    private AlivcEventResponse mVideoEncoderSuccRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->video encoder start success");
            Toast.makeText(LiveCameraActivity.this, "event->video encoder start success", Toast.LENGTH_SHORT).show();
        }
    };
    // 视频编码设备打开失败
    private AlivcEventResponse mVideoEncoderFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->video encoder start failed");
            Toast.makeText(LiveCameraActivity.this, "event->video encoder start failed", Toast.LENGTH_SHORT).show();
        }
    };
    // 视频编码失败事件
    private AlivcEventResponse mVideoEncodeFrameFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->video encode frame failed");
            Toast.makeText(LiveCameraActivity.this, "event->video encode frame failed", Toast.LENGTH_SHORT).show();
        }
    };
    // 初始化成功
    private AlivcEventResponse mInitDoneRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->live recorder initialize completely");
            Toast.makeText(LiveCameraActivity.this, "event->live recorder initialize completely", Toast.LENGTH_SHORT).show();
        }
    };
    // 数据丢失事件-丢帧
    private AlivcEventResponse mDataDiscardRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Bundle bundle = event.getBundle();
            int discardFrames = 0;
            if (bundle != null) {
                discardFrames = bundle.getInt(AlivcEvent.EventBundleKey.KEY_DISCARD_FRAMES);
            }
            Log.d(TAG, "event->data discard, the frames num is " + discardFrames);
            Toast.makeText(LiveCameraActivity.this, "event->data discard, the frames num is " + discardFrames, Toast.LENGTH_SHORT).show();
        }
    };
    // 音频设备打开失败
    private AlivcEventResponse mAudioCaptureOpenFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event-> audio capture device open failed");
            Toast.makeText(LiveCameraActivity.this, "event-> audio capture device open failed", Toast.LENGTH_SHORT).show();
        }
    };
    // 音频编码失败事件
    private AlivcEventResponse mAudioEncodeFrameFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event-> audio encode frame failed");
            Toast.makeText(LiveCameraActivity.this, "event-> audio encode frame failed", Toast.LENGTH_SHORT).show();
        }
    };
//    public void testPublish(boolean isPublish, final String url) {
//        if(isPublish) {
//            mMediaRecorder.startRecord(url);
//            Log.d(TAG, "Start Record Time:" + System.currentTimeMillis());
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    testPublish(false, url);
//                }
//            }, 10000);
//        }else {
//            mMediaRecorder.stopRecord();
//            Log.d(TAG, "Stop Record Time:" + System.currentTimeMillis());
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    testPublish(true, url);
//                }
//            }, 500);
//        }
//    }
//    private Handler mHandler = new Handler();
}
