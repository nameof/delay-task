package com.nameof.timer;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 单独的任务执行器
 * @author chengpan
 *
 */
public class Executor extends Thread{
	
	private BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

	private Collection<Task> unProcessedTasks = new HashSet<>();
	
	public Collection<Task> getUnProcessedTasks() {
		return unProcessedTasks;
	}

	public void setUnProcessedTasks(Collection<Task> unProcessedTasks) {
		this.unProcessedTasks = unProcessedTasks;
	}

	public void execute(Task task) {
		try {
			queue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while (!isInterrupted()) {
			Task task = null;
			try {
				task = queue.take();
			} catch (InterruptedException e) {
				break;
			}
			try {
				task.run();
			} catch (Throwable e) {}
		}
		
		unProcessedTasks.addAll(queue);
	}
}
