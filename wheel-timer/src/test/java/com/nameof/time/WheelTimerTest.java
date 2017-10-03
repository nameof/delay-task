package com.nameof.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.nameof.timer.WheelTimer;

public class WheelTimerTest {
	
	private static final SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss:SSS");
	
	@Test
	public void testArgs() {
		WheelTimer w = new WheelTimer();
		w.addTask(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				System.out.println(s.format(new Date()) + "执行了！");
			}
		}, -1, TimeUnit.SECONDS);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}
	}

	@Test
	public void testStop() {
		WheelTimer w = new WheelTimer();
		
		for (int i = 0; i < 20; i++) {
			System.out.println(s.format(new Date()) + "提交任务！" );
			w.addTask(new Runnable() {
				
				@Override
				public void run() {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
					System.out.println(s.format(new Date()) + "执行了！");
				}
			}, 0, TimeUnit.SECONDS);
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}
		System.out.println(w.stop().size());
	}
	
	@Test
	public void testTimer() {
		WheelTimer w = new WheelTimer();
		for (int i = 0; i < 3; i++) {
			System.out.println(s.format(new Date()) + "提交任务！" );
			w.addTask(new Runnable() {
				
				@Override
				public void run() {
					System.out.println(s.format(new Date()) + "执行了！");
				}
			}, 5, TimeUnit.SECONDS);
		}
		
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testMillSecTimer() {
		WheelTimer w = new WheelTimer();
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < 3; i++) {
			final int delay = r.nextInt(1000) + 100;
			w.addTask(new Runnable() {
				
				private String jobName = s.format(new Date()) + "提交任务！延时"+ delay + "ms--";
				
				@Override
				public void run() {
					System.out.println(jobName + s.format(new Date()) + "执行了！");
				}
			}, delay, TimeUnit.MILLISECONDS);
		}
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
