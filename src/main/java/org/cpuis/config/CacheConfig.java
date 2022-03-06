package org.cpuis.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * @author fujiazheng
 */
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    @Override
    public CacheManager cacheManager() {
        return new SimpleCacheManager ();
    }

}
