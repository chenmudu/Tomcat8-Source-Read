/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.util.threads;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * As task queue specifically designed to run with a thread pool executor. The
 * task queue is optimised to properly utilize threads within a thread pool
 * executor. If you use a normal queue, the executor will spawn threads when
 * there are idle threads and you wont be able to force items onto the queue
 * itself.
 *
 * 任务队列专门设计为使用线程池执行器运行。任务队列经过优化以正确利用线程池执行
 * 程序中的线程。如果使用普通队列，executor将在有空闲线程时产生线程，并且您无法
 * 将任务强制添加到队列本身。
 *
 * 自定义的一个任务队列。目的：强制将任务加入到任务队列中.
 * 原因：为了实现I/O密集型的任务，区分于JDK原生为了CPU密集型而设计的线程池及其队列。
 */
public class TaskQueue extends LinkedBlockingQueue<Runnable> {

    private static final long serialVersionUID = 1L;

    /**
     * tomcat 自定义线程池的引用。
     * 不让序列化
     */
    private transient volatile ThreadPoolExecutor parent = null;

    // No need to be volatile. This is written and read in a single thread
    // (when stopping a context and firing the  listeners)
    private Integer forcedRemainingCapacity = null;

    public TaskQueue() {
        super();
    }

    public TaskQueue(int capacity) {
        super(capacity);
    }

    public TaskQueue(Collection<? extends Runnable> c) {
        super(c);
    }

    /**
     * 设置当前队列的父线程池为：Tomcat- I/0 - Executor
     * @param tp
     */
    public void setParent(ThreadPoolExecutor tp) {
        parent = tp;
    }

    public boolean force(Runnable o) {
        if (parent == null || parent.isShutdown()) throw new RejectedExecutionException("Executor not running, can't force a command into the queue");
        return super.offer(o); //forces the item onto the queue, to be used if the task is rejected
    }

    /**
     * 当释放了大量空闲线程的时候，我们去尝试放入队列中。
     * @param o
     * @param timeout
     * @param unit
     * @return  将任务强制加入队列是否成功。
     * @throws InterruptedException
     */
    public boolean force(Runnable o, long timeout, TimeUnit unit) throws InterruptedException {
        //tomcat线程池已为null,或当tomcat的线程池关闭时。
        if (parent == null || parent.isShutdown()) {
            throw new RejectedExecutionException("Executor not running, can't force a command into the queue");
        }
        //forces the item onto the queue, to be used if the task is rejected
        //强制的将其放入TaskQueue中，当任务被拒绝时去使用  重写了offer方法。
        return super.offer(o,timeout,unit);
    }

    /**
     * 原本的逻辑是:队列不满，不创建线程，是为了CPU密集型而作。
     *
     * 修改的逻辑：当达到corePoolSize的时候，优先创建线程,后进行入队。
     *
     *
     * 原因：CPU密集型不要大量线程,原因在于线程忙于计算,创建多的线程会导致上下文的切换，降低任务的处理速度。
     *
     * 而I/O密集型：大部分在阻塞的等待I/O的read & write，增加线程数，提高并发度，尽可能多的处理任务。
     *
     * 而原生JDK的处理逻辑中第二步,会先进队列后，直到队列满才会去创建临时线程.此时改写逻辑即可。
     *
     */
    @Override
    public boolean offer(Runnable o) {
      //we can't do any checks
        //这里：当不是tomcat自定义的ThreadPoolExecutor的时候，会走默认的LinkedBolockingQueue的逻辑。
        if (parent==null) {
            return super.offer(o);
        }
        //we are maxed out on threads, simply queue the object
        /**
         * 线程池内的数量 == 最大线程数。 无法创建线程，只能放入到队列内了。
         */
        if (parent.getPoolSize() == parent.getMaximumPoolSize()) {
            return super.offer(o);
        }
        //we have idle threads, just add it to the queue
        /**
         * 判断tomcat自定义的ThreadPoolExecutor的已提交但没完成的任务数量。
         * 如果任务数量 <= 当前线程池线程数量.说明：有空闲线程,加入则可立即执行。
         */
        if (parent.getSubmittedCount()<=(parent.getPoolSize())) {
            return super.offer(o);
        }
        //if we have less threads than maximum force creation of a new thread
        /**
         * 如果当前的线程池数量 < 最大线程数量。立刻返回false。JDK的线程池就会去优先创建线程。
         * 这就达到了先创建线程，后加入队列。
         * {@link java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable) 中的第二步。}
         *
         * 这样会达到一个什么样的目的呢：当大于corePoolSize的时候，不是直接入队，而是先创建临时线程,
         * 当总线程大于maxPoolSize的时候，才会入队。保证了足够的线程去执行任务。符合I/O密集型适用场景。
         */
        if (parent.getPoolSize()<parent.getMaximumPoolSize()) {
            return false;
        }
        //if we reached here, we need to add it to the queue
        return super.offer(o);
    }


    @Override
    public Runnable poll(long timeout, TimeUnit unit)
            throws InterruptedException {
        Runnable runnable = super.poll(timeout, unit);
        if (runnable == null && parent != null) {
            // the poll timed out, it gives an opportunity to stop the current
            // thread if needed to avoid memory leaks.
            parent.stopCurrentThreadIfNeeded();
        }
        return runnable;
    }

    @Override
    public Runnable take() throws InterruptedException {
        if (parent != null && parent.currentThreadShouldBeStopped()) {
            return poll(parent.getKeepAliveTime(TimeUnit.MILLISECONDS),
                    TimeUnit.MILLISECONDS);
            // yes, this may return null (in case of timeout) which normally
            // does not occur with take()
            // but the ThreadPoolExecutor implementation allows this
        }
        return super.take();
    }

    @Override
    public int remainingCapacity() {
        if (forcedRemainingCapacity != null) {
            // ThreadPoolExecutor.setCorePoolSize checks that
            // remainingCapacity==0 to allow to interrupt idle threads
            // I don't see why, but this hack allows to conform to this
            // "requirement"
            return forcedRemainingCapacity.intValue();
        }
        return super.remainingCapacity();
    }

    public void setForcedRemainingCapacity(Integer forcedRemainingCapacity) {
        this.forcedRemainingCapacity = forcedRemainingCapacity;
    }

}
