package com.nameof.timer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Executor extends Thread{
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	private BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

	public void execute(Task task) {
		try {
			if (task != null) {
				queue.put(task);
			}
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
			} catch (InterruptedException e) {return;}
			
			try {
				task.run();
			} catch (Throwable e) {
				try {
					task.getExceptionHandler().handle(task, e);
				} catch (Throwable e1) {
					logger.error("exception from exceptionhandler", e1);
				}
			}
		}
	}
	
	public Set<Task> getUnprocessedTasks() {
		return new HashSet<>(queue);
	}
}
