// IMp3Aidl.aidl
package com.example.zhanyang.seekbarmp3;

// Declare any non-default types here with import statements

interface IMp3Aidl {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    /**
     *  开始播放
     */
    void start();
    //停止播放
    void stop();
    //暂停
    void pause();
    //进行拖拽到对应的进度
    void seekTo(in int progress);
    //进行判断是否已经启动
    boolean hasStart();
}
