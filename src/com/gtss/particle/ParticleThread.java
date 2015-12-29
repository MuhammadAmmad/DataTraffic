package com.gtss.particle;

import java.util.ArrayList;

import android.graphics.Color;
import android.util.Log;

/**
 * 用来生产和绘制粒子的线程
 * */
public class ParticleThread extends Thread {

	String TAG = "ParticleThread";
	boolean isRunning;
	ParticleView father;
	int sleepSpan = 100;// 80;//每一次绘制的间歇时间，越大意味着绘制间隔越大，看上去越卡吨，排除帧率问题
	double time = 0;
	double span = 0.2;// 0.15;//粒子加速度一类的变化快慢程度
	int x, y;// 粒子颜色，发射源坐标
	int w, h;// 粒子运动区域，从左上角开始计算

	/**
	 * 在(x,y)处发射color颜色的粒子
	 * */
	public ParticleThread(ParticleView father, int x, int y, int w, int h) {
		this.isRunning = true;
		this.father = father;

		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	@Override
	public void run() {
		while (isRunning) {
			father.ps.addParticle(x, y, time);// 输入4种颜色
			ArrayList<Particle> tempSet = father.ps.particleSet;
			int count = tempSet.size();
			// Log.v(TAG, "thread " + this.getId() + ",particle size:" + count);
			for (int i = 0; i < count; i++) {

				// 这个地方会概率性发生FC
				Particle particle = tempSet.get(i);
				double timeSpan = time - particle.startTime;
				// 此处是模拟抛物线运动,timespan是运行时间
				// 计算X坐标
				int tempx = (int) (particle.startX + particle.hor_v * timeSpan);
				// 计算Y坐标
				int tempy = (int) (particle.startY + particle.ver_v * timeSpan + 4.9
						* timeSpan * timeSpan);
				// 超过屏幕下边，释放粒子资源，否则链表无限申请内存，程序会拖累死系统
				if (tempy > h) {
					tempSet.remove(particle);
					count = tempSet.size();
				}

				// 此处是计算粒子位置和半径大小，会导致没有向上的喷泉状了
				//particle.r = (int) (timeSpan / 8);//

				particle.x = tempx;
				particle.y = tempy;
			}
			time += span;
			try {
				Thread.sleep(sleepSpan);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		super.run();
	}
}
