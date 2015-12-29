package com.gtss.music;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IInterface;
import android.util.Log;
import android.widget.Toast;

public class MusicPlayerService extends Service {

	private String TAG = "";
	MediaPlayer mPlayer = new MediaPlayer();
	String[] Mp3Site = new String[] {
			"http://60.190.216.110:8880/accompany_onlinePlay_130115exut/memberaccompany/songfolder/2011/8/21/494d91777cb540399eaa4120fe6cf0d2.mp3",
			"http://60.190.216.110:8880/accompany_onlinePlay_130115exut/memberaccompany/songfolder/2011/8/21/93eb36ad7091435485ff4f7de9814a8d.mp3" };
	Context mContext;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		// Log.v(TAG, "onBind");
		return new LocalBinder();
	}

	public class LocalBinder extends Binder {
		public MusicPlayerService getService() {
			return MusicPlayerService.this;
		}

		@Override
		public void linkToDeath(DeathRecipient recipient, int flags) {
			// TODO Auto-generated method stub
			super.linkToDeath(recipient, flags);
		}
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		// Log.v(TAG, "onCreate");
		mContext = this.getApplicationContext();
		new PlayMusicThread(Mp3Site).start();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		// Log.v(TAG, "onUnbind");
		return super.onUnbind(intent);
	}

	private class PlayMusicThread extends Thread {
		String[] url;
		int index, count;

		public PlayMusicThread(String[] path) {
			url = path;
			count = path.length;
		}

		void initPlayer() {
			mPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					Log.v(TAG, "onCompletion");
					Toast.makeText(mContext, "onCompletion", Toast.LENGTH_SHORT)
							.show();
					// mPlayer.stop();
					// index++;
					// if (index < count) {
					// try {
					//
					// mPlayer.setDataSource(url[index]);
					// mPlayer.prepare();
					// } catch (Exception e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// mPlayer.release();
					// mPlayer = null;
					// }
					// mPlayer.start();
					// } else {
					// mPlayer.release();
					// mPlayer = null;
					//
					// }
				}
			});
			mPlayer.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					Log.v(TAG, "onPrepared");
				}
			});
			mPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

				@Override
				public void onBufferingUpdate(MediaPlayer arg0, int pos) {
					// TODO Auto-generated method stub
					Log.v(TAG, "onBufferingUpdate " + pos + " percent.");
				}
			});
			mPlayer.setOnErrorListener(new OnErrorListener() {

				@Override
				public boolean onError(MediaPlayer arg0, int what, int extra) {
					// TODO Auto-generated method stub
					Log.v(TAG, "onError error:" + what + ",extra:" + extra);
					return false;
				}
			});
			mPlayer.setOnInfoListener(new OnInfoListener() {

				@Override
				public boolean onInfo(MediaPlayer arg0, int what, int extra) {
					// TODO Auto-generated method stub
					Log.v(TAG, "onError error:" + what + ",extra:" + extra);
					return false;
				}
			});

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			// Log.v(TAG, "PlayMusicThread run...");
			try {
				mPlayer.setDataSource(url[index]);
				initPlayer();
				mPlayer.prepare();
				// mPlayer.setLooping(true);

				mPlayer.setScreenOnWhilePlaying(true);
				// mPlayer.setVolume(0.8f, 0.2f);// 左右声道百分比
				mPlayer.start();
			} catch (Exception e) {
				e.printStackTrace();
				mPlayer.release();
				mPlayer = null;
			}

		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mPlayer.release();
		mPlayer = null;
		// Log.v(TAG, "onDestroy");
	}

}
