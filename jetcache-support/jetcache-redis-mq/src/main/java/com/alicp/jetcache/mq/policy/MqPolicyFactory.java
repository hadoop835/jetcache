package com.alicp.jetcache.mq.policy;

import com.alicp.jetcache.mq.config.CommonCacheProperties;
/**
 * @author chenzhh
 */
public class MqPolicyFactory {
    /**
     *
     * @param commonCacheProperties
     * @return
     */
    public  static final RocketMqCacheNotifyPolicy newRocketMqCacheNotifyPolicy(CommonCacheProperties commonCacheProperties){

         return new RocketMqCacheNotifyPolicy(commonCacheProperties);
    }
}
