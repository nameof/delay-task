package com.nameof.timer.expire;

import redis.clients.jedis.JedisPubSub;

public class RedisMsgPubSubListener  extends JedisPubSub {  
    
	private TaskExpireListener listener;
	
	private static final String EXPIRE_EVENT_NAME = "__keyevent@0__:expired";
	
	public RedisMsgPubSubListener(TaskExpireListener listener) {
		this.listener = listener;
	}
  
    @Override  
    public void onMessage(String channel, String message) {
		if (EXPIRE_EVENT_NAME.equals(channel) && message.startsWith(ExpireTimer.TASK_PREFIX)) {
    		listener.process(message);
        }
    }  
    
}