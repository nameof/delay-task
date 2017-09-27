package com.nameof.timer;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


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
		boolean interrupted = false;
		while (!isInterrupted()) {
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				interrupted = true;
				break;
			}
		}
		
		unProcessedTasks.addAll(queue);
		
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
