package com.gtss.particle;

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * 做一个烟花、喷泉表演粒子效果。每一束发射不同颜色的粒子。或者星星闪烁，通过通透度。加入一些手势检测，比如偏动、翻转手机，粒子也会受影响
 * */
public class ParticleView extends SurfaceView implements SurfaceHolder.Callback {

	DrawThread dt;
	ParticleSet ps;

	ArrayList<ParticleThread> PtList = new ArrayList<ParticleThread>();
	String fps = "FPS:N/A";

	public ParticleView(Context context) {
		super(context);

		WindowManager wm = (WindowManager) context
				.getSystemService(Service.WINDOW_SERVICE);
		int w = wm.getDefaultDisplay().getWidth();
		int h = wm.getDefaultDisplay().getHeight();
		Log.v("a", "w:" + w + ",h:" + h);

		this.getHolder().addCallback(this);
		dt = new DrawThread(this, getHolder());
		ps = new ParticleSet();
		/**
		 * 粒子发射源坐标，运动区域（全屏, 粒子运动区域，从左上角开始计算）
		 * */
		PtList.add(new ParticleThread(this, w / 2, 300, w, h));
		// PtList.add(new ParticleThread(this, 2 * w / 3, 300, w, h));
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		if (dt == null) {
			dt = new DrawThread(this, getHolder());
		}
		if (!dt.isAlive()) {
			dt.start();
		}

		for (ParticleThread pt : PtList) {
			if (!pt.isAlive()) {
				pt.start();
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		dt.isRunning = false;
		dt = null;

		for (ParticleThread pt : PtList) {
			if (!pt.isAlive()) {
				pt.stop();
				pt.isRunning = false;
				pt = null;
			}
		}
	}

	public void doDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		canvas.drawColor(Color.BLACK);
		ArrayList<Particle> particleSet = ps.particleSet;
		Paint paint = new Paint();
		// 绘制粒子
		for (int i = 0; i < particleSet.size(); i++) {
			Particle p = particleSet.get(i);
			paint.setColor(p.color);
			int tempX = p.x;
			int tempY = p.y;
			int tempR = p.r;
			RectF oval = new RectF(tempX, tempY, tempX + 2 * tempR, tempY + 2
					* tempR);
			paint.setAlpha(0xff);
			canvas.drawOval(oval, paint);
		}
		paint.setColor(Color.WHITE);
		paint.setTextSize(18); // 字体大小
		paint.setAntiAlias(true); // 设置抗锯齿

		canvas.drawText(fps, 15, 15, paint);
	}
}
