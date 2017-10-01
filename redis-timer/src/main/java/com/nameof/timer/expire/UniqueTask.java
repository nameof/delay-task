package com.nameof.timer.expire;

import com.nameof.timer.Task;

public abstract class UniqueTask extends Task{

	private static final long serialVersionUID = -5064843282300968260L;

	private String id;
	
	private long deadline;
	
	public UniqueTask(String id) {
		this.id = id;
	}
	
	public UniqueTask(String id, long deadline) {
		super();
		this.id = id;
		this.deadline = deadline;
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