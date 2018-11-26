/**
 * 
 */
package platform.core;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

public class Mote {

	private String id;
	private String type;

	private float latitude;
	private float longitude;
	private float temperature;
	private float humidity;
	private int battery;

	/**
	 * 
	 */
	public Mote() {
		// TODO Auto-generated constructor stub
	}
	
	public Mote(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setId(JSONObject id) {
		id.get("id");
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public float getLatitude() {
		return latitude;
	}

	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	public float getTemperature() {
		return temperature;
	}

	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}
	
	public float getHumidity() {
		return humidity;
	}

	public void setHumidity(float humidity) {
		this.humidity = humidity;
	}

	public int getBattery() {
		return battery;
	}

	public void setBattery(int battery) {
		this.battery = battery;
	}
	
	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
	public static void main(String[] Args) {
		Mote sens = new Mote();
		
		sens.setBattery(9);
		sens.setHumidity(90);
		sens.setId("1990");
		
		System.out.println(sens.toJson());
	}

/*	public void setMoteResource(String response, String res, boolean b, boolean c) {
		// TODO Auto-generated method stub
		
	}*/
	
	public void setFieldFromJson(JSONObject fields) {
		try {
			if(fields.has("id"))
				setId((String)fields.get("id"));
			if (fields.has("type"))
				setType((String)fields.get("type"));
			if (fields.has("lat"))
				setLatitude(Float.parseFloat(fields.get("lat").toString()));
			if(fields.has("lng"))
				setLatitude(Float.parseFloat(fields.get("lng").toString()));
			if(fields.has("temperature"))
				setTemperature(Float.parseFloat(fields.get("temperature").toString()));
			if(fields.has("humidity"))
				setHumidity(Float.parseFloat(fields.get("humidity").toString()));
			if(fields.has("battery"))
				setBattery(Integer.parseInt(fields.get("battery").toString()));
		} catch(JSONException e) {
			System.err.println(e.getMessage());
		}
	}
	
}
