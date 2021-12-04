package com.alicp.jetcache.mq.config;

import com.alicp.jetcache.anno.support.SimpleCacheManager;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.autoconfigure.JetCacheAutoConfiguration;
import com.alicp.jetcache.mq.policy.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 是否开启缓存广播机制
 * @author chenzhh
 */
@Configuration
@Import({JetCacheAutoConfiguration.class,})
public class EnableAutoCacheMessagePublisher {
    @Autowired
    private ConfigurableEnvironment  configurableEnvironment;
    @Autowired
    private RedisConnectionFactory connectionFactory;
    /**
     * jetcache.mq.type=rocketmq
     * 依赖 {@link SpringConfigProvider}
     * @return
     */
    @Bean(initMethod = "start",destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = CacheNotifyPolicy.PREFIX,name = "type",havingValue = "rocketmq")
    public RocketMqCacheNotifyPolicy rocketmqCacheMessagePublisher(){
        return MqPolicyFactory.newRocketMqCacheNotifyPolicy(configurableEnvironment);
    }

    /**
     *
     * jetcache.mq.type=rabbitmq
     * @return
     */
    @Bean(initMethod = "start",destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = CacheNotifyPolicy.PREFIX,name = "type",havingValue = "rabbitmq")
    public RabbitMqCacheNotifyPolicy rabbitMqCacheMessagePublisher(){
        return MqPolicyFactory.newRabbitMqCacheNotifyPolicy(configurableEnvironment);
    }

    /**
     *
     * jetcache.mq.type=redismq
     * @return
     */
    @Bean(initMethod = "start",destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = CacheNotifyPolicy.PREFIX,name = "type",havingValue = "redismq")
    public RedisMqCacheNotifyPolicy redisMqCacheMessagePublisher(){
        return MqPolicyFactory.newRedisMqCacheNotifyPolicy(configurableEnvironment,new StringRedisTemplate(connectionFactory));
    }

    @Bean
    @ConditionalOnBean(value = {RedisMqCacheNotifyPolicy.class})
    @ConditionalOnProperty(prefix = CacheNotifyPolicy.PREFIX,name = "type",havingValue = "redismq")
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,RedisMqCacheNotifyPolicy redisMqCacheNotifyPolicy) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        RedisMqCacheNotifyPolicy.CacheRedisMessageListener cacheRedisMessageListener = new RedisMqCacheNotifyPolicy.CacheRedisMessageListener(redisMqCacheNotifyPolicy);
        listenerContainer.addMessageListener(cacheRedisMessageListener, new PatternTopic(redisMqCacheNotifyPolicy.getChannel()));
        return listenerContainer;
    }

}
