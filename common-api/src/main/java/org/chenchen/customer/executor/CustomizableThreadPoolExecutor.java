package org.chenchen.customer.executor;

import com.sun.istack.internal.Nullable;
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
 * @Description: 定制化线程池.服务于I/O密集型。
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
 *     int activeAccount = executor.getActiveCount();
 *     log.info("the number of threads currently alive is : {}" + activeAccount);
 */
@Data
public class CustomizableThreadPoolExecutor extends ThreadPoolExecutor implements EsurientThreadPoolEexcutor {
    /**
     * 当关闭线程池的时候是否等待当前的任务执行完毕。
     * 此变量可在线程池已存在 且 未销毁之前设置。
     * for example :
     */
    private Boolean waitForTasksToCompleteOnShutdown = false;

    /**
     *
     * 等待终止当前池内所有任务的时间(可设置)
     * 此变量可在线程池已存在 且 未销毁之前设置。
     */
    private Integer awaitTerminationSeconds = 0;

    //名称传递.
    private static String THREAD_PREFIX ;

    //名称传递。
    private static String THREAD_NAME_PRE;

    /**
     * 初始化线程池之前调用此方法设置对应的线程名称前缀。
     * for example : CustomizableThreadPoolExecutor.SET_THREAD_PREFIX_NAME("example-prefix")
     *               CustomizableThreadPoolExecutor.startInitializeCustomThreadPoolExecutor(xxxxxx)
     * @param threadNamePrefix
     */
    public static void SET_THREAD_PREFIX_NAME(String threadNamePrefix) {
        THREAD_NAME_PRE = threadNamePrefix;
        setThreadPrefix();
    }

    private static void setThreadPrefix() {
        THREAD_PREFIX = THREAD_NAME_PRE;
        //获取不到所有的线程。
    }
    /**
     *  代表提交但是还未完成的任务数量值。
     *  包括：处于任务队列中的任务以及提交给工作线程,但是工作线程还未执行的任务的总和。
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
     * 这里禁止指令重排即可.
     * 变异的DCL。
     */
    private volatile static CustomizableThreadPoolExecutor CURRENT_THREAD_POOL_EXECUTOR = null;

    /**
     * 更好实现方式是使用关键字Synchronized。
     * 因为大概率情况下只有一个线程去调用了{@link CustomizableThreadPoolExecutor#startInitializeCustomThreadPoolExecutor
     * (boolean, int, int, long, java.util.concurrent.TimeUnit,java.util.concurrent.BlockingQueue,
     * java.util.concurrent.ThreadFactory,java.util.concurrent.RejectedExecutionHandler)}方法。
     * 锁的升级不会频繁且很难变成重量级锁。且无锁状态的synchronized和轻量级锁并不比{@link ReentrantLock}锁代价昂贵。
     * 更甚至于比其轻巧。
     */
    private static Lock EXECUTOR_LOCK = new ReentrantLock();



