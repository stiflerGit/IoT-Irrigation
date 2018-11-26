package it.unipi.iot.in;

import java.io.IOException;
import java.util.List;

import javax.lang.model.type.NullType;

import org.eclipse.jetty.util.thread.ThreadClassLoaderScope;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import platform.core.*;

@WebSocket
public class WebMotesServer {

	public Session session;

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		Adn.webSocket = null;
		System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		System.out.println("Error: " + t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		this.session = session;
		Adn.webSocket = this;
		System.out.println("Connect: " + session.getRemoteAddress().getAddress());
	}

	@OnWebSocketMessage
	public void onMessage(String message) throws IOException {

		System.out.println(message);

		JSONObject root = new JSONObject(message);

		String action = root.getString("action");
		if (action == null) {
			System.out.println("No Action");
			return;
		}
		
		switch (action) {
		case "get":
			sendMotesList();
			break;
		case "update":
			// validate data and then send back the updated mote
			// update only have one object at time
			JSONObject jsonMote = root.getJSONObject("mote");
			Mote mote = Adn.motes.get(jsonMote.getString("id"));
			String[] fields = JSONObject.getNames(jsonMote);
			mote.setFieldFromJson(jsonMote);
			for (String field: fields) {
				String cse = Constants.MN_CSE_URI + "/AE_Monitor/" + jsonMote.getString("id") + "/" +
						jsonMote.getString("id") + "-Resource_" + field;
				String value = jsonMote.get(field).toString(); 
				Mca.getInstance().createContentInstance(cse, value);
			}
			break;
		default:
			System.err.println("Action Not Implemented" + action);
			break;
		}
	}
	
	public void sendMotesList() throws IOException {
		JSONArray list = sensorList();
		JSONObject jsonResp = new JSONObject();
		jsonResp.put("action", "list");
		jsonResp.put("motes", list);
//		System.out.println(jsonResp.toString());
		session.getRemote().sendString(jsonResp.toString());
	}

	private JSONArray sensorList() {
		JSONArray arr = new JSONArray();
		System.out.println("GETTING SENSOR LIST");
		List<Mote> list = Adn.getSensorsList();
		for(int i = 0; i < list.size(); i++) {
//			System.out.println(list.get(i).getId());
			Gson json = new Gson();
			arr.put(json.toJson(list.get(i)));
		}
		return arr;
	}
	
	public void send(String body) throws IOException {
		session.getRemote().sendString(body);
	}
}
