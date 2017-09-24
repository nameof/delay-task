package com.nameof.timer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;


public class WheelTimer {
	public static final int QUEUE_SIZE = 64;
	
	private Solt[] wheel = new Solt[QUEUE_SIZE];
	
	private final int mask = wheel.length - 1;
	
	private ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
	
	private long duration =  100;
	
	private Thread workerThread = new Thread(new Worker());
	
	private volatile long startTime;
	
	public void addTask(int delay, Runnable job) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(delay) - startTime;
		tasks.add(new Task(job, deadline));
	}
	
	public void start() {
		startTime = System.nanoTime();
        if (startTime == 0) {
            startTime = 1;
        }
		workerThread.start();
	}
	
	private void initSolt(int position) {
		if (wheel[position] == null) {
			synchronized (wheel) {
				if (wheel[position] == null) {
					wheel[position] = new Solt();
				}
			}
		}
	}
	
	private final class Worker implements Runnable {

		private int current = 0;
		
		@Override
		public void run() {

			while (true) {
				
				final long deadline = waitForNextTick();
                if (deadline > 0) {
                    transferTasks();
                    
                    current = (current & mask);
                    
                    if (wheel[current] != null)
                    	wheel[current].executeTask(deadline);
                    
                    current++;
                }
				
				
			}
		}
		
		 private long waitForNextTick() {
	            long deadline = duration * (current + 1);

	            for (;;) {
	                final long currentTime = System.nanoTime() - startTime;
	                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

	                if (sleepTimeMs <= 0) {
	                    if (currentTime == Long.MIN_VALUE) {
	                        return -Long.MAX_VALUE;
	                    } else {
	                        return currentTime;
	                    }
	                }

	            }
	        }
		
		private void transferTasks() {
			for (int i = 0; i < 1000; i++) {
                Task task = tasks.poll();
                if (task == null) {
                    //处理完成
                    break;
                }

                long calculated = task.getDeadline() / duration;
                task.setRound((int) ((calculated - current) / wheel.length));

                final long ticks = Math.max(calculated, current); // Ensure we don't schedule for past.
                int stopIndex = (int) (ticks & mask);
                
				initSolt(stopIndex);
                wheel[stopIndex].addTask(task);
            }
		}
		
	}
}
