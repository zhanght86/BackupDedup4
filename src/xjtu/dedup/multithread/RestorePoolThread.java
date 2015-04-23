package xjtu.dedup.multithread;

import java.util.concurrent.BlockingQueue;

import org.opendedup.util.SDFSLogger;

public class RestorePoolThread extends Thread {
	private BlockingQueue<RestoreClient> taskQueue = null;
	private boolean isStopped = false;
	

	public RestorePoolThread(BlockingQueue<RestoreClient> queue) {
		taskQueue = queue;
	}

	public void run() {
		while (!isStopped()) {
			try {
				RestoreClient runnable = taskQueue.take();
				System.out.println("start the actual job!");
					try {
						runnable.dojob();
					} catch (Exception e) {
						e.printStackTrace();
					}
			} catch (Exception e) {
				SDFSLogger.getLog().fatal( "unable to execute thread", e);
				// SDFSLogger.getLog() or otherwise report exception,
				// but keep pool thread alive.
			}
		}
	}

	public synchronized void exit() {
		isStopped = true;
		this.interrupt(); // break pool thread out of dequeue() call.
	}

	public synchronized boolean isStopped() {
		return isStopped;
	}
}
