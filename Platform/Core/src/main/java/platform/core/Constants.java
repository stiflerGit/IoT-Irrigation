package platform.core;

public class Constants {
	/**
	 * Loopback address
	 */
	public static final String LOOPBACK = "127.0.0.1";
	//public static final String LOOPBACK = "10.0.2.2";

	/**
	 * Border Router global IPv6 address
	 */
	public static final String BR_ADDR = "fd00::c30c:0:0:1";
	
	
	/******************** Middle Node Constants **********************/
	/**
	 * Middle Node CSE CoAP URI.
	 */
	public static final String MN_CSE_COAP = "coap://" + LOOPBACK + ":5684/~";
	/**
	 * Middle Node CSE ID.
	 */
	public static final String MN_CSE_ID = "mn-cse";
	/**
	 * Middle Node CSE Name.
	 */
	public static final String MN_CSE_NAME = "mn-name";	
	/**
	 * Middle Node CSE complete URI.
	 */
	public static final String MN_CSE_URI = MN_CSE_COAP + "/" + MN_CSE_ID + "/" + MN_CSE_NAME;
	/**
	 * CoAP Monitor port Middle Node CSE.
	 */
	public static final int MN_COAP_PORT = 5687;
	

	/******************** Infrastructure Node Constants **********************/
	/**
	 * Infrastructure Node CSE CoAP URI.
	 */
	public static final String IN_CSE_COAP = "coap://" + LOOPBACK + ":5683/~";
	/**
	 * Infrastructure Node CSE ID.
	 */
	public static final String IN_CSE_ID = "in-cse";
	/**
	 * Infrastructure Node CSE Name.
	 */
	public static final String IN_CSE_NAME = "in-name";
	/**
	 * Infrastructure Node CSE complete URI.
	 */
	public static final String IN_CSE_URI = IN_CSE_COAP + "/" + IN_CSE_ID + "/" + IN_CSE_NAME;
	/**
	 * CoAP Monitor port Infrastructure Node CSE.
	 */
	public static final int IN_COAP_PORT = 5686;
	
	public static final int NETWORK_NUM = 2;
	
}
