/*
 * Copyright (c) 2014 AsyncHttpClient Project. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.asynchttpclient.websocket;

import static org.asynchttpclient.async.util.TestUtils.findFreePort;
import static org.asynchttpclient.async.util.TestUtils.newJettyHttpServer;
import static org.asynchttpclient.async.util.TestUtils.newJettyHttpsServer;
import static org.testng.Assert.assertEquals;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.ProxyServer;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Proxy usage tests.
 */
public abstract class ProxyTunnellingTest extends AbstractBasicTest {

    private Server server2;

    @BeforeClass(alwaysRun = true)
    public void setUpGlobal() throws Exception {
        port1 = findFreePort();
        server = newJettyHttpServer(port1);
        server.setHandler(new ConnectHandler());
        server.start();

        port2 = findFreePort();

        server2 = newJettyHttpsServer(port2);
        server2.setHandler(getWebSocketHandler());
        server2.start();

        logger.info("Local HTTP server started successfully");
    }

    @Override
    public WebSocketHandler getWebSocketHandler() {
        return new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(EchoSocket.class);
            }
        };
    }

    @AfterClass(alwaysRun = true)
    public void tearDownGlobal() throws Exception {
        server.stop();
        server2.stop();
    }

    protected String getTargetUrl() {
        return String.format("wss://127.0.0.1:%d/", port2);
    }

    @Test(timeOut = 60000)
    public void echoText() throws Exception {

        ProxyServer ps = new ProxyServer(ProxyServer.Protocol.HTTPS, "127.0.0.1", port1);
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setProxyServer(ps).build();
        AsyncHttpClient asyncHttpClient = getAsyncHttpClient(config);
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> text = new AtomicReference<String>("");

            WebSocket websocket = asyncHttpClient.prepareGet(getTargetUrl()).execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketTextListener() {

                @Override
                public void onMessage(String message) {
                    text.set(message);
                    latch.countDown();
                }

                @Override
                public void onFragment(String fragment, boolean last) {
                }

                @Override
                public void onOpen(WebSocket websocket) {
                }

                @Override
                public void onClose(WebSocket websocket) {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    latch.countDown();
                }
            }).build()).get();

            websocket.sendTextMessage("ECHO");

            latch.await();
            assertEquals(text.get(), "ECHO");
        } finally {
            asyncHttpClient.close();
        }
    }
}
oxyServer(new ProxyServer(ProxyServer.Protocol.HTTPS, "127.0.0.1", port1))//
                .build();
        AsyncHttpClient asyncHttpClient = getAsyncHttpClient(config);
        try {
            Future<Response> responseFuture = asyncHttpClient.executeRequest(new RequestBuilder("GET").setUrl(getTargetUrl2()).build(), new AsyncCompletionHandlerBase() {

                public void onThrowable(Throwable t) {
                    t.printStackTrace();
                    logger.debug(t.getMessage(), t);
                }

                @Override
                public Response onCompleted(Response response) throws Exception {
                    return response;
                }
            });
            Response r = responseFuture.get();
            assertEquals(r.getStatusCode(), 200);
            assertEquals(r.getHeader("X-Connection"), "keep-alive");
        } finally {
            asyncHttpClient.close();
        }
    }

    @Test(groups = { "online", "default_provider" })
    public void testSimpleAHCConfigProxy() throws IOException, InterruptedException, ExecutionException, TimeoutException {

        SimpleAsyncHttpClient client = new SimpleAsyncHttpClient.Builder()//
                .setProviderClass(getProviderClass())//
                .setProxyProtocol(ProxyServer.Protocol.HTTPS).setProxyHost("127.0.0.1").setProxyPort(port1).setFollowRedirects(true).setUrl(getTargetUrl2())//
                .setHeader("Content-Type", "text/html")//
                .build();
        try {
            Response r = client.get().get();

            assertEquals(r.getStatusCode(), 200);
            assertEquals(r.getHeader("X-Connection"), "keep-alive");
        } finally {
            client.close();
        }
    }
}
