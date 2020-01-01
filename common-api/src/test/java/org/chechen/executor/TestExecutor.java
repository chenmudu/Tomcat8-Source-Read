package org.chechen.executor;

import lombok.NoArgsConstructor;
import org.chenchen.customer.executor.CustomThreadPoolExecutor;
import org.chenchen.customer.queue.CustomTaskQueue;

import java.util.Random;

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
 * @Date: 2020/1/1 21:17
 * @Description: 测试定制化线程池的功能是否满足基本功能。
 */
public class TestExecutor {
    private static final TestExecutorConfig config = new TestExecutorConfig();

    public static void main(String[] args) {
        testCustomExecutor();
    }

    /**
     *
     * Task-16----Current Thread Name is : pool-1-thread-16     --sleep 120ms
     * Task-11----Current Thread Name is : pool-1-thread-11     --sleep 254ms
     * Task-9----Current Thread Name is : pool-1-thread-9     --sleep 994ms
     * Task-8----Current Thread Name is : pool-1-thread-8     --sleep 1000ms
     * Task-10----Current Thread Name is : pool-1-thread-10     --sleep 1236ms
     * Task-25----Current Thread Name is : pool-1-thread-10     --sleep 112ms
     * Task-3----Current Thread Name is : pool-1-thread-3     --sleep 1809ms
     * Task-19----Current Thread Name is : pool-1-thread-19     --sleep 1900ms
     * Task-28----Current Thread Name is : pool-1-thread-19     --sleep 673ms
     * Task-23----Current Thread Name is : pool-1-thread-9     --sleep 1895ms
     * Task-22----Current Thread Name is : pool-1-thread-11     --sleep 3531ms
     * Task-7----Current Thread Name is : pool-1-thread-7     --sleep 3842ms
     * Task-4----Current Thread Name is : pool-1-thread-4     --sleep 4022ms
     * Task-20----Current Thread Name is : pool-1-thread-20     --sleep 4322ms
     * Task-13----Current Thread Name is : pool-1-thread-13     --sleep 4691ms
     * Task-14----Current Thread Name is : pool-1-thread-14     --sleep 4912ms
     * Task-1----Current Thread Name is : pool-1-thread-1     --sleep 5021ms
     * Task-5----Current Thread Name is : pool-1-thread-5     --sleep 5109ms
     * Task-26----Current Thread Name is : pool-1-thread-10     --sleep 4044ms
     * Task-17----Current Thread Name is : pool-1-thread-17     --sleep 5440ms
     * Task-15----Current Thread Name is : pool-1-thread-15     --sleep 5848ms
     * Task-6----Current Thread Name is : pool-1-thread-6     --sleep 5870ms
     * Task-30----Current Thread Name is : pool-1-thread-9     --sleep 3086ms
     * Task-2----Current Thread Name is : pool-1-thread-2     --sleep 6421ms
     * Task-24----Current Thread Name is : pool-1-thread-8     --sleep 5612ms
     * Task-18----Current Thread Name is : pool-1-thread-18     --sleep 6655ms
     * Task-21----Current Thread Name is : pool-1-thread-16     --sleep 6655ms
     * Task-12----Current Thread Name is : pool-1-thread-12     --sleep 6934ms
     * Task-27----Current Thread Name is : pool-1-thread-3     --sleep 5803ms
     * Task-29----Current Thread Name is : pool-1-thread-19     --sleep 6013ms
     *
     * 从{@link TestExecutorConfig}中可看到：
     * 1. 核心线程数8，最大线程数为20.目前的任务个数为100. 队列的长度为100.
     * 2. 按照JDK线程池的逻辑来讲：任务coreThreadCount = 8的时候就会全部进入队列内。
     *    直到队列已满且无闲置线程后才会去创建线程。此时任务个数30.所以说线程的个数
     *    应该是8.然后等待。
     * 3. 采用定制化的线程池后，我们可以观察什么指数：
     *      3.1     观察执行 任务编号(1 - 核心线程数)的任务 的线程名称和被执行的任务的编号数字是否一致.
     *          (这是为了验证 < {@link TestExecutorConfig#corePoolSize}是否新建线程来执行任务)
     *          这是最基本的要求，即满足原始JDK线程池的执行策略。
     *
     *      3.2     观察执行任务的线程种类个数：理应等同于{@link TestExecutorConfig#maxPoolSize}
     *          (为了验证当任务数大于核心线程数后，此线程池采取的策略)
     *          这是扩展需求,当 > {@link TestExecutorConfig#corePoolSize}后是新建线程执行任务还是直接入队列。‘
     *          即：是否满足优先创建线程，而不是优先进入队列。
     *
     *      3.3     观察执行 任务编号 > 最大线程数任务的 线程的名称：是否是池内已存在的线程。
     *          (这是为了验证当达到最大线程数的时候,会不会进入队列，等待线程池内空闲线程的调度)
     *          这是基本要求，要满足当线程池内调度水平到达峰值后的平缓策略被执行。
     */
    private static void testCustomExecutor() {
        final CustomTaskQueue taskQueue = new CustomTaskQueue(config.getQueueSize());
        CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(true, config.getCorePoolSize(), config.getMaxPoolSize(), config.getKeepAliveTime(), config.getTimeUnit(), taskQueue);
        taskQueue.setParentExecutor(executor);
        for(int i = 0; i < config.getForLoopCount(); i++) {
            executor.execute(new Runner("Task-" + (i + 1)));
        }
    }

    @NoArgsConstructor
    public static class Runner implements Runnable {
        private String currentRunnerName;

        public Runner(String name) {
            this.currentRunnerName = name;
        }

        private Random random = new Random();


        @Override
        public void run() {
            int sleepRandomTime = random.nextInt(7000);
            try {
                Thread.currentThread().sleep(sleepRandomTime);
            } catch (InterruptedException e) {
                //
            }
            System.out.println(currentRunnerName + "----Current Thread Name is : " + Thread.currentThread().getName() + "     --sleep " + sleepRandomTime + "ms");
        }
    }
}
