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

/**
 * 基于redis过期时间实现的定时任务，通过注册__keyevent@0__:expired事件，来执行对应计时的任务.
 * 精度取决于redis的过期和通知机制，从 Redis 2.6 起，过期时间误差缩小到0-1毫秒.
 * @author chengpan
 */
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
		if (task != null) {
			try {
				task.run();
			} catch (Throwable e) {
				try {
					task.getExceptionHandler().handle(task, e);
				} catch (Throwable e1) {
					logger.error("exception from exceptionhandler", e1);
				}
			}
		}
	}
	
	private class Subscriber extends Thread {

		private JedisPubSub jps = new RedisMsgPubSubListener(ExpireTimer.this);

		private static final String EXPIRE_EVENT_NAME = "__keyevent@0__:expired";
		
		@Override
		public void run() {
			JedisUtil.getJedis().subscribe(jps, EXPIRE_EVENT_NAME);
            logger.debug("subscriber exit");
            JedisUtil.returnResource();
		}
		
		public void exit() {
			jps.unsubscribe(EXPIRE_EVENT_NAME);
		}
		
	}
}
