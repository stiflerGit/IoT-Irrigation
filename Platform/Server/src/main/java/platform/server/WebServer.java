package platform.server;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.json.JSONObject;

import platform.core.Constants;

@WebSocket
public class WebServer {
	public static Session currentSession = null;

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		currentSession = null;
		System.out.println("Close: statusCode = " + statusCode + ", reason = " + reason);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		currentSession = null;
		System.out.println("Error: " + t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		currentSession = session;
		if (WebServer.currentSession != null)
			for (int i = 0; i < ADN.getMotes().size(); i++) {
				String str = ADN.getMotes().get(i).toJSON();

				JSONObject object = new JSONObject();
				object.put("data", new JSONObject(str));
				object.put("action", "POST");

				send(object.toString());
			}
		System.out.println("Connect: " + currentSession.getRemoteAddress().getAddress());
	}

	@OnWebSocketMessage
	public void onMessage(String message) {

		JSONObject root = new JSONObject(message);

		if (root.getString("action").equals("PUT")) {

			JSONObject data = (JSONObject) root.get("data");
			JSONObject updated = (JSONObject) data.get("updated");
			
			Iterator<?> keys = data.keys();
			String key;
			int i = -1;

			while (keys.hasNext()) {
				key = (String) keys.next();
				System.out.println(key + " " + data.get(key).toString());
				if (key.equals("nome")) {
					for (i = 0; i < ADN.getMotes().size(); i++) {
						if (ADN.getMotes().get(i).name.equals(data.get(key).toString()))
							break;
					}
				}
			}
			
			keys = updated.keys();

			while (keys.hasNext()) {
				key = (String) keys.next();
				if (updated.getBoolean(key)) {

					String k = (key.equals("tipo")) ? "type" : key;
					String content = "{'" + k + "':'" + data.get(key).toString() + "\'}";
					
					ADN.getMotes().get(i).setMoteResource(content, k);
					
					ADN.getMca().createContentInstance(Constants.IN_CSE_URI + "/" + ADN.getAE().getRn() + "/"
							+ ADN.getMotes().get(i).name + "/" + ADN.getMotes().get(i).name + "-actuator_" + k, content);
				}
			}
		}
	}

	
	public static void send(String data) {
		try {
			currentSession.getRemote().sendString(data);
			System.out.println(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}		

}
