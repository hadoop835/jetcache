package com.alicp.jetcache.mq.policy;

import com.alicp.jetcache.AbstractCache;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheUtil;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.mq.msg.CommandMsg;
import com.alicp.jetcache.mq.util.NetworkInterfaceUtil;
import com.alicp.jetcache.support.CacheMessage;
import com.alicp.jetcache.support.CacheMessagePublisher;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;
import java.util.Objects;

/**
 * RocketMq 同步数据
 * @author chenzhh
 */
public class RocketMqCacheNotifyPolicy implements CacheNotifyPolicy, CacheMessagePublisher, MessageListenerConcurrently {

    private DefaultMQProducer defaultMQProducer;

    private DefaultMQPushConsumer defaultMQPushConsumer;
    @Autowired
    private SpringConfigProvider springConfigProvider;

    private ConfigurableEnvironment environment;
    /**
     * 缓存主题
     */
    private String topic;
    /**
     * 分组
     */
    private String group;
    /**
     *
     * @param environment
     */
    public RocketMqCacheNotifyPolicy(ConfigurableEnvironment environment){
        this.environment = environment;
        String address = getProp(this.environment,"address");
        Objects.requireNonNull(address,"rocketmq连接地址不能为空【address】");
        String _group = getProp(this.environment,"group");
        if(Objects.isNull(_group)){
            _group = this.environment.getProperty("spring.application.name");
        }
        String suffix = getProp(this.environment,"suffix");
        if(!Objects.nonNull(suffix)){
            String[] activeProfiles = this.environment.getActiveProfiles();
            if(Objects.nonNull(activeProfiles) && activeProfiles.length > 0){
                suffix = activeProfiles[0];
            }
        }
        log.info("环境变量后缀：jetcache.mq.suffix,值：{}",suffix);
        Objects.requireNonNull(address,"rocketmq分组不能为空【group】");
        String _topic = getProp(this.environment,"topic");
        Objects.requireNonNull(_topic,"rocketmq主题不能为空【topic】");
        if(Objects.nonNull(suffix)){
            this.topic = _topic+"-"+suffix;
            this.group =_group+"-"+suffix;
        }else{
            this.topic = _topic;
            this.group =_group;
        }

        //发送者
        this.defaultMQProducer = new DefaultMQProducer(group+"-"+suffix);
        this.defaultMQProducer.setNamesrvAddr(address);
        //消费者
        this.defaultMQPushConsumer = new DefaultMQPushConsumer(group+"-"+suffix);
        this.defaultMQPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        this.defaultMQPushConsumer.setNamesrvAddr(address);
        this.defaultMQPushConsumer.setMessageModel(MessageModel.BROADCASTING);
    }

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
            if(Objects.nonNull(value)){
                cache.remove(key);
            }
        }
    }
    /**
     * 发布消息
     * @param commandMsg
     */
    @Override
    public void publish(CommandMsg commandMsg) {
        commandMsg.setId(LOCAL_COMMAND);
        Message message = new Message(this.topic,commandMsg.toJson().getBytes());
        try {
            this.defaultMQProducer.send(message);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            log.info("发送消息失败:{}",e);
        }
    }

    @Override
    public boolean isLocalCommand(CommandMsg commandMsg) {
        return LOCAL_COMMAND.equals(commandMsg.getId());
    }

    @Override
    public void start()  {
        try {
            this.defaultMQProducer.start();
            this.defaultMQPushConsumer.subscribe(this.topic,"*");
            this.defaultMQPushConsumer.registerMessageListener(this);
            this.defaultMQPushConsumer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if(defaultMQProducer != null){
            defaultMQProducer.shutdown();
        }
        if(defaultMQPushConsumer != null){
            defaultMQPushConsumer.shutdown();
        }
    }

    @Override
    public void publish(String area, String cacheName, CacheMessage cacheMessage) {
        CommandMsg commandMsg = new  CommandMsg();
        commandMsg.setArea(area);
        commandMsg.setCacheName(cacheName);
        commandMsg.setCacheMessage(cacheMessage);
        publish(commandMsg);


    }

    /**
     * 消费消息
     * @param list
     * @param consumeConcurrentlyContext
     * @return
     */
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        for(MessageExt msg : list) {
            CommandMsg cmd = CommandMsg.toCommandMsg(new String(msg.getBody()));
            handleCommand(cmd);
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

    }
}
