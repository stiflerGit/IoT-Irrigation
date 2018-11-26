package platform.gateway;

import java.util.ArrayList;

import platform.core.*;


public class DiscoverThread extends Thread {
	private static Mca MCA = Mca.getInstance();
	
	private String in_cse;

	private ArrayList<String> ae_in = new ArrayList<String>();
	private ArrayList<String> container_in = new ArrayList<String>();
	private ArrayList<String> resource_in = new ArrayList<String>();
	
	private boolean stop = false;


	public DiscoverThread(String cse_in) {
		this.in_cse = cse_in;// Constants.MN_CSE_COAP + "/" + Constants.IN_CSE_ID;
	}

	public void stopDiscover() {
		this.stop = true;
	}

	public void run() {

		while (!this.stop) {

			ae_in = MCA.discoverResources(in_cse, "?fu=1&rty=2&lbl=AE_Controller");

			if (ae_in != null && ae_in.size() > 0) {

				container_in = MCA.discoverResources(in_cse, "?fu=1&rty=3&lbl=Mote");
				
				if (container_in != null && container_in.size() > 0) {
					for (int i = 0; i < container_in.size(); i++)
						container_in.set(i, container_in.get(i).substring(container_in.get(i).lastIndexOf("/") + 1));

					for (String cont : container_in) {
						resource_in = MCA.discoverResources(in_cse, "?fu=1&rty=3&lbl=Resource-" + cont);
												
						if (resource_in != null && resource_in.size() > 0) {
							ArrayList<String> subs = new ArrayList<>(resource_in);
	
							for (int i = 0; i < resource_in.size(); i++)
								resource_in.set(i, resource_in.get(i).substring(resource_in.get(i).lastIndexOf("-") + 1));

							for (String res : resource_in) {
								if (res.contains("type") && !ADN.subscriptions.contains("Subscription-" + cont + "-" + res)) {
									MCA.createSubscription(Constants.IN_CSE_COAP + subs.get(resource_in.indexOf(res)), "Subscription-" + cont + "-" + res,
											"coap://127.0.0.1:" + Constants.MN_COAP_PORT + "/CoapMonitor");
									ADN.subscriptions.add("Subscription-" + cont + "-" + res);
									System.out.println("Subscribed to: " + Constants.IN_CSE_COAP + subs.get(resource_in.indexOf(res)));
								}
							}
						}
					}
				}
			}
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