    /**
     * 提供一个初始化线程池的方法。
     * 你可以将其包装放入对应的容器内进行使用。
     * 调用此方法前应该设置某些参数。如：prefixThreadName.
     * @param preStartFlag      {@link }
     * @param corePoolSize      {@link java.util.concurrent.ThreadPoolExecutor#corePoolSize}
     * @param maximumPoolSize   {@link java.util.concurrent.ThreadPoolExecutor#maximumPoolSize}
     * @param keepAliveTime     {@link java.util.concurrent.ThreadPoolExecutor#keepAliveTime}
     * @param unit               keepAliveTime的时间单位。
     * @param workQueue         {@link java.util.concurrent.ThreadPoolExecutor#workQueue}
     * @param threadFactory     {@link java.util.concurrent.ThreadPoolExecutor#threadFactory}  可以为null。取JDK默认线程工厂。
     * @param handler           {@link java.util.concurrent.ThreadPoolExecutor#handler}         可以为null。取当前JDK线程池默认策略。
     * @param threadGroup       {@link CustomizableDefaultThreadFactory#group}                  可以为null。取当前调用线程的线程组。
     * @return  CURRENT_THREAD_POOL_EXECUTOR
     */
    public static CustomizableThreadPoolExecutor startInitializeCustomThreadPoolExecutor(@Nullable boolean preStartFlag, @Nullable int corePoolSize,
                                                                                         @Nullable int maximumPoolSize, @Nullable long keepAliveTime,
                                                                                         @Nullable TimeUnit unit, @Nullable BlockingQueue<Runnable> workQueue,
                                                                                         ThreadFactory threadFactory, RejectedExecutionHandler handler,
                                                                                         ThreadGroup threadGroup) {
        //确保实例化此线程池在锁的范围内。变异的DCL。
        EXECUTOR_LOCK.lock();
        ThreadPoolExecutor executor= null;;
        try {
            if(Objects.nonNull(CURRENT_THREAD_POOL_EXECUTOR)) {
                //throw new RuntimeException("The current thread pool has been started");
                return CURRENT_THREAD_POOL_EXECUTOR;
            }

            if(Objects.isNull(threadFactory)) {
                //threadFactory = new CustomizableDefaultThreadFactory(null, THREAD_PREFIX);
                threadFactory = getDefaultCustomizableThreadFactory(threadGroup, THREAD_PREFIX);
            } else {
                //todo. do nothing. get thread group from your param.
            }
            //构造必要线程池。
            if(Objects.nonNull(threadFactory) && Objects.nonNull(handler)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
            } else if(Objects.isNull(threadFactory) && Objects.isNull(handler)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            } else if(Objects.isNull(threadFactory)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler );
            } else if(Objects.isNull(handler)) {
                executor = new CustomizableThreadPoolExecutor(preStartFlag, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            } else {
                //不可能发生的.
            }
        } finally {
            EXECUTOR_LOCK.unlock();
        }
        CURRENT_THREAD_POOL_EXECUTOR = (CustomizableThreadPoolExecutor) executor;
        return CURRENT_THREAD_POOL_EXECUTOR;
    }


