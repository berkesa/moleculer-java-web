/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.web;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;

import io.datatree.Promise;
import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.web.router.Route;
import services.moleculer.web.servlet.MoleculerServlet;
import services.moleculer.web.servlet.websocket.EndpointDeployer;

/**
 * "J2EE" server mode (run as Servlet).
 */
public class JettyWebSocketTest extends TestCase {

	protected Server server;
	protected ServiceBroker broker;
	protected ApiGateway gateway;
	protected WebSocketClient client;

	@SuppressWarnings("deprecation")
	@Override
	protected void setUp() throws Exception {

		// --- TEST SERVLET CONTAINER ---

		server = new Server();
		ServerConnector serverConnector = new ServerConnector(server);
		serverConnector.setHost("127.0.0.1");
		serverConnector.setPort(3000);
		ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		servletContextHandler.setContextPath("/");

		WebSocketServerContainerInitializer.configureContext(servletContextHandler);
		servletContextHandler.addEventListener(new EndpointDeployer());

		// Create non-blocking servlet
		MoleculerServlet servlet = new MoleculerServlet();
		ServletHolder servletHolder = new ServletHolder(servlet);

		servletHolder.setInitParameter("moleculer.config", "/services/moleculer/web/moleculer.config.xml");
		servletHolder.setInitParameter("moleculer.force.blocking", "false");
		servletHolder.setInitParameter("moleculer.inprocess", "false");
		servletContextHandler.addServlet(servletHolder, "/*");

		HandlerCollection handlerCollection = new HandlerCollection();
		handlerCollection.addHandler(servletContextHandler);
		server.setHandler(handlerCollection);
		server.addConnector(serverConnector);

		server.start();

		// --- CREATE SERVICE BROKER ---

		ServiceBroker broker = servlet.getBroker();

		// Open local REPL console
		// broker.repl();

		// --- ADD A ROUTE TO REST SERVICE ---

		Route route = new Route();
		route.addAlias("/test", "test.send");

		ApiGateway gateway = servlet.getGateway();
		gateway.addRoute(route);

		gateway.setWebSocketFilter(new WebSocketFilter() {

			@Override
			public Promise onConnect(WebRequest request) {
				return new Promise(res -> {
					assertEquals("/ws/test/q", request.getPath());
					assertEquals("key=value", request.getQuery());
					new Thread() {
						public void run() {
							try {
								Thread.sleep(200);
								res.resolve(true);
							} catch (Exception e) {
								res.reject(e);
							}
						}
					}.start();
				});
			};
		});

		// --- TEST MOLCEULER SERVICE ---

		// Moleculer Service, which sends a websocket message
		broker.createService(new Service("test") {

			@SuppressWarnings("unused")
			Action send = ctx -> {

				Tree packet = new Tree();
				packet.put("path", "/ws/test/q");
				packet.put("data", 123);

				ctx.broadcast("websocket.send", packet);

				return "Data submitted!";
			};

		});

		// --- TEST CLIENT ---

		// Emulate web browser (see "websocket.js")
		URI uri = new URI("ws://localhost:3000/ws/test/q?key=value");
		client = new WebSocketClient(uri, new Draft_6455()) {

			@Override
			public void onMessage(String message) {
				System.out.println("MSG RECEIVED: " + message);
				msg = message;
			}

			@Override
			public void onOpen(ServerHandshake handshake) {
				System.out.println("WS OPENED");
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				System.out.println("WS CLOSED");
			}

			@Override
			public void onError(Exception ex) {
				ex.printStackTrace();
			}

		};
		client.connect();
	}

	// ---------------- TESTS ----------------

	protected String msg;

	@Test
	public void testWS() throws Exception {
		assertNull(msg);
		Thread.sleep(1000);
		URL url = new URL("http://localhost:3000/test?key=value");
		HttpURLConnection c = (HttpURLConnection) url.openConnection();
		assertEquals(200, c.getResponseCode());
		Thread.sleep(500);
		assertEquals("123", msg);
	}

	// ---------------- STOP ----------------

	@Override
	protected void tearDown() throws Exception {
		if (server != null) {
			server.stop();
		}
		if (client != null) {
			client.close();
		}
	}

}