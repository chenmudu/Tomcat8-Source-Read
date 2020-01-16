package org.chenchen.customer.executor;

import lombok.Data;
import org.chenchen.customer.EsurientThreadPoolEexcutor;
import org.chenchen.customer.exception.TaskDoNotExecutedException;
import org.chenchen.customer.queue.CustomizableTaskQueue;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MIT License
 * <p>
 * Copyright (c) 2019 chenmudu (陈晨)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * @Author chenchen6
 * @Date: 2020/1/1 12:17
 * @Description: 定制化线程池：为I/O密集型任务所量身定制。
 *
 * 关于线程池的其他属性你可以直接从当前线程池的父类{@link ThreadPoolExecutor}中获取：
 * {@link ThreadPoolExecutor#getQueue()}
 * {@link ThreadPoolExecutor#getMaximumPoolSize()}
 * {@link ThreadPoolExecutor#getPoolSize()}
 * {@link ThreadPoolExecutor#getCorePoolSize()}
 * {@link ThreadPoolExecutor#getTaskCount()}
 * {@link ThreadPoolExecutor#getActiveCount()}
 * {@link ThreadPoolExecutor#getCompletedTaskCount()}
 * {@link ThreadPoolExecutor#getKeepAliveTime(TimeUnit)}
 * {@link ThreadPoolExecutor#getLargestPoolSize()}
 *
 * 例：CustomizableThreadPoolExecutor executor = CustomizableThreadPoolExecutor.startInitializeCustomThreadPoolExecutor(xxx);
 * int activeAcount = executor.getActiveCount();
 */
@Data
public class CustomizableThreadPoolExecutor extends ThreadPoolExecutor implements EsurientThreadPoolEexcutor {

    //名称传递.
    private static String THREAD_PREFIX ;

    //名称传递。
    private static String THREAD_NAME_PRE;

    //给定可以初始化Name外部方法。
    public static void SET_THREAD_PREFIX_NAME(String threadNamePrefix) {
        THREAD_NAME_PRE = threadNamePrefix;
        setThreadPrefix();
    }

    private static void setThreadPrefix() {
        THREAD_PREFIX = THREAD_NAME_PRE;
        //获取不到所有的线程。
    }
    /**
     * 代表提交但是还未完成的任务数量值。
     * 包括：处于任务队列中的任务以及提交给工作线程,但是工作线程还未执行的任务的总和。
     *
     * 1. getQueueSize + getActiveCount = submmitedTaskCount.get();
     *
     * 2. getQueueSize + getActiveCount + getCompletedTaskCount = getTaskCount;
     *
     * 3. 1 & 2 ==> submmitedTaskCount.get() + getCompletedTaskCount = getTaskCount;
     *
     */
    private AtomicInteger submmitedTaskCount = new AtomicInteger(0);


    /**
     * 防止应用脑回路短路。启用了多个线程去启动这个类。
     * 也可以做成线程安全的单例.懒。
     */
    private volatile static CustomizableThreadPoolExecutor CURRENT_THREAD_POOL_EXECUTOR = null;

    //其实不用也行,声明成Synchronized。大概率情况下不会有多个线程去初始化线程池。
    private static Lock EXECUTOR_LOCK = new ReentrantLock();
    /**
     * 提供一个初始化线程池的方法。
     * @param preStartFlag
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param threadFactory
     * @param handler
     * @return
     */
    public static CustomizableThreadPoolExecutor startInitializeCustomThreadPoolExecutor(boolean preStartFlag, int corePoolSize,
                                                                                         int maximumPoolSize, long keepAliveTime,
                                                                                         TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                                                                         ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        EXECUTOR_LOCK.lock();
        ThreadPoolExecutor executor;
        try {
            if(Objects.nonNull(CURRENT_THREAD_POOL_EXECUTOR)) {
                //防止哪个框架脑回路不对。 一口气起了几个 线程来启这个线程池。
                //Spring容器的启动应该是main线程来启吧。应该只是一个。
                //throw new RuntimeException("The current thread pool has been started");
                return CURRENT_THREAD_POOL_EXECUTOR;
            }

            if(Objects.isNull(threadFactory)) {
                //threadFactory = new CustomDefaultThreadFactory(null, THREAD_PREFIX);
                threadFactory = getDefaultCustomizableThreadFactory(null, THREAD_PREFIX);
            } else {
                //什么也不做. 你给了我一个threadFactory。命名我不管了。这我无法决定。
            }
            executor = null;
            if(Objects.nonNull(threadFactory) && Objects.nonNull(handler)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
            } else if(Objects.isNull(threadFactory) && Objects.isNull(handler)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            } else if(Objects.isNull(threadFactory)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler );
            } else if(Objects.isNull(handler)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            } else {
                //不可能发生的。傻子才不选上面的几个构造器。
            }
        } finally {
            EXECUTOR_LOCK.unlock();
        }
        CURRENT_THREAD_POOL_EXECUTOR = (CustomizableThreadPoolExecutor) executor;
        return CURRENT_THREAD_POOL_EXECUTOR;
    }

    private CustomizableThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new CustomDefaultThreadFactory());
        preStartAllCoreThreads(preStartFlag);
    }

    private CustomizableThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        preStartAllCoreThreads(preStartFlag);
    }

    private CustomizableThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        preStartAllCoreThreads(preStartFlag);
    }

    private CustomizableThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        preStartAllCoreThreads(preStartFlag);
    }

    /**
     * 获取当前线程池。
     * @return
     */
    private ThreadPoolExecutor getCurrentThreadPoolExecutor() {
        Objects.requireNonNull(this.CURRENT_THREAD_POOL_EXECUTOR, "Current ThreadPoolExecutor not initialized!");
        return CURRENT_THREAD_POOL_EXECUTOR;
    }
    /**
     * 线程池处理任务入口。
     * @param runnableTask  当前要被执行的任务。
     */
    @Override
    public void execute(Runnable runnableTask) {
        Objects.requireNonNull(runnableTask, "Current runnable Task can't be null.");
        execute(runnableTask, Integer.valueOf("0"), TimeUnit.SECONDS);
    }


    /**
     * 线程池处理任务入口。
     * @param runnableTask     当前所被执行的任务。
     * @param timeOut           超时时间。
     * @param timeUnit          超时时间单位。
     */
    public void execute(Runnable runnableTask, long timeOut, TimeUnit timeUnit) {
        //还是判断下Null吧。
        Objects.requireNonNull(runnableTask);
        submmitedTaskCount.incrementAndGet();
        try {
            super.execute(runnableTask);
        } catch (RejectedExecutionException exception) {
            try {
                //保证在无法入队的情况仍然尝试去入队
                esurientStrategy2RejectedExecutionException(exception, runnableTask, timeOut, timeUnit);
            } catch (Exception e) {
                submmitedTaskCount.decrementAndGet();
                throw e;
            }
        }
    }

    private static ThreadFactory getDefaultCustomizableThreadFactory(ThreadGroup threadGroup, String threadNamePrefix) {
        return new CustomDefaultThreadFactory(threadGroup, threadNamePrefix);
    }

    /**
     *  线程池处理完后的预留工作。
     * @param runnable      当前已执行过的任务本身。
     * @param throwable     可能抛出的异常。
     */
    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        submmitedTaskCount.decrementAndGet();
        //还可以做更多,例如清除MDC的值？或记录对应此任务的状态?是否有异常发生?或者计算此次任务的执行时间。
        super.afterExecute(runnable, throwable);
    }



    /**
     * 是否预先加载好所有的核心线程,取决于开发者。
     * 所以预留此方法是正确的。
     *
     * @param preStartFlag      是否提前加载核心线程的标志。.
     */
    private void preStartAllCoreThreads(boolean preStartFlag) {
        if(preStartFlag) {
            super.prestartAllCoreThreads();
        }
    }



    /**
     * 饥饿策略，保证任务最大程度的入队并被线程所执行。
     *
     * @param exception         第一次捕获到的异常。
     * @param runnableTask      被线程所要执行的任务。
     * @param timeOut           超时时间。
     * @param unit              超时时间的单位。
     */
    private void esurientStrategy2RejectedExecutionException(RejectedExecutionException exception, Runnable runnableTask, long timeOut, TimeUnit unit) {
        if(super.getQueue() instanceof CustomizableTaskQueue) {
            final CustomizableTaskQueue taskQueue = (CustomizableTaskQueue) super.getQueue();
            try {
                //强制入队,最大限度去执行超出线程池承载能力的任务。
                if(!taskQueue.forceInsertTaskQueue(runnableTask, timeOut, unit)) {
                    throw new RejectedExecutionException("Current queue capacity is full!");
                }
            } catch (InterruptedException e) {
                throw new RejectedExecutionException(e);
            }
        } else {
            throw exception;
        }
    }


//////////////////////////////////////////////////////////////////////////// ////////////////////////////
    //提供获取结果的方法。

    /**
     * 抄袭Spring的方法。同步获取方法(em 不推荐)。
     * 线程执行任务成功后返回null。异常的话会返回异常信息吧。
     * @param task
     * @return
     */
    //@Deprecated
    @Override
    public Future<?> submit(Runnable task) {
        ExecutorService executor = getCurrentThreadPoolExecutor();
        Future<?> currentFutureTask = null;
        try {
            //currentFutureTask = executor.submit(task);
            currentFutureTask = super.submit(task);
        } catch (Exception e) {
            //顺手打个日志.Baby。
            throw new TaskDoNotExecutedException("Current executor :{" +  executor + "}didn't execute current task : { " + task
                    + "}" , e);
        }
        return currentFutureTask;
    }

    /**
     * 抄袭Spring的方法。同步获取方法(em  不推荐。)。
     * 获取结果是依靠提交顺序批量获取。资源消耗大。
     * 待测。
     * @param task
     * @param <T>
     * @return
     */
    //@Deprecated
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        //ExecutorService executor = getCURRENT_THREAD_POOL_EXECUTOR();
        Future<T> currentFutureTask = null;
        try {
            //currentFutureTask = executor.submit(task);
            currentFutureTask = super.submit(task);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentFutureTask;
    }
    //关于

    /**
     * 待测试。
     * @param task
     * @return
     */
    //@Deprecated
    public CompletableFuture<Object> doSubmit(Callable<Object> task) {
        ExecutorService executor = getCurrentThreadPoolExecutor();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }, executor);
    }

    //明日增加关于优雅关闭的内容。




    /**
     * 抄袭Executors.DefaultThreadFactory.
     * 很久之前想改造工厂去修改线程池创建线程种类然后去拿到返回值和异常信息。
     * 啧啧啧。当时真的是特么太年轻。
     *
     *
     * 此线程表明,final修饰的引用对象的值是可以改变的.不变的只是当前引用的地址不变。
     * 而具体的实例里的属性是可以被改变的。
     *
     */
    static class CustomDefaultThreadFactory implements ThreadFactory {

        //区分此池和彼池的标志。
        private static final AtomicInteger poolNumber = new AtomicInteger(1);

        private ThreadGroup group;

        //不是为了能看清目前的线程个数。傻子才去写这个东西。
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        //这种东西就应该去配置文件加载。算了先写死.
        private static String namePrefix;


        //public CustomDefaultThreadFactory() {}
        public CustomDefaultThreadFactory(ThreadGroup group, String threadNamePrefix) {
            this.group = group;
            namePrefix = threadNamePrefix;
            threadFactoryInit();
        }


        //String[] subWayOfXi'An ;

        CustomDefaultThreadFactory() {
            threadFactoryInit();
        }


        //JDK线程池会去调此方法去生产线程的。
        public Thread newThread(Runnable r) {
            //线程不也是一个一个的new出来的。JVM里C++方法new出来的。
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                //线程等级关于window和Linux  欸。算了还是取5.反正影响不大。
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }

        /**
         * 初始化抽离出来。
         */
        private void threadFactoryInit() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            threadNamePrefixHandler();

        }

        private void threadNamePrefixHandler() {
            if(Objects.isNull(namePrefix) || Objects.equals("", namePrefix.trim())) {
                namePrefix = "custom💗-pool-☞" +
                        (poolNumber.getAndIncrement()) +
                        "-thread☀-";
            } else {
                namePrefix = namePrefix + "-pool-" + poolNumber.getAndIncrement() + "-thread-";
            }
        }
    }

    /**
     * 暂时先扔这儿。
     * 下次改。
     */
    public void shutdown() {
        //这个方法默认是会 等待任务完成才会关闭。
        ExecutorService executor = getCurrentThreadPoolExecutor();
        //安全关闭
        if(Objects.nonNull(executor)) {
            //安全关闭  等待任务结束。
            if(true) {  //关闭的标志。可以做成配置。
                executor.shutdown();
            } else {
                for(Runnable currentRunnable : executor.shutdownNow()) {
                    if(currentRunnable instanceof Future) {
                        ((Future<?>)currentRunnable).cancel(true);
                    }
                }
            }

            //设置等待终止的时间。
            //明日修改。
        }
    }
}
