package com.example.floatwindow;

import java.util.Calendar;
 
import com.gtss.music.MusicPlayerService;
import com.gtss.particle.ParticleView;
import com.umeng.analytics.MobclickAgent;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * 此处使用壁纸做背景的一个空白activity，让我之前service启动的悬浮框浮动在上面。实现类似与qq和微信的效果
 * */
public class LockScreenFloatActivity extends Activity {
	String TAG = "LockScreenFloatActivity";

	MusicPlayerService mMusicPlayerService = null;
	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder ibinder) {
			// TODO Auto-generated method stub
			Log.v(TAG, "onServiceConnected ");
			mMusicPlayerService = ((MusicPlayerService.LocalBinder) ibinder)
					.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
			Log.v(TAG, "onServiceDisconnected ");
			mMusicPlayerService = null;
		}
	};

	private void bindMusicService() {
		this.bindService(new Intent(this, MusicPlayerService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	// Animation mLoadAnimation;
	// ImageView mImage;
	// ///////////////////////////////////////////////////////////////////
 
	boolean mIs24 = false;
	boolean mSetDate = false;
	IntentFilter mIntentFilter = new IntentFilter();
	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			if (Intent.ACTION_TIME_TICK.equals(arg1.getAction())) {
				Calendar c = Calendar.getInstance();
 
			}
		}
	};

	// ////////////////////////////////////////////////////

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.v(TAG, "oncreate");
		MyApp.LockScreenFloatActivityRunning = true;
		requestWindowFeature(Window.FEATURE_NO_TITLE); // 不显示标题
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//
		// | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		ParticleView pv = new ParticleView(this);
		this.setContentView(pv);

		// this.setContentView(R.layout.lockscreen_bg_layout);
		// ParticleView lz = (ParticleView) findViewById(R.id.partice_view);
		// SeekBar sb = (SeekBar) findViewById(R.id.seekbar);
		// Button btn = (Button) findViewById(R.id.music_next);
		// btn.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		// });
		// 绑定音乐服务，播放歌曲
		bindMusicService();

		// this.setContentView(R.layout.lockscreen_bg_layout);
		// mImage = (ImageView) findViewById(R.id.bg_pet);
		//
		// mLoadAnimation =
		// AnimationUtils.loadAnimation(getApplicationContext(), R.anim.pet);
		//
		// mImage.setAnimation(mLoadAnimation); // 为控件设置动画
		// mLoadAnimation.setFillAfter(true); // 停留在结束位置
		// mLoadAnimation.setFillEnabled(true);
		// mLoadAnimation.startNow();

		mIntentFilter.addAction(Intent.ACTION_TIME_TICK);

		// /////////////////////////////////////////////////////
		Calendar c = Calendar.getInstance();

		ContentResolver cv = this.getContentResolver();
		String strTimeFormat = android.provider.Settings.System.getString(cv,
				android.provider.Settings.System.TIME_12_24);

		if (strTimeFormat.equals("24")) {
			mIs24 = true;
		}
 
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		MyApp.LockScreenFloatActivityRunning = false;
		Intent i = new Intent();
		i.setAction(MyApp.DESTROY_ACTION);
		this.sendBroadcast(i);
		// 此处彻底停止音乐播放服务。也可以不停止，使其在后台无限运行
		this.unbindService(mServiceConnection);
	}

}
