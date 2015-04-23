package xjtu.dedup.multithread;

import java.util.concurrent.BlockingQueue;

import org.opendedup.util.SDFSLogger;

public class BackupRestorePoolThread extends Thread {
	private BlockingQueue<BackupClient> taskQueue = null;
	private boolean isStopped = false;
	private BackupClient backupClient;

	public BackupRestorePoolThread(BlockingQueue<BackupClient> queue) {
		taskQueue = queue;
	}

	public BackupRestorePoolThread(int i){
		System.out.println("thread "+i+" started.");
	}
	public BackupRestorePoolThread(BackupClient backupClient){	
		this.backupClient=backupClient;
	}
	public synchronized void run() {
		while (!isStopped()) {
			try {
			//	BackupClient runnable = taskQueue.take();
					try {
						System.out.println("do the actual job!");
						//runnable.dojob();
						backupClient.execBackupJob();
						Thread.sleep(5000);
						this.notify();
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
