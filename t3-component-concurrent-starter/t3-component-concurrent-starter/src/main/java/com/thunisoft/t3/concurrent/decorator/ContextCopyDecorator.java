package com.thunisoft.t3.concurrent.decorator;

import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Description:<b>请求上下文复制装饰器</b>
 *
 * @author LKL
 * @since 2020-08-21 09:44
 **/
public class ContextCopyDecorator implements TaskDecorator {
    /**
     * Decorate the given {@code Runnable}, returning a potentially wrapped
     * {@code Runnable} for actual execution.
     * @param runnable the original {@code Runnable}
     * @return the decorated {@code Runnable}
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        RequestAttributes context = RequestContextHolder
                .currentRequestAttributes();
        return () -> {
            try {
                RequestContextHolder.setRequestAttributes(context);
                runnable.run();
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }
}
