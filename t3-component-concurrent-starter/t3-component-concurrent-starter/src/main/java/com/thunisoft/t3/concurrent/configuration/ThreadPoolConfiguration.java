package com.thunisoft.t3.concurrent.configuration;

import com.thunisoft.t3.concurrent.configuration.initializer.impl.ThreadPoolTaskExecutorInitializer;
import com.thunisoft.t3.concurrent.configuration.model.ThreadPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

/**
 * ThreadPoolConfiguration
 * 全局配置线程池，用于异步编程使用，一般应用于并发请求多个次远程调用
 * @author fengyq
 * @version 1.0
 * @date 2019-07-02
 *
 */
@Configuration
@ConfigurationProperties(prefix = "concurrent")
@ComponentScan("com.thunisoft.t3.concurrent")
@Component
public class ThreadPoolConfiguration {

    /**
     * 默认线程池配置对象
     */
    private ThreadPoolConfig threadpool = new ThreadPoolConfig();

    /**
     * Since 1.0.1-SNAPSHOT
     * 自定义线程池配置对象映射
     */
    private Map<String, ThreadPoolConfig> customThreadPools = new HashMap<>(8);

    /**
     * Description:<b>初始化默认线程池</b>
     *
     * @author LKL
     * @since 2020-08-14 16:04
     * @return 默认线程池
     */
    @Bean("t3-default-completable-executor")
    public Executor defaultExecutor() {
        // 队列满的策略设置为直接执行
        ThreadPoolTaskExecutorInitializer initializer = new ThreadPoolTaskExecutorInitializer();
        CallerRunsPolicy policy = new ThreadPoolExecutor.CallerRunsPolicy();
        return initializer.initializeThreadPool(getThreadpool(), policy);
    }

    public ThreadPoolConfig getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(ThreadPoolConfig threadpool) {
        this.threadpool = threadpool;
    }

    public Map<String, ThreadPoolConfig> getCustomThreadPools() {
        return customThreadPools;
    }

    public void setCustomThreadPools(
            Map<String, ThreadPoolConfig> customThreadPools) {
        this.customThreadPools = customThreadPools;
    }
}
