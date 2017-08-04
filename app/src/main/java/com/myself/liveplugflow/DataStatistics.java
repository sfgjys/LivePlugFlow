package com.myself.liveplugflow;


public class DataStatistics implements Runnable {
    private Thread mStatisticThread;
    private ReportListener mReportListener = null;
    private long mInterval;

    public DataStatistics(long interval) {
        mInterval = interval;
    }

    @Override
    public void run() {
        while (true) {// 不断循环更新性能日志
            try {
                if(mReportListener != null) {
                    mReportListener.onInfoReport();
                }
                Thread.sleep(mInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void setReportListener(ReportListener listener) {
        this.mReportListener = listener;
    }

    public void start() {
        mStatisticThread = new Thread(this);
        mStatisticThread.start();// 开启子线程
    }

    public void stop() {
        // 停止线程，先interrupt在interrupt
        mReportListener = null;
        mStatisticThread.interrupt();
        try {
            mStatisticThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface ReportListener {
        void onInfoReport();
    }
}
