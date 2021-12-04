package com.alicp.jetcache.mq.msg;

import com.alibaba.fastjson.JSON;
import com.alicp.jetcache.support.CacheMessage;

import java.io.Serializable;

/**
 * 发生消息
 * @author chenzhh
 */
public class CommandMsg implements Serializable {

    /**
     * 唯一标识
     */
    private String id;
    /**
     * 区域
     */
    private String area;
    /**
     * 缓存名称
     */
    private String cacheName;
    /**
     * 缓存消息
     */
    private CacheMessage cacheMessage;
    /**
     * 命令类型
     */
    private CommandEnum commandEnum;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public CacheMessage getCacheMessage() {
        return cacheMessage;
    }

    public void setCacheMessage(CacheMessage cacheMessage) {
        this.cacheMessage = cacheMessage;
    }

    public CommandEnum getCommandEnum() {
        return commandEnum;
    }

    public void setCommandEnum(CommandEnum commandEnum) {
        this.commandEnum = commandEnum;
    }

    /**
     * 对象转字符串
     * @return
     */
  public  String  toJson(){
        return JSON.toJSONString(this);
  }

    /**
     * 字符串转对象
     * @param json
     * @return
     */
  public static CommandMsg  toCommandMsg(String json){
        return JSON.parseObject(json,CommandMsg.class);
  }
}
