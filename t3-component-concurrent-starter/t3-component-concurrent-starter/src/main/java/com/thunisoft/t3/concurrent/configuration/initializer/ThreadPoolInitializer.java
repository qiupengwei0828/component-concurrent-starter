package com.thunisoft.t3.concurrent.configuration.initializer;

import com.thunisoft.t3.concurrent.configuration.model.ThreadPoolConfig;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * Description:<b>线程池初始化器</b>
 *
 * @author LKL
 * @since 2020-08-14 17:52
 **/
public interface ThreadPoolInitializer {

    /**
     * Description:<b>线程池初始化逻辑</b>
     *
     * @author LKL
     * @since 2020-08-14 17:54
     * @param threadPoolConfig 线程池配置对象
     * @param reject 自定义Handler
     * @return 初始化好的线程池
     */
    default Executor initializeThreadPool(ThreadPoolConfig threadPoolConfig,
            RejectedExecutionHandler reject) {
        return null;
    }
}
