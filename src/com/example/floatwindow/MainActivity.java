package com.example.floatwindow;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import com.umeng.analytics.AnalyticsConfig;
import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.onlineconfig.UmengOnlineConfigureListener;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * 这只是一个空壳，类似与开关一样，启动和销毁服务
 * 做一个烟花、喷泉表演粒子效果。每一束发射不同颜色的粒子。或者星星闪烁，通过通透度。加入一些手势检测，比如偏动、翻转手机，粒子也会受影响
 * 做一个节日定时播放歌曲，可以集成到内部，也可以播放在线歌曲。比如说生日、七夕、
 * */

public class MainActivity extends Activity {
	MyApp app;
	String TAG = "MainActivity";
	TelephonyManager mTelephonyManager;

	// //////////////////////////////////////////////////////////////////////////////////
	// umeng start
	// //////////////////////////////////////////////////////////////////////////////////
	/**
	 * umeng welcome title info from umeng server
	 * */
	final static String UMENG_WELCOME_TITLE = "WelcomeTitle";
	final static String UMENG_DEVICE_INFO = "DeviceInfo";

	// //////////////////////////////////////////////////////////////////////////////////
	// umeng end
	// //////////////////////////////////////////////////////////////////////////////////

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTelephonyManager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);// 获取当前手机管理器
		Log.v(TAG, new UmengDataPackage().toString());

		MobclickAgent.setDebugMode(true);

		/**
		 * online params
		 * */
		MobclickAgent.updateOnlineConfig(this);
		String value = MobclickAgent.getConfigParams(this, UMENG_WELCOME_TITLE);
		MobclickAgent
				.setOnlineConfigureListener(new UmengOnlineConfigureListener() {
					@Override
					public void onDataReceived(JSONObject data) {
						Log.v(TAG,
								"umeng...configure bg server to update params.");
					}
				});

		/** 设置是否对日志信息进行加密, 默认false(不加密). */
		AnalyticsConfig.enableEncrypt(false);

		app = (MyApp) getApplication();
		Intent intent = new Intent(MainActivity.this, FxService.class);

		if (!app.serviceRunning) {
			// 启动FxService
			startService(intent);
			app.serviceRunning = true;

		} else {
			// 参照service的destroy方法，有个flag用来停止while运行的task,否则即使service停止了，但是while一直在running
			stopService(intent);
			app.serviceRunning = false;
			// 有service在后台运行的时候必须，杀死当前进程必须先停止服务，否则程序会自动重启。
			// 但是这儿，我使用stopservice(intent)后，程序又自动重启了，貌似是停止的不是当前正在运行的服务。
			// android.os.Process.killProcess(android.os.Process.myPid());
			// System.exit(0);

			MobclickAgent.onKillProcess(this);
		}
		finish();

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		MobclickAgent.onResume(this);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("manufacture", Build.MANUFACTURER);
		map.put("imei", mTelephonyManager.getDeviceId());
		// MUST be after onResume
		MobclickAgent.onEvent(this, UMENG_DEVICE_INFO, map);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		MobclickAgent.onPause(this);
	}

	void getAllIface() {

		try {
			List<NetworkInterface> networkInterfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			StringBuilder sb = new StringBuilder();

			for (NetworkInterface networkInterface : networkInterfaces) {
				sb.append(" " + networkInterface.getDisplayName());
			}
			Log.v(TAG, "" + sb.toString());
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	void dump() {
		String name = "/proc/net/xt_qtaguid/stats";
		// Log.v(TAG, "readTotalQta " + name);
		FileReader fr = null;
		try {
			fr = new FileReader(name);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(fr);

		String line = null;

		try {
			while ((line = reader.readLine()) != null) {
				Log.v(TAG, "" + line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * IMEI国际移动设备识别码（IMEI：International Mobile Equipment Identification
	 * Number）是区别移动设备的标志，储存在移动设备中，可用于监控被窃或无效的移动设备。目前GSM和WCDMA手机终端需要使用IMEI号码
	 * */
	// String getImei() {
	//
	// return mTelephonyManager.getSubscriberId();
	// }

	/**
	 * IMSI国际移动用户识别码(IMSI International Mobile Subscriber Identification
	 * Number)国际上为唯一识别一个移动用户所分配的号码,是区别移动用户的标志，储存在SIM卡中，可用于区别移动用户的有效信息。
	 * */
	// String getImsi() {
	//
	// return mTelephonyManager.getDeviceId();
	// }

	/**
	 * 回传信息类，回传到服务器作统计,主要是用户设备信息，来自类Build。考虑打包成json数据
	 * */
	class UmengDataPackage {
		String board = Build.BOARD;
		String brand = Build.BRAND;
		String device = Build.DEVICE;
		String displayid = Build.DISPLAY;
		String fingerprint = Build.FINGERPRINT;
		String manufacture = Build.MANUFACTURER;
		String model = Build.MODEL;
		String sdk = Build.VERSION.SDK;
		String imei = mTelephonyManager.getDeviceId();
		String imsi = mTelephonyManager.getSubscriberId();
		String phonenumber = mTelephonyManager.getLine1Number();

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "UmengDataPackage:	board:" + Build.BOARD + ",brand:"
					+ Build.BRAND + ",device:" + Build.DEVICE + ",display:"
					+ Build.DISPLAY + ",fingerprint:" + Build.FINGERPRINT
					+ ",manufacture:" + Build.MANUFACTURER + ",model:"
					+ Build.MODEL + ",product:" + Build.PRODUCT + ",sdk:"
					+ Build.VERSION.SDK + ",imei:" + imei + ",imsi:" + imsi
					+ ",phone:" + phonenumber + ",sim operator:"
					+ mTelephonyManager.getSimOperator() + ",net operator:"
					+ mTelephonyManager.getNetworkOperator()/*
															 * +
															 * ",cell location:"
															 * +
															 * mTelephonyManager
															 * .
															 * getCellLocation()
															 * .toString()
															 */;
		}

	}
}
