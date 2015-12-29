package com.example.floatwindow;

import android.app.Application;
import android.util.Log;

/**
 * 无他，唯保存全局变量而已。谁让它得生命周期最长呢，被共享呢
 * */
public class MyApp extends Application {
	/**
	 * 流量监控服务是否还在运行
	 * */
	public boolean serviceRunning = false;
	/**
	 * 我们的锁屏界面activity是否还在。有可能用户没有设置锁屏。
	 */
	public static boolean LockScreenFloatActivityRunning = false;
	/**
	 * 销毁LockScreenFloatActivity的广播
	 * */
	public final static String DESTROY_ACTION = "unlockmine";
}
