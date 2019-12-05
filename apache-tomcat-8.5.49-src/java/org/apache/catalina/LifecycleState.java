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
package org.apache.catalina;

/**
 * The list of valid states for components that implement {@link Lifecycle}.
 * See {@link Lifecycle} for the state transition diagram.
 */
public enum LifecycleState {
    /**
     * 新建状态(还未开始) (不可用，无状态)
     */
    NEW(false, null),
    /**
     * 初始化中(正在初始化) (不可用，初始化之前的状态)
     */
    INITIALIZING(false, Lifecycle.BEFORE_INIT_EVENT),
    /**
     * 初始化后(初始化后) (不可用，初始化后的状态)
     */
    INITIALIZED(false, Lifecycle.AFTER_INIT_EVENT),
    /**
     * 开始之前(准备开始) (不可用，开始前的状态)
     */
    STARTING_PREP(false, Lifecycle.BEFORE_START_EVENT),
    /**
     * 开始中(正在开始)  (-可用-， 开始状态)
     */
    STARTING(true, Lifecycle.START_EVENT),
    /**
     * 开始后(已经开始) (-可用-， 开始后状态)
     */
    STARTED(true, Lifecycle.AFTER_START_EVENT),
    /**
     *停止前(还未停止) (-可用-， 停止前状态)
     */
    STOPPING_PREP(true, Lifecycle.BEFORE_STOP_EVENT),
    /**
     * 停止中(正在停止) (不可用，停止中状态)
     */
    STOPPING(false, Lifecycle.STOP_EVENT),
    /**
     * 停止后(已经停止) (不可用，停止后状态)
     */
    STOPPED(false, Lifecycle.AFTER_STOP_EVENT),
    /**
     * 销毁中(正在销毁) (不可用,销毁前状态)
     */
    DESTROYING(false, Lifecycle.BEFORE_DESTROY_EVENT),
    /**
     * 销毁后(已经销毁) (不可用,销毁后状态)
     */
    DESTROYED(false, Lifecycle.AFTER_DESTROY_EVENT),
    /**
     * 失败状态(启动失败) (不可用,已经失败状态)
     */
    FAILED(false, null);

    /**
     * 是否可用(是否可以调用get和set以及生命周期以外的公共方法的判断值)
     */
    private final boolean available;
    /**
     * 生命周期事件
     */
    private final String lifecycleEvent;

    private LifecycleState(boolean available, String lifecycleEvent) {
        this.available = available;
        this.lifecycleEvent = lifecycleEvent;
    }

    /**
     * May the public methods other than property getters/setters and lifecycle
     * methods be called for a component in this state? It returns
     * <code>true</code> for any component in any of the following states:
     *
     * 在这种状态下，可以为组件调用属性getter /setter和生命周期方法以外的公共方法吗?
     * 当容器处于以下状态的时候，他可以去调用。
     * <ul>
     * <li>{@link #STARTING}</li>
     * <li>{@link #STARTED}</li>
     * <li>{@link #STOPPING_PREP}</li>
     * </ul>
     *
     * @return <code>true</code> if the component is available for use,
     *         otherwise <code>false</code>
     */
    public boolean isAvailable() {
        return available;
    }

    public String getLifecycleEvent() {
        return lifecycleEvent;
    }
}
