package com.example.floatwindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.example.floatwindow.FxService.TrafficDataTask.ProcessNetRate;
import com.umeng.analytics.MobclickAgent;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.os.IBinder;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 这是整个应用的核心，采用悬浮框的形式及时显示各个应用流量，负责后台逻辑计算，前台视图切换. ===>采用mWindowManager添加和移除视图得方式
 * ===>TrafficDataTask用来异步解析各个应用的网速 ===>mFloatLayout用来显示列表形式显示的网速布局
 * ===>mFloatPetLayout宠物布局，用另一个线程控制动画效果
 * ===>//http://blog.csdn.net/stevenhu_223/article/details/8504058 ===>//
 * 有service在后台运行的时候必须，杀死当前进程必须先停止服务，否则程序会自动重启
 * 
 * 这里有个bug，当系统运行足够多程序和网络状态之后（比如翻强），每次读取网络数据只会读取112行，导致无法检测到位于最后的程序网速。
 * 难道非要通过getUidRxByte()?
 * */
public class FxService extends Service {

	private static final String TAG = "FxService";
	final static boolean Debug = true;

	private IBinder mBinder = new FxService.LocalBinder();

	private class LocalBinder extends Binder {
		FxService getService() {
			return FxService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	// //////////////////////////////////////////////////////////////////////////////////
	// umeng start
	// //////////////////////////////////////////////////////////////////////////////////
	/**
	 * define some events to umeng:kill process
	 * */
	final static String UMENG_EVENT_KILL = "LongClickPetToKillProcessEvent";
	/**
	 * define some events to umeng:click from list to pet
	 * */
	final static String UMENG_EVENT_CONVERT_TO_LIST = "ClickToListViewFromPetEvent";
	/**
	 * define some events to umeng:click from pet to list
	 * */
	final static String UMENG_EVENT_CONVERT_TO_PET = "ClickToPetViewFromListEvent";
	/**
	 * define some events to umeng:touch to move pet
	 * */
	final static String UMENG_EVENT_MOVE_PET = "TouchMovePetEvent";
	/**
	 * define some events to umeng:touch to move list
	 * */
	final static String UMENG_EVENT_MOVE_LIST = "TouchMoveListViewEvent";
	// //////////////////////////////////////////////////////////////////////////////////
	// umeng eng
	// //////////////////////////////////////////////////////////////////////////////////

	IntentFilter mFilter = new IntentFilter();
	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context con, Intent intent) {
			// TODO Auto-generated method stub
			Log.v(TAG, "		onreceive " + intent.getAction());
			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {

				// mPetHatTextView.setVisibility(View.VISIBLE);
				if (!isInFloatListView) {
					// mPetHatTextView.setText("I Love Hat.");
				} else {
					mFloatInfoView.setText("Moses Loves List.");
				}
				Intent i = new Intent();
				i.setClass(con, LockScreenFloatActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				con.startActivity(i);
			} else if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
			} else if (MyApp.DESTROY_ACTION.equals(intent.getAction())) {
				// mPetHatTextView.setVisibility(View.GONE);
				if (!isInFloatListView) {

				} else {
					mFloatInfoView.setText(R.string.app_name);
				}
			}
		}
	};

	/**
	 * 是否停掉service中的task，因为当停掉整个应用的时候，服务还在后台跑，系统甚至会重启这个应用
	 * */
	boolean mStopTask = false;

	TrafficDataTask mTrafficDataTask;
	/**
	 * 记录当前正在使用的网速列表
	 * */
	List<ProcessNetRate> mRates;

	/**
	 * 当前网速
	 * */
	String mCurrentTxRate, mCurrentRxRate, mCurrentRate;
	TrafficDataFloatWindowAdapter mTrafficDataFloatWindowAdapter;
	Context mContext;
	/**
	 * 当网速大于20K的时候，我也比较关注这家伙
	 * */
	long THRESHOLD = (1 << 10) * 20;
	// 定义浮动窗口布局
	LinearLayout mFloatLayout, mFloatPetLayout;
	/**
	 * 位于pet头顶的显示
	 * */
	// TextView mPetHatTextView;
	WindowManager.LayoutParams wmParams;

	// 创建浮动窗口设置布局参数的对象
	WindowManager mWindowManager;
	/**
	 * 是否处于显示总流量界面
	 * */
	boolean isInFloatListView = true;
	TextView mFloatInfoView;
	/**
	 * 宠物头顶显示的网速
	 * */
	TextView mPetSummary;
	ListView mFloatListView;

