package platform.server;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.eclipse.californium.core.CoapResource;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import org.json.JSONObject;
import org.json.JSONException;

import platform.core.Constants;


public class CoapMonitorThread extends Thread {

	private String name;


	public class CoapMonitor extends CoapServer {

		private String name;


		void addEndpoints() {
			for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
				if (((addr instanceof Inet4Address)) || (addr.isLoopbackAddress())) {
					InetSocketAddress bindToAddress = new InetSocketAddress(addr, Constants.IN_COAP_PORT);
					addEndpoint(new CoapEndpoint(bindToAddress));
				}
			}
		}


		public CoapMonitor(String name) throws SocketException {
			this.name = name;
			add(new Resource[] { new Monitor() });
		}

		class Monitor extends CoapResource {
		
			public Monitor() {
				super(name);
				getAttributes().setTitle(name);
				setObservable(true);
			}

			public void handlePOST(CoapExchange exchange) {
				exchange.respond(ResponseCode.CREATED);
				byte[] content = exchange.getRequestPayload();
				String contentStr = new String(content);
				System.out.println("handlePOST: " + contentStr);
				try {
					JSONObject root = new JSONObject(contentStr);
					JSONObject m2msgn = root.getJSONObject("m2m:sgn");

					if (m2msgn.has("m2m:nev")) {
						JSONObject nev = m2msgn.getJSONObject("m2m:nev");
						JSONObject rep = nev.getJSONObject("m2m:rep");
						JSONObject m2mcin = rep.getJSONObject("m2m:cin");
						
						String reply = m2mcin.getString("con");
	
						JSONObject m2mcont = new JSONObject(ADN.getMca().om2mRequest("GET", 0, Constants.IN_CSE_COAP, m2mcin.getString("pi").substring(1), ""));
						
						m2mcont = m2mcont.getJSONObject("m2m:cnt");
						System.out.println(m2mcont);
						String[] uri = m2mcont.getString("rn").split("-");
												
						String uri_mote = null;
						for (String string : uri) {
							if (string.contains("Mote")) {
								uri_mote = string;
								break;
							}
						}
				
						String uri_res = null;
						for (String string : uri) {
							if (string.contains("sensor") || string.contains("actuator"))
								uri_res = string;
						}
	
						System.out.println("New content: " + reply);
	
						for (int i = 0; i < ADN.getMotes().size(); i++) {
	
				
							if (ADN.getMotes().get(i).name.equals(uri_mote)) {
								ADN.getMotes().get(i).mutex.doWait();
								ADN.getMotes().get(i).setMoteResource(reply, uri_res.substring(uri_res.lastIndexOf("_") + 1, uri_res.length()));
								ADN.getMotes().get(i).mutex.doNotify();

								if (WebServer.currentSession != null) {
									String str = ADN.getMotes().get(i).toJSON();
									
									JSONObject object = new JSONObject();
									object.put("data", new JSONObject(str));
									object.put("action", "PUT");
								
									WebServer.send(object.toString());
								}
							}
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			public void handleGET(CoapExchange exchange) {
				exchange.respond("handleGET(CoapExchange exchange)");
			}

		}

	}

	public CoapMonitorThread(String name) {
		this.name = name;
	}

	public void run() {
		CoapMonitor server;
		try {
			server = new CoapMonitor(this.name);
			server.addEndpoints();
			server.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

}
