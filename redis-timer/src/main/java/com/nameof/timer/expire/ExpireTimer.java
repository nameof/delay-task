package com.nameof.timer.expire;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.nameof.jedis.JedisUtil;

public class ExpireTimer implements TaskExpireListener{
	
	public static final String TASK_PREFIX = "delay:task:";
	
	private static final String PRESENT = "";
	
	private Map<String, UniqueTask> tasks = new ConcurrentHashMap<>();
	
	private Jedis jedis = null;
	
	/** 任务状态 */
	public static final int WORKER_STATE_INIT = 0;
	public static final int WORKER_STATE_STARTED = 1;
	public static final int WORKER_STATE_SHUTDOWN = 2;
	
	@SuppressWarnings({ "unused" })
	private volatile int workerState = WORKER_STATE_INIT; // 0 - init, 1 - started, 2 - shut down
	
	private static final AtomicIntegerFieldUpdater<ExpireTimer> WORKER_STATE_UPDATER =
	        AtomicIntegerFieldUpdater.newUpdater(ExpireTimer.class, "workerState");
	
	private Subscriber subThread = new Subscriber();
	
	public void addTask(UniqueTask task, int delay, TimeUnit unit) {
		start();
		String realId = TASK_PREFIX + task.getId();
		task.setDeadline(System.currentTimeMillis() + unit.toMillis(delay));
		jedis.setex(realId, (int)unit.toSeconds(delay), PRESENT);
		tasks.put(realId, task);
	}
	
	private void start() {
		 switch (WORKER_STATE_UPDATER.get(this)) {
	         case WORKER_STATE_INIT:
	             if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
	            	 subThread.start();
	            	 jedis = JedisUtil.getNonThreadJedis();
	             }
	             break;
	         case WORKER_STATE_STARTED:
	             break;
	         case WORKER_STATE_SHUTDOWN:
	             throw new IllegalStateException("cannot be started once stopped");
	         default:
	             throw new Error("Invalid WorkerState");
		 }
	}
	
	
	/** 停止并返回尚未被处理的任务 */
	public Collection<UniqueTask> stop() {
		if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
			jedis.close();//释放资源
			subThread.exit();
			return Collections.unmodifiableCollection(tasks.values());
		}
		return Collections.emptySet();
	}

	@Override
	public void process(String taskId) {
		UniqueTask task = tasks.remove(taskId);
		task.run();
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
