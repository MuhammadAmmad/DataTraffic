package com.gtss.particle;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class DrawThread extends Thread {
	ParticleView pv;
	SurfaceHolder suraceHolder;
	boolean isRunning;
	int sleepSpan = 15;
	long start = System.nanoTime();
	int count = 0;

	public DrawThread(ParticleView pv, SurfaceHolder suraceHolder) {
		this.isRunning = true;
		this.pv = pv;
		this.suraceHolder = suraceHolder;
	}

	@Override
	public void run() {
		Canvas canvas = null;
		while (isRunning) {
			try {
				canvas = suraceHolder.lockCanvas();
				synchronized (suraceHolder) {
					pv.doDraw(canvas);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (canvas != null) {
					suraceHolder.unlockCanvasAndPost(canvas);
				}
			}
			this.count++;
			if (count == 20) {
				count = 0;
				long tempStamp = System.nanoTime(); // 获取当前时间
				long span = tempStamp - start; // 获取时间间隔
				start = tempStamp; // 为start重新赋值
				double fps = Math.round(100000000000.0 / span * 20) / 100.0;// 计算帧速率
				pv.fps = "FPS:" + fps;// 将计算出的帧速率设置到BallView的相应字符串对象中

			}
			try {
				Thread.sleep(sleepSpan);// 线程休眠一段时间
			} catch (Exception e) {
				e.printStackTrace();// 捕获并打印异常
			}
		}
	}

}