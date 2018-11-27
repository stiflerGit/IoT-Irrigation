package platform.gateway;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;

import platform.core.*;

public class ObserveThread extends Thread {

	public String container_cse;
	private Mca mca;
	private Semaphore semaphore;
	private boolean stop;
	private String last_content;
	
	private CoapClient client;
	private ObserveHandler handler;
	private CoapObserveRelation relation;


	class ObserveHandler implements CoapHandler {

		private String uri;
		private String content;
		private Semaphore semaphore;


		public ObserveHandler(Semaphore semaphore, String uri) {
			this.uri = uri;
			this.semaphore = semaphore;
			this.content = "";
		}


		public String getContent() {
			return this.content;
		}

		
		@Override
		public void onLoad(CoapResponse response) {
			this.content = response.getResponseText();
			System.out.println("Received new content " + this.content + " from " + this.uri);
			this.semaphore.doNotify();
		}

		
		@Override
		public void onError() {
			this.content = "";
			System.err.println("Unable to receive new content from " + this.uri);
			this.semaphore.doNotify();
		}

	}


	public ObserveThread(String uri_res, String container_cse) {
		URI uri = null;
		try {
			uri = new URI(uri_res);
		} catch (URISyntaxException e) {
			System.err.println("Invalid URI: " + e.getMessage());
		}
		this.container_cse = container_cse;
		this.mca = Mca.getInstance();
		this.semaphore = new Semaphore(false);
		this.stop = false;
		this.last_content = "";

		this.client = new CoapClient(uri);
		this.handler = new ObserveHandler(semaphore, uri.toString());
		System.out.println(this.container_cse);
	}

	
	public void stopObserve() {
		this.relation.proactiveCancel();
		this.stop = true;
	}

	
	public void run() {
		this.relation = this.client.observe(this.handler);
		
		while(!this.stop) {
			this.semaphore.doWait();
			String content = handler.getContent();

			if (!content.equals("") && !content.equals(last_content)) {
				if (this.container_cse.equalsIgnoreCase("BorderRouter"))
					ADN.modifyMote(content);
				else
					this.mca.createContentInstance(this.container_cse, content);
				last_content = content;
			}
		}
	}

}
