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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import com.nameof.jedis.JedisUtil;
import com.nameof.timer.AbstractTimer;
import com.nameof.timer.Executor;
import com.nameof.timer.Task;
/**
 * 使用redis的排序集合实现的延时任务，集合元素的值为任务，分数为任务的过期unix时间戳.
 * 所以{@link ZSetTimer}要做的就是每次以当前时间戳去轮询redis，取出分数小于当前时间戳的集合元素，执行任务.
 * 定时精度取决于轮询的时间间隔，当然我们也会因为CAP原则在频繁轮询中做出权衡.
 * @author chengpan
 */
public class ZSetTimer extends AbstractTimer {
	
	private byte[] queueName = "delay_task_queue".getBytes();
	
	private Jedis jedis = null;
	
	private Set<Task> unprocessedTasks = new HashSet<>();
	
	private Thread workerThread = new Thread(new Worker());
	
	public void doAddTask(Task job, int delay, TimeUnit unit) {
		long score = System.currentTimeMillis() + unit.toMillis(delay);
		job.setDeadline(score);
		jedis.zadd(queueName, score, serialize(job));
	}
	
	protected void onStart() {
         workerThread.start();
         jedis = JedisUtil.getNonThreadJedis();
	}
	
	/** 停止时间轮，并返回*本地*尚未被执行的任务，不包含redis中剩余的任务 */
	public Collection<Task> onStop() {
		jedis.close();//释放资源
		waitForWorkerTerminate();
		return unprocessedTasks;
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

	private byte [] serialize(Object obj) {
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
        	throw new RuntimeException("serialize error", e);
        }
    }
    
    private Object deserizlize(byte[] byt) {
        ObjectInputStream oii=null;
        ByteArrayInputStream bis=null;
        bis=new ByteArrayInputStream(byt);
        try {
            oii=new ObjectInputStream(bis);
            Object obj=oii.readObject();
            return obj;
        }
        catch (Exception e) {
        	logger.error("deserialize error", e);
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
				
			    List<Task> tasks = takeExpireTask();
			    
				for (Task t : tasks) {
					executor.execute(t);
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
    	
    	/**
    	 * 使用redis的multi队列进行检测过期任务，删除过期任务2个动作的执行，避免并发情况下同一个时间点的刚被提交的任务还未执行就被删除
    	 * 此处无需watch，因为redis队列保证执行操作不会被其他客户端提交的命令阻断，所以除非自身的程序逻辑需要watch这样的"伪回滚"
    	 * @return 探测到的到期任务
    	 */
    	private List<Task> takeExpireTask() {
    	    double current = System.currentTimeMillis();
            Transaction tx = JedisUtil.getJedis().multi();
            Response<Set<byte[]>> response = tx.zrangeByScore(queueName, 0, current);
            tx.zremrangeByScore(queueName, 0, current);
            tx.exec();
            
            Set<byte[]> set = response.get();
            if (set == null)
                return Collections.emptyList();
            
            List<Task> list = new ArrayList<>(set.size());
            for (byte[] b : set) {
                list.add((Task) deserizlize(b));
            }
            return list;
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
