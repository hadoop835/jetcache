/**
 * Created on 2022/08/01.
 */
package com.alicp.jetcache.template;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheBuilder;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.CacheMonitor;
import com.alicp.jetcache.CacheUtil;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.external.ExternalCacheBuilder;
import com.alicp.jetcache.support.BroadcastManager;
import com.alicp.jetcache.support.CacheNotifyMonitor;

import java.util.function.Function;

/**
 * @author huangli
 */
public class NotifyMonitorInstaller implements CacheMonitorInstaller {

    private final Function<String, CacheBuilder> remoteBuilderTemplate;

    public NotifyMonitorInstaller(Function<String, CacheBuilder> remoteBuilderTemplate) {
        this.remoteBuilderTemplate = remoteBuilderTemplate;
    }

    @Override
    public void addMonitors(CacheManager cacheManager, Cache cache, QuickConfig quickConfig) {
        if (quickConfig.getSyncLocal() == null || !quickConfig.getSyncLocal()) {
            return;
        }
        if (!(CacheUtil.getAbstractCache(cache) instanceof MultiLevelCache)) {
            return;
        }
        String area = quickConfig.getArea();
        final ExternalCacheBuilder cacheBuilder = (ExternalCacheBuilder) remoteBuilderTemplate.apply(area);
        if (cacheBuilder == null || !cacheBuilder.supportBroadcast()
                || cacheBuilder.getConfig().getBroadcastChannel() == null) {
            return;
        }

        if (cacheManager.getBroadcastManager(area) == null) {
            BroadcastManager cm = cacheBuilder.createBroadcastManager(cacheManager);
            if (cm != null) {
                cm.startSubscribe();
                cacheManager.putBroadcastManager(area, cm);
            }
        }

        CacheMonitor monitor = createMonitor(cacheManager, quickConfig, area);
        cache.config().getMonitors().add(monitor);
    }

    protected CacheMonitor createMonitor(CacheManager cacheManager, QuickConfig quickConfig, String area) {
        return new CacheNotifyMonitor(cacheManager, area, quickConfig.getName());
    }


}
