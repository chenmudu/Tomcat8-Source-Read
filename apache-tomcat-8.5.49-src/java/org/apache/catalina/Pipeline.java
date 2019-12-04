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

import java.util.Set;

/**
 * <p>Interface describing a collection of Valves that should be executed
 * in sequence when the <code>invoke()</code> method is invoked.  It is
 * required that a Valve somewhere in the pipeline (usually the last one)
 * must process the request and create the corresponding response, rather
 * than trying to pass the request on.</p>
 * 当<code>invoke</code>方法被调用的时候，这个接口用于去描述多个Vlue(阀门)
 * 在队列中如何被执行的.在pipeline的某个地方(通常是最后一个)，它是必须放置一个
 * Valve()的。而且她必须去处理当前这个请求并响应，而不能只是试图去传递请求。
 *
 * <p>There is generally a single Pipeline instance associated with each
 * Container.  The container's normal request processing functionality is
 * generally encapsulated in a container-specific Valve, which should always
 * be executed at the end of a pipeline.  To facilitate this, the
 * <code>setBasic()</code> method is provided to set the Valve instance that
 * will always be executed last.  Other Valves will be executed in the order
 * that they were added, before the basic Valve is executed.</p>
 *
 *
 * Value相当于Filter,Pipeline就是FilterChain。Connector和Container
 * (包括Engine->Host->Context->ServletWrapper)的交互。是基于Pipeline去进行传递的。
 * 每个容器与容器，以及容器和Connector的交互都是通过一个一个的Value去进行交互。多个Value
 * 存在于一个Pipeline内，所以所有的Value去交互的时候都是通过所依附的Pipeline去进行交互。
 * 这里的交互不仅仅包括请求而且还有对应的相应。一个请求和一个响应才叫做一整个请求的生命周期。
 * 这样的传递直到最后一个StandardWrapperValve拿到资源然后去调用Servlet后才会去返回。
 * @author Craig R. McClanahan
 * @author Peter Donald
 * @translator chenchen6(chenmudu@gmail.com/chenchen6@tuhu.cn)
 */
public interface Pipeline {

    /**
     * @return the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).
     */
    public Valve getBasic();


    /**
     * <p>Set the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).  Prior to setting the basic Valve,
     * the Valve's <code>setContainer()</code> will be called, if it
     * implements <code>Contained</code>, with the owning Container as an
     * argument.  The method may throw an <code>IllegalArgumentException</code>
     * if this Valve chooses not to be associated with this Container, or
     * <code>IllegalStateException</code> if it is already associated with
     * a different Container.</p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    public void setBasic(Valve valve);


    /**
     * <p>Add a new Valve to the end of the pipeline associated with this
     * Container.  Prior to adding the Valve, the Valve's
     * <code>setContainer()</code> method will be called, if it implements
     * <code>Contained</code>, with the owning Container as an argument.
     * The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to
     * be associated with this Container, or <code>IllegalStateException</code>
     * if it is already associated with a different Container.</p>
     *
     * <p>Implementation note: Implementations are expected to trigger the
     * {@link Container#ADD_VALVE_EVENT} for the associated container if this
     * call is successful.</p>
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException if this Container refused to
     *  accept the specified Valve
     * @exception IllegalArgumentException if the specified Valve refuses to be
     *  associated with this Container
     * @exception IllegalStateException if the specified Valve is already
     *  associated with a different Container
     */
    public void addValve(Valve valve);


    /**
     * @return the set of Valves in the pipeline associated with this
     * Container, including the basic Valve (if any).  If there are no
     * such Valves, a zero-length array is returned.
     */
    public Valve[] getValves();


    /**
     * Remove the specified Valve from the pipeline associated with this
     * Container, if it is found; otherwise, do nothing.  If the Valve is
     * found and removed, the Valve's <code>setContainer(null)</code> method
     * will be called if it implements <code>Contained</code>.
     *
     * <p>Implementation note: Implementations are expected to trigger the
     * {@link Container#REMOVE_VALVE_EVENT} for the associated container if this
     * call is successful.</p>
     *
     * @param valve Valve to be removed
     */
    public void removeValve(Valve valve);


    /**
     * @return the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).
     */
    public Valve getFirst();


    /**
     * Returns true if all the valves in this pipeline support async, false otherwise
     * @return true if all the valves in this pipeline support async, false otherwise
     */
    public boolean isAsyncSupported();


    /**
     * @return the Container with which this Pipeline is associated.
     */
    public Container getContainer();


    /**
     * Set the Container with which this Pipeline is associated.
     *
     * @param container The new associated container
     */
    public void setContainer(Container container);


    /**
     * Identifies the Valves, if any, in this Pipeline that do not support
     * async.
     *
     * @param result The Set to which the fully qualified class names of each
     *               Valve in this Pipeline that does not support async will be
     *               added
     */
    public void findNonAsyncValves(Set<String> result);
}
