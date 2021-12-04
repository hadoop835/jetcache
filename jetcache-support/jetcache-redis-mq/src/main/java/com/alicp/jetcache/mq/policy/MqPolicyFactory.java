package com.alicp.jetcache.mq.policy;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author chenzhh
 */
public class MqPolicyFactory {
    /**
     * RocketMq
     * @param configurableEnvironment
     * @return
     */
    public  static final RocketMqCacheNotifyPolicy newRocketMqCacheNotifyPolicy(ConfigurableEnvironment configurableEnvironment){
         return new RocketMqCacheNotifyPolicy(configurableEnvironment);
    }

    /**
     * RabbitMq
     * @param configurableEnvironment
     * @return
     */
    public  static final RabbitMqCacheNotifyPolicy newRabbitMqCacheNotifyPolicy(ConfigurableEnvironment configurableEnvironment){
        return new RabbitMqCacheNotifyPolicy(configurableEnvironment);
    }

    /**
     * redisMq
     * @param configurableEnvironment
     * @return
     */
    public  static final RedisMqCacheNotifyPolicy newRedisMqCacheNotifyPolicy(ConfigurableEnvironment configurableEnvironment, StringRedisTemplate stringRedisTemplate){
        return new RedisMqCacheNotifyPolicy(configurableEnvironment,stringRedisTemplate);
    }

}
