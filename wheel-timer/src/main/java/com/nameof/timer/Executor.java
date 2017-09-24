package com.nameof.timer;

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
		while (true) {
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
