
package platform.core;


public class Semaphore {
	
	final Object monitor = new Object();
	volatile boolean lock;
	
	
	public Semaphore(boolean lock) {
		this.lock = lock;
	}
	
	
	public void doWait() {
		synchronized(this.monitor) {
			while (!this.lock) {
				try {
					this.monitor.wait();
				} catch (InterruptedException e) {}
			}
			this.lock = false;
		}	
	}
	
	
	public void doNotify() {
		synchronized(this.monitor) {
			this.lock = true;
			this.monitor.notifyAll();
		}
	}
	
}

