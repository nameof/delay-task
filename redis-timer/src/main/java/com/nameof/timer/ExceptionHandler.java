package com.nameof.timer;

import java.io.Serializable;

public interface ExceptionHandler extends Serializable{
	void handle(Task task, Throwable e);
}
