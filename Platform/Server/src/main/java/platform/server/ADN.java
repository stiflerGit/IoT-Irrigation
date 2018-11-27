package platform.server;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONException;
import org.json.JSONObject;

import platform.core.*;

public class ADN {

	private static Mca MCA = Mca.getInstance();
	private static AE AE_Controller;

	private static ArrayList<String> mote_uri = new ArrayList<String>();
	private static ArrayList<Mote> motes = new ArrayList<Mote>();

	private static CopyOnWriteArrayList<Container> containers = new CopyOnWriteArrayList<Container>();
	private static CopyOnWriteArrayList<String> subscriptions = new CopyOnWriteArrayList<String>();

	private static DiscoverThread discover_thread;
	private static CoapMonitorThread monitor_thread;
	
	/**
	 * Private constructor for the ADN class.
	 */
	private ADN() {}

	
	private static void initMotes(String mn_cse) {
		discover_thread = new DiscoverThread(mn_cse);
		if (discover_thread.getState() == Thread.State.NEW)
			discover_thread.start();

		monitor_thread = new CoapMonitorThread("CoapMonitorServer");
		if (monitor_thread.getState() == Thread.State.NEW)
			monitor_thread.start();
		
		
	}


	public static JSONObject getLa(String ae, String mote, String res) {

		JSONObject root = null;
		int retry = 0;

		while (root == null) {
			String uri_res = Constants.MN_CSE_URI + "/" + ae + "/" + mote + "/" + mote + "-" + res;
			System.out.println("getLa" + uri_res);
			String response = MCA.om2mRequest("GET", 0, uri_res, "la", "");
			if (response.equals("Resource not found"))
				return null;

			try {
				root = new JSONObject(response);
			} catch (JSONException e) {
				retry++;
				if (retry == 100) {
					e.printStackTrace();
					System.exit(-1);
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		}
		return root;	
	}

	public static Mca getMca() {
		return MCA;
	}
	
	public static AE getAE() {
		return AE_Controller;
	}
	
	public static ArrayList<String> getMoteUri() {
		return mote_uri;
	}
	
	public static ArrayList<Mote> getMotes() {
		return motes;
	}
	
	public static CopyOnWriteArrayList<Container> getContainers() {
		return containers;
	}
	
	public static CopyOnWriteArrayList<String> getSubscriptions() {
		return subscriptions;
	}
	


	
	public static void main(String[] args) throws Exception {

		System.out.println("---------- | Infrastructure Node ADN | ----------");

		AE_Controller = MCA.createAE(Constants.IN_CSE_URI, "AE_Controller", "AE_Controller");
		
		initMotes(Constants.IN_CSE_COAP + "/" + Constants.MN_CSE_ID);
		
		Server server = new Server(8000);
		
		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setDirectoriesListed(true);
		resource_handler.setWelcomeFiles(new String[] { "index.html" });
		resource_handler.setResourceBase("./src/main/webapp/");

		WebSocketHandler wsHandler = new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.register(WebServer.class);
			}
		};
		
		ContextHandler context = new ContextHandler();
		context.setContextPath("/motes");
		context.setHandler(wsHandler);
		
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resource_handler, context });
		server.setHandler(handlers);

		server.setHandler(handlers);
		server.start();
		//server.join();
	}

}


