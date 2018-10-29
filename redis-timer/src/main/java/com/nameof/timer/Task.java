package com.nameof.timer;

import java.io.Serializable;
import java.util.UUID;

abstract public class Task implements Serializable {

	private static final long serialVersionUID = -8639671839184198860L;

	private String id;
	
	private long deadline;
	
	private ExceptionHandler handler;
	
	private static final ExceptionHandler defaultHandler = new ExceptionHandler() {
		
		private static final long serialVersionUID = 1L;

		@Override
		public void handle(Task task, Throwable e) {
			e.printStackTrace();
		}
	};
	
	public Task() {
		this(UUID.randomUUID().toString());
	}
	
	public Task(String id) {
		this(id, defaultHandler);
	}
	
	public Task(String id, ExceptionHandler handler) {
		this(id, -1, handler);
	}
	
	public Task(String id, long deadline, ExceptionHandler handler) {
		this.id = id;
		this.deadline = deadline;
		this.handler = handler;
	}

	public abstract void run();
	
	public void setExceptionHandler(ExceptionHandler handler) {
		this.handler = handler;
	}

	public ExceptionHandler getExceptionHandler() {
		return handler;
	}

	public void setDeadline(long deadline) {
		this.deadline = deadline;
	}

	public long getDeadline() {
		return deadline;
	}

	public String getId() {
		return id;
	}
}
