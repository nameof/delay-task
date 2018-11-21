# delay-task
时间轮算法、redis的zset、redis的key过期订阅事件等几种方式实现的延时任务调度

#References
https://github.com/netty/netty/blob/4.1/common/src/main/java/io/netty/util/HashedWheelTimer.java

TODO
===========
添加Task自身的任务状态，如取消<br/>
保证任务可用，如落盘机制
