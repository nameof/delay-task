package com.nameof.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.nameof.jedis.JedisUtil;
import com.nameof.timer.expire.ExpireTimer;
import com.nameof.timer.expire.RedisMsgPubSubListener;
import com.nameof.timer.expire.UniqueTask;

public class ExpireTimerTest {
	@Test
	@SuppressWarnings("serial")
    public void testTimer() throws Exception{  
		ExpireTimer timer = new ExpireTimer();
		final SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss:SSS");
		System.out.println(s.format(new Date()) + "提交了！");
		for (int i = 0; i < 3; i++) {
			final int id = i;
			timer.addTask(new UniqueTask("" + id) {
				
				@Override
				public void run() {
					System.out.println(s.format(new Date()) + "执行了！");
				}
			}, 5, TimeUnit.SECONDS);
		}
		
		timer.addTask(new UniqueTask("5") {
			
			@Override
			public void run() {
				System.out.println(s.format(new Date()) + "执行了！");
			}
		}, 100, TimeUnit.SECONDS);
		Thread.sleep(7000);
		System.out.println(timer.stop().size());
    } 
	
	@Test
	public void testPubSub() throws InterruptedException {
		JedisUtil.getJedis().subscribe(new RedisMsgPubSubListener(null), "__keyevent@0__:expired");
	}
}
