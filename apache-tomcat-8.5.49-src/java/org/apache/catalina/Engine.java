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
 * 首先：Catalina 是Tomcat使用Apache实现的Servlet容器的名字。作用：解析Servlet。
 * An <b>Engine</b> is a Container that represents the entire Catalina servlet
 * engine.  It is useful in the following types of scenarios:
 * 一个Engine是一个引擎，可以代表整个Catalina servlet。它适用于下列情况：
 * (可以作为解析Servlet的容器。)
 * <ul>
 * <li>You wish to use Interceptors that see every single request processed
 *     by the entire engine.
 *     您希望使用一个或多个拦截器去查看这个引擎如何处理每一个single request。
 *     (用拦截器去查看一个请求的完整过程。)
 * <li>You wish to run Catalina in with a standalone HTTP connector, but still
 *     want support for multiple virtual hosts.
 *     您希望使用一个独立的HTTP connector去运行Catalina，但是仍然想支持多个虚拟主机。
 *     (单机连接器时，支持多个虚拟主机。)
 * </ul>
 * In general, you would not use an Engine when deploying Catalina connected
 * to a web server (such as Apache), because the Connector will have
 * utilized the web server's facilities to determine which Context (or
 * perhaps even which Wrapper) should be utilized to process this request.
 * 总而言之，当你部署Servlet容器到一个Web服务器(比如Apache)时，你不应该使用Engine。
 * 因为此时Connector会利用Web服务器的功能去确定用哪个Context(甚至是Wrapper)去处理这个请求。
 * (北鼻,求你别用了。)
 * <p>
 * The child containers attached to an Engine are generally implementations
 * of Host (representing a virtual host) or Context (representing individual
 * an individual servlet context), depending upon the Engine implementation.
 * 附加(继承/实现)到Engine的子容器通常是Host或Context的实现类，具体要看接口的实现情况。
 * （实现Engine的容器一般是Host或Context的实现类。）
 * <p>
 * If used, an Engine is always the top level Container in a Catalina
 * hierarchy. Therefore, the implementation's <code>setParent()</code> method
 * should throw <code>IllegalArgumentException</code>.
 * 在Catalina体系中,Engine是最顶级的容器.，所以使用setParent()方法时会抛出IllegalArgumentException这个异常。
 * (级别高，在Servlet容器中，即在Container责任链中是作为和Connector通讯的第一个人.所以不要去用Container接口中的
 * setParent()方法去设置级别,不然会给你扔出一个异常让你懵逼。)
 *
 *
 * @author Craig R. McClanahan
 * @translator chenchen6(chenmudu@gmail.com/chenchen6@tuhu.cn)
 */
public interface Engine extends Container {

    /**
     * @return the default host name for this Engine.
     */
    public String getDefaultHost();


    /**
     * Set the default hostname for this Engine.
     *
     * @param defaultHost The new default host
     */
    public void setDefaultHost(String defaultHost);


    /**
     * @return the JvmRouteId for this engine.
     */
    public String getJvmRoute();


    /**
     * Set the JvmRouteId for this engine.
     *
     * @param jvmRouteId the (new) JVM Route ID. Each Engine within a cluster
     *        must have a unique JVM Route ID.
     */
    public void setJvmRoute(String jvmRouteId);


    /**
     * @return the <code>Service</code> with which we are associated (if any).
     */
    public Service getService();


    /**
     * Set the <code>Service</code> with which we are associated (if any).
     *
     * @param service The service that owns this Engine
     */
    public void setService(Service service);
}
