/*
 * @(#)CompletableFutureUtil.java 2019年7月3日 下午9:42:20
 * t3-la
 * Copyright 2019 Thuisoft, Inc. All rights reserved.
 * THUNISOFT PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.thunisoft.t3.concurrent.completable;

import com.thunisoft.t3.concurrent.constant.ThreadPoolConstants;
import com.thunisoft.t3.concurrent.holder.customthreadpool.ExecutorsHolder;
import lombok.extern.slf4j.Slf4j;
import net.tascalate.concurrent.CompletableTask;
import net.tascalate.concurrent.Promise;
import net.tascalate.concurrent.Promises;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * CompletableHelper
 *
 * @author fengyq
 * @version 1.0
 * @date 2019-07-03
 *
 */
@Slf4j
@Component
public class CompletableHelper {

    /**
     * 默认线程池
     */
    private static Executor defaultExecutor;

    /** 全部线程池映射关系 **/
    private static Map<String, Executor> allExecutorMappings = new HashMap<>(8);

    /**
     * Description:<b>根据线程池名称获取指定线程池</b>
     *
     * @author LKL
     * @since 2020-08-14 18:18
     * @param executorName 线程池名称
     * @return 指定线程池
     */
    private static Executor getExecutor(String executorName) {
        return Optional.ofNullable(allExecutorMappings.get(executorName))
                .orElse(defaultExecutor);
    }

    /**
     * 初始化线程池
     * @param defaultExecutor 默认线程池
     */
    @Autowired
    private void init(
            @Qualifier("t3-default-completable-executor") Executor defaultExecutor,
            ExecutorsHolder customThreadPoolConfiguration) {
        CompletableHelper.defaultExecutor = defaultExecutor;
        /**
         * Since 1.0.1-SNAPSHOT，默认线程池和所有自定义线程池均通过这个映射关系获取
         * 由于1.0.1-SNAPSHOT之前的defaultExecutor提供了静态的setter方法，不确定是否有调用方手动set，暂时保留
         * 建议统一使用getExecutor()方法获取
         */
        CompletableHelper.allExecutorMappings = customThreadPoolConfiguration
                .getAllExecutorsMappings();
    }

    /**
     * 开始一个异步编程，获得Promise对象
     * 内部自动使用全局配置的线程池bean对象，不会导致sleuth断链
     * @see com.thunisoft.t3.concurrent.configuration.ThreadPoolConfiguration
     * @param supplier 提供者
     * @return Promise对象
     */
    public static <U> Promise<U> promise(Supplier<U> supplier) {
        return promise(supplier, ThreadPoolConstants.C_KEY_THREAD_POOL_DEFAULT);
    }

    /**
     * Description:<b>获取Promise对象</b>
     *
     * @author LKL
     * @since 2020-08-10 19:02
     * @param supplier 供给者
     * @param threadPoolName 线程池名称
     * @return Promise对象
     */
    public static <U> Promise<U> promise(Supplier<U> supplier,
            String threadPoolName) {
        return CompletableTask
                .supplyAsync(supplier, getExecutor(threadPoolName));
    }

    /**
     * 开始一个异步编程，获得Promise对象
     * @param runnable 异步运行任务
     * @return Promise对象
     */
    public static Promise<Void> promise(Runnable runnable) {
        return promise(runnable, ThreadPoolConstants.C_KEY_THREAD_POOL_DEFAULT);
    }

    /**
     * Description:<b>获取Promise对象</b>
     *
     * @author LKL
     * @since 2020-08-10 19:02
     * @param runnable Runnable对象
     * @param threadPoolName 线程池名称
     * @return Promise对象
     */
    public static Promise<Void> promise(Runnable runnable,
            String threadPoolName) {
        return CompletableTask.runAsync(runnable, getExecutor(threadPoolName));
    }

    /**
     * 直接获取所有结果以list返回，顺序和传入suppliers顺序一致
     * 默认在出错时取消未完成线程，使用waitAll重载方法可改变此行为
     * @param suppliers 同返回类型多个supplier
     * @return 所有结果
     */
    @SafeVarargs
    public static <U> List<U> getAll(Supplier<U>... suppliers) {
        return getAll(true, suppliers);
    }

    /**
     * 直接获取所有结果以list返回，顺序和传入suppliers顺序一致
     * @param cancelRunning 是否在出错时取消未完成线程
     * @param suppliers 同返回类型多个supplier
     * @return 所有结果
     */
    @SafeVarargs
    private static <U> List<U> getAll(boolean cancelRunning,
            Supplier<U>... suppliers) {
        return getAll(cancelRunning,
                ThreadPoolConstants.C_KEY_THREAD_POOL_DEFAULT, suppliers);
    }

    /**
     * Description:<b>获取所有执行结果</b>
     *
     * @author LKL
     * @since 2020-08-11 09:18
     * @param cancelRunning 是否在出错时取消未完成线程
     * @param threadPoolName 线程池名称
     * @param suppliers 提供者
     * @return 执行结果
     */
    @SafeVarargs
    private static <U> List<U> getAll(boolean cancelRunning,
            String threadPoolName, Supplier<U>... suppliers) {
        List<Promise<U>> promiseList = Arrays.stream(suppliers)
                .map(sup -> CompletableTask
                        .supplyAsync(sup, getExecutor(threadPoolName)))
                .collect(Collectors.toList());
        return Promises.all(cancelRunning, promiseList).join();
    }

    /**
     * 等待多个promise都完成
     * 默认等待所有Promise都执行完，使用waitAll重载方法可决定是否在出错时取消未完成线程。
     * @param promises 需等待的promises
     * @return 所有结果
     */
    public static List<?> waitAll(List<Promise<?>> promises) {
        return waitAll(promises, false);
    }

    /**
     * 等待多个promise都完成
     * @param promises 需等待的promises
     * @param cancelRunning 是否在出错时取消未完成线程，对于必须要获得所有结果才有意义的调用推荐为true 
     * @return 所有结果
     */
    public static List<?> waitAll(List<Promise<?>> promises,
            boolean cancelRunning) {
        try {
            // Promises.all内部实现支持出错时取消未完成线程，可以减少资源消耗
            return Promises.all(cancelRunning, promises).join();
        } catch (Exception e) {
            /*
             * 线程池等待异常是MultiTargetException，堆栈只能显示到waitAll那一行代码，对排错意义不大
             * 且p1异常而p2无异常时包含的异常也是2个，不易理解
             * 推荐使用Promise.exceptionally处理异常，此处在异常消息中打印提示
             */
            log.error("promises waitall error! " + e.getMessage()
                    + "，请在各处Promise.exceptionally中处理异常和打印详情！");
            return null;
        }
    }

    /**
     * 获取异步编程使用的默认线程池<br>
     * 内部线程池是自动注入的名为defaultExecutor的bean
     * @return the defaultExecutor
     */
    public static Executor getDefaultExecutor() {
        return defaultExecutor;
    }

    /**
     * 设置异步编程使用的默认线程池<br>
     * CompletableHelper自动获得defaultExecutor bean无需外部设置线程池，仅应在无bean时调用set方法
     * @param defaultExecutor 默认线程池
     */
    public static void setDefaultExecutor(Executor defaultExecutor) {
        if (CompletableHelper.defaultExecutor != null) {
            log.warn(
                    "CompletableHelper自动获得defaultExecutor bean无需外部设置线程池，仅应在无bean时调用set方法");
        }
        CompletableHelper.defaultExecutor = defaultExecutor;
    }

}
