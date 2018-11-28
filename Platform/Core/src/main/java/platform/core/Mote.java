package platform.core;

import org.json.JSONObject;

import com.google.gson.Gson;


public class Mote {

	public String name;
	public String type;
	public Position position;
	public int battery;
	public float temperature;
	public float humidity;
	public int irrigation;
	
	public Updated updated;
	public Semaphore mutex;


	class Position {
		public float lat;
		public float lng;
	
		public Position() {}

		public Position setPosition(float lat, float lng) {
			this.lat = lat;
			this.lng = lng;
			return this;
		}
		
	}
	
	class Updated {
		public boolean temperature;
		public boolean humidity;
		public boolean battery;
		public boolean position;
		public boolean type;
		public boolean irrigation;
	
		public Updated() {
			temperature = false;
			battery = false;
			humidity = false;
			position = false;
			type = false;
			irrigation = false;
		}

	}
	
	/**
	 * Constructor for class Mote
	 *  
	 */
	public Mote(String name) {		
		this.name = name;
		this.type = "";
		this.battery = 0;
		this.temperature = 0;
		this.position = new Position();
		this.humidity = 0;
		this.irrigation = 0;
		this.mutex = new Semaphore(true);
		this.updated = new Updated();
	}

	
	public void setMoteResource(String content, String res) {

		JSONObject object = new JSONObject(content);
		
		if (res.equals("type")) {
			this.type = object.getString("type");
			this.updated.type = true;
		} else if (res.equals("irrigation")) {
			this.irrigation = object.getInt("irrigation");
			this.updated.irrigation = true;
		} else if (res.equals("battery")) {
			this.battery = object.getInt("battery");
			this.updated.battery = true;
		} else if (res.equals("gps")) {
			this.position = position.setPosition((float) object.getDouble("lat"), (float) object.getDouble("lng"));
			this.updated.position = true;
		} else if (res.equals("temperature")) {
			this.temperature = (float) object.getDouble("temperature");
			this.updated.temperature = true;
		} else if (res.equals("humidity")) {
			this.humidity = (float) object.getDouble("humidity");
			this.updated.humidity = true;
		}
	}
	
	
	public String toJSON() {
		Gson gson = new Gson();
		String json = gson.toJson(this);
		this.updated.temperature = false;
		this.updated.battery = false;
		this.updated.humidity = false;
		this.updated.position = false;
		this.updated.type = false;
		this.updated.irrigation = false;
		return json;
	}

}
