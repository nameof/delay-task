package com.nameof.timer;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提供任务的接口，不做赘述。
 * 任务状态机制，参考netty的时间轮实现:
 * https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/HashedWheelTimer.java
 *
 * @author chengpan
 */
public abstract class AbstractTimer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 任务状态
     */
    public static final int WORKER_STATE_INIT = 0;
    public static final int WORKER_STATE_STARTED = 1;
    public static final int WORKER_STATE_SHUTDOWN = 2;

    @SuppressWarnings({"unused"})
    private volatile int workerState = WORKER_STATE_INIT; // 0 - init, 1 - started, 2 - shut down

    protected static final AtomicIntegerFieldUpdater<AbstractTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractTimer.class, "workerState");

    public void addTask(Task job, int delay, TimeUnit unit) {
        if (job == null) {
            throw new NullPointerException("job");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (delay < 0) {
            throw new IllegalArgumentException("delay must be >= 0");
        }
        start();
        doAddTask(job, delay, unit);
    }

    protected abstract void doAddTask(Task job, int delay, TimeUnit unit);

    private void start() {
        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    onStart();
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }
    }

    protected abstract void onStart();

    /**
     * 停止并返回尚未被处理的任务
     */
    public Collection<Task> stop() {
        if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            return onStop();
        }
        return Collections.emptySet();
    }

    protected abstract Collection<Task> onStop();
}
