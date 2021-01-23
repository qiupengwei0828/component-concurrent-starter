# t3-component-concurrent-starter

[TOC]

异步编程封装组件
* 首要目标是，解决异步编程可能导致调用链断开的问题
* 内部自动创建线程池bean，可通过配置文件进行线程池参数调整
* 简化和封装异步线程编程



## 关于java8异步编程
java8提供了`CompletableFuture`支持分阶段异步编程，简化了异步编程方式，不过默认提供的接口存在一些使用问题和误区：
* 所有接口都可选择性的指定线程池，如常用接口`CompletableFuture.supplyAsync(Supplier<U> supplier)`，开发人员可能不容易每次都记得传入外部线程池，在代码复查时发现大量此情况。当不指定外部线程池时会共用内部默认线程池，默认线程数=cpu核心数，即4核cpu时仅4线程，任务队列长度较长，在大并发时会有相当多任务堆积，相当于降低了性能；
* 在使用sleuth追踪调用链时，用CompletableFuture内建线程池会导致调用链截断，这一点也要求必须指定外部线程池，且必须声明为bean，sleuth会代理线程池bean以保证调用链信息透传；
* ` CompletableFuture`在操作多个Future对象时的allOf、anyOf接口返回`CompletableFuture<Void>`，即不传递每个Future的返回结果，不便于外部获取
* `CompletableFuture`不支持超时、延迟（java8中不支持），`CompletableFuture.cancel`不支持取消Running状态线程



## 引入组件
```xml
    <dependency>
        <groupId>com.thunisoft.t3</groupId>
        <artifactId>t3-component-concurrent-starter</artifactId>
        <version>1.0.0</version>
    </dependency>

```
组件内部创建了线程池，配置方式为：
```yaml
concurrent:
  threadpool:
    thread-name-prefix: t3-ba-Thread-
    core-pool-size: 16
    max-pool-size: 128
    queue-capacity: 0
    keep-alive-seconds: 30
  # Since 1.0.1-SNAPSHOT 自定义线程池配置
  custom-thread-pools:
    # 自定义线程池名称，当不希望任务跑在默认线程池时，使用该名称指定
    pdfThreadPool:
      thread-name-prefix: t3-pdf-Thead-
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 1
      keep-alive-seconds: 20
    # 自定义线程池名称，当不希望任务跑在默认线程池时，使用该名称指定
    logThreadPool:
      thread-name-prefix: t3-log-Thead-
      core-pool-size: 3
      max-pool-size: 6
      queue-capacity: 1
      keep-alive-seconds: 15
```
完整配置清单

|配置项|含义|
|--|--|
|concurrent.threadpool.threadNamePrefix|线程名前缀，默认t3-Thread-，建议主动定义线程前缀|
|concurrent.threadpool.corePoolSize|最小线程数，默认取cpu核数但不超过16线程|
|concurrent.threadpool.maxPoolSize|最大线程数，默认取cpu核数×8但不超过128线程|
|concurrent.threadpool.queueCapacity|队列长度，默认为0即不使任务在队列等待，线程池队列满处理策略为CallerRunsPolicy会使任务由调用者直接执行|
|concurrent.threadpool.keep-alive-seconds|线程存活时间，默认30s|

> 关于线程池配置  
  IO密集型应用：可以使用较大的线程池，参考公式：cpu核数×((线程等待时间+线程占cpu时间)/线程占cpu时间)，设置较小等待队列。  
  计算密集型应用：推荐最大线程池=cpu核数，设定较大等待队列  
以微服务并发调用为场景，线程池使用主要属于IO密集型。

## 如何使用

### 简单使用
从`CompletableHelper.promise`开始，同步改成异步

```java
// get多条数据，分别get如下：
Promise<MsAjJc> p1 = CompletableHelper.promise(()->msAjJcApi.getMsAjJc(ajbh, jbfy));
Promise<MsAjJa> p2 = CompletableHelper.promise(()->msAjJaApi.getMsAjJa(ajbh, jbfy));
// 继续提交多个promise，至少创建2个或以上promise才有必要使用多线程，注意不要只使用1个线程然后立即get/join，这样还不如不用

// 使用join或get依次阻塞获得数据，get抛出Checked Exception必须显式捕获，join抛出Unchecked Exception非必须捕获
MsAjJc msAjJc = p1.join();
MsAjJa msAjJa = p2.join();
```
同类型异步获取为集合并整体计算

```java
// 一起get如下，可对同类数据数据使用getAll，方便取数据后整体计算
List<ResponseObject> responseList = CompletableHelper.getAll(
    ()->sxApi.getYcsxCount(ajId, jbfy), 
    ()->sxApi.getKcsxCount(ajId, jbfy));
int sxCount = responseList.stream().mapToInt(ResponseObject::getCount).sum();
```

### 分步处理
两次远程调用，分别get数据并有各自的后续处理，后续处理在thenApply或thenAccept方法中完成，使程序前后逻辑更连续。
```java
CompletableHelper.waitAll(
    CompletableHelper.promise(()->msAjJcApi.getMsAjJc(ajbh, jbfy)).thenAccept(msAjJc->{
        if (msAjJc != null) {
            laxxVO.setAjjcBh(msAjJc.getBh());
            BeanUtils.copyProperties(msAjJc, ajxxVo)
        }
    }),
    CompletableHelper.promise(()->msAjJaApi.getMsAjJa(ajbh, jbfy)).thenAccept(msAjJa->{
        if (msAjJa != null) {
            laxxVO.setAjJaBh(msAjJa.getBh());
            BeanUtils.copyProperties(msAjJa, lajaVo, ObjectUtil.getNullPropertyNames(msAjJa));
        }
    })
);
```
> 后续处理包括thenAccept/thenAcceptAsync、thenApply/thenApplyAsync、whenComplete/whenCompleteAsync等;  
几个方法的区别：thenApply可将结果一直传递下去，进行多个步骤处理；thenAccept不再向下传递结果，相当于只应放在最后一步；whenComplete方法会传递异常对象；
关于结尾是否带Async：不带Async结尾的方法是待promise部分执行完后交回原线程执行，纯内存操作推荐此类方法；带Async的方法是继续使用线程池执行，通常对较耗时的如远程调用可使用此类方法；

### 分步处理+数据整合
先远程调用获取list数据，获得结果后整合到本方法内pojo，整合两次调用结果使用thenCombine
```java
return CompletableHelper.promise(() -> msAjApi.getMsAjVOPageData(pageQuery)) // 查list
        .thenApplyAsync(msajList -> {
            // 处理案号
        }).thenCombineAsync(CompletableHelper.promise(() -> { // 查count，并与list组合
            if (tableParam.isRequestCount()) {
                ResponseObject countRes = msAjApi.countMsAjVO(pageQuery);
                return Objects.nonNull(countRes) ? countRes.getTotal() : 0;
            }
            return 0;
        }), (msajList, count) -> {
            return new PageableData(msajList, count);
        }).join();
```
> 注意这里由于后续有thenCombineAsync整合两个结果，前面应用thenApplyAsync使结果传递下来，不用thenAccept/thenAcceptAsync

## CHANGELOGS

### 1.0.1-SNAPSHOT
- 除了默认线程池，增加自定义线程池的配置，允许手动指定不同的线程池
- 支持请求上下文自动复制，防止异步线程中无法获取用户登录信息
### 1.0.0
首次发布