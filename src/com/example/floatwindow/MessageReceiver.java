package com.example.floatwindow;

import java.util.List;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 这个用来开机自启动，但是发现暂时还起不来。在小米手机上，正好禁掉了这2个广播的接收
 * */
public class MessageReceiver extends BroadcastReceiver {

	static String TAG = "MessageReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.v(TAG, "MessageReceiver onReceive " + intent.getAction());
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
				|| intent.getAction().equals(
						"android.net.conn.CONNECTIVITY_CHANGE")
				|| Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())
				|| Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {

			context.startService(new Intent(context, FxService.class));
		}
	}

	/**
	 * 用来判断服务是否运行.com.example.floatwindow/.FxService
	 * 
	 * @param context
	 * @param className
	 *            判断的服务名字
	 * @return true 在运行 false 不在运行
	 */
	public static boolean isServiceRunning(Context mContext, String className) {
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager
				.getRunningServices(30);
		if (!(serviceList.size() > 0)) {
			return false;
		}
		for (int i = 0; i < serviceList.size(); i++) {
			Log.v(TAG, "" + serviceList.get(i).service.getClassName());
			if (serviceList.get(i).service.getClassName().equals(className) == true) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}
}
