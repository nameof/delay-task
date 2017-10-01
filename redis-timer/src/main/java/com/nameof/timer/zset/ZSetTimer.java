package com.nameof.timer.zset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import redis.clients.jedis.Jedis;

import com.nameof.jedis.JedisUtil;
import com.nameof.timer.Executor;
import com.nameof.timer.Task;

public class ZSetTimer {
	
	private byte[] queueName = "delay_task_queue".getBytes();
	
	private Jedis jedis = null;
	
	private Set<Task> unprocessedTasks = new HashSet<>();
	
	/** 任务状态 */
	public static final int WORKER_STATE_INIT = 0;
	public static final int WORKER_STATE_STARTED = 1;
	public static final int WORKER_STATE_SHUTDOWN = 2;
	
	@SuppressWarnings({ "unused" })
	private volatile int workerState = WORKER_STATE_INIT; // 0 - init, 1 - started, 2 - shut down
	
	private static final AtomicIntegerFieldUpdater<ZSetTimer> WORKER_STATE_UPDATER =
	        AtomicIntegerFieldUpdater.newUpdater(ZSetTimer.class, "workerState");
	
	private Thread workerThread = new Thread(new Worker());
	
	public void addTask(Task job, int delay, TimeUnit unit) {
		start();
		long score = System.currentTimeMillis() + unit.toMillis(delay);
		jedis.zadd(queueName, score, serialize(job));
	}
	
	private void start() {
		 switch (WORKER_STATE_UPDATER.get(this)) {
	         case WORKER_STATE_INIT:
	             if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
	                 workerThread.start();
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
	
	/** 停止时间轮，并返回*本地*尚未被执行的任务，不包含redis中剩余的任务 */
	public Collection<Task> stop() {
		if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
			jedis.close();//释放资源
			waitForWorkerTerminate();
			return unprocessedTasks;
		}
		return Collections.emptySet();
	}
	
	/** 等待工作线程结束，以便完成未执行Task的转移 */
	private void waitForWorkerTerminate() {
		
        while (workerThread.isAlive()) {
        	workerThread.interrupt();
            try {
            	workerThread.join(100);
            } catch (InterruptedException ignored) {}
        }
	}

	private static byte [] serialize(Object obj) {
    	if (obj == null) {
    		return null;
    	}
        ObjectOutputStream obi=null;
        ByteArrayOutputStream bai=null;
        try {
            bai=new ByteArrayOutputStream();
            obi=new ObjectOutputStream(bai);
            obi.writeObject(obj);
            byte[] byt=bai.toByteArray();
            return byt;
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
    
    private static Object deserizlize(byte[] byt) {
    	if (byt == null) {
    		return null;
    	}
        ObjectInputStream oii=null;
        ByteArrayInputStream bis=null;
        bis=new ByteArrayInputStream(byt);
        try {
            oii=new ObjectInputStream(bis);
            Object obj=oii.readObject();
            return obj;
        }
        catch (Exception e) {
        	e.printStackTrace();
		}
        return null;
    }

    private class Worker implements Runnable {

    	private Executor executor = new Executor();
    	
    	@Override
		public void run() {

    		boolean interrupted = false;
			executor.start();
			
			while (WORKER_STATE_UPDATER.get(ZSetTimer.this) == WORKER_STATE_STARTED) {
				double current = System.currentTimeMillis();
				Set<byte[]> set = JedisUtil.getJedis().zrangeByScore(queueName, 0, current);
				if (set != null) {
					List<Task> list = new ArrayList<>(set.size());
					for (byte[] b : set) {
						list.add((Task) deserizlize(b));
					}
					
					for (Task t : list) {
						if (t != null)
							executor.execute(t);
					}
					
					//TODO 任务执行成功，事务性细粒度删除，做到任务HA
					JedisUtil.getJedis().zremrangeByScore(queueName, 0, current);
				}
				
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
			
			JedisUtil.returnResource();
			
			interrupted = terminateExecutor() || interrupted;
			
			unprocessedTasks.addAll(executor.getUnprocessedTasks());
			
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
    	
		private boolean terminateExecutor() {
			boolean interrupted = false;
            while (executor.isAlive()) {
            	executor.interrupt();
                try {
                	executor.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            return interrupted;
		}
    }
}
