package com.nameof.timer;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Solt {
	private ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();

	public ConcurrentLinkedQueue<Task> getTasks() {
		return tasks;
	}
	
	public void addTask(Task task) {
		tasks.add(task);
	}
	
	public void executeTask(long deadline) {
		Iterator<Task> iterator = tasks.iterator();
		while (iterator.hasNext()) {
			Task task = iterator.next();
			if (task.round() <= 0 && task.getDeadline() < deadline) {
				iterator.remove();
				task.run();
			}
		}
	}
}
