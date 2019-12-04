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

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 * <p>A <b>Valve</b> is a request processing component associated with a
 * particular Container.  A series of Valves are generally associated with
 * each other into a Pipeline.  The detailed contract for a Valve is included
 * in the description of the <code>invoke()</code> method below.</p>
 *
 * 一个Valve是与特定容器相关联的请求处理的一个组件。多个Value对象通常相互链接成
 * 一个Pipeline。在下面的<code>invoke()</code>方法可以看到详细的内容。
 * (Pipeline由多个Value组成,而一个Value与对应容器中用来处理请求的一个组件.)
 *
 * <b>HISTORICAL NOTE</b>:  The "Valve" name was assigned to this concept
 * because a valve is what you use in a real world pipeline to control and/or
 * modify flows through it.
 * “Valve”的名字被赋予了这个概念，是因为一个Valve是你在现实世界中用来控制和/或修改
 *  所有流经它的物体。
 *
 *
 * Value(阀门)作用：拦截请求，并在到达目标前做一些骚操作，类似于Servlet中的过滤器(虽然这里非常不恰当)，
 * 而这个Value可以在任何容器类组件中出现。所以Value经常被用来记录客户端和服务器的信息，这种技术被称为
 * 请求转储(request dumping)。请求转储记录的内容包括请求/相应的头部信息和对应的Cookie至文件中。
 * @author Craig R. McClanahan
 * @author Gunnar Rjnning
 * @author Peter Donald
 * @translator chenchen6(chenmudu@gmail.com/chenchen6@tuhu.cn)
 */
public interface Valve {


    //-------------------------------------------------------------- Properties

    /**
     * @return the next Valve in the pipeline containing this Valve, if any.
     *          如果存在下一个阀门，就去获取这个阀门。
     */
    public Valve getNext();


    /**
     * Set the next Valve in the pipeline containing this Valve.
     * 在包含这个阀门(Value)的管道中去设置下一个阀门(Value)。
     *
     * @param valve The new next valve, or <code>null</code> if none
     */
    public void setNext(Valve valve);


    //---------------------------------------------------------- Public Methods


    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    public void backgroundProcess();


    /**
     * <p>Perform request processing as required by this Valve.</p>
     *
     * <p>An individual Valve <b>MAY</b> perform the following actions, in
     * the specified order:</p>
     * <ul>
     * <li>Examine and/or modify the properties of the specified Request and
     *     Response.
     * <li>Examine the properties of the specified Request, completely generate
     *     the corresponding Response, and return control to the caller.
     * <li>Examine the properties of the specified Request and Response, wrap
     *     either or both of these objects to supplement their functionality,
     *     and pass them on.
     * <li>If the corresponding Response was not generated (and control was not
     *     returned, call the next Valve in the pipeline (if there is one) by
     *     executing <code>getNext().invoke()</code>.
     * <li>Examine, but not modify, the properties of the resulting Response
     *     (which was created by a subsequently invoked Valve or Container).
     * </ul>
     *
     * <p>A Valve <b>MUST NOT</b> do any of the following things:</p>
     * <ul>
     * <li>Change request properties that have already been used to direct
     *     the flow of processing control for this request (for instance,
     *     trying to change the virtual host to which a Request should be
     *     sent from a pipeline attached to a Host or Context in the
     *     standard implementation).
     * <li>Create a completed Response <strong>AND</strong> pass this
     *     Request and Response on to the next Valve in the pipeline.
     * <li>Consume bytes from the input stream associated with the Request,
     *     unless it is completely generating the response, or wrapping the
     *     request before passing it on.
     * <li>Modify the HTTP headers included with the Response after the
     *     <code>getNext().invoke()</code> method has returned.
     * <li>Perform any actions on the output stream associated with the
     *     specified Response after the <code>getNext().invoke()</code> method has
     *     returned.
     * </ul>
     *
     * @param request The servlet request to be processed
     * @param response The servlet response to be created
     *
     * @exception IOException if an input/output error occurs, or is thrown
     *  by a subsequently invoked Valve, Filter, or Servlet
     * @exception ServletException if a servlet error occurs, or is thrown
     *  by a subsequently invoked Valve, Filter, or Servlet
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException;


    public boolean isAsyncSupported();
}
