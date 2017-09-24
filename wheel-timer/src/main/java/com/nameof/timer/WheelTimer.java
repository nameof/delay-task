package com.nameof.timer;

import java.util.concurrent.ConcurrentLinkedQueue;


public class WheelTimer {
	public static final int QUEUE_SIZE = 64;
	
	private Solt[] wheel = new Solt[QUEUE_SIZE];
	
	private ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
	
	private long duration =  500;
	
	private Thread workerThread = new Thread(new Worker());
	
	public WheelTimer() {
		init();
	}
	
	private void init() {
		for (int i = 0; i < wheel.length; i++) {
			wheel[i] = new Solt();
		}
	}
	
	public void addTask(int delay, Runnable job) {
		tasks.add(new Task(job, delay));
	}
	
	public void start() {
		workerThread.start();
	}
	
	private final class Worker implements Runnable {

		private long current = 0;
		
		private Executor executor = new Executor();
		
		@Override
		public void run() {

			executor.start();
			
			while (true) {
				
                transferTasks();
                
                int idx = (int) (current % wheel.length);
                Solt solt = wheel[idx];
                if (solt != null) {
                	solt.executeTask(executor);
                }
                
                waitForNextStep();
                
                current++;
			}
		}
		
		private void waitForNextStep() {
			try {
				Thread.sleep(duration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void transferTasks() {
			for (int i = 0; i < 1000; i++) {
                Task task = tasks.poll();
                if (task == null) {
                    //处理完成
                    break;
                }

                int stopIndex = (int) ((current + task.getDelay() * 1000 / duration) % wheel.length);
                int round = (int) (task.getDelay() * 1000 / duration / wheel.length);
                task.setRound(round);
                
                wheel[stopIndex].addTask(task);
            }
		}
		
	}
}
