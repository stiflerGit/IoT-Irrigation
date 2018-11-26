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

			private String get_message(JSONObject content, String uri_res) {
				
				String message = null;
				
				if (uri_res.equalsIgnoreCase("type")) {
					message = "type=" + content.getString("type");
				} else if (uri_res.equalsIgnoreCase("battery")) {
					message = "value=" + content.getInt("battery");		
				} else if (uri_res.equalsIgnoreCase("temperature")) {
					message = "value=" + content.getDouble("temperature");
				} else if (uri_res.equalsIgnoreCase("humidity")) {
					message = "value=" + content.getDouble("humidity");
				} else if (uri_res.equalsIgnoreCase("gsps")) {
					message = "lat=" + content.getDouble("lat") + "," +  "lng=" + content.getDouble("lng");
				}
				System.out.println("New mote value: " + message);
				return message;
			}

			private void mote_post(String mote, String uri_res, String uri_mote, String reply) {
				URI uri = null;

				try {
					uri = new URI("coap://[" + mote + "]:5683/" + uri_res);
				} catch (URISyntaxException e) {
					System.err.println("Invalid URI: " + e.getMessage());
					System.exit(-1);
				}

				CoapClient client = new CoapClient(uri);
				JSONObject root = new JSONObject(reply);
				String message = get_message(root, uri_res);
				CoapResponse response = client.post(message, MediaTypeRegistry.TEXT_PLAIN);
				System.out.println(response.getResponseText());
				if (uri_res.equalsIgnoreCase("type"))
					ADN.updateMote(uri_mote, root);
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

				try {
					JSONObject root = new JSONObject(contentStr);
					JSONObject m2msgn = root.getJSONObject("m2m:sgn");
					JSONObject nev = m2msgn.getJSONObject("nev");
					JSONObject rep = nev.getJSONObject("rep");
					String reply = rep.getString("con");

					String[] uri = m2msgn.getString("sur").split("/");

					String uri_mote = null;
					for (String string : uri) {
						if (string.contains("Mote"))
							uri_mote = string;
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
						if (string.contains("Resource"))
							uri_res = string;
					}

					System.out.println("New content: " + reply);
					mote_post(addr, uri_res, uri_mote, reply);

				} catch (JSONException e) {}
			}

			public void handleGET(CoapExchange exchange) {
				exchange.respond("handleGET(CoapExchange exchange)");
			}

		}
			
	}
	
	
	public CoapMonitorThread(String name, ArrayList<String>mote_addr, ArrayList<String>mote_uri) {
		this.name = name;
	}
	
	
	public void run() {
		CoapMonitor server;
		try {
			server = new CoapMonitor(this.name);//, this.mote_addr, this.mote_uri);
			server.addEndpoints();
			server.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

}
