package org.chenchen.customer.executor;

import lombok.Data;
import org.chenchen.customer.EsurientThreadPoolEexcutor;
import org.chenchen.customer.queue.CustomTaskQueue;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
 */
@Data
public class CustomThreadPoolExecutor extends ThreadPoolExecutor implements EsurientThreadPoolEexcutor {

    /**
     * 代表提交但是还未完成的任务数量值。
     * 包括：处于任务队列中的任务以及提交给工作线程,但是工作线程还未执行的任务的总和。
     */
    private AtomicInteger submmitedTaskCount = new AtomicInteger(0);

    public CustomThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        preStartAllCoreThreads(preStartFlag);
    }

    public CustomThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        preStartAllCoreThreads(preStartFlag);
    }

    public CustomThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        preStartAllCoreThreads(preStartFlag);
    }

    public CustomThreadPoolExecutor(boolean preStartFlag, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        preStartAllCoreThreads(preStartFlag);
    }

    /**
     * 线程池处理任务入口。
     * @param runnableTask  当前要被执行的任务。
     */
    @Override
    public void execute(Runnable runnableTask) {
        Objects.requireNonNull(runnableTask, "current runnableTask can't be null.");
        execute(runnableTask, Integer.valueOf("0"), TimeUnit.SECONDS);
    }


    /**
     * 线程池处理任务入口。
     * @param runnableTask     当前所被执行的任务。
     * @param timeOut           超时时间。
     * @param timeUnit          超时时间单位。
     */
    private void execute(Runnable runnableTask, long timeOut, TimeUnit timeUnit) {
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



    /**
     *  线程池处理完后的预留工作。
     * @param runnable      当前已执行过的任务本身。
     * @param throwable     可能抛出的异常。
     */
    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        submmitedTaskCount.decrementAndGet();
        //还可以做更多,例如清除MDC的值？或记录对应此任务的状态?是否有异常发生?
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
        if(super.getQueue() instanceof CustomTaskQueue) {
            final CustomTaskQueue taskQueue = (CustomTaskQueue) super.getQueue();
            try {
                //强制入队,最大限度去执行超出线程池承载能力的任务。
                if(!taskQueue.forceInsertTaskQueue(runnableTask, timeOut, unit)) {
                    throw new RejectedExecutionException("current queue capacity is full!");
                }
            } catch (InterruptedException e) {
                throw new RejectedExecutionException(e);
            }
        } else {
            throw exception;
        }
    }
}
