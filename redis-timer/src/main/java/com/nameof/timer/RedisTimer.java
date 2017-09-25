package com.nameof.timer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.nameof.jedis.JedisUtil;

public class RedisTimer {
	
	private byte[] queueName = "delay_task_queue".getBytes();
	
	private Thread workThread = new Thread(new Worker());
	
	public void addTask(Task job, int delay, TimeUnit unit) {
		//start();
		long score = System.currentTimeMillis() + unit.toMillis(delay);
		JedisUtil.getJedis().zadd(queueName, score, serialize(job));
	}
	
	public void start() {
		workThread.start();
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

		public void run() {
			while (true) {
				double current = System.currentTimeMillis();
				Set<byte[]> set = JedisUtil.getJedis().zrangeByScore(queueName, 0, current);
				List<Task> list = new ArrayList<>(set.size());
				if (set != null) {
					for (byte[] b : set) {
						list.add((Task) deserizlize(b));
					}
					
					for (Task t : list) {
						t.run();
					}
					
					JedisUtil.getJedis().zremrangeByScore(queueName, 0, current);
				}
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					JedisUtil.returnResource();
				}
			}
		}
    	
    }
}
