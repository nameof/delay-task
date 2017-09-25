package com.nameof.timer;

import java.io.Serializable;

abstract public class Task implements Serializable{

	private static final long serialVersionUID = -8639671839184198860L;
	
	public abstract void run();

}
