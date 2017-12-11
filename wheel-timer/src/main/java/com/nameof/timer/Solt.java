package com.nameof.timer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 任务槽
 * @author chengpan
 *
 */
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
				executor.execute(task);//避免耗时操作，使用额外的执行线程执行
			}
		}
	}
	
	/** 返回当前槽尚未执行的Task */
	public Set<Task> getUnprocessedTaks() {
		Set<Task> unproTasks = new HashSet<>(tasks);
		return unproTasks;
	}
}
