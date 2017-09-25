package com.nameof.timer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 对于新添加的任务{@link WheelTimer}首先将其缓存到自身的{@link #tasks}中
 * {@link #workerThread}会在下一次的指针移动时，将缓存的任务放置到对应的时间轮槽中
 * 所以，时间轮任务执行时间的精度会受到指针移动的时间间隔影响
 * @author Chengpan
 */
public class WheelTimer {//TODO 提高延时精度
	/** 轮子大小 */
	public static final int QUEUE_SIZE = 64;
	
	private Solt[] wheel = new Solt[QUEUE_SIZE];
	
	/** 添加任务时的缓存队列，在下一次移动时，才将其放入时间轮 */
	private ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
	
	/** step频率，毫秒 */
	private long duration = 900;
	
	private Thread workerThread = new Thread(new Worker());
	
	/** 任务状态 */
	public static final int WORKER_STATE_INIT = 0;
	public static final int WORKER_STATE_STARTED = 1;
	public static final int WORKER_STATE_SHUTDOWN = 2;
	
	@SuppressWarnings({ "unused" })
	private volatile int workerState = WORKER_STATE_INIT; // 0 - init, 1 - started, 2 - shut down
	
	private static final AtomicIntegerFieldUpdater<WheelTimer> WORKER_STATE_UPDATER =
	        AtomicIntegerFieldUpdater.newUpdater(WheelTimer.class, "workerState");
	
	public WheelTimer() {
		init();
	}
	
	private void init() {
		for (int i = 0; i < wheel.length; i++) {
			wheel[i] = new Solt();
		}
	}
	
	public void addTask(int delay, Runnable job) {
		start();
		tasks.add(new Task(job, delay));
	}
	
	private void start() {
		 switch (WORKER_STATE_UPDATER.get(this)) {
	         case WORKER_STATE_INIT:
	             if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
	                 workerThread.start();
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
	
	public void stop() {
		WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN);
	}
	
	/** 时间轮执行器，轮询时间轮和任务，调度执行 */
	private final class Worker implements Runnable {

		private long current = 0;
		
		private Executor executor = new Executor();
		
		@Override
		public void run() {

			executor.start();
			
			while (WORKER_STATE_UPDATER.get(WheelTimer.this) == WORKER_STATE_STARTED) {
				
                transferTasks();
                
                int idx = (int) (current % wheel.length);
                Solt solt = wheel[idx];
                if (solt != null) {
                	solt.executeTask(executor);
                }
                
                waitForNextStep();
                
                current++;
			}
			
			terminateExecutor();
		}
		
		private void waitForNextStep() {
			try {
				Thread.sleep(duration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 在执行当前槽中任务之前，将缓存队列中的任务，放置到对应的槽
		 */
		private void transferTasks() {
			//一次转移10000个任务到时间轮中
			for (int i = 0; i < 10000; i++) {
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
		
		private void terminateExecutor() {

			boolean interrupted = false;
            while (executor.isAlive()) {
            	executor.interrupt();
                try {
                	executor.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }

            if (interrupted) {
                Thread.currentThread().interrupt();
            }
		}
	}
}
