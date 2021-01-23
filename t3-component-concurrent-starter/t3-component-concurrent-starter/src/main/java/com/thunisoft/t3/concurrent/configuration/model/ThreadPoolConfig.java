package com.thunisoft.t3.concurrent.configuration.model;

import java.util.concurrent.RejectedExecutionHandler;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * ThreadPoolConfig
 * 
 * @author fengyq
 * @version 1.0
 * @date 2020-06-22
 * 
 */
public class ThreadPoolConfig {
    
    /**
     * 线程前缀，默认t3-Thread-
     */
    private String threadNamePrefix;

    /**
     * 最小线程数，默认取cpu核数但不超过16线程
     */
    private Integer corePoolSize;

    /**
     * 最大线程数，默认corePoolSize×8但不超过128线程
     */
    private Integer maxPoolSize;

    /** 
     * 队列长度，默认0，即直接用线程执行或调用者同步执行而不在队列等待
     */
    private int queueCapacity = 0;

    /**
     * 线程存活时间，默认30s
     */
    private int keepAliveSeconds = 30;
    

    public ThreadPoolTaskExecutor buildExecutor(RejectedExecutionHandler reject) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        corePoolSize = (corePoolSize == null || corePoolSize < 0) ? Math.min(16, Runtime.getRuntime().availableProcessors()) : corePoolSize;
        maxPoolSize = (maxPoolSize == null || maxPoolSize < corePoolSize) ? Math.min(128, corePoolSize * 8) : maxPoolSize;
        threadNamePrefix = threadNamePrefix == null ? "t3-Thread-" : threadNamePrefix;
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(reject);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    /**
     * @return the corePoolSize
     */
    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * @param corePoolSize the corePoolSize to set
     */
    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    /**
     * @return the maxPoolSize
     */
    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * @param maxPoolSize the maxPoolSize to set
     */
    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * @return the queueCapacity
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * @param queueCapacity the queueCapacity to set
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * @return the keepAliveSeconds
     */
    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    /**
     * @param keepAliveSeconds the keepAliveSeconds to set
     */
    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    /**
     * @return the threadNamePrefix
     */
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    /**
     * @param threadNamePrefix the threadNamePrefix to set
     */
    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

}
