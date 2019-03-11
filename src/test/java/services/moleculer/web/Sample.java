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

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.web.netty.NettyServer;

public class Sample {

	public static void main(String[] args) {
		System.out.println("START");
		try {
			ServiceBroker broker = ServiceBroker.builder().build();

			NettyServer server = new NettyServer();
			broker.createService(server);

			ApiGateway gateway = new ApiGateway();
			gateway.setDebug(true);
			broker.createService(gateway);

			gateway.addServeStatic("/static", "/templates").setEnableReloading(true);
			
			gateway.addRoute("/test", "test.send");
			
			broker.createService(new Service("test") {

				@SuppressWarnings("unused")
				public Action send = ctx -> {
					
					Tree payload = new Tree();
					payload.put("a", 3);
					
					gateway.sendWebSocket("/ws/test", payload);
					return payload;
				};

			});
			broker.start();

		} catch (Exception cause) {
			cause.printStackTrace();
		}
	}

}