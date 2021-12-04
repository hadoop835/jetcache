package com.alicp.jetcache.mq.config;

import com.alicp.jetcache.anno.support.SimpleCacheManager;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.autoconfigure.JetCacheAutoConfiguration;
import com.alicp.jetcache.mq.policy.MqPolicyFactory;
import com.alicp.jetcache.mq.policy.RocketMqCacheNotifyPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 是否开启缓存广播机制
 * @author chenzhh
 */
@Configuration
@EnableConfigurationProperties(CommonCacheProperties.class)
@Import({JetCacheAutoConfiguration.class,})
public class EnableAutoCacheMessagePublisher {
    @Autowired
    private CommonCacheProperties commonCacheProperties;

    private String type;
    /**
     * 依赖 {@link SpringConfigProvider}
     * @return
     */
    @Bean(initMethod = "start",destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "jetcache.mq",name = "type",havingValue = "rocketmq")
    //@ConditionalOnBean(value = {SpringConfigProvider.class, SimpleCacheManager.class})
    public RocketMqCacheNotifyPolicy rocketmqCacheMessagePublisher(){
        return MqPolicyFactory.newRocketMqCacheNotifyPolicy(commonCacheProperties);
    }
}
