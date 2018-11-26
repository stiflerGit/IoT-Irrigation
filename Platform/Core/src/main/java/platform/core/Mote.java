package platform.core;

import org.json.JSONObject;

//import com.google.gson.Gson;

public class Mote {

	public String name;
	public float lat;
	public float lng;
	public int battery;
	public float temperature;
	public float humidity;
	
	public boolean new_temperature;
	public boolean new_humidity;
	public boolean new_battery;
	public boolean new_position;

	public boolean is_new;
	public Semaphore mutex;
	public boolean first_time;


	/**
	 * Constructor for class Mote
	 *  
	 */
	public Mote(String name) {		
		this.name = name;
		this.battery = 0;
		this.temperature = 0;
		this.lat = 0;
		this.lng = 0;
		this.humidity = 0;
	
		this.new_battery = false;
		this.new_temperature = false;
		this.new_humidity = false;
		this.new_position = false;
	
		this.is_new = false;
		this.first_time = true;
		mutex = new Semaphore(true);
	}


	/* Getter methods */

	public String getName() {
		return name;
	}
	

	public float getTemperature() {
		return temperature;
	}
	

	public float getHumidity() {
		return humidity;
	}
	

	public JSONObject getPosition() {
		JSONObject object = new JSONObject();
		object.put("lat", lat);
		object.put("lng", lng);
		
		return object;
	}
	
	public int getBattery() {
		return battery;
	}


	public JSONObject getMoteResource(String res) {
		JSONObject object = new JSONObject();
	
		//object.put("name", name);
		if (res.equals("temperature")) {
			object.put("val", temperature);
		}
		if (res.equals("humidity")) {
			object.put("val", humidity);
		}
		if (res.equals("battery")) {
			object.put("val", battery);
		}
		if (res.equals("position")) {
			JSONObject root = new JSONObject();
			root.put("lat", lat);
			root.put("lng", lng);
			object.put("val", root);
			
		}

		return object;
	}
		
	/* Setter methods */

	public void setName(String name) {
		this.name = name;
	}
		
		
	public void setTemperature(float temperature, boolean update) {
		this.temperature = temperature;
		if (update) {
			this.new_temperature = true;
			this.is_new = true;
		}
	}
	
	
	public void setHumidity(float humidity, boolean update) {
		this.humidity = humidity;
		if (update) {
			this.new_humidity = true;
			this.is_new = true;
		}
	}


	public void setPosition(JSONObject object, boolean update) {
		float lat = (float) object.getDouble("lat");
		float lng = (float) object.getDouble("lng");
		this.lat = lat;
		this.lng = lng;
		if (update) {
			this.new_position = true;
			this.is_new = true;
		}
	}


	public void setBattery(int battery, boolean update) {
		this.battery = battery;
		if (update) {
			this.new_battery = true;
			this.is_new = true;
		}
	}

	
	public void setMoteResource(String content, String res, boolean update) {//, boolean first_time) {

		JSONObject object = new JSONObject(content);
		
		if (res.equals("type")) {
			setName("Mote_" + object.getInt("id") + "_" + object.getString("type"));
		} else if (res.equals("battery")) {
			setBattery(object.getInt("battery"), update);
		} else if (res.equals("gps")) {
			setPosition(object, update);
		} else if (res.equals("temperature")) {
			setTemperature((float) object.getDouble("temperature"), update);
		} else if (res.equals("humidity")) {
			setHumidity((float) object.getDouble("humidity"), update);
		}
	}
	
	/*
	public String toJSON() {
		this.new_battery = false;
		this.new_position = false;
		this.new_temperature = false;
		this.new_humidity = false;
		this.is_new = false;
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return json;
	}
	

	public Mote toMote(String json) {
		Gson gson = new Gson();
		Mote deserialized = gson.fromJson(json, Mote.class);
		return deserialized;
	
	}
	*/
	

	/*
	public JSONObject getUpdateContent(String res) {
		JSONObject object = new JSONObject();
	
		object.put("name", name);
		if (new_temperature) {
			object.put("temperature", temperature);
			new_temperature = false;
		}
		if (new_humidity) {
			object.put("humidity", humidity);
			new_temperature = false;
		}
		if (new_battery) {	
			object.put("battery", battery);
			new_battery = false;
		}
		if (new_position) {
			object.put("lat", lat);
			object.put("lng", lng);
			new_position = false;
			
		}
		
		is_new = false;
		return object;
	}
	*/
	
	
	

}
