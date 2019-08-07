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
package services.moleculer.web.servlet;

import static services.moleculer.web.common.GatewayUtils.getService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import services.moleculer.ServiceBroker;
import services.moleculer.web.ApiGateway;
import services.moleculer.web.servlet.websocket.ServletWebSocketRegistry;

public class MoleculerServlet extends HttpServlet {

	// --- UID ---

	private static final long serialVersionUID = -1038240217177335483L;

	// --- SERVICE BROKER'S SPRING CONTEXT (XML-BASED OR SPRING BOOT) ---

	protected final AtomicReference<ConfigurableApplicationContext> context = new AtomicReference<>();

	// --- MOLECULER COMPONENTS ---

	protected ServiceBroker broker;
	protected ApiGateway gateway;

	// --- WEBSOCKET REGISTRY ---

	private ServletWebSocketRegistry webSocketRegistry;

	private int webSocketCleanupSeconds = 15;

	// --- WORKING MODE (SYNC / ASYNC) ---

	private WorkingMode workingMode;

	// --- OTHER VARIABLES ---

	protected long timeout;

	// --- INIT / START ---

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (context.get() != null) {
			return;
		}
		try {

			// Start with SpringBoot
			String springApp = config.getInitParameter("moleculer.application");
			ConfigurableApplicationContext ctx = null;
			if (springApp != null && !springApp.isEmpty()) {

				// Create "args" String array by Servlet config
				HashSet<String> set = new HashSet<>();
				Enumeration<String> e = config.getInitParameterNames();
				while (e.hasMoreElements()) {
					String key = e.nextElement();
					String value = config.getInitParameter(key);
					if (value != null) {
						set.add(key + '=' + value);
					}
				}
				String[] args = new String[set.size()];
				set.toArray(args);

				// Class of the SpringApplication
				String springAppName = "org.springframework.boot.SpringApplication";
				Class<?> springAppClass = Class.forName(springAppName);

				// Input types of "run" method
				Class<?>[] types = new Class[2];
				types[0] = Class.class;
				types[1] = new String[0].getClass();
				Method m = springAppClass.getDeclaredMethod("run", types);

				// Input objects of "run" method
				Object[] in = new Object[2];
				in[0] = Class.forName(springApp);
				in[1] = args;

				// Load app with Spring Boot
				ctx = (ConfigurableApplicationContext) m.invoke(null, in);
				context.set(ctx);

			} else {

				// Start by using Spring XML config
				String configPath = config.getInitParameter("moleculer.config");
				if (configPath == null || configPath.isEmpty()) {
					configPath = "/WEB-INF/moleculer.config.xml";
				}
				File file = new File(configPath);
				if (file.isFile()) {
					ctx = new FileSystemXmlApplicationContext(configPath);
				} else {
					ctx = new ClassPathXmlApplicationContext(configPath);
				}
				context.set(ctx);
				ctx.start();
			}

			// Get ServiceBroker from Spring
			broker = ctx.getBean(ServiceBroker.class);

			// Find ApiGateway
			gateway = getService(broker, ApiGateway.class);
			if (gateway == null) {
				throw new ServletException("ApiGateway Service not defined!");
			}

			// Create WebSocket registry
			webSocketRegistry = new ServletWebSocketRegistry(config, broker, webSocketCleanupSeconds);
			gateway.setWebSocketRegistry(webSocketRegistry);

			// Autodetect working mode
			if (workingMode == null) {
				boolean asyncSupported = false;
				try {
					Class.forName("javax.servlet.ReadListener");
					asyncSupported = true;
				} catch (Throwable notFound) {
				}
				config.getServletContext().log("Moleculer running with "
						+ (asyncSupported ? "non-blocking" : "blocking") + " request processor.");
				if (asyncSupported) {
					workingMode = new AsyncWorkingMode(this);
				} else {
					workingMode = new BlockingWorkingMode(this);
				}
			}

		} catch (ServletException servletException) {
			throw servletException;
		} catch (Exception fatal) {
			throw new ServletException("Unable to load Moleculer Application!", fatal);
		}
	}

	// --- PROCESS REQUEST ---

	@Override
	public void service(ServletRequest req, ServletResponse rsp) throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) rsp;
		try {

			// WebSocket handling
			if (request.getHeader("Upgrade") != null) {
				webSocketRegistry.service(request, response);
				return;
			}

			// Process GET/POST/PUT/etc. requests
			workingMode.service(request, response);

		} catch (IllegalStateException illegalState) {

			// Switching back to blocking mode
			if (workingMode instanceof AsyncWorkingMode) {
				req.getServletContext().log("Switching back to blocking mode.");
				workingMode = new BlockingWorkingMode(this);

				// Repeat processing
				try {
					workingMode.service(request, response);
				} catch (Throwable cause) {
					handleError(response, cause);
				};
			} else {
				handleError(response, illegalState);
			}
		} catch (Throwable unknownError) {
			handleError(response, unknownError);
		}
	}

	// --- LOG ERROR ---

	protected void handleError(HttpServletResponse response, Throwable cause) {
		try {
			if (gateway == null) {
				response.sendError(404);
				logError("APIGateway Moleculer Service not found!", cause);
			} else {
				response.sendError(500);
				logError("Unable to process request!", cause);
			}
		} catch (Throwable ignored) {
		}
	}

	protected void logError(String message, Throwable cause) {
		if (broker != null) {
			broker.getLogger(MoleculerServlet.class).error(message, cause);
			return;
		}
		ServletContext ctx = getServletContext();
		if (ctx != null) {
			ctx.log(message, cause);
			return;
		}
		System.err.println(message);
		if (cause != null) {
			cause.printStackTrace();
		}
	}

	// --- DESTROY / STOP ---

	@Override
	public void destroy() {
		super.destroy();

		// Stop Atmosphere
		if (webSocketRegistry != null) {
			webSocketRegistry.stopped();
			webSocketRegistry = null;
		}

		// Stop Spring Context
		ConfigurableApplicationContext ctx = context.getAndSet(null);
		if (ctx != null) {
			try {
				ctx.stop();
			} catch (Throwable ignored) {
			}
		}
	}

	// --- GETTERS ---

	public ApiGateway getGateway() {
		return gateway;
	}

	public ServiceBroker getBroker() {
		return broker;
	}

	// --- SETTERS (FOR TESTING) ---

	public void setWorkingMode(WorkingMode workingMode) {
		this.workingMode = workingMode;
	}

}