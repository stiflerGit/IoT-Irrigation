package platform.server;

import java.util.ArrayList;

import org.json.JSONObject;

import platform.core.*;

public class DiscoverThread extends Thread {

	private String mn_cse;

	private ArrayList<String> ae_mn = new ArrayList<String>();
	private ArrayList<String> container_mn = new ArrayList<String>();
	private ArrayList<String> resource_mn = new ArrayList<String>();

	private boolean stop = false;

	public DiscoverThread(String cse_mn) {
		this.mn_cse = cse_mn;
	}

	public void stopDiscover() {
		this.stop = true;
	}

	public void run() {

		while (!this.stop) {

			ae_mn = ADN.getMca().discoverResources(mn_cse, "?fu=1&rty=2&lbl=AE_Monitor");

			boolean new_mote = false;
			
			if (ae_mn != null && ae_mn.size() > 0) {

				for (int i = 0; i < ae_mn.size(); i++)
					ae_mn.set(i, ae_mn.get(i).substring(ae_mn.get(i).lastIndexOf("/") + 1));
					
				container_mn = ADN.getMca().discoverResources(mn_cse, "?fu=1&rty=3&lbl=Mote");

				if (container_mn != null && container_mn.size() > 0) {
					for (int i = 0; i < container_mn.size(); i++)
						container_mn.set(i, container_mn.get(i).substring(container_mn.get(i).lastIndexOf("/") + 1));

					if (container_mn.size() > ADN.getMoteUri().size()) {
						new_mote = true;
						container_mn.removeAll(ADN.getMoteUri());
					} else {
						for (int i = 0; i < ADN.getMoteUri().size(); i++) {
							if (!container_mn.contains(ADN.getMoteUri().get(i))) {

								if (ADN.getContainers().get(i).getRn().contains(ADN.getMoteUri().get(i)))
									ADN.getContainers().remove(i);
								if (ADN.getSubscriptions().get(i).contains(ADN.getMoteUri().get(i)))
									ADN.getSubscriptions().remove(i);

								ADN.getMca().deleteContainer(Constants.IN_CSE_URI + "/" + ADN.getAE().getRn(), ADN.getMoteUri().get(i));
								ADN.getMoteUri().remove(i);

							// WEBSOCKET DELETE
								if (WebServer.currentSession != null) {
									String str = ADN.getMotes().get(i).toJSON();
									
									JSONObject object = new JSONObject();
									object.put("data", new JSONObject(str));
									object.put("action", "DELETE");
								
									WebServer.send(object.toString());
								}
								ADN.getMotes().remove(i);
								new_mote = false;
							}
						}
					}
				}
			}

			if (new_mote == true) {
				
				String cse = Constants.IN_CSE_URI + "/" + ADN.getAE().getRn();

				for (String mote : container_mn) {
					System.out.println("New mote: " + mote);
					ADN.getMoteUri().add(mote);
					ADN.getContainers().add(ADN.getMca().createContainer(cse, mote, "Mote"));
					ADN.getMotes().add(new Mote(mote));
					
					resource_mn = ADN.getMca().discoverResources(mn_cse, "?fu=1&rty=3&lbl=sensor-" + mote);

					if (resource_mn != null && resource_mn.size() > 0) {
						ArrayList<String> subs = new ArrayList<String>(resource_mn);
						for (int i = 0; i < resource_mn.size(); i++) {
							resource_mn.set(i, resource_mn.get(i).substring(resource_mn.get(i).lastIndexOf("-") + 1));
							System.out.println(resource_mn.get(i));
						}

						for (String res : resource_mn) {

							JSONObject root = ADN.getLa(ae_mn.get(0), mote, res);
							
							if (root != null && root.has("m2m:cin")) {
								JSONObject m2mcin = root.getJSONObject("m2m:cin");
								String response = m2mcin.getString("con");

								ADN.getMotes().get(ADN.getMotes().size() - 1).setMoteResource(response, res.replace("sensor_", ""));
							}

							ADN.getMca().createSubscription(Constants.MN_CSE_COAP + subs.get(resource_mn.indexOf(res)), "Subscription-" + mote + "-" + res,
									"coap://127.0.0.1:" + Constants.IN_COAP_PORT + "/CoapMonitorServer");
							ADN.getSubscriptions().add("Subscription-" + mote + "-" + res);
							System.out.println("Subscribed to: " + Constants.MN_CSE_COAP + subs.get(resource_mn.indexOf(res)));
						}
					}
					
					resource_mn = ADN.getMca().discoverResources(mn_cse, "?fu=1&rty=3&lbl=actuator-" + mote);

					if (resource_mn != null && resource_mn.size() > 0) {
						for (int i = 0; i < resource_mn.size(); i++)
							resource_mn.set(i, resource_mn.get(i).substring(resource_mn.get(i).lastIndexOf("-") + 1));

							for (String res : resource_mn) {
								JSONObject root = ADN.getLa(ae_mn.get(0), mote, res);
							
							if (root != null && root.has("m2m:cin")) {
								JSONObject m2mcin = root.getJSONObject("m2m:cin");
								String response = m2mcin.getString("con");

								ADN.getMotes().get(ADN.getMotes().size() - 1).setMoteResource(response, res.replace("actuator_", ""));
								
							}
							ADN.getContainers().add(ADN.getMca().createContainer(cse + "/" + mote, mote + "-" + res, "actuator-" + mote));
							//ADN.MCA.createContentInstance(Constants.IN_CSE_COAP + "/" + Constants.IN_CSE_ID + "/" + ADN.AE_Controller.getRn() + "/" + ADN.containers.get(ADN.containers.size() - 1).getRn(),
								//	"{\"type\":\"" + mote.split("_")[1] + "\",\"" + mote.split("_")[2] + "\"}");
						}
					}

					
					// WEBSOCKET POST
					if (WebServer.currentSession != null) {
						String str = ADN.getMotes().get(ADN.getMotes().size() - 1).toJSON();
						
						JSONObject object = new JSONObject();
						object.put("data", new JSONObject(str));
						object.put("action", "POST");
					
						WebServer.send(object.toString());
					}

				}
			}
			try {
				sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}