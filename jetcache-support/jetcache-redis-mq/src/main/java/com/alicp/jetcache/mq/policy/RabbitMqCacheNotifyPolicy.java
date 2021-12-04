package com.alicp.jetcache.mq.policy;

import com.alicp.jetcache.AbstractCache;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheUtil;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.mq.msg.CommandMsg;
import com.alicp.jetcache.support.CacheMessage;
import com.alicp.jetcache.support.CacheMessagePublisher;
import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMq 进行广播通知
 * @author chenzhh
 */
public class RabbitMqCacheNotifyPolicy implements CacheNotifyPolicy, CacheMessagePublisher,Consumer {
    @Autowired
    private SpringConfigProvider springConfigProvider;
    /**
     * 获取配置文件
     */
    private ConfigurableEnvironment environment;
    /**
     * 连接工厂
     */
    private ConnectionFactory factory;
    /**
     * 发送者
     */
    private Connection connectionProducer;
    private Channel  channelProducer;
    /**
     * 消费者
     */
    private Connection connectionConsumer;
    private Channel channelConsumer;

    private String exchange;

    /**
     * 初始化
     * @param environment
     */
    public RabbitMqCacheNotifyPolicy(ConfigurableEnvironment environment){
          factory = new ConnectionFactory();
          this.environment = environment;

          String _exchange = getProp(environment,"exchange");
          Objects.requireNonNull(_exchange,"rabbitmq交换机参数不能空【exchange】");
          String suffix = getProp(this.environment,"suffix");
          if(!Objects.nonNull(suffix)){
            String[] activeProfiles = this.environment.getActiveProfiles();
            if(Objects.nonNull(activeProfiles) && activeProfiles.length > 0){
                suffix = activeProfiles[0];
            }
         }
          log.info("环境变量后缀：jetcache.mq.suffix,值：{}",suffix);
        if(Objects.nonNull(suffix)){
            this.exchange = _exchange+"-"+suffix;
        }else{
            this.exchange = _exchange;
        }
          String address = getProp(this.environment,"address");
          Objects.requireNonNull(address,"rabbitmq连接地址不能为空【address】");
          try {
            factory.setUri(address);
          } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error("连接地址失败：{},连接地址：{}",e,address);
         }
          String username = getProp(environment,"username");
          factory.setUsername(username);
          String password = getProp(environment,"password");
          factory.setPassword(password);
          String virtualHost = getProp(environment,"virtualHost");
          if(Objects.isNull(virtualHost)){
              virtualHost = getProp(environment,"virtual-host");
          }
          Objects.requireNonNull(virtualHost,"rabbitmq连接地址不能为空【virtualHost】");
          factory.setVirtualHost(virtualHost);
          String protocol = getProp(environment,"protocol");
          if(Objects.nonNull(protocol)){
              try {
                  factory.useSslProtocol(protocol);
              } catch (NoSuchAlgorithmException | KeyManagementException e) {
                  log.error("rabbitmq连接地址不能为空【protocol】,异常",e);
              }
          }

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

    @Override
    public void publish(CommandMsg commandMsg) {
        commandMsg.setId(LOCAL_COMMAND);
        //失败重连
        if(!channelProducer.isOpen() || !connectionProducer.isOpen()) {
            synchronized (RabbitMqCacheNotifyPolicy.class) {
                if(!channelProducer.isOpen() || !connectionProducer.isOpen()) {
                    try {
                        connectionProducer = factory.newConnection();
                        channelProducer = connectionProducer.createChannel();
                        channelProducer.basicPublish(exchange, "", null, commandMsg.toJson().getBytes());
                    } catch (IOException | TimeoutException e) {
                        log.error("rabbitmq创建生产者和消费者异常,异常:{},连接：{}",e,factory.toString());
                    }

                }
            }
        }
    }

    @Override
    public boolean isLocalCommand(CommandMsg commandMsg) {
        return LOCAL_COMMAND.equals(commandMsg.getId());
    }

    @Override
    public void start() {
        try {
            connectionProducer = factory.newConnection();
            channelProducer = connectionProducer.createChannel();
            channelProducer.exchangeDeclare(exchange, "fanout");
            connectionConsumer = factory.newConnection();
            channelConsumer = connectionConsumer.createChannel();
            channelConsumer.exchangeDeclare(exchange, "fanout");
            String queueName = channelConsumer.queueDeclare().getQueue();
            channelConsumer.queueBind(queueName, exchange, "");
            channelConsumer.basicConsume(queueName, true, (Consumer) this);
        } catch (IOException | TimeoutException e) {
            log.error("rabbitmq创建生产者和消费者连接,异常:{}",e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if(channelProducer != null){
                channelProducer.close();
            }
            if(connectionProducer != null){
                connectionProducer.close();
            }
            if(channelConsumer != null){
                channelConsumer.close();
            }
            if(connectionConsumer != null){
                connectionConsumer.close();
            }
        } catch (IOException | TimeoutException e) {
             log.error("rabbitmq创建生产者和消费者关闭连接异常:{}",e);
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
     * 处理接收消息
     * @param s
     * @param envelope
     * @param basicProperties
     * @param bytes
     * @throws IOException
     */
    @Override
    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        CommandMsg cmd = CommandMsg.toCommandMsg(new String(bytes));
        handleCommand(cmd);
    }

    @Override
    public void handleConsumeOk(String s) {

    }

    @Override
    public void handleCancelOk(String s) {

    }

    @Override
    public void handleCancel(String s) throws IOException {

    }

    @Override
    public void handleShutdownSignal(String s, ShutdownSignalException e) {

    }

    @Override
    public void handleRecoverOk(String s) {

    }


}
