package com.nameof.timer;

import java.io.Serializable;
import java.util.UUID;

abstract public class Task implements Serializable{

	private static final long serialVersionUID = -8639671839184198860L;

	private String id;
	
	private long deadline;
	
	public Task() {
		this(UUID.randomUUID().toString());
	}
	
	public Task(String id) {
		this.id = id;
	}
	
	public Task(String id, long deadline) {
		this.id = id;
		this.deadline = deadline;
	}

	public abstract void run();
	
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
