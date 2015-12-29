package com.gtss.particle;

import java.util.ArrayList;

import android.graphics.Color;

public class ParticleSet {

	public static final int PARTICLE_COUNT = 9999;// 不加以控制的话，无线循环会耗尽系统内存，形成一个无限长的链表
	ArrayList<Particle> particleSet;

	public ParticleSet() {
		particleSet = new ArrayList<Particle>();
	}

	/**
	 * 在(x,y)处发射color颜色的粒子
	 * */
	public void addParticle(int x, int y, double startTime) {
		for (int i = 0; i < 7; i++) {
			int r = 1;// 粒子半径
			double ver_v = -100 + 20 * (Math.random());// 垂直运行速度
			double hor_v = -15 + 30 * (Math.random());// 水平运行速度
			// if (particleSet.size() < PARTICLE_COUNT) {//
			// 不加以控制的话，无线循环会耗尽系统内存，形成一个无限长的链表
			// 要吗参考 ParticleThread中超过屏幕下边，释放粒子资源
			Particle particle = new Particle(getColor(i), r, ver_v, hor_v, x,
					y, startTime);
			particleSet.add(particle);
			// }
		}
	}

	public int getColor(int i) {
		int color = Color.RED;
		switch (i % 7) {
		case 0:
			color = Color.RED;
			break;
		case 1:
			color = Color.CYAN;

			break;
		case 2:
			color = Color.YELLOW;
			break;
		case 3:
			color = Color.GRAY;
			break;
		case 4:
			color = Color.GREEN;
			break;
		case 5:
			color = Color.BLUE;
			break;
		case 6:
			color = Color.WHITE;
		}

		return color;
	}
}
