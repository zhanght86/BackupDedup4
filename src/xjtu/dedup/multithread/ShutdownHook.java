package xjtu.dedup.multithread;

import org.opendedup.sdfs.servers.SDFSService;

class ShutdownHook extends Thread {
	private SDFSService service;
	public ShutdownHook(SDFSService service) {
		this.service = service;
	}
	
	public void run() {
		System.out.println("Please Wait while shutting down SDFS");
		System.out.println("Data Can be lost if this is interrupted");
		service.stopbackupservice();
		System.out.println("All Data Flushed");
		System.out.println("SDFS Shut Down Cleanly");

	}
}