    private CustomizableThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new CustomizableDefaultThreadFactory());
        preStartAllCoreThreads(preStartFlag);
    }

    /**
     *
     * {@link ThreadPoolExecutor#defaultHandler}
     */
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
     * DCL已确保。
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
        return new CustomizableDefaultThreadFactory(threadGroup, threadNamePrefix);
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
        /**
         * for example :
         * if (null == runnable && runnable instanceof Future<?>) {
         *       try {
         *         Object result = ((Future<?>) runnable).get();
         *       } catch (CancellationException ce) {
         *           t = ce;
         *       } catch (ExecutionException ee) {
         *           t = ee.getCause();
         *       } catch (InterruptedException ie) {
         *           Thread.currentThread().interrupt(); // ignore/reset
         *       }
         *     }
         *
         */
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
     * 饥饿策略.保证任务最大程度的入队并被线程所执行。
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
                //因为之前的I/O任务可能瞬间释放大量的线程从而使任务队列处于 非full size的状态。
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
     * 测试通过,可以拿到对应的信息。
     * 线程执行任务成功后返回null。异常的话会返回异常信息吧。
     * @param task
     * @return
     */
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
     *  无论有返回值或无返回值.指定泛型的返回值和未指定泛型的返回值
     *  都会调用execute(Runnable runTask)方法。故此我们只需要在线程池
     *  入口处做当前任务数量的增减即可。无需在此类方法中进行对<code>submmitedTaskCount</code>
     *  值进行改变。
     * @param task
     * @param <T>
     * @return
     */
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

    /**
     *
     * @param   task
     * @return
     */
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


    /**
     * 定制化的默认线程工厂。
     */
    static class CustomizableDefaultThreadFactory implements ThreadFactory {

        /**
         * 区分此池和彼池的标志。
         */
        private static final AtomicInteger poolNumber = new AtomicInteger(1);

        /**
         * 当前线程所属线程组。
         */
        private ThreadGroup group;

        /**
         * 线程标识编号。
         */
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        /**
         * 最终的namePrefix.
         */
        private static String namePrefix;


        //public CustomizableDefaultThreadFactory() {}
        public CustomizableDefaultThreadFactory(ThreadGroup group, String threadNamePrefix) {
            this.group = group;
            namePrefix = threadNamePrefix;
            threadFactoryInit();
        }


        //String[] subWayOfXi'An ;

        CustomizableDefaultThreadFactory() {
            threadFactoryInit();
        }


        /**
         * 此方法将在JDK线程池创建时被调用。
         * @param   runnableTask
         * @return
         */
        public Thread newThread(Runnable runnableTask) {
            //此线程来自于JVM.since JDK 1.5
            Thread workThread = new Thread(group, runnableTask,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (workThread.isDaemon())
                workThread.setDaemon(false);
            if (workThread.getPriority() != Thread.NORM_PRIORITY)
                //线程等级取默认5.
                workThread.setPriority(Thread.NORM_PRIORITY);
            return workThread;
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

        /**
         * 线程名称前缀处理。
         */
        private void threadNamePrefixHandler() {
            if(Objects.isNull(namePrefix) || Objects.equals("", namePrefix.trim())) {
                namePrefix = "customizable-pool-" +
                        (poolNumber.getAndIncrement()) +
                        "-thread-";
            } else {
                namePrefix = namePrefix + "-pool-" + poolNumber.getAndIncrement() + "-thread-";
            }
        }
    }

    /**
     * 关闭此线程池。当前容器销毁的时候。
     * 因为JAVA线程和OS线程是一对一。
     * 线程有多么珍贵。
     * 你最珍贵。
     */
    public void destory() {
        ExecutorService executor = getCurrentThreadPoolExecutor();
        if(Objects.nonNull(executor)) {
            if(waitForTasksToCompleteOnShutdown) {
                //安全关闭。
                executor.shutdown();
            } else {
                //非安全关闭。直接kill。取消任务。
                cancelRemainingTask();
            }
            //必要时等待关闭。
            awaitTerminationIfNecessary(executor);
        }
    }

    /**
     * 对于Future必须调用cancel去取消任务。
     *
     */
    private void cancelRemainingTask() {
        ThreadPoolExecutor executor = getCurrentThreadPoolExecutor();
        if(Objects.nonNull(executor)) {
            for(Runnable currentRunnable : executor.shutdownNow()) {
                if(currentRunnable instanceof Future) {
                    ((Future<?>)currentRunnable).cancel(true);
                }
            }
        }

    }

    /**
     * 如果需要则在指定时间内等待终止。
     * @param executor
     */
    private void awaitTerminationIfNecessary(ExecutorService executor) {
        if(Objects.isNull(executor)) {
            return ;
        }
        if(this.awaitTerminationSeconds > 0) {
            awaitTerminationAndDoSomething(this.awaitTerminationSeconds, TimeUnit.SECONDS, executor);
        }
    }

    /**
     *  Blocks until all tasks have completed execution after a shutdown
     *  request, or the timeout occurs, or the current thread is
     *  interrupted, whichever happens first
     *  会一直阻塞住直到有以下几种情况发生：
     *  1. 所有任务都完成在关闭命令之后。
     *  2. 处于超时状态。
     *  3. 当前此线程中断标志为true。
     * @param awaitTerminationSeconds 等待终止的最大时间。
     * @param unit                      等待终止的最大时间的时间单位。
     * @param executor                  对应线程池。
     */
    private void awaitTerminationAndDoSomething(int awaitTerminationSeconds, TimeUnit unit, ExecutorService executor) {
//        System.out.println("awaitTerminationAndDoSomething start");
        try {
            if(!executor.awaitTermination(awaitTerminationSeconds, unit)) {
                //todo print some log for your machine. it's up to you.
                //give you current time log.
                //System.out.println("awaitTerminationAndDoSomething true true true.");
            }
        } catch (InterruptedException e) {
            //todo print some log for your machine.it's up to you.
        }
        //System.out.println("awaitTerminationAndDoSomething end.");
        //中断调用此线程池关闭的线程。
        Thread.currentThread().interrupt();
    }

    /**
     *
     */
//    private void stopAboutQueue() {
//        CustomizableTaskQueue currentQueue =
//                            getQueue() instanceof CustomizableTaskQueue ?
//                                        (CustomizableTaskQueue)super.getQueue() : null;
//        if(Objects.nonNull(currentQueue)) {
//
//        } else {
//
//        }
//    }
}
