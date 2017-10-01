package com.nameof.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.nameof.timer.Task;
import com.nameof.timer.zset.ZSetTimer;

public class ZSetTimerTest {
	@Test
	//FIXME something wrong ??
	public void testTimer() throws InterruptedException {
		ZSetTimer rt = new ZSetTimer();
		final SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss:SSS");
		for (int i = 0; i < 2; i++) {
			System.out.println(s.format(new Date()) + "提交任务！" );
			rt.addTask(new PrintTask(), 10, TimeUnit.SECONDS);
		}
		
		TimeUnit.SECONDS.sleep(12);
		
		rt.stop();
	}
}

class PrintTask extends Task {

	private static final long serialVersionUID = 1L;

	@Override
	public void run() {
		SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss:SSS");
		System.out.println(s.format(new Date()) + "执行了！");
	}
	
}