	/**
	 * 通知刷新整个list
	 * */
	final int MSG_UPDATE_LIST = 0;
	/**
	 * 通知刷新宠物头顶的网速
	 * */
	final int MSG_UPDATE_SUMMARY = 1;
	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if (MSG_UPDATE_LIST == msg.what) {
				mTrafficDataFloatWindowAdapter.notifyDataSetChanged();
			} else if (MSG_UPDATE_SUMMARY == msg.what) {

				if (!isInFloatListView) {
					if (mPetSummary.getVisibility() == View.GONE) {
						mPetSummary.setVisibility(View.VISIBLE);
					}
					mPetSummary.setText(mCurrentRate);
				}
			}
		}

	};

	static {
		/**
		 * javah -classpath
		 * ~/Desktop/adt-bundle-linux-x86_64-20131030/sdk/platforms
		 * /android-19/android.jar:bin/classes/ -d jni
		 * com.example.floatwindow.FxService
		 * */
		// java只能读取111行，之后的读取不到，导致无法显示出来。
		// * 我觉得是上次读取效率太低的缘故，导致底层关闭重新刷新了。决定使用jni重写读写文件，参考
		// android_net_TrafficStats.cpp
		// * (frameworks\base\core\jni) 5566 2015-7-28
		System.loadLibrary("trafficstats");
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		mContext = this;
		init();

		createFloatView();
		createFloatPetView();
	}

	/**
	 * 获取系统总wifi发射流量
	 * */
	long getWifiTx() {
		// public static long getRxBytes(String iface)
		Class clazz = TrafficStats.class;
		Method m1 = null;
		long ret = 0;
		try {
			m1 = clazz.getDeclaredMethod("getTxBytes", String.class);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ret;
		}

		try {
			ret = (Long) m1.invoke(clazz.newInstance(), "wlan0");
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Log.v(TAG, "getWifiTx:" + ret);
		return ret;
	}

	/**
	 * 获取系统总wifi接收流量
	 * */
	long getWifiRx() {
		// public static long getRxBytes(String iface)
		Class clazz = TrafficStats.class;
		Method m1 = null;
		long ret = 0;
		try {
			m1 = clazz.getDeclaredMethod("getRxBytes", String.class);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ret;
		}

		try {
			ret = (Long) m1.invoke(clazz.newInstance(), "wlan0");
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Log.v(TAG, "getWifiRx:" + ret);
		return ret;
	}

	/**
	 * > reflected getRxBytes("rmnet0")!获取系统总移动接收流量
	 * */
	long getMobRx() {
		return TrafficStats.getMobileRxBytes();
	}

	/**
	 * > reflected getTxBytes("rmnet0")!获取系统总移动发送流量
	 * */
	long getMobTx() {
		return TrafficStats.getMobileTxBytes();
	}

	/**
	 * 获取系统总接收流量
	 * */
	long getTotalRx() {
		return TrafficStats.getTotalRxBytes();
	}

	/**
	 * 获取系统总发送流量
	 * */
	long getTotalTx() {
		return TrafficStats.getTotalTxBytes();
	}

	/**
	 * 获取uid的总接收流量
	 * */
	public long getUidRx(int uid) {
		return TrafficStats.getUidRxBytes(uid);
	}

	/**
	 * 获取uid的总发送流量
	 * */
	public long getUidTx(int uid) {
		return TrafficStats.getUidTxBytes(uid);
	}

	/**
	 * 获取uid总wifi接收流量
	 * */
	private static native long getUidWifiRx(int uid);

	/**
	 * 获取uid总wifi发送流量
	 * */
	private static native long getUidWifiTx(int uid);

	/**
	 * 获取uid总mobile发送流量
	 * */
	private static native long getUidMobTx(int uid);

	/**
	 * 获取uid总mobile接收流量
	 * */
	private static native long getUidMobRx(int uid);

	void init() {
		mFilter.addAction(Intent.ACTION_SCREEN_ON);
		mFilter.addAction(Intent.ACTION_SCREEN_OFF);
		mFilter.addAction(Intent.ACTION_TIME_TICK);
		mFilter.addAction(MyApp.DESTROY_ACTION);
		registerReceiver(mBroadcastReceiver, mFilter);

		ActivityManager am = (ActivityManager) this
				.getSystemService(Service.ACTIVITY_SERVICE);

	}

	/**
	 * 屏幕是否点亮，我已经使用广播监听器对其进行监听了
	 * */
	boolean isScreenOn() {
		PowerManager pm = (PowerManager) mContext
				.getSystemService(Service.POWER_SERVICE);
		return pm.isScreenOn();
	}

	/**
	 * 
	 * 屏幕是否上锁，包括紧急拨号，图案锁等
	 */
	boolean isScreenLocked() {
		KeyguardManager km = (KeyguardManager) mContext
				.getSystemService(Service.KEYGUARD_SERVICE);
		// "	,locked:" + km.isKeyguardLocked() + "		,secure:" +
		// km.isKeyguardSecure()
		Log.v(TAG, "	restricted:" + km.inKeyguardRestrictedInputMode());

		return km.inKeyguardRestrictedInputMode();
	}

	void createFloatPetView() {
		// 以屏幕左上角为原点，设置x、y初始值，相对于gravity
		wmParams.x = 20;
		wmParams.y = 20;

		mFloatPetLayout = (LinearLayout) LayoutInflater.from(
				getApplicationContext()).inflate(R.layout.pet, null, false);

		ImageView pet = (ImageView) mFloatPetLayout.findViewById(R.id.pet);
		// mPetHatTextView = (TextView) mFloatPetLayout.findViewById(R.id.hat);
		mPetSummary = (TextView) mFloatPetLayout.findViewById(R.id.summary);
		pet.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				MobclickAgent.onEvent(mContext, UMENG_EVENT_CONVERT_TO_LIST);
				isInFloatListView = true;
				mWindowManager.removeViewImmediate(mFloatPetLayout);
				mWindowManager.addView(mFloatLayout, wmParams);
			}
		});

		mFloatPetLayout.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				// TODO Auto-generated method stub
				Log.v(TAG,
						"long click to close Pet ,pid:"
								+ android.os.Process.myPid());
				MobclickAgent.onEvent(mContext, UMENG_EVENT_KILL);
				MobclickAgent.onKillProcess(mContext);

				// 有service在后台运行的时候必须，杀死当前进程必须先停止服务，否则程序会自动重启
				stopSelf();
				android.os.Process.killProcess(android.os.Process.myPid());
				return false;
			}
		});

		// 这个地方必须是对pet操作，对mFloatPetLayout不走这儿
		pet.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				MobclickAgent.onEvent(mContext, UMENG_EVENT_MOVE_PET);
				// getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标

				wmParams.x = (int) event.getRawX()
						- mFloatPetLayout.getMeasuredWidth() / 2;

				// 减25为状态栏的高度
				wmParams.y = (int) event.getRawY()
						- mFloatPetLayout.getMeasuredHeight() / 2 - 25;

				// 刷新
				mWindowManager.updateViewLayout(mFloatPetLayout, wmParams);
				return false; // 此处必须返回false，否则OnClickListener获取不到监听
			}
		});
	}

	private void createFloatView() {

		wmParams = new WindowManager.LayoutParams();
		// 获取的是WindowManagerImpl.CompatModeWrapper
		mWindowManager = (WindowManager) getApplication().getSystemService(
				getApplication().WINDOW_SERVICE);

		// Log.i(TAG, "mWindowManager--->" + mWindowManager);
		// 设置window type
		wmParams.type = LayoutParams.TYPE_PHONE;
		// 设置图片格式，效果为背景透明
		wmParams.format = PixelFormat.RGBA_8888;
		// 设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
		wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
		// 调整悬浮窗显示的停靠位置为左侧置顶
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
		// 以屏幕左上角为原点，设置x、y初始值，相对于gravity
		wmParams.x = 0;
		wmParams.y = 0;

		// 设置悬浮窗口长宽数据
		wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

		/*
		 * // 设置悬浮窗口长宽数据 wmParams.width = 200; wmParams.height = 80;
		 */

		LayoutInflater inflater = LayoutInflater.from(getApplication());
		// 获取浮动窗口视图所在布局
		mFloatLayout = (LinearLayout) inflater.inflate(R.layout.float_layout,
				null);

		// 添加mFloatLayout
		mWindowManager.addView(mFloatLayout, wmParams);

		// 浮动窗口按钮
		mFloatInfoView = (TextView) mFloatLayout.findViewById(R.id.float_info);
		mFloatListView = (ListView) mFloatLayout.findViewById(R.id.list);
		mTrafficDataFloatWindowAdapter = new TrafficDataFloatWindowAdapter();
		mFloatListView.setAdapter(mTrafficDataFloatWindowAdapter);

		// /!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		(mTrafficDataTask = new TrafficDataTask()).execute();

		mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
				View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
				.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		// Log.i(TAG, "Width/2--->" + mFloatInfoView.getMeasuredWidth() / 2);
		// Log.i(TAG, "Height/2--->" + mFloatInfoView.getMeasuredHeight() / 2);

		// 设置监听浮动窗口的触摸移动
		mFloatLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				// getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
				MobclickAgent.onEvent(mContext, UMENG_EVENT_MOVE_LIST);
				wmParams.x = (int) event.getRawX()
						- mFloatLayout.getMeasuredWidth() / 2;

				// 减25为状态栏的高度
				wmParams.y = (int) event.getRawY()
						- mFloatLayout.getMeasuredHeight() / 2 - 25;

				// 刷新
				mWindowManager.updateViewLayout(mFloatLayout, wmParams);
				return false; // 此处必须返回false，否则OnClickListener获取不到监听
			}
		});
		/**
		 * 点击总信息才切换视图，点击ListView进入详情界面
		 */
		mFloatInfoView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				MobclickAgent.onEvent(mContext, UMENG_EVENT_CONVERT_TO_PET);
				isInFloatListView = false;
				mWindowManager.removeViewImmediate(mFloatLayout);
				mWindowManager.addView(mFloatPetLayout, wmParams);
			}
		});
	}

	@Override
	public void onDestroy() {
		Log.v("a", "service destory");
		// TODO Auto-generated method stub
		super.onDestroy();
		uninit();
		mStopTask = true;
		if (mFloatLayout != null && isInFloatListView) {
			// 移除悬浮窗口
			mWindowManager.removeView(mFloatLayout);
		}
		if (mFloatPetLayout != null && !isInFloatListView) {

			mWindowManager.removeViewImmediate(mFloatPetLayout);
		}
	}

	void uninit() {
		this.unregisterReceiver(mBroadcastReceiver);
	}

	public String getDataSize(long size) {

		android.util.Log.v("aa", "40g:" + (1 << 40) + "    " + 1024 * 1024
				* 1024 * 1024 + "=========" + size);
		android.util.Log.v("aa", "30M:" + (1 << 30) + "   " + 1024 * 1024
				* 1024 + "=========" + size);
		java.text.NumberFormat formater = java.text.NumberFormat
				.getInstance(Locale.US);
		formater.setMaximumFractionDigits(1);
		if (size < 1024) {
			return size + "B";
		} else if (size < 1024 * 1024) {
			float kbsize = size / 1024f;
			return formater.format(kbsize) + "KB";
		} else if (size < 1024 * 1024 * 1024) {
			float mbsize = size / 1024f / 1024f;
			return formater.format(mbsize) + "MB";
		} else if (size < 1l << 40)/*
									 * ！！！！！！一定不是size < 1024 * 1024 * 1024 *
									 * 1024 看后面 ~~~~~
									 */{
			float gbsize = size / 1024f / 1024f / 1024f;
			return formater.format(gbsize) + "GN";
		} else {
			return "0B";
		}
	}

	/**
	 * long MUST !!
	 * */
	String convert(long data) {

		DecimalFormat df = new DecimalFormat("0.00");
		String ret = "0B";
		if (data >= 0 && data < (1l << 10)) {

		} else if (data < (1l << 20)) {
			ret = df.format(((float) data / (1l << 10))) + "K";
		} else if (data < (1l << 30)) {

			ret = df.format(((float) data / (1l << 20))) + "M";
		} else if (data < (1l << 40)) {

			ret = df.format(((float) data / (1l << 30))) + "G";
		}

		return ret;
	}

	public String getNetRate(long data) {

		DecimalFormat df = new DecimalFormat("0.00");

		String ret = "";
		if (true) {
			ret = df.format(((float) data / (1 << 10))) + "K/s";
		} else {
			if (data < 1 << 10) {
				ret = data + "B/s";
			} else if (data < 1 << 20) {
				ret = df.format(((float) data / (1 << 10))) + "K/s";
			} else if (data < 1 << 30) {
				ret = df.format(((float) data / (1 << 20))) + "M/s";
			} else if (data < 1 << 40) {
				ret = df.format(((float) data / (1 << 30))) + "G/s";
			}
		}
		return ret;
	}

	/**
	 * 存储来自于/proc/net/xt_qtaguid/stats的每个应用的流量信息。每一个对象代表一个节点，文件中的一行
	 * */
	class ProcessNetInfo {
		int uid;// uid
		int cnt_set;// 前台1,后台0
		String iface;// 接口名称rmnet0,wlan0
		long rx;// 接收流量
		long tx;// 发送流量
		/**
		 * 接收的移动数据流量。每一个对象代表一个节点，文件中的一行
		 * */
		long mobile_rx;
		/**
		 * 发送的移动数据流量。每一个对象代表一个节点，文件中的一行
		 * */
		long mobile_tx;

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return getAppNameByUid(uid) + "	" + uid + "	" + cnt_set + "		"
					+ iface + "	" + rx + "		" + tx;
		}

		@Override
		public boolean equals(Object o) {
			// TODO Auto-generated method stub
			if (null != o && (o instanceof ProcessNetInfo)) {
				return uid == ((ProcessNetInfo) o).uid;
			} else
				return false;
		}

		// String name;// 应用名称
		// String pkgName;// 包名
	};

	/**
	 * 
	 * 合并所有行，此链表返回所有进程的联网信息。每一个对象代表一个节点，文件中的一行.这个地方只能读取111行，之后的读取不到，导致无法显示出来。
	 * 我觉得是上次读取效率太低的缘故，导致底层关闭重新刷新了。决定使用jni重写读写文件，参考 android_net_TrafficStats.cpp
	 * (frameworks\base\core\jni) 5566 2015-7-28
	 * 
	 * 
	 * Lines with cnt_set==0 are for background data.
	 * 
	 * Lines with cnt_set==1 are for foreground data.
	 * 
	 * @return
	 * @throws IOException
	 */

	List<ProcessNetInfo> readUidQta() throws IOException {
		String QtaName = "/proc/net/xt_qtaguid/stats";
		List<ProcessNetInfo> mProcessNetInfoList = new ArrayList<ProcessNetInfo>();

		FileReader fr = new FileReader(QtaName);
		BufferedReader reader = new BufferedReader(fr);

		String line = null;
		int lineCount = 0;
		long rx_total = 0, tx_total = 0;

		while ((line = reader.readLine()) != null) {
			// if (Debug)
			// Log.v(TAG, "\n\n" + line);

			lineCount++;
			/**
			 * 排除第一行，并且第三列是0，第四列大于10000,
			 * */
			// && Integer.valueOf(line.split(" ")[3]).intValue() > 10000
			if (!line.contains("idx") && "0x0".equals(line.split(" ")[2])) {

				ProcessNetInfo pni = new ProcessNetInfo();
				pni.uid = Integer.valueOf(line.split(" ")[3]).intValue();
				pni.cnt_set = Integer.valueOf(line.split(" ")[4]).intValue();
				pni.iface = line.split(" ")[1];

				rx_total += Long.valueOf(line.split(" ")[5]).longValue();

				tx_total += Long.valueOf(line.split(" ")[7]).longValue();
				/**
				 * 对每个节点记录数据流量。移动数据暂时接口定义为rmnet0,wifi接口暂时定为wlan0。
				 * 此处如果使用了vpn或者翻墙软件会虚拟出一个tun0接口。得注意
				 * */
				if (line.split(" ")[1].equals("rmnet0")) {
					pni.mobile_rx = pni.rx = Long.valueOf(line.split(" ")[5])
							.longValue();
					pni.mobile_tx = pni.tx = Long.valueOf(line.split(" ")[7])
							.longValue();
				} else {
					pni.mobile_tx = 0;
					pni.mobile_rx = 0;
					pni.rx = Long.valueOf(line.split(" ")[5]).longValue();
					pni.tx = Long.valueOf(line.split(" ")[7]).longValue();
				}
				/*
				 * if (Debug) Log.v(TAG, "+++++++++++++++++++" +
				 * pni.toString());
				 */

				// 此处只看uid意味着会合并同一应用下的前后台和任意联网方式
				if (!mProcessNetInfoList.contains(pni)) {
					mProcessNetInfoList.add(pni);
				} else {
					int index = mProcessNetInfoList.indexOf(pni);
					mProcessNetInfoList.get(index).rx += pni.rx;
					// 此处，合并前后台流量，有2行
					mProcessNetInfoList.get(index).mobile_rx += pni.mobile_rx;
					mProcessNetInfoList.get(index).tx += pni.tx;
					mProcessNetInfoList.get(index).mobile_tx += pni.mobile_tx;
				}
				// (mProcessNetInfoList);

			}
		}
		if (Debug)
			Log.v(TAG, QtaName + "	$$$$$$$$$$$$$$$$$$$$$$$ " + lineCount);
		reader.close();
		fr.close();
		// Log.v(TAG, "readUidQta rx_total:" + rx_total + ",tx_total:" +
		// tx_total);
		// 排序
		// Collections.sort(mProcessNetInfoList, new
		// ComparatorAppNetSpeed());
		return mProcessNetInfoList;
	}

	/**
	 * to fetch in-time traffic-data
	 * */
	public class TrafficDataTask extends
			android.os.AsyncTask<Void, String, Void> {

		/**
		 * 合并了前台和后台、所有联网方式流量，记得每次使用前要清空
		 */
		List<ProcessNetInfo> last, now;
		String QtaName = "/proc/net/xt_qtaguid/stats";

		public TrafficDataTask() {

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			if (Debug)
				Log.v(TAG, "TrafficDataTask " + Thread.currentThread().getId()
						+ "		" + System.currentTimeMillis());
			while (true) {
				// 在finish activity后服务还在跑
				if (mStopTask)
					return null;
				// Log.v(TAG, "\n\n\n ");

				last = now = null;

				long rx_last = getTotalRx(), tx_last = getTotalTx();
				long rx_wifi_last = getWifiRx(), tx_wifi_last = getWifiTx();
				long rx_mob_last = getMobRx(), tx_mob_last = getMobTx();
				try {

					last = readUidQta();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				long rx_now = getTotalRx(), tx_now = getTotalTx();
				long rx_wifi_now = getWifiRx(), tx_wifi_now = getWifiTx();
				long rx_mob_now = getMobRx(), tx_mob_now = getMobTx();

				long rx_delta = (rx_now - rx_last), tx_delta = tx_now - tx_last;
				long rx_wifi_delta = rx_wifi_now - rx_wifi_last, tx_wifi_delta = tx_wifi_now
						- tx_wifi_last;
				long rx_mob_delta = rx_mob_now - rx_mob_last, tx_mob_delta = tx_mob_now
						- tx_mob_last;

				// Log.v(TAG, "RX -> " + rx_delta + ",TX -> " + tx_delta
				// + ",Total -> " + (rx_delta + tx_delta));

				try {
					// readTotalQta();

					now = readUidQta();

					// Log.v(TAG, "Dumping last !");

					mRates = getAllNetRate();

					mHandler.sendEmptyMessage(MSG_UPDATE_LIST);
					mHandler.sendEmptyMessage(MSG_UPDATE_SUMMARY);
					dumpAllRates(mRates);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// getWifiRx();
				// getWifiTx();
				mCurrentTxRate = getNetRate(tx_delta);
				mCurrentRxRate = getNetRate(rx_delta);
				mCurrentRate = getNetRate(rx_delta + tx_delta);
				// publishProgress(getNetRate(rx_delta),
				// getNetRate(rx_wifi_delta), getNetRate(rx_mob_delta),
				// getNetRate(tx_delta), getNetRate(tx_wifi_delta),
				// getNetRate(tx_mob_delta), getNetRate(rx_delta
				// + tx_delta));
			}
		}

		@Override
		protected void onProgressUpdate(String... values) {
			// TODO Auto-generated method stub
			// super.onProgressUpdate(values);
			// mFloatInfoView.setText("Up:" + values[3] + ",Down:" + values[0]
			// + ",Total:" + values[6]);
			// Log.v(TAG, "总下行速度：" + values[0] + ",总上行速度:" + values[3] +
			// ",总网速："
			// + values[6]);
		}

		private String intToIp(int ip) {

			return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "."
					+ ((ip >> 16) & 0xFF) + "." + (ip >> 24 & 0xFF);
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}

		/**
		 * to reflect to get private method:getMobileIfaces..........NOT OK
		 * */
		String[] getAllMobileIfaces() {
			Class c = TrafficStats.class;
			Method m;
			String[] ret = null;
			try {
				m = c.getDeclaredMethod("getMobileIfaces", null);
				m.invoke(c.newInstance(), null);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ret;
		}

		// /proc/net/xt_qtaguid/iface_stat_fmt
		//
		/**
		 * 这个接口也可以使用反射TrafficData,getRxBytes("wlan0"),其实它就是从底层文件读取出的
		 * */
		void readTotalQta() throws IOException {
			String name = "/proc/net/xt_qtaguid/iface_stat_fmt";
			// Log.v(TAG, "readTotalQta " + name);
			BufferedReader reader = new BufferedReader(new FileReader(name));
			String line = null;
			int lineCount = 0;
			while ((line = reader.readLine()) != null) {

				lineCount++;
				if (!(line.contains("ifname") /*
											 * || line.contains("lo") || line
											 * .contains("p2p0")
											 */)) {
					// Log.v(TAG, line);
					String ret = "ifname:" + line.split(" ")[0]
							+ ",total_skb_rx_bytes:" + line.split(" ")[1]
							+ ",total_skb_tx_bytes:" + line.split(" ")[3];
					Log.v(TAG, ret);
				}
			}
			reader.close();
		}

		/**
		 * 用来给应用列表排序
		 * */
		class ComparatorAppNetSpeed implements Comparator {

			@Override
			public int compare(Object arg0, Object arg1) {
				// TODO Auto-generated method stub

				int ret = 0;
				if ((((ProcessNetInfo) arg0).rx + ((ProcessNetInfo) arg0).tx) > (((ProcessNetInfo) arg0).tx + ((ProcessNetInfo) arg0).rx))
					ret = 1;
				else if ((((ProcessNetInfo) arg0).rx + ((ProcessNetInfo) arg0).tx) > (((ProcessNetInfo) arg0).tx + ((ProcessNetInfo) arg0).rx))
					ret = 0;
				else
					ret = -1;
				return ret;
			}

			@Override
			public String toString() {
				// TODO Auto-generated method stub
				return super.toString();
			}

		}

		/**
		 * 一个应用网速
		 * */
		class ProcessNetRate {
			int uid;
			/**
			 * 接收速率
			 * */
			long rx_rate;
			/**
			 * 发送速率
			 * */
			long tx_rate;
			/**
			 * 这个进程，从开机到现在为止的接收移动流量
			 * */
			long total_mobile_rx;
			/**
			 * 这个进程，从开机到现在为止的发送移动流量
			 * */
			long total_mobile_tx;
			/**
			 * 从开机到现在为止的流量积累
			 * */
			long total_heap;
			String appName;
			/**
			 * 是否是新产生的联网应用
			 * */
			boolean yong;

			// String pkgName;

			@Override
			public String toString() {
				// TODO Auto-generated method stub
				return "ProcessNetRate " + appName + " uid:" + uid
						+ ",rx_rate:" + rx_rate + ",tx_rate:" + tx_rate
						+ ",	single heap:" + total_heap + ",	yong:" + yong;
			}
		}

		/**
		 * 按理说，"/proc/net/xt_qtaguid/stats"只会产生新的行，比如新的链接方式、新的联网应用，即使中间曾断网过，
		 * 也一直记录在
		 * 以后一秒列表为基准，如果出现在前一秒列表，则取其差为网速（后一秒>=前一秒）;如果少的，则网速为0;如果多的则为新产生的联网应用
		 * */
		List<ProcessNetRate> getAllNetRate() {

			List<ProcessNetRate> ra = new ArrayList<ProcessNetRate>();
			for (ProcessNetInfo p_now : now) {
				/*
				 * if (Debug) Log.v(TAG, "		now:" + p_now.uid + "..." +
				 * getAppNameByUid(p_now.uid) + "...rx:" + p_now.rx);
				 */
				int count = 0;
				ProcessNetInfo p_last = null;
				// 如果在前一秒中找到了，并且后一秒大于前一秒，说明这个程序联网
				for (; count < last.size(); count++) {
					p_last = last.get(count);
					/*
					 * if (Debug) Log.v(TAG, "		last:" + p_last.uid + "..." +
					 * getAppNameByUid(p_last.uid) + "...rx:" + p_last.rx);
					 */
					if (p_last.equals(p_now)) {

						ProcessNetRate r = new ProcessNetRate();
						r.uid = p_last.uid;
						r.total_mobile_rx = p_now.mobile_rx;
						r.total_mobile_tx = p_now.mobile_tx;
						r.total_heap = (p_now.rx + p_now.tx);// now
																// instead
																// of

						r.rx_rate = (p_now.rx > p_last.rx) ? (p_now.rx - p_last.rx)
								: 0;
						r.tx_rate = (p_now.tx > p_last.tx) ? (p_now.tx - p_last.tx)
								: 0;
						r.yong = false;// if hitted

						if (null == getAppNameByUid(p_last.uid))
							r.appName = "system";
						else
							r.appName = getAppNameByUid(p_now.uid);
						if (r.rx_rate > 0 || r.tx_rate > 0) {
							if (Debug)
								Log.v(TAG, "		----adding " + r);
							ra.add(r);
						}
						break;//
					}
				}

				// 否则为新产生的联网应用
				if (count == last.size()) {
					if (null != getAppNameByUid(p_now.uid)) {
						if (Debug)
							Log.v(TAG, "		----新产生一个联网应用  "
									+ getAppNameByUid(p_now.uid) + "		"
									+ p_now.uid);

						final ProcessNetRate r = new ProcessNetRate();
						r.uid = p_now.uid;
						r.rx_rate = p_now.rx;
						r.tx_rate = p_now.tx;
						r.total_mobile_rx = p_now.mobile_rx;
						r.total_mobile_tx = p_now.mobile_tx;
						r.appName = getAppNameByUid(p_now.uid);
						r.total_heap = (p_now.rx + p_now.tx);// now instead
						r.yong = true;// if new one

						ra.add(r);
						/**
						 * 必须在主线程中显示，在service中activty必须有焦点才显示.“A toast can be
						 * created and displayed from an Activity or Service. If
						 * you create a toast notification from a Service,it
						 * appears in front of the Activity currently in focus.”
						 * */
						if (Debug) {
							Handler handler = new Handler(
									Looper.getMainLooper());
							handler.post(new Runnable() {
								public void run() {

									Log.v(TAG, "---toast to reminder "
											+ r.appName);

									Toast.makeText(
											getApplicationContext(),
											"新产生一个联网应用 :"
													+ getAppNameByUid(r.uid),
											Toast.LENGTH_SHORT).show();
								}
							});
						}
					}
				}
			}

			return ra;
		}

		void dumpAllRates(List<ProcessNetRate> ra) {
			if (null == ra)
				return;
			if (Debug) {
				Log.v(TAG, "=========");
				for (ProcessNetRate pnr : ra) {
					Log.v(TAG, pnr.appName + "	uid:" + pnr.uid + ", 下行速度rx:"
							+ getNetRate(pnr.rx_rate) + ",上行速度tx:"
							+ getNetRate(pnr.tx_rate) + ",	heap:"
							+ convert(pnr.total_heap) + ",	yong:" + pnr.yong);
				}
				Log.v(TAG, "=========");
			}
		}
	}

	public class TrafficDataFloatWindowAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if (null == mRates) {

				return 2;
			}

			return mRates.size() + 2;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int pos, View v, ViewGroup arg2) {

			/*
			 * if (pos >= getCount()) return v;
			 */
			// TODO Auto-generated method stub
			ViewHolder holder = null;
			if (v != null) {

				holder = (ViewHolder) v.getTag();
			} else {

				holder = new ViewHolder();

				v = LayoutInflater.from(mContext).inflate(
						R.layout.single_app_netinfo, null, false);
				holder.name = (TextView) v.findViewById(R.id.name);
				holder.up = (TextView) v.findViewById(R.id.up);
				holder.down = (TextView) v.findViewById(R.id.down);
				holder.total = (TextView) v.findViewById(R.id.total);
				holder.mobile = (TextView) v.findViewById(R.id.mobile);
				holder.heap = (TextView) v.findViewById(R.id.heap);

				v.setTag(holder);
			}
			// http://www.cnblogs.com/flappy/archive/2012/07/06/2579169.html
			if (0 == pos) {
				holder.name.setText("Name");
				holder.up.setText("\u2191");
				holder.down.setText("\u2193");
				holder.total.setText("\u2195");
				holder.mobile.setText("Mobile");
				holder.heap.setText("All");
			} else if (1 == pos) {
				holder.name.setText("All processes");
				holder.up.setText(mCurrentTxRate);
				holder.down.setText(mCurrentRxRate);
				holder.total.setText(mCurrentRate);
				// 从开机到现在使用的所有移动数据流量
				holder.mobile.setText(convert(getMobRx() + getMobTx()));
				// 从开机到现在所有的流量
				holder.heap.setText(convert(getTotalRx() + getTotalTx()));
			} else {
				// 如果是新产生的联网进程，设置颜色。否则恢复，因为之前可能被修改，这里又被重用。当网速大于20K的时候，我也比较关注这家伙
				if (mRates.get(pos - 2).yong
						|| (mRates.get(pos - 2).rx_rate + mRates.get(pos - 2).tx_rate) > THRESHOLD) {
					holder.name.setTextColor(Color.RED);
					holder.down.setTextColor(Color.RED);
					holder.up.setTextColor(Color.RED);
					holder.total.setTextColor(Color.RED);
					holder.mobile.setTextColor(Color.RED);
					holder.heap.setTextColor(Color.RED);
				} else {
					holder.name.setTextColor(Color.WHITE);
					holder.down.setTextColor(Color.WHITE);
					holder.up.setTextColor(Color.WHITE);
					holder.total.setTextColor(Color.WHITE);
					holder.mobile.setTextColor(Color.WHITE);
					holder.heap.setTextColor(Color.WHITE);
				}
				holder.name.setText(mRates.get(pos - 2).appName);
				holder.up.setText(getNetRate(mRates.get(pos - 2).tx_rate));
				holder.down.setText(getNetRate(mRates.get(pos - 2).rx_rate));
				holder.total.setText(getNetRate(mRates.get(pos - 2).rx_rate
						+ mRates.get(pos - 2).tx_rate));
				holder.mobile
						.setText(convert(mRates.get(pos - 2).total_mobile_rx
								+ mRates.get(pos - 2).total_mobile_tx));
				holder.heap.setText(convert(mRates.get(pos - 2).total_heap));
			}
			return v;
		}

		class ViewHolder {
			/**
			 * 当前进程的名称，上行网速，下行网速，网速，自上次开机到现在使用移动数据流量,自上次开机到现在使用流量
			 * */
			TextView name, up, down, total, mobile, heap;
		}
	}

	String getAppNameByUid(int uid) {
		List<ApplicationInfo> list = mContext.getPackageManager()
				.getInstalledApplications(0);

		for (ApplicationInfo i : list) {
			if (uid == i.uid)
				return (String) i.loadLabel(mContext.getPackageManager());
		}

		return null;
	}

}
