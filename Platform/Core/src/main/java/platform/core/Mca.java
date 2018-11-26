package platform.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public final class Mca {

	private static Mca instance = null;


	private Mca() {}


	public String om2mRequest(String request_type, int resource_type, String cse, String param1, String param2) {
		URI uri = null;

		try {
			if (request_type.equalsIgnoreCase("GET"))
				uri = new URI(cse + "/" + param1);
			else
				uri = new URI(cse);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		CoapClient client = new CoapClient(uri);
		Request request = null;

		if (request_type.equalsIgnoreCase("GET")) {
			request = Request.newGet();
		} else if (request_type.equalsIgnoreCase("DELETE")) {
			request = Request.newDelete();
		} else if (request_type.equalsIgnoreCase("POST")) {
			request = Request.newPost();
			request.getOptions().addOption(new Option(267, resource_type));
		} else if (request_type.equalsIgnoreCase("PUT")) {
			request = Request.newPut();
			request.getOptions().addOption(new Option(267, resource_type));
		}

		request.getOptions().addOption(new Option(256, "admin:admin"));
		request.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_JSON);
		request.getOptions().setAccept(MediaTypeRegistry.APPLICATION_JSON);

		if (request_type.equalsIgnoreCase("POST") || request_type.equalsIgnoreCase("PUT")) {

			JSONObject content = new JSONObject();
			JSONObject root = new JSONObject();

			if (resource_type == 2) {
				content.put("api", param1.concat("-ID"));
				content.put("rr", "true");
				content.put("rn", param1);
				content.put("lbl", param2);
				root.put("m2m:ae", content);
			} else if (resource_type == 3) {
				content.put("rn", param1);
				content.put("lbl", param2);
				root.put("m2m:cnt", content);
			} else if (resource_type == 4) {
				content.put("cnf", "Data");
				content.put("con", param1);
				root.put("m2m:cin", content);
			} else if (resource_type == 23) {
				content.put("rn", param1);
				content.put("nu", param2);
				content.put("nct", 2);
				root.put("m2m:sub", content);
			}

			String body = root.toString();
			request.setPayload(body);
		}

		CoapResponse responseBody = client.advanced(request);

		if (responseBody == null) {
			System.err.println("MCA: Error in om2mRequest():" + request_type + ", " + resource_type + ", " + "no response from " + cse);
			System.exit(-1);
		}

		if (request_type.equalsIgnoreCase("DELETE"))
			return new String("Delete Resource of type " + resource_type);

		return new String(responseBody.getPayload());
	}

	
	/* ------------------------------------------------------------------------------- */
	/* Get function */
	/* ------------------------------------------------------------------------------- */
	public AE getAE(String cse, String rn) {
		JSONObject object = null;
		
		String response = om2mRequest("GET", 0, cse, rn, "");
		if (response == null || response.contains("Resource not found")) {
			System.err.println("MCA: Error in get AE, " + cse + "/" + rn + " not found");
			System.exit(-1);
		}

		try {
			object = new JSONObject(response);
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return new AE(object);
	}
	
	
	public Container getContainer(String cse, String rn) {
		JSONObject object = null;
		
		String response = om2mRequest("GET", 0, cse, rn, "");
		if (response == null || response.contains("Resource not found")) {
			System.err.println("MCA: Error in get Container, " + cse + "/" + rn + " not found");
			System.exit(-1);
		}

		try {
			object = new JSONObject(response);
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return new Container(object);
	}
	
	public ContentIstance getContentIstance(String cse, String rn) {
		JSONObject object = null;
		
		String response = om2mRequest("GET", 0, cse, rn, "");
		if (response == null || response.contains("Resource not found")) {
			System.err.println("MCA: Error in get Container, " + cse + "/" + rn + " not found");
			System.exit(-1);
		}

		try {
			object = new JSONObject(response);
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return new ContentIstance(object);
	}

		
	public static Mca getInstance() {
		if (instance == null) {
			instance = new Mca();
		}
		return instance;
	}

	
	/* ------------------------------------------------------------------------------- */
	/* Create function */
	/* ------------------------------------------------------------------------------- */
	public AE createAE(String cse, String rn, String lbl) {
				
		String response = om2mRequest("POST", 2, cse, rn, lbl);
		
		if (response.contains("Name already present"))
			return getAE(cse, rn);

		JSONObject object = null;
		try {
			object = new JSONObject(response);
		} catch (JSONException e) {
			System.err.println("MCA: Error in create AE, " + cse + "/" + rn + " ===> " + response);
			e.printStackTrace();
			System.exit(-1);
		}
		return new AE(object);
	}
	
	
	public Container createContainer(String cse, String rn, String lbl) {

		String response = om2mRequest("POST", 3, cse, rn, lbl);

		if (response.contains("Name already present"))
			return getContainer(cse, rn);

		JSONObject object = null;
		try {
			object = new JSONObject(response);
		} catch (JSONException e) {
			System.err.println("MCA: Error in create Container, " + cse + "/" + rn + " ===> " + response);
			e.printStackTrace();
			System.exit(-1);
		}
		return new Container(object);
	}


	public void createContentInstance(String cse, String val) {
		om2mRequest("POST", 4, cse, val, "");
	}


	public void createSubscription(String cse, String rn, String notificationUrl) {
		om2mRequest("POST", 23, cse, rn, notificationUrl);
	}


	/* ------------------------------------------------------------------------------- */
	/* Delete function */
	/* ------------------------------------------------------------------------------- */
	public void deleteContainer(String cse, String rn) {
		om2mRequest("DELETE", 0, cse + "/" + rn, "", "");
	}

	
	/* ------------------------------------------------------------------------------- */
	/* Update function */
	/* ------------------------------------------------------------------------------- */
	public String updateContainer(String cse, String old_rn, String new_rn) {
		return om2mRequest("PUT", 3, cse + "/" + old_rn, new_rn, new_rn);
	}

	
	public Container setContainer(Container container, String response) {
		JSONObject object = null;
		try {
			object = new JSONObject(response);
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		//container.setRn((String) new_rn);
		//container.setLbl((String) new_rn);
		object = (JSONObject) object.get("m2m:cnt");
		container.setRn((String) object.get("rn"));
		container.setTy((Integer) object.get("ty"));
		container.setRi((String) object.get("ri"));
		container.setPi((String) object.get("pi"));
		container.setCt((String) object.get("ct"));
		container.setLt((String) object.get("lt"));
		container.setSt((Integer) object.get("st"));
		container.setOl((String) object.get("ol"));
		container.setLa((String) object.get("la"));
		container.setLbl((String) object.get("lbl"));

		return container;
	}

	
	public void updateSubscription(String cse, String old_rn, String new_rn, String notificationUrl) {
		om2mRequest("PUT", 23, cse + "/" + old_rn, new_rn, notificationUrl);
	}


	/* ------------------------------------------------------------------------------- */
	/* Discover function */
	/* ------------------------------------------------------------------------------- */
	public ArrayList<String> discoverResources(String cse, String query) {
		String response = om2mRequest("GET", 0, cse, query, "");
		JSONObject object = new JSONObject(response);
		JSONArray array = object.getJSONArray("m2m:uril");

		if (array.length() == 0)
			return null;

		ArrayList<String> result = new ArrayList<String>(array.length());
		for (int i = 0; i < array.length(); i++)
			result.add(array.getString(i));

		return result;
	}
		
}