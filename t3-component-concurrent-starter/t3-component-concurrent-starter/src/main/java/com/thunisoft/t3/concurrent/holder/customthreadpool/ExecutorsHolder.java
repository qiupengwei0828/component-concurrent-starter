package com.thunisoft.t3.concurrent.holder.customthreadpool;

import com.thunisoft.t3.concurrent.configuration.ThreadPoolConfiguration;
import com.thunisoft.t3.concurrent.configuration.initializer.impl.ThreadPoolTaskExecutorInitializer;
import com.thunisoft.t3.concurrent.configuration.model.ThreadPoolConfig;
import com.thunisoft.t3.concurrent.constant.ThreadPoolConstants;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Description:<b>线程池封装类，包含默认线程池和自定义线程池</b>
 *
 * @author LKL
 * @since 2020-08-13 11:41
 **/
@Component
public class ExecutorsHolder implements ApplicationContextAware {

    /** 线程池配置 **/
    @Autowired
    private ThreadPoolConfiguration threadPoolConfiguration;

    /** 默认线程池 **/
    @Autowired
    @Qualifier("t3-default-completable-executor")
    private Executor defaultExecutor;

    /** 全部线程池映射关系 **/
    /** Key为配置文件中指定的线程池名称，Value为初始化好的线程池对象 **/
    private Map<String, Executor> allExecutorsMappings = new HashMap<>(8);

    /**
     * Set the ApplicationContext that this object runs in.
     * Normally this call will be used to initialize the object.
     * <p>Invoked after population of normal bean properties but before an init callback such
     * as {@link InitializingBean#afterPropertiesSet()}
     * or a custom init-method. Invoked after {@link ResourceLoaderAware#setResourceLoader},
     * {@link ApplicationEventPublisherAware#setApplicationEventPublisher} and
     * {@link MessageSourceAware}, if applicable.
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws ApplicationContextException in case of context initialization errors
     * @throws BeansException if thrown by application context methods
     * @see BeanInitializationException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
            if (context
                    .getBeanFactory() instanceof DefaultListableBeanFactory) {
                //为防止调用链断开，将自定义线程池动态注入IOC容器中，beanName指定的是配置文件中指定的线程池名称
                DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context
                        .getBeanFactory();
                threadPoolConfiguration.getCustomThreadPools().forEach(
                        (customThreadPoolName, customThreadPoolConfig) -> {
                            //初始化线程池对象
                            ThreadPoolTaskExecutor customThreadPoolTaskExecutor = initializeCustomThreadPoolTaskExecutor(
                                    customThreadPoolConfig);
                            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                                    .genericBeanDefinition(
                                            ThreadPoolTaskExecutor.class,
                                            () -> customThreadPoolTaskExecutor);
                            beanFactory.registerBeanDefinition(
                                    customThreadPoolName,
                                    beanDefinitionBuilder.getBeanDefinition());
                            //将每一个自定义线程池放入映射关系中
                            allExecutorsMappings.put(customThreadPoolName,
                                    customThreadPoolTaskExecutor);
                        });
                //将默认线程池放入映射关系中，Key使用常量defaultThreadPool
                this.allExecutorsMappings
                        .put(ThreadPoolConstants.C_KEY_THREAD_POOL_DEFAULT,
                                defaultExecutor);
            }
        }
    }

    /**
     * Description:<b>初始化自定义线程池</b>
     *
     * @author LKL
     * @since 2020-08-14 17:42
     * @param customThreadPoolConfig 自定义线程池配置对象
     */
    private ThreadPoolTaskExecutor initializeCustomThreadPoolTaskExecutor(
            ThreadPoolConfig customThreadPoolConfig) {
        ThreadPoolTaskExecutorInitializer initializer = new ThreadPoolTaskExecutorInitializer();
        ThreadPoolExecutor.CallerRunsPolicy policy = new ThreadPoolExecutor.CallerRunsPolicy();
        return (ThreadPoolTaskExecutor) initializer
                .initializeThreadPool(customThreadPoolConfig, policy);
    }

    public Map<String, Executor> getAllExecutorsMappings() {
        return allExecutorsMappings;
    }

}
