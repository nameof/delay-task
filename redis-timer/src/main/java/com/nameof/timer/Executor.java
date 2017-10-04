package com.nameof.timer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Executor extends Thread{
	
	private BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

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
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				return;
			} catch (Throwable e) {
				// TODO logger
				System.out.println(e);
			}
		}
	}
	
	public Set<Task> getUnprocessedTasks() {
		return new HashSet<>(queue);
	}
}
