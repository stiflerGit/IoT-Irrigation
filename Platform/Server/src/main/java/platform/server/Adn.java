package platform.server;

import java.io.IOException;
// java standards
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
// JSON
import org.json.JSONObject;
// core
import platform.core.*;
// web server
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class Adn {

	private static final Adn istance = new Adn();

	public static Adn getIstance() {
		return istance;
	}

	public static Mca MCA = Mca.getInstance();

	public static AE aEcontroller;
	// Rn to Container 
	public static HashMap<String, Container> containers = new HashMap<String, Container>();
	
	// ID(RN) to Mote
	public static HashMap<String, Mote> motes = new HashMap<String, Mote>();

	public static List<String> subscriptions = new ArrayList<String>();

	public static WebMotesServer webSocket;

	public static Mca getMCA() {
		return MCA;
	}

	public static AE getController() {
		return aEcontroller;
	}

	public static ArrayList<Container> getContainers() {
		return new ArrayList<Container>(containers.values());
	}

	public static List<Mote> getSensorsList() {
		return new ArrayList<Mote>(motes.values());
	}

	public static void clientUpdate() {
		try {
			if (webSocket != null)
				webSocket.sendMotesList();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] Args) throws Exception {
		Adn adn = new Adn();

		aEcontroller = MCA.createAE(Constants.IN_CSE_URI, "AE_Controller", "AE_Controller");

		// start the discoverer thread
		DiscoverThread discoverer = adn.new DiscoverThread("discoverer");
		discoverer.start();

		// CoAP server for handling notifications from the subscriptions
		CoapMonitorThread coapMonitor = new CoapMonitorThread("CoapMonitor", 6000, motes);
		coapMonitor.start();

		// initSensors(Constants.IN_CSE_COAP + "/" + Constants.MN_CSE_ID);

		// Create a basic Jetty server object that will listen on port 8080. Note that
		// if you set this to port 0
		// then a randomly available port will be assigned that you can either look in
		// the logs for the port,
		// or programmatically obtain it for use in test cases.
		Server server = new Server(8000);

		// Create the ResourceHandler. It is the object that will actually handle the
		// request for a given file. It is
		// a Jetty Handler object so it is suitable for chaining with other handlers as
		// you will see in other examples.
		ResourceHandler resource_handler = new ResourceHandler();

		// Configure the ResourceHandler. Setting the resource base indicates where the
		// files should be served out of.
		// In this example it is the current directory but it can be configured to
		// anything that the jvm has access to.
		resource_handler.setDirectoriesListed(true);
		resource_handler.setWelcomeFiles(new String[] { "index.html" });
		resource_handler.setResourceBase("./src/main/webapp/");

		WebSocketHandler ws_handler = new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.register(WebMotesServer.class);
			}
		};

		ContextHandler context = new ContextHandler();
		context.setContextPath("/motes");
		context.setHandler(ws_handler);
		// Add the ResourceHandler to the server.
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resource_handler, ws_handler });
		server.setHandler(handlers);

		// start the webserver
		server.start();
		server.join();

		while (true) {

		}

	}

	class DiscoverThread extends Thread {

		private int stop = 0;

		public DiscoverThread(String name) {
			// TODO Auto-generated constructor stub
			super(name);
		}

		public void run() {
			while (this.stop == 0) {
				try {
					sleep(10000);
				} catch (InterruptedException e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				String parentCont = "";
				String[] tmp;
				List<String> mnCNTs;
				ArrayList<String> motesRNs = new ArrayList<String>();
				ArrayList<String> newMotes = new ArrayList<String>();
				ArrayList<String> oldMotes = new ArrayList<String>();
				// discover the containers on the MN
				mnCNTs = MCA.discoverResources(Constants.MN_CSE_COAP, "?fu=1&rty=3&lbl=Mote");
				// Collect Motes Resource names (that are equal to their id)
				if (mnCNTs == null || mnCNTs.isEmpty()) {
					continue;
				}
				for (String cntRi : mnCNTs) {
					tmp = cntRi.split("/");
					motesRNs.add(tmp[tmp.length - 1]);
				}
				// newMotes
				for (String moteid : motesRNs) {
					Mote m = motes.get(moteid);
					if (m == null) {
						newMotes.add(moteid);
					}
				}
				if (!newMotes.isEmpty()) {
					newMotes(newMotes);
				}
				// oldMotes
				for (String moteid : motes.keySet()) {
					if (!motesRNs.contains(moteid)) {
						oldMotes.add(moteid);
						motes.remove(moteid);
					}
				}
				if (!oldMotes.isEmpty()) {
					oldMotes(oldMotes);
				}
				// WebSocket Update
				if ((!newMotes.isEmpty() || !oldMotes.isEmpty()) && webSocket != null) {
					try {
						webSocket.sendMotesList();
					} catch (IOException e) {
						System.err.println(e.getMessage());
						e.printStackTrace();
					}
				}
			}

		}

		private void newMotes(List<String> newMotes) {
			// Update the Model first since in order to update resources values
			// of each mote we need the mote already in the model
			newMotesUpdateModel(newMotes);
			newMotesUpdateOM2M(newMotes);
		}

		private void oldMotes(List<String> oldMotes) {
			oldMotesUpdateOM2M(oldMotes);
			oldMotesUpdateModel(oldMotes);
		}

		private void newMotesUpdateOM2M(List<String> newMotes) {
			String parentCont = "";
			String[] tmp;
			List<String> mnCNTs;
			// for each new mote we have to create a container on the IN
			for (String newMoteRn : newMotes) {
				containers.put(newMoteRn,
						MCA.createContainer(Constants.IN_CSE_URI + "/" + aEcontroller.getRn(), newMoteRn, newMoteRn));
				// discover all Containers for a sensor
				mnCNTs = MCA.discoverResources(Constants.MN_CSE_COAP, "?fu=1&rty=3&lbl=Resource-" + newMoteRn);
				if (mnCNTs == null || mnCNTs.isEmpty())
					break;
				// create a container for each resource of the mote we want to control
				for (String cont : mnCNTs) {
					tmp = cont.split("/");
					// Create the container for the controlled resources
					parentCont = Constants.IN_CSE_URI + "/" + aEcontroller.getRn() + "/" + newMoteRn;
					containers.put(tmp[tmp.length - 1],
							MCA.createContainer(parentCont, tmp[tmp.length - 1], tmp[tmp.length - 1]));
					// Subscribe for the useful resources
					String cse = Constants.MN_CSE_URI + "/AE_Monitor/" + newMoteRn + "/" + tmp[tmp.length - 1];
					MCA.createSubscription(cse, "Subscription-" + newMoteRn + "-" + tmp[tmp.length - 1],
							"coap://127.0.0.1:6000/CoapMonitor");
					subscriptions.add("Subscription-" + newMoteRn + "-" + tmp[tmp.length - 1]);
					System.out.println("Subscribed to: " + cse);
					// get initial data for each resource
					ContentIstance CIN = MCA.getContentIstance(Constants.MN_CSE_COAP +"/"+ cont.substring(1), "la");
					JSONObject field = new JSONObject(CIN.getCon());
					motes.get(newMoteRn).setFieldFromJson(field);
//					System.out.println(CIN.getCon());
				}
			}
		}

		private void newMotesUpdateModel(List<String> newMotes) {
			for (String newMoteRn : newMotes) {
				Mote m = new Mote();
				m.setId(newMoteRn);
				Adn.motes.put(newMoteRn, m);
			}
		}

		private void oldMotesUpdateOM2M(List<String> oldMotes) {
			for (String oldMote : oldMotes) {
				// delete the container
				MCA.deleteContainer(Constants.IN_CSE_URI + "/" + aEcontroller.getRn(), oldMote);
			}
		}

		private void oldMotesUpdateModel(List<String> oldMotes) {
			for (String oldMote : oldMotes) {
				motes.remove(oldMote);
			}
		}
	}
}
