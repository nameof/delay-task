package com.nameof.timer.expire;

import redis.clients.jedis.JedisPubSub;

public class RedisMsgPubSubListener  extends JedisPubSub {  
    
	private TaskExpireListener listener;
	
	public RedisMsgPubSubListener(TaskExpireListener listener) {
		this.listener = listener;
	}
	
	@Override  
    public void unsubscribe() {  
        super.unsubscribe();  
    }  
  
    @Override  
    public void unsubscribe(String... channels) {  
        super.unsubscribe(channels);  
    }  
  
    @Override  
    public void subscribe(String... channels) {  
        super.subscribe(channels);  
    }  
  
    @Override  
    public void psubscribe(String... patterns) {  
        super.psubscribe(patterns);  
    }  
  
    @Override  
    public void punsubscribe() {  
        super.punsubscribe();
    }  
  
    @Override  
    public void punsubscribe(String... patterns) {  
        super.punsubscribe(patterns);  
    }  
  
    @Override  
    public void onMessage(String channel, String message) {
    	if ("__keyevent@0__:expired".equals(channel) && message.startsWith(ExpireTimer.TASK_PREFIX)) {
    		listener.process(message);
        }
    }  
  
    @Override  
    public void onPMessage(String pattern, String channel, String message) {}
  
    @Override  
    public void onSubscribe(String channel, int subscribedChannels) {}  
  
    @Override  
    public void onPUnsubscribe(String pattern, int subscribedChannels) {}  
  
    @Override  
    public void onPSubscribe(String pattern, int subscribedChannels) {}  
  
    @Override  
    public void onUnsubscribe(String channel, int subscribedChannels) {}  
}