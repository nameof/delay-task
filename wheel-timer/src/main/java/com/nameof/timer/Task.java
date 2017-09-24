package com.nameof.timer;

public class Task {

	private int round = 0;
	
	private Runnable job;
	
	private long delay;
	
	public Task(Runnable job, long delay) {
		this.job = job;
		this.delay = delay;
	}
	
	public void run() {
		job.run();
	}

	public int round() {
		return round--;
	}
	
	public int getRound() {
		return round;
	}
	
	public void setRound(int round) {
		this.round = round;
	}

	public void setJob(Runnable job) {
		this.job = job;
	}

	public long getDelay() {
		return delay;
	}
}
