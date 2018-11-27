/**
 * @file	ADN.java
 * @brief	Middle Node ADN implementation.
 */

package platform.gateway;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.WebLink;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import platform.core.*;

public class ADN {

	private static Mca MCA = Mca.getInstance();
	private static AE AE_Monitor;

	private static ArrayList<String> mote_uri = new ArrayList<String>();
	private static ArrayList<String> mote_addr = new ArrayList<String>();

	private static CopyOnWriteArrayList<Container> containers = new CopyOnWriteArrayList<Container>();
	private static CopyOnWriteArrayList<String> subscriptions = new CopyOnWriteArrayList<String>();

	private static CopyOnWriteArrayList<ObserveThread> observe_threads = new CopyOnWriteArrayList<ObserveThread>();
	private static DiscoverThread discover_thread;
	private static CoapMonitorThread monitor_thread;


	/**
	 * @brief Constructor of ADN class.
	 * 
	 */
	private ADN() {}


	/**
	 * @brief	Obtain the mote adresses through the border router.
	 * - border router use coap and expose two resource
	 * - getRoutes resource allow only get request and the response
	 * is a JSON array that contains all the address that are registered
	 * under the border router
	 * - evtRoutes is an event resource that allow observing
	 * a message arrives each time that border router register 
	 * an arrival or a departure of the mote
	 * 
	 * @param[in]	br_addr border router address.
	 * @param[out]	mote_addr list of discovered addresses mote.
	 * 
	 */
	private static void getMotesAddresses(String br_addr) {
		URI uri = null;

		try {
			uri = new URI("coap://[" + br_addr + "]:5683/");
		} catch (URISyntaxException e) {
			System.err.println("Invalid URI: " + e.getMessage());
			System.exit(-1);
		}

		CoapClient client = new CoapClient(uri);
		Set<WebLink> resources = client.discover();

		if (resources != null) {

			for (WebLink res : resources) {

				String uri_res = res.getURI().replace("/", "");

				if (uri_res.equalsIgnoreCase("getRoutes")) {
					try {
						uri = new URI("coap://[" + br_addr + "]:5683/" + uri_res);
					} catch (URISyntaxException e) {
						System.err.println("Invalid URI: " + e.getMessage());
						System.exit(-1);
					}

					client = new CoapClient(uri);
					CoapResponse response = client.get();
						
					if (response != null) {
						System.out.println(response.getResponseText());
						JSONObject object = new JSONObject(response.getResponseText());
						JSONArray routes = object.getJSONArray("routes");

						if (routes != null && routes.length() > 0) {
							for (Object r : routes) {
								addMote(r.toString());
							}
						}
					} else {
						System.out.println("No response received" + " from " + "coap://[" + br_addr + "]:5683/" + uri_res);
					}
				}
				
				if (uri_res.equalsIgnoreCase("evtRoutes")) {
					observe_threads.add(new ObserveThread("coap://[" + br_addr + "]:5683/" + uri_res, "BorderRouter"));
					Runtime.getRuntime().addShutdownHook(new Thread() {
						public void run() {
							observe_threads.get(observe_threads.size() - 1).stopObserve();
						}
					});
				}
			}
		}

		for (ObserveThread ot : observe_threads) {
			if (ot.container_cse.equals("BorderRouter")) {
				if ((observe_threads.get(observe_threads.indexOf(ot))).getState() == Thread.State.NEW)
				observe_threads.get(observe_threads.indexOf(ot)).start();
			}
		}
		
		discover_thread = new DiscoverThread(Constants.MN_CSE_COAP + "/" + Constants.IN_CSE_ID);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				discover_thread.stopDiscover();
			}
		});
		if (discover_thread.getState() == Thread.State.NEW)
			discover_thread.start();
		
		monitor_thread = new CoapMonitorThread("CoapMonitorGateway");//, mote_addr, mote_uri);
		if (monitor_thread.getState() == Thread.State.NEW)
			monitor_thread.start();

	}
			

	/**
	 * @brief	Add mote.
	 * If mote address is not present in the mote_addr list:
	 * - add address in the mote_addr list
	 * - discover resource inside mote
	 * - register uri mote based on response obtained from resource with uri=type
	 * - add container for the mote on MN-CSE with uri given from the previous response
	 * - register resource (add container and observing thread)
	 * 
	 * @param[in]	addr address of the discovered mote.
	 * 
	 */
	private static void addMote(String addr) {

		if (!addr.equals("") && !mote_addr.contains(addr)) {
			mote_addr.add(addr);

			URI uri = null;

			try {
				uri = new URI("coap://[" + addr + "]:5683");
			} catch (URISyntaxException e) {
				System.err.println("Invalid URI: " + e.getMessage());
				System.exit(-1);
			}

			CoapClient client = new CoapClient(uri);
			Set<WebLink> resources = client.discover();

			if (resources != null) {

				for (WebLink res : resources) {
					String uri_res = res.getURI();		// uri_res = /sensor/name or /actuator/type

					if (uri_res.contains("name")) {
						try {
							uri = new URI("coap://[" + addr + "]:5683/" + uri_res);
						} catch (URISyntaxException e) {
							System.err.println("Invalid URI: " + e.getMessage());
							System.exit(-1);
						}

						client = new CoapClient(uri);
						CoapResponse response = client.get();

						if (response != null) {
							JSONObject object = new JSONObject(response.getResponseText());
							String name = object.getString("name");

							System.out.println("Found Mote: " + name);

							mote_uri.add(name);

							String ae_cse = Constants.MN_CSE_URI + "/" + AE_Monitor.getRn();
							containers.add(MCA.createContainer(ae_cse, name, "Mote"));

							String container_cse = ae_cse + "/" + containers.get(containers.size() - 1).getRn();
							registerResources(addr, resources, container_cse);

							System.out.println("Registered resources for mote: " + name);
						} else {
							System.out.println("No response received" + " from " + "coap://[" + addr + "]" + ":5683/" + uri_res);
						}
					}
				}
			}
		}
	}


	/**
	 * @brief Remove mote.
	 * - Search in mote_addr list the input address that we want to remove
	 * - if the input address is present store the index
	 * - stop observing thread for the resources inside the mote
	 * - delete Container on MN-CSE having uri = coap://127.0.0.1:5684/~/mn-cse/mn-name/AE_Monitor/Mote_id_type;
	 * - remove address and uri from the list
	 * 
	 * @param[in] addr address of mote that we want to remove.
	 * 
	 */
	private static void removeMote(String addr) {

		if (mote_addr.contains(addr)) {
			int index = mote_addr.indexOf(addr);
			String uri = mote_uri.get(index);
			
			mote_uri.remove(index);
			mote_addr.remove(index);
			
			for (ObserveThread ot : observe_threads) {
				if (ot.container_cse.contains(uri)) {
					observe_threads.get(observe_threads.indexOf(ot)).stopObserve();
					observe_threads.remove(observe_threads.indexOf(ot));
				}
			}
			
			String ae_cse = Constants.MN_CSE_URI + "/" + AE_Monitor.getRn();

			for (Container c : containers) {
				if (c.getRn().contains(uri)) {
					if (c.getRn().contains("-")) {
						String cont = c.getRn().split("-")[0]; 
						MCA.deleteContainer(ae_cse + "/" + cont, c.getRn());
					} else 
						MCA.deleteContainer(ae_cse, c.getRn());
					containers.remove(containers.indexOf(c));
				}
			}
			
			for (String subs : subscriptions) {
				if (subs.contains(uri))
					subscriptions.remove(subscriptions.indexOf(subs));
			}

			System.out.println("Removed Mote: " + uri);

		}
	}


	/**
	 * @brief Register resources for each mote.
	 * - For each resources, create container and add observing thread
	 * 
	 * @param[in] mote address of mote.
	 * @param[in] resources set of mote resources.
	 * @param[in] container_cse cse of mote container.
	 * 
	 */
	private static void registerResources(String addr, Set<WebLink> resources, String container_cse) {
		
		for (WebLink res : resources) {
			String name = mote_uri.get(mote_addr.indexOf(addr));
			String uri_res = res.getURI().replace("/", "_").substring(1);

			// Observable resource (sensor)
			//if (!uri_res.contains(".well-known_core") && !uri_res.contains("name") && !uri_res.contains("actuator")) {
			if (uri_res.contains("sensor") && !uri_res.contains("name")) {
				name = name + "-" + uri_res;
				containers.add(MCA.createContainer(container_cse, name, uri_res.split("_")[0] + "-" + mote_uri.get(mote_addr.indexOf(addr))));


				observe_threads.add(new ObserveThread("coap://[" + addr + "]:5683/" + uri_res.replace("_", "/"), container_cse + "/" + name));
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						observe_threads.get(observe_threads.size() - 1).stopObserve();
					}
				});
				if ((observe_threads.get(observe_threads.size() - 1)).getState() == Thread.State.NEW)
					observe_threads.get(observe_threads.size() - 1).start();
			// Simple resource that allow the post method to change the value (actuator)
			} else if (uri_res.contains("actuator")) {
				name = name + "-" + uri_res;
				System.out.println(container_cse +"/" + name);
				containers.add(MCA.createContainer(container_cse, name, uri_res.split("_")[0] + "-" + mote_uri.get(mote_addr.indexOf(addr))));
				
				URI uri = null;
				try {
					uri = new URI("coap://[" + addr +"]:5683/" + uri_res.replace("_", "/"));
				} catch (URISyntaxException e) {
					System.err.println("Invalid URI: " + e.getMessage());
					System.exit(-1);
				}
				CoapClient client = new CoapClient(uri);
				CoapResponse response = client.get();
				if (response != null) {
					MCA.createContentInstance(container_cse + "/" + name, response.getResponseText());
				} else {
					System.out.println("No response received" + " from " + "coap://[" + addr + "]" + ":5683/" + uri_res.replace("_", "/"));
				}
			}
		}
	}


	/**
	 * @brief	Modify mote.
	 * - add or remove mote according to message receive from observing thread of border router.
	 * 
	 * @param[in]	message message received from border router to notify added or removed mote.
	 * 
	 */
	public static void modifyMote(String content) {
		JSONObject object = null;
		try {
			object = new JSONObject(content);
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println(content);
		}
		String routes;
		if (object.has("add")) {
			routes = object.getString("add");
			addMote(routes);
		} else if (object.has("rm")) {
			routes = object.getString("rm");
			removeMote(routes);
		}
	}


	public static Mca getMca() {
		return MCA;
	}
	
	public static AE getAE() {
		return AE_Monitor;
	}
	
	public static ArrayList<String> getMoteUri() {
		return mote_uri;
	}
	
	public static ArrayList<String> getMoteAddr() {
		return mote_addr;
	}
	
	public static CopyOnWriteArrayList<Container> getContainers() {
		return containers;
	}
	
	public static CopyOnWriteArrayList<String> getSubscriptions() {
		return subscriptions;
	}
	

	/**
	 * @brief MAIN of the Middle Node Adn.
	 * 
	 */
	public static void main(String[] args) throws InterruptedException {

		System.out.println("---------- | Middle Node ADN | ----------");

		AE_Monitor = MCA.createAE(Constants.MN_CSE_URI, "AE_Monitor", "AE_Monitor");

		getMotesAddresses(Constants.BR_ADDR);

		//CoapMonitorThread thread = new CoapMonitorThread("CoapMonitor", mote_addr, mote_uri);
		//thread.start();

	}
}