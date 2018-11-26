package platform.core;

import org.json.JSONException;
import org.json.JSONObject;

public class ContentIstance {
	public final int type = 4;
	String rn;
	String ri;
	String pi;
	String ct;
	String lt;
	int st;
	String cnf;
	int cs;
	String con;
	
	public ContentIstance(JSONObject om2mCIN) {
		JSONObject data = null;
		try {
			data = (JSONObject) om2mCIN.get("m2m:cin");
		} catch (JSONException e) {
			// it is not a m2m Application Entity
			System.err.println("Error in the JSON Input m2m:ae missing");
			System.err.println(om2mCIN.toString());
			e.printStackTrace();
			System.exit(-1);
		}
		try {
			// this info must be present
			this.setRn((String) data.get("rn"));
			if ((Integer) data.get("ty") != type) {
				System.err.println("Not a Content Istance");
				System.exit(-1);
			}
			this.setRi(data.getString("ri"));
			this.setPi(data.getString("pi"));
			this.setCt(data.getString("ct"));
			this.setLt(data.getString("lt"));
//			int st;
//			String cnf;
//			int cs;
			this.setCon(data.getString("con"));
		} catch (JSONException e) {
			// missing info
			System.err.println("missing data for om2m CIN");
			System.err.println(data.toString());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public String getRn() {
		return rn;
	}
	public void setRn(String rn) {
		this.rn = rn;
	}
	public String getRi() {
		return ri;
	}
	public void setRi(String ri) {
		this.ri = ri;
	}
	public String getPi() {
		return pi;
	}
	public void setPi(String pi) {
		this.pi = pi;
	}
	public String getCt() {
		return ct;
	}
	public void setCt(String ct) {
		this.ct = ct;
	}
	public String getLt() {
		return lt;
	}
	public void setLt(String lt) {
		this.lt = lt;
	}
	public int getSt() {
		return st;
	}
	public void setSt(int st) {
		this.st = st;
	}
	public String getCnf() {
		return cnf;
	}
	public void setCnf(String cnf) {
		this.cnf = cnf;
	}
	public int getCs() {
		return cs;
	}
	public void setCs(int cs) {
		this.cs = cs;
	}
	public String getCon() {
		return con;
	}
	public void setCon(String con) {
		this.con = con;
	}
	
	
}
