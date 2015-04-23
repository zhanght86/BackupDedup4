package xjtu.dedup.multithread;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import org.opendedup.util.SDFSLogger;

public class BackupRestoreThreadPool {
	private ArrayBlockingQueue<BackupClient> taskQueue = null;
	private List<BackupRestorePoolThread> threads = new ArrayList<BackupRestorePoolThread>();
	private boolean isStopped = false;
	int poolSize;
	private Vector vector;
	
	public BackupRestoreThreadPool(int noOfThreads, int maxNoOfTasks) {
		taskQueue = new ArrayBlockingQueue<BackupClient>(maxNoOfTasks);
		poolSize=maxNoOfTasks;
	}
	public void add(BackupClient task){
		if (this.isStopped) {
			SDFSLogger.getLog().warn("threadpool is stopped will not execute task");
			return;
		}
			try {
				this.taskQueue.put(task);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
	public void execute()
		{
		for (int i=0;i<poolSize;i++) {
			BackupRestorePoolThread thread;
			try {
				thread = new BackupRestorePoolThread(taskQueue.take());
				thread.start();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public synchronized void stop() {
		this.isStopped = true;
		for (BackupRestorePoolThread thread : threads) {
			thread.exit();
		}
	}
}
