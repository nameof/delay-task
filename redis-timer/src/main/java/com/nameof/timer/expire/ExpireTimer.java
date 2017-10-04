package com.nameof.timer.expire;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.nameof.jedis.JedisUtil;
import com.nameof.timer.AbstractTimer;
import com.nameof.timer.Task;

public class ExpireTimer extends AbstractTimer implements TaskExpireListener{
	
	public static final String TASK_PREFIX = "delay:task:";
	
	private static final String PRESENT = "";
	
	private Map<String, Task> tasks = new ConcurrentHashMap<>();
	
	private Jedis jedis = null;
	
	private Subscriber subThread = new Subscriber();
	
	public void doAddTask(Task task, int delay, TimeUnit unit) {
		String realId = TASK_PREFIX + task.getId();
		task.setDeadline(System.currentTimeMillis() + unit.toMillis(delay));
		jedis.setex(realId, (int)unit.toSeconds(delay), PRESENT);
		tasks.put(realId, task);
	}
	
	protected void onStart() {
    	 subThread.start();
    	 jedis = JedisUtil.getNonThreadJedis();
	}
	
	
	/** 停止并返回尚未被处理的任务 */
	protected Collection<Task> onStop() {
		jedis.close();//释放资源
		subThread.exit();
		return Collections.unmodifiableCollection(tasks.values());
	}

	@Override
	public void process(String taskId) {
		Task task = tasks.remove(taskId);
		try {
			task.run();
		} catch (Throwable e) {
			// TODO: logger
			System.out.println(e);
		}
	}
	
	private class Subscriber extends Thread {

		private JedisPubSub jps = new RedisMsgPubSubListener(ExpireTimer.this);
		
		@Override
		public void run() {
            JedisUtil.getJedis().subscribe(jps, "__keyevent@0__:expired");
            System.out.println("exit");
            JedisUtil.returnResource();
		}
		
		public void exit() {
			jps.unsubscribe("__keyevent@0__:expired");
		}
		
	}
}
