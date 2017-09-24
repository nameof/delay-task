package com.nameof.timer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Client {

	public static void main(String[] args) {
		WheelTimer w = new WheelTimer();
		w.start();
		final SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss:SSS");
		for (int i = 0; i < 2; i++) {
			System.out.println(s.format(new Date()) + "提交任务！" );
			w.addTask(6, new Runnable() {
				
				@Override
				public void run() {
					System.out.println(s.format(new Date()) + "执行了！");
				}
			});
		}
	}
}
