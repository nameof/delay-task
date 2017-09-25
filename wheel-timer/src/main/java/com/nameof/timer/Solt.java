package com.nameof.timer;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Solt {
	
	/** 任务链 */
	private ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
	
	public ConcurrentLinkedQueue<Task> getTasks() {
		return tasks;
	}
	
	public void addTask(Task task) {
		tasks.add(task);
	}
	
	public void executeTask(Executor executor) {
		Iterator<Task> iterator = tasks.iterator();
		while (iterator.hasNext()) {
			Task task = iterator.next();
			if (task.round() <= 0) {
				iterator.remove();
				//task.run();
				executor.execute(task);//避免耗时操作，使用额外的执行线程执行
			}
		}
	}
}
