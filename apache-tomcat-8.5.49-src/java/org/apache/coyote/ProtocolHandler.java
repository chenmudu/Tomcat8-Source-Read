/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.coyote;

import java.util.concurrent.Executor;

import org.apache.tomcat.util.net.SSLHostConfig;

/**
 * Abstract the protocol implementation, including threading, etc.
 * 抽象出协议的实现,包括线程。
 * This is the main interface to be implemented by a coyote protocol.
 * Adapter is the main interface to be implemented by a coyote servlet
 * container.
 * 适配器是要由coyote(通讯连接协议(Http/Ajp)) servlet容器实现的主接口。
 * 适配器只要是由coyote servlet容器实现的主接口。
 * @author Remy Maucherat
 * @author Costin Manolache
 * @see Adapter
 *
 * 你可以点一下他的子类。发现两种协议。三种处理方式。总计6种组合。详见tomcat官网。Bio的请求处理模式已经取消。
 * {@see <a href="http://tomcat.apache.org/tomcat-8.0-doc/config/http.html>http://tomcat.apache.org/tomcat-8.0-doc/config/http.html</a> }
 *
 * 这儿啰嗦一下：BIO, NIO, NIO2, APR.
 * 阻塞与非阻塞只是体现在(Connector组件对于读取请求头以及等待下一个请求时的策略而言)
 * Read Request Headers 和 Wait for next Request 以及 SSL Handshake上面。详见上链接。
 */

//
public interface ProtocolHandler {

    /**
     * Return the adapter associated with the protocol handler.
     * @return the adapter
     */
    public Adapter getAdapter();


    /**
     * The adapter, used to call the connector.
     *
     * @param adapter The adapter to associate
     */
    public void setAdapter(Adapter adapter);


    /**
     * The executor, provide access to the underlying thread pool.
     *
     * @return The executor used to process requests
     */
    public Executor getExecutor();


    /**
     * Initialise the protocol.
     *
     * @throws Exception If the protocol handler fails to initialise
     */
    public void init() throws Exception;


    /**
     * Start the protocol.
     *
     * @throws Exception If the protocol handler fails to start
     */
    public void start() throws Exception;


    /**
     * Pause the protocol (optional).
     *
     * @throws Exception If the protocol handler fails to pause
     */
    public void pause() throws Exception;


    /**
     * Resume the protocol (optional).
     *
     * @throws Exception If the protocol handler fails to resume
     */
    public void resume() throws Exception;


    /**
     * Stop the protocol.
     *
     * @throws Exception If the protocol handler fails to stop
     */
    public void stop() throws Exception;


    /**
     * Destroy the protocol (optional).
     *
     * @throws Exception If the protocol handler fails to destroy
     */
    public void destroy() throws Exception;


    /**
     * Close the server socket (to prevent further connections) if the server
     * socket was bound on {@link #start()} (rather than on {@link #init()}
     * but do not perform any further shutdown.
     */
    public void closeServerSocketGraceful();


    /**
     * Requires APR/native library
     *
     * @return <code>true</code> if this Protocol Handler requires the
     *         APR/native library, otherwise <code>false</code>
     */
    public boolean isAprRequired();


    /**
     * Does this ProtocolHandler support sendfile?
     *
     * @return <code>true</code> if this Protocol Handler supports sendfile,
     *         otherwise <code>false</code>
     */
    public boolean isSendfileSupported();


    public void addSslHostConfig(SSLHostConfig sslHostConfig);
    public SSLHostConfig[] findSslHostConfigs();


    public void addUpgradeProtocol(UpgradeProtocol upgradeProtocol);
    public UpgradeProtocol[] findUpgradeProtocols();
}
