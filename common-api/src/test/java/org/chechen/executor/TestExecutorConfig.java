package org.chechen.executor;

import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

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
 * @Date: 2020/1/1 21:18
 * @Description:
 */
@Data
public class TestExecutorConfig implements Serializable {

    /**
     * 核心线程数
     */
    private final int corePoolSize = 5;

    /**
     * 最大线程数
     */
    private final int maxPoolSize = 10;

    /**
     * 空闲线程存活的时间
     */
    private final long keepAliveTime = 60;

    /**
     * 空闲线程存活时间单位
     */
    private final TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 任务个数。外部测试数据循环的次数
     */
    private final long forLoopCount = 28;

    /**
     * 任务队列的长度。
     */
    private final int queueSize = 20;
}
