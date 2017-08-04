package com.myself.liveplugflow;

import android.app.Application;
import android.content.Context;

import com.duanqu.qupai.jni.ApplicationGlue;


public class MyApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        // 加载阿里云推流 .so 库
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("qupai-media-jni");
        System.loadLibrary("qupai-media-thirdparty");

        // 阿里云推流 jar 包中的方法，具体是调用了native方法（nativeInitialize）
        ApplicationGlue.initialize(this);

        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
