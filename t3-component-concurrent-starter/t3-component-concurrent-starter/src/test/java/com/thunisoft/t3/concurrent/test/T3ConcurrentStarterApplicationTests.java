package com.thunisoft.t3.concurrent.test;

import com.thunisoft.t3.concurrent.completable.CompletableHelper;
import com.thunisoft.t3.concurrent.configuration.ThreadPoolConfiguration;
import com.thunisoft.t3.concurrent.configuration.model.ThreadPoolConfig;
import lombok.extern.slf4j.Slf4j;
import net.tascalate.concurrent.Promise;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * T3ConcurrentStarterApplicationTests
 *
 * @author fengyq
 * @version 1.0
 * @date 2019-07-11
 *
 */
@Slf4j
public class T3ConcurrentStarterApplicationTests {

    @BeforeClass
    public static void init() {
        // 为juint初始化线程池
        if (CompletableHelper.getDefaultExecutor() == null) {
            ThreadPoolConfiguration config = new ThreadPoolConfiguration();
            config.setThreadpool(new ThreadPoolConfig());
            CompletableHelper.setDefaultExecutor(config.defaultExecutor());
        }
    }

    @Test
    public void testGetAll() {
        // getAll，多线程异步调用并组合为list返回，直接传入多个Supplier
        List<String> result = CompletableHelper.getAll(() -> {
            threadSleep(500);
            return "01";
        }, () -> {
            threadSleep(100);
            return "02";
        }, () -> {
            return "03";
        });
        Assert.isTrue(
                "01".equals(result.get(0)) && "02".equals(result.get(1)) && "03"
                        .equals(result.get(2)), "返回结果与传入顺序一致");
        log.info("getAll ok! result: {}", result);
    }

    private void threadSleep(int time) {
        try {
            if (time > 0) {
                TimeUnit.MILLISECONDS.sleep(time);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWaitAll() {
        List<Object> resultList = new ArrayList<>();

        Promise<Void> p1 = CompletableHelper.promise(() -> {
            int sleep = 1000;
            threadSleep(sleep);
            return Arrays.asList(new Integer[] { sleep });
        }).thenAcceptAsync(s -> {
            resultList.add("p1");
        });
        Promise<?> p2 = CompletableHelper.promise(() -> {
            int sleep = 200;
            threadSleep(sleep);
            return sleep;
        }).whenComplete((s, ex) -> {
            resultList.add("p2");
        });
        Promise<Void> p3 = CompletableHelper.promise(() -> {
            int sleep = 0;
            threadSleep(sleep);
            Map<String, Object> map = new HashMap<>();
            map.put("sleep", sleep);
            return map;
        }).thenAccept(s -> {
            resultList.add("p3");
        });
        Promise<Void> p4 = CompletableHelper.promise(() -> {
            int sleep = 500;
            threadSleep(sleep);
            Map<String, Object> map = new HashMap<>();
            map.put("sleep", sleep);
            return map;
        }).thenAccept(s -> {
            resultList.add("p4");
        });
        List<?> result = CompletableHelper
                .waitAll(Arrays.asList(p1, p2, p3, p4));

        resultList.add("afterWaitAll");
        Assert.isTrue(
                "afterWaitAll".equals(resultList.get(resultList.size() - 1)),
                "waitAll应阻塞等待全部线程执行完毕！");
        // 应该在testInMethod所有执行后打印
        log.info("testWaitAll ok! result:{}", result);
    }

    @Test
    public void testCombine() {
        List<String> inputList = Arrays.asList("01", "02", "03");
        Promise<Map<String, Object>> p = CompletableHelper.promise(() -> {
            return new ArrayList<>(inputList);
        }).thenCombineAsync(CompletableHelper.promise(() -> inputList.size()),
                (list, size) -> {
                    Map<String, Object> result = new HashMap<String, Object>();
                    result.put("list", list);
                    result.put("size", size);
                    return result;
                });
        Map<String, Object> result = p.join();
        Assert.isTrue(result.containsKey("list") && result.containsKey("size")
                        && inputList.equals(((List<?>) result.get("list")))
                        && inputList.size() == (Integer) result.get("size"),
                "Combine应组合两次promise结果");
        log.info("testCombine ok! result:{}", result);

    }

}
