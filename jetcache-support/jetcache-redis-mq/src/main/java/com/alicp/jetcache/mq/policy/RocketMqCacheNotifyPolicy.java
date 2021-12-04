package com.alicp.jetcache.mq.policy;

import com.alicp.jetcache.AbstractCache;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheUtil;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.mq.config.CommonCacheProperties;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RocketMq 同步数据
 * @author chenzhh
 */
public class RocketMqCacheNotifyPolicy implements CacheNotifyPolicy, CacheMessagePublisher, MessageListenerConcurrently {

    private CommonCacheProperties cacheProperties;

    private DefaultMQProducer defaultMQProducer;

    private DefaultMQPushConsumer defaultMQPushConsumer;
    /**
     *
     */
    @Autowired
    private SpringConfigProvider springConfigProvider;

    /**
     *
     * @param cacheProperties
     */
    public RocketMqCacheNotifyPolicy(CommonCacheProperties cacheProperties){
        this.cacheProperties = cacheProperties;
        //发送者
        this.defaultMQProducer = new DefaultMQProducer(cacheProperties.getGroup());
        this.defaultMQProducer.setNamesrvAddr(cacheProperties.getAddress());

        //消费者
        this.defaultMQPushConsumer = new DefaultMQPushConsumer(cacheProperties.getGroup());
        this.defaultMQPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        this.defaultMQPushConsumer.setNamesrvAddr(cacheProperties.getAddress());
        this.defaultMQPushConsumer.setMessageModel(MessageModel.BROADCASTING);
    }

    /**
     *
     */
    private static  final String LOCAL_COMMAND = NetworkInterfaceUtil.getMachineId();

    @Override
    public void clear(CommandMsg commandMsg) {
        log.info("删除缓存：{}",commandMsg.toJson());
        Cache cache = springConfigProvider.getCacheManager().getCache(commandMsg.getArea(),commandMsg.getCacheName());
        if(Objects.nonNull(cache)) {
            AbstractCache abstractCache = CacheUtil.getAbstractCache(cache);
            if (abstractCache instanceof MultiLevelCache) {
                MultiLevelCache multiLevelCache = (MultiLevelCache) abstractCache;
                Cache[] caches = multiLevelCache.caches();
                Cache local_cache = caches[0];
                Cache remote_cache = caches[1];

                CacheMessage cacheMessage = commandMsg.getCacheMessage();

                for(Object key : cacheMessage.getKeys()){
                   log.info("远程查询：{}",remote_cache.get(key));
                }
                invalidate(local_cache, cacheMessage.getKeys());
            }
        }
    }

    private  void invalidate(Cache cache,Object[] keys){
        for(Object key : keys){
            Object value =  cache.get(key);
            log.info("本地查询：{}",value);
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
        Message message = new Message(cacheProperties.getCacheTopic(),commandMsg.toJson().getBytes());
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
            this.defaultMQPushConsumer.subscribe(cacheProperties.getCacheTopic(),"*");
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
