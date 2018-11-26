package platform.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AE {
	private String rn;
	private int ty;
	private String ri;
	private String pi;
	private String ct;
	private String lt;
	private List<String> lbl;
	private List<String> acpi;
	private String et;
	private String api;
	private String aei;
	private boolean rr;

	public AE(JSONObject object) {

		try {
			object = (JSONObject) object.get("m2m:ae");
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			this.setRn((String) object.get("rn"));
			this.setTy((Integer) object.get("ty"));
			this.setRi((String) object.get("ri"));
			this.setPi((String) object.get("pi"));
			this.setCt((String) object.get("ct"));
			this.setLt((String) object.get("lt"));
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			JSONArray array = object.getJSONArray("lbl");
			if (array.length() > 0) {
				this.lbl = new ArrayList<String>(array.length());
				for (int i = 0; i < array.length(); i++)
					this.setLbl(array.getString(i));
			}
		} catch (JSONException e) {
			System.out.println("Label for AE: " + this.getRn() + " missing");
		}
	}

	public int getTy() {
		return ty;
	}

	public void setTy(int ty) {
		this.ty = ty;
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

	public List<String> getAcpi() {
		return acpi;
	}

	public void setAcpi(List<String> acpi) {
		this.acpi = acpi;
	}

	public String getEt() {
		return et;
	}

	public void setEt(String et) {
		this.et = et;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public String getAei() {
		return aei;
	}

	public void setAei(String aei) {
		this.aei = aei;
	}

	public boolean isRr() {
		return rr;
	}

	public void setRr(boolean rr) {
		this.rr = rr;
	}

	public String getRn() {
		return rn;
	}

	public void setRn(String rn) {
		this.rn = rn;
	}
	
	public String getLbl() {
		return this.lbl.get(0);
	}

	public void setLbl(String lbl) {
		this.lbl.add(lbl); 
	}

	/*
	public List<String> getLbl() {
		return lbl;
	}

	public void setLbl(List<String> lbl) {
		this.lbl = lbl;
	}
	*/

}