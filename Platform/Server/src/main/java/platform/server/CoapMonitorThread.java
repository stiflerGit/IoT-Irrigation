package it.unipi.iot.in;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.json.JSONException;
import org.json.JSONObject;

import platform.core.*;

public class CoapMonitorThread extends Thread {

	private int coapPort;
	public String name;
	public HashMap<String, Mote> motes;

	public CoapMonitorThread(String name, int port, HashMap<String, Mote> motes) {
		this.name = name;
		this.coapPort = port;
		this.motes = motes;
	}

	public void run() {
		CoapMonitor server;
		try {
			server = new CoapMonitor(this.name, coapPort, motes);
			server.addEndpoints();
			server.start();
		} catch (SocketException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public class CoapMonitor extends CoapServer {

		private int coapPort;
		public String name;
		public HashMap<String, Mote> motes;

		void addEndpoints() {
			for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
				if (((addr instanceof Inet4Address)) || (addr.isLoopbackAddress())) {
					InetSocketAddress bindToAddress = new InetSocketAddress(addr, coapPort);
					addEndpoint(new CoapEndpoint(bindToAddress));
				}
			}
		}

		public CoapMonitor(String name, int port, HashMap<String, Mote> motes) throws SocketException {
			this.name = name;
			this.motes = motes;
			this.coapPort = port;
			add(new Resource[] { new Monitor() });
		}

		public void start() {
			addEndpoints();
			super.start();
		}

		class Monitor extends CoapResource {

			public Monitor() {
				super(name);
				getAttributes().setTitle(name);
				setObservable(true);
			}

			// this function is executed when there is a subscription response
			public void handlePOST(CoapExchange exchange) {
				int i = 0;

				exchange.respond(ResponseCode.CREATED);
				byte[] content = exchange.getRequestPayload();
				String contentStr = new String(content);

				try {
					JSONObject root = new JSONObject(contentStr);
					JSONObject m2msgn = root.getJSONObject("m2m:sgn");
					JSONObject nev = m2msgn.getJSONObject("m2m:nev");
					JSONObject rep = nev.getJSONObject("m2m:rep");
					JSONObject cin = rep.getJSONObject("m2m:cin");
					String pi = cin.getString("pi");
					String reply = cin.getString("con");
					String parent = cin.getString("pi");
					// resource container
					Container resourceContainer = Mca.getInstance().getContainer(Constants.MN_CSE_COAP, parent.substring(1));
					// Mote container
					String resParent = resourceContainer.getPi().substring(1);
					Container moteContainer = Mca.getInstance().getContainer(Constants.MN_CSE_COAP, resParent);
					Mote moteToUpdate = Adn.motes.get(moteContainer.getRn());
					JSONObject fields = new JSONObject(reply);
					moteToUpdate.setFieldFromJson(fields);
					Adn.clientUpdate();

				} catch (JSONException e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
//					System.exit(-1);
				}
			}
			
			@Override
			public void handleGET(CoapExchange exchange) {
				exchange.respond("handleGet");
			}
			
		}
	}
}
