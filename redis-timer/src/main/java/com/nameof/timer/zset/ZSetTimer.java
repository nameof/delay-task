package com.nameof.timer.zset;

import com.nameof.jedis.JedisUtil;
import com.nameof.jedis.SerializeUtils;
import com.nameof.timer.AbstractTimer;
import com.nameof.timer.ExceptionHandler;
import com.nameof.timer.Executor;
import com.nameof.timer.Task;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 使用redis的排序集合实现的延时任务，集合元素的值为任务，分数为任务的过期unix时间戳.
 * 所以{@link ZSetTimer}要做的就是每次以当前时间戳去轮询redis，取出分数小于当前时间戳的集合元素，执行任务.
 * 定时精度取决于轮询的时间间隔，当然我们也会因为CAP原则在频繁轮询中做出权衡.
 * 由于任务自身有{@link ExceptionHandler}，所以客户端可自定义实现任务失败后重发到redis等这样逻辑的Handler
 *
 * @author chengpan
 */
public class ZSetTimer extends AbstractTimer {

    private byte[] queueName = "delay_task_queue".getBytes();

    private Jedis jedis = null;

    private Set<Task> unprocessedTasks = new HashSet<>();

    private Thread workerThread = new Thread(new Worker());

    public void doAddTask(Task job, int delay, TimeUnit unit) {
        long score = System.currentTimeMillis() + unit.toMillis(delay);
        job.setDeadline(score);
        jedis.zadd(queueName, score, SerializeUtils.serialize(job));
    }

    protected void onStart() {
        workerThread.start();
        jedis = JedisUtil.getNonThreadJedis();
    }

    /**
     * 停止时间轮
     * @return 返回本地尚未被执行的任务，不包含redis中尚未被执行的任务
     */
    public Collection<Task> onStop() {
        jedis.close();
        waitForWorkerTerminate();
        return unprocessedTasks;
    }

    /**
     * 等待工作线程结束
     */
    private void waitForWorkerTerminate() {
        boolean clientInterrupted = false;
        while (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(100);
            } catch (InterruptedException ignored) {
                clientInterrupted = true;
            }
        }
        if (clientInterrupted) Thread.currentThread().interrupt();
    }

    private class Worker implements Runnable {

        private Executor executor = new Executor();

        @Override
        public void run() {

            executor.start();

            while (WORKER_STATE_UPDATER.get(ZSetTimer.this) == WORKER_STATE_STARTED) {

                List<Task> tasks = takeExpireTask();
                tasks.forEach(executor::execute);

                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ignore) { }
            }
            workerExit();
        }

        private void workerExit() {
            JedisUtil.returnResource();

            terminateExecutor();

            unprocessedTasks.addAll(executor.getUnprocessedTasks());
        }

        private List<Task> takeExpireTask() {
            Set<byte[]> set = getExpiredTaskData();
            return serializeToTask(set);
        }

        private List<Task> serializeToTask(Set<byte[]> set) {
            if (set == null)
                return Collections.emptyList();
            List<Task> list = new ArrayList<>(set.size());
            set.forEach((b) -> list.add((Task) SerializeUtils.deserizlize(b)));
            return list;
        }

        /**
         * 使用redis的multi队列进行检测过期任务，删除过期任务2个动作的执行，避免并发情况下同一个时间点的刚被提交的任务还未执行就被删除。<br/>
         * 此处无需watch，因为redis队列保证执行操作不会被其他客户端提交的命令阻断。<br/>
         * 即使timer在向redis发送这两条命令的间隔中,任务队列被其它客户端修改，修改无非就有提交了过期任务，或未过期任务这两种情况。前者
         * 会在exec时正好被拿出来执行，后者尚未到执行时刻，所以均对程序无影响，所以除非自身的程序需要互斥写redis队列，才需要watch这样的"伪回滚"。
         *
         * @return 探测到的到期任务
         */
        private Set<byte[]> getExpiredTaskData() {
            long current = System.currentTimeMillis();
            Transaction tx = JedisUtil.getJedis().multi();
            Response<Set<byte[]>> response = tx.zrangeByScore(queueName, 0, current);
            tx.zremrangeByScore(queueName, 0, current);
            tx.exec();
            return response.get();
        }

        private void terminateExecutor() {
            while (executor.isAlive()) {
                executor.interrupt();
                try {
                    executor.join(100);
                } catch (InterruptedException ignored) { }
            }
        }
    }
}
