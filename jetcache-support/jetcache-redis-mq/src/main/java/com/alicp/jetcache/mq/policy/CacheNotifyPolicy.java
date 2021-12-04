package com.alicp.jetcache.mq.policy;

import com.alicp.jetcache.mq.msg.CommandEnum;
import com.alicp.jetcache.mq.msg.CommandMsg;
import com.alicp.jetcache.mq.util.NetworkInterfaceUtil;
import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 缓存通知
 * @author chenzhh
 */
public interface CacheNotifyPolicy {
    /**
     * 日志
     */
    public static final Logger log = LoggerFactory.getLogger(CacheNotifyPolicy.class);
    /**
     *实例唯一标识
     */
    public static  final String LOCAL_COMMAND = NetworkInterfaceUtil.getMachineId();

    /**
     * 配置文件前缀
     */
    public static  final String PREFIX="jetcache.mq";
    /**
     * 清除全部本地缓存
     */
    void  clear(CommandMsg commandMsg);
    /**
     * 发布消息
     * @param commandMsg
     */
   void publish(CommandMsg commandMsg);

    /**
     * 是否本地缓存
     * @param commandMsg
     * @return
     */
   boolean isLocalCommand(CommandMsg commandMsg);

    /**
     * 启动
     */
   void start() ;

    /**
     * 关闭资源
     */
   void shutdown();

    /**
     * 处理缓存事件逻辑
     * @param cmd the received command
     */
    default void handleCommand(CommandMsg cmd) {
        if (cmd == null || isLocalCommand(cmd)){
            return;
        }
        clear(cmd);

    }

    /**
     * 获取配置文件
     * @param environment
     * @param key
     * @return
     */
    default String  getProp(ConfigurableEnvironment environment,String key){
        return environment.getProperty(PREFIX+"."+key);
    }

}
