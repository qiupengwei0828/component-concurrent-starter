package com.thunisoft.t3.concurrent.configuration.initializer.impl;

import com.thunisoft.t3.concurrent.configuration.initializer.ThreadPoolInitializer;
import com.thunisoft.t3.concurrent.configuration.model.ThreadPoolConfig;
import com.thunisoft.t3.concurrent.decorator.ContextCopyDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * Description:<b>ThreadPoolTaskExecutor初始化器</b>
 *
 * @author LKL
 * @since 2020-08-14 17:57
 **/
public class ThreadPoolTaskExecutorInitializer
        implements ThreadPoolInitializer {

    /**
     * Description:<b>线程池初始化逻辑</b>
     *
     * @author LKL
     * @since 2020-08-14 17:54
     * @param threadPoolConfig 线程池配置对象
     * @param reject 自定义Handler
     * @return 初始化好的线程池
     */
    @Override
    public Executor initializeThreadPool(ThreadPoolConfig threadPoolConfig,
            RejectedExecutionHandler reject) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = (threadPoolConfig.getCorePoolSize() == null
                || threadPoolConfig.getCorePoolSize() < 0) ?
                Math.min(16, Runtime.getRuntime().availableProcessors()) :
                threadPoolConfig.getCorePoolSize();
        int maxPoolSize = (threadPoolConfig.getMaxPoolSize() == null
                || threadPoolConfig.getMaxPoolSize() < corePoolSize) ?
                Math.min(128, corePoolSize * 8) :
                threadPoolConfig.getMaxPoolSize();
        String threadNamePrefix =
                threadPoolConfig.getThreadNamePrefix() == null ?
                        "t3-Thread-" :
                        threadPoolConfig.getThreadNamePrefix();
        executor.setTaskDecorator(new ContextCopyDecorator());
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(threadPoolConfig.getQueueCapacity());
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(reject);
        executor.setKeepAliveSeconds(threadPoolConfig.getKeepAliveSeconds());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
