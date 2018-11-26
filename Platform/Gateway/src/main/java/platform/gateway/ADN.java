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

	public static AE AE_Monitor;
	public static ArrayList<String> mote_uri = new ArrayList<String>();
	private static ArrayList<String> mote_addr = new ArrayList<String>();

	public static ArrayList<String> subscriptions = new ArrayList<String>();
	private static CopyOnWriteArrayList<Container> containers = new CopyOnWriteArrayList<Container>();
	
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
		
		monitor_thread = new CoapMonitorThread("CoapMonitor", mote_addr, mote_uri);
		if (monitor_thread.getState() == Thread.State.NEW)
			monitor_thread.start();

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
					String uri_res = res.getURI().replace("/", "");

					if (uri_res.equalsIgnoreCase("type")) {
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
							int id = object.getInt("id");
							String type = object.getString("type");

							System.out.println("Found Mote: " + id + ", " + type);
							String name = "Mote_" + id + "_" + type;

							mote_uri.add(name);

							String ae_cse = Constants.MN_CSE_URI + "/" + AE_Monitor.getRn();
							containers.add(MCA.createContainer(ae_cse, name, "Mote"));

							String container_cse = ae_cse + "/" + containers.get(containers.size() - 1).getRn();
							registerResources(addr, resources, container_cse);

							System.out.println("Registered resources for mote: " + id + ", " + type);
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

			System.out.println("Removed Mote: " + uri.split("_")[1] + ", " + uri.split("_")[2]);

			
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
			String uri_res = res.getURI().replace("/", "");

			if (!uri_res.equalsIgnoreCase(".well-knowncore") && !uri_res.contains("type")) {
				name = name + "-Resource_" + uri_res;
				containers.add(MCA.createContainer(container_cse, name, "Resource-" + mote_uri.get(mote_addr.indexOf(addr))));

				//if (!uri_res.equalsIgnoreCase("type")) {
					observe_threads.add(new ObserveThread("coap://[" + addr + "]:5683/" + uri_res, container_cse + "/" + name));
					Runtime.getRuntime().addShutdownHook(new Thread() {
						public void run() {
							observe_threads.get(observe_threads.size() - 1).stopObserve();
						}
					});
					if ((observe_threads.get(observe_threads.size() - 1)).getState() == Thread.State.NEW)
						observe_threads.get(observe_threads.size() - 1).start();
				//}
			}
		}
	}


	/**
	 * @brief When a mote uri change, in consequence of fact that
	 * CoapMonitor receive a notifications from IN and a mote type 
	 * is changed, being mote uri equals mote name (obtained by
	 * make a GET request to resource type) we change also the
	 * mote uri of the containers and the uri in the observing
	 * thread where the thread receive notifications from observing.
	 * 
	 * @param[in] uri_mote old uri of the mote that we want to change
	 * @param[in] message message received from border router to notify added or removed mote.
	 * 
	 */
	public static void updateMote(String old_uri_mote, JSONObject new_content) {

		int id = new_content.getInt("id");
		String type = new_content.getString("type");
		String new_uri_mote = "Mote_" + id + "_" + type;

		if (mote_uri.contains(old_uri_mote)) {
			int i = mote_uri.indexOf(old_uri_mote);
			mote_uri.set(i, new_uri_mote);

			String ae_cse = Constants.MN_CSE_URI + "/" + AE_Monitor.getRn();
	
			for (ObserveThread ot : observe_threads) {
				if (ot.container_cse.contains(old_uri_mote)) {
					String uri_res = ot.container_cse.substring(ot.container_cse.lastIndexOf("/") + 1);
					ot.container_cse = ae_cse + "/" + new_uri_mote + "/" + uri_res;
				}
			}

			//String response = MCA.updateContainer(ae_cse, old_uri_mote, new_uri_mote);
			for (Container cont : containers) {
				if (cont.getRn().contains(old_uri_mote)) {
					String response = MCA.updateContainer(ae_cse, cont.getRn(), cont.getRn().replace(old_uri_mote, new_uri_mote));
					i = containers.indexOf(cont);
					Container new_container = MCA.setContainer(containers.get(i), response);
					containers.set(i, new_container);
					//break;
				}
			}
			//Container new_container = MCA.setContainer(containers.get(i), response);
			//containers.set(i, new_container);
			
			for (String subs : subscriptions) {
				if (subs.contains(old_uri_mote))
					i = subscriptions.indexOf(subs);
					String uri_res = subs.substring(subs.lastIndexOf("-") + 1);
					subscriptions.set(i, new_uri_mote + "-" + uri_res);
					MCA.updateSubscription(Constants.IN_CSE_COAP + "/" + "AE_Controller" + "/" + old_uri_mote + "/" + uri_res, subs,
						 new_uri_mote + "-" + uri_res, "coap://127.0.0.1:" + Constants.MN_COAP_PORT + "/CoapMonitor");
			}
			System.out.println("Updated Mote: " + old_uri_mote.split("_")[1] + ", " + old_uri_mote.split("_")[2] + " ===> " + id + ", " + type);
		}
	}
	
	
	public static ArrayList<String> getMoteUri() {
		return mote_uri;
	}
	
	public static ArrayList<String> getMoteAddr() {
		return mote_addr;
	}
	/**
	 * @brief Discover all the useful resources on the IN.
	 * 
	 * @param in_cse URI of the IN
	 * @return List of the discovered resources
	 */
	/*
	private static void discoverIN(String in_cse) {
		
		String[] ae_in = null;
		String[] containers_in = null;
		String[] resource_in = null;
		
		// Search the "AE_Controller" AE on the IN
		ae_in = MCA.discoverResources(in_cse, "?fu=1&rty=2&lbl=AE_Controller");
		
		if (ae_in == null) 
			return;

		for (String uri : mote_uri) {
			containers_in = MCA.discoverResources(in_cse + "/" + ae_in, "?fu=1&rty=3&lbl=" + uri);

			if (containers_in == null)
				return;

			for (String cont_in : containers_in) {
				resource_in = MCA.discoverResources(in_cse + "/" + ae_in + "/" + cont_in, "?fu=1&rty=3");

				if (resource_in == null)
					return;

				for (String res_in : resource_in) {

					if (res_in.contains("type")) {
						String cse = in_cse + "/" + ae_in + "/" + cont_in + "/" + res_in;
						MCA.createSubscription(cse , "Subscription-" + cont_in + "-" + res_in, "coap://127.0.0.1:5686/CoapMonitor");
						subscriptions.add("Subscription-" + cont_in + "-" + res_in);
						System.out.println("Subscribed to: " + cse);
					}
						
				}
			}
		}
		
	}
	*/
	



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