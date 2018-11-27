package platform.gateway;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
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
					InetSocketAddress bindToAddress = new InetSocketAddress(addr, Constants.MN_COAP_PORT);
					addEndpoint(new CoapEndpoint(bindToAddress));
				}
			}
		}

		public CoapMonitor(String name) throws SocketException {
			this.name = name;
			add(new Resource[] { new Monitor() });
		}
		

		class Monitor extends CoapResource {
			
			private String getUriResource(String resource) {
				return resource.replace("_", "/");
			}

			private String get_message(JSONObject content, String uri_res) {
				
				String message = null;
				
				if (uri_res.equalsIgnoreCase("actuator_type")) {
					message = "type=" + content.getString("type");
				} else if (uri_res.equalsIgnoreCase("actuator_irrigation")) {
					message = "irrigation=" + content.getInt("irrigation");
				}
				System.out.println("New mote value: " + message);
				return message;
			}

			private void mote_post(String mote, String uri_res, String uri_mote, String reply) {
				URI uri = null;
				String uri_resource = getUriResource(uri_res);
				
				try {
					uri = new URI("coap://[" + mote + "]:5683/" + uri_resource);
				} catch (URISyntaxException e) {
					System.err.println("Invalid URI: " + e.getMessage());
					System.exit(-1);
				}
				System.out.println("MOTE_POST_START: " + uri);
				CoapClient client = new CoapClient(uri);
				JSONObject root = new JSONObject(reply);
				String message = get_message(root, uri_res);
				CoapResponse response = client.post(message, MediaTypeRegistry.TEXT_PLAIN);
				
				response = client.get();
				
				ADN.getMca().createContentInstance(Constants.MN_CSE_URI + "/" + ADN.getAE().getRn() + "/"
							+ uri_mote + "/" + uri_mote + "-" + uri_res, response.getResponseText());
							//"{'" + uri_res.substring(uri_res.lastIndexOf("_") + 1, uri_res.length()) + "':'" + 
							//message.substring(message.lastIndexOf("=") + 1, message.length()) + "'}");
				System.out.println(response.getResponseText());
			}
			
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
	
						String addr = null;
						ArrayList<String> mote_uri = ADN.getMoteUri();
						ArrayList<String> mote_addr = ADN.getMoteAddr();
						for (String string : mote_uri) {
							if (uri_mote.equals(mote_uri.get(mote_uri.indexOf(string))))
								addr = mote_addr.get(mote_uri.indexOf(string));
						}
						
						String uri_res = null;
						for (String string : uri) {
							if (string.contains("actuator"))
								uri_res = string;
						}

						System.out.println("New content: " + reply);
						mote_post(addr, uri_res, uri_mote, reply);
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
