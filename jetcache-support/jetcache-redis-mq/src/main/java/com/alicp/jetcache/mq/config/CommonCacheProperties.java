package com.alicp.jetcache.mq.config;

import com.alicp.jetcache.autoconfigure.JetCacheProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author chenzhh
 */
@AutoConfigureBefore(JetCacheProperties.class)
@ConfigurationProperties(prefix = "jetcache.mq")
public class CommonCacheProperties {
    /**
     * 组名
     */
    @Value("${spring.application.name:}")
    private  String  group;
    /**
     * 地址
     */
    private String  address;
    /**
     * 用户名
     */
    private String  username;
    /**
     * 密码
     */
    private String  password;
    /**
     * 缓存主题
     */
    private String cacheTopic;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCacheTopic() {
        return cacheTopic;
    }

    public void setCacheTopic(String cacheTopic) {
        this.cacheTopic = cacheTopic;
    }
}
