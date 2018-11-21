package com.nameof.timer.expire;

public interface TaskExpireListener {
    void process(String taskId);
}
