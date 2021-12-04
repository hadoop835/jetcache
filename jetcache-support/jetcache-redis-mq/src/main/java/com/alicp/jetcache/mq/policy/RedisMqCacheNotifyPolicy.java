package com.alicp.jetcache.mq.policy;

import com.alicp.jetcache.AbstractCache;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheUtil;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.mq.msg.CommandMsg;
import com.alicp.jetcache.support.CacheMessage;
import com.alicp.jetcache.support.CacheMessagePublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Objects;

/**
 * 采用Redis广播方式
 * @author chenzhh
 */
public class RedisMqCacheNotifyPolicy implements CacheNotifyPolicy, CacheMessagePublisher {
    @Autowired
    private SpringConfigProvider springConfigProvider;

    private ConfigurableEnvironment environment;

    private StringRedisTemplate stringRedisTemplate;
    /**
     * redis 通道
     */
    private  String  channel;
    /**
     * 初始化
     * @param environment
     */
    public RedisMqCacheNotifyPolicy(ConfigurableEnvironment environment,StringRedisTemplate stringRedisTemplate){
         this.environment = environment;
         String _channel = getProp(environment,"channel");
         Objects.requireNonNull(_channel,"redis消息广播通道不能为空【channel】");
        String suffix = getProp(this.environment,"suffix");
        if(!Objects.nonNull(suffix)){
            String[] activeProfiles = this.environment.getActiveProfiles();
            if(Objects.nonNull(activeProfiles) && activeProfiles.length > 0){
                suffix = activeProfiles[0];
            }
        }
        log.info("环境变量后缀：jetcache.mq.suffix,值：{}",suffix);
        if(Objects.nonNull(suffix)){
            this.channel = _channel+"-"+suffix;
        }else{
            this.channel = _channel;
        }
        this.stringRedisTemplate = stringRedisTemplate;


    }

    /**
     *
     * @param commandMsg
     */
    @Override
    public void clear(CommandMsg commandMsg) {
        Cache cache = springConfigProvider.getCacheManager().getCache(commandMsg.getArea(),commandMsg.getCacheName());
        if(Objects.nonNull(cache)) {
            AbstractCache abstractCache = CacheUtil.getAbstractCache(cache);
            if (abstractCache instanceof MultiLevelCache) {
                MultiLevelCache multiLevelCache = (MultiLevelCache) abstractCache;
                Cache[] caches = multiLevelCache.caches();
                Cache local_cache = caches[0];
                CacheMessage cacheMessage = commandMsg.getCacheMessage();
                Cache remote_cache = caches[1];
                for(Object key : cacheMessage.getKeys()){
                    log.info("读取远程缓存：{}",remote_cache.get(key));
                }
                invalidate(local_cache, cacheMessage.getKeys());
            }
        }
    }

    /**
     * 删除本地缓存
     * @param cache
     * @param keys
     */
    private  void invalidate(Cache cache,Object[] keys){
        for(Object key : keys){
            Object value =  cache.get(key);
            log.info("读取本地缓存：{}",value);
            if(Objects.nonNull(value)){
                cache.remove(key);
            }
        }
    }

    @Override
    public void publish(CommandMsg commandMsg) {
        commandMsg.setId(LOCAL_COMMAND);
        this.stringRedisTemplate.convertAndSend(this.channel,commandMsg.toJson());
    }

    @Override
    public boolean isLocalCommand(CommandMsg commandMsg) {
        return LOCAL_COMMAND.equals(commandMsg.getId());
    }

    @Override
    public void start() {


    }

    @Override
    public void shutdown() {

    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    public String getChannel() {
        return channel;
    }

    /**
     *
     * @param area
     * @param cacheName
     * @param cacheMessage
     */
    @Override
    public void publish(String area, String cacheName, CacheMessage cacheMessage) {
        CommandMsg commandMsg = new  CommandMsg();
        commandMsg.setArea(area);
        commandMsg.setCacheName(cacheName);
        commandMsg.setCacheMessage(cacheMessage);
        publish(commandMsg);
    }

    /**
     * 消息监听
     */
    public  static  class  CacheRedisMessageListener implements MessageListener{
        private RedisMqCacheNotifyPolicy redisMqCacheNotifyPolicy;
        public CacheRedisMessageListener(RedisMqCacheNotifyPolicy redisMqCacheNotifyPolicy){
            this.redisMqCacheNotifyPolicy = redisMqCacheNotifyPolicy;
        }

        @Override
        public void onMessage(Message message, byte[] bytes) {
            RedisSerializer<?> serializer =   this.redisMqCacheNotifyPolicy.getStringRedisTemplate().getValueSerializer();
            String body = (String)serializer.deserialize(message.getBody());
            CommandMsg cmd = CommandMsg.toCommandMsg(body);
            this.redisMqCacheNotifyPolicy.handleCommand(cmd);
        }
    }
}
