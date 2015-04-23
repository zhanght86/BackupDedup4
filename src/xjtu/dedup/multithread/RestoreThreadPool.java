package xjtu.dedup.multithread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.opendedup.util.SDFSLogger;

public class RestoreThreadPool {
	private ArrayBlockingQueue<RestoreClient> taskQueue = null;
	private List<RestorePoolThread> threads = new ArrayList<RestorePoolThread>();
	private boolean isStopped = false;
	

	public RestoreThreadPool(int noOfThreads, int maxNoOfTasks) {
		taskQueue = new ArrayBlockingQueue<RestoreClient>(maxNoOfTasks);

		for (int i = 0; i < noOfThreads; i++) {
			threads.add(new RestorePoolThread(taskQueue));
		}
		for (RestorePoolThread thread : threads) {
			System.out.println("start the thread");
			thread.start();
		}
	}

	public void execute(RestoreClient task)
			{
		if (this.isStopped) {
			SDFSLogger.getLog().warn("threadpool is stopped will not execute task");
			return;
		}
			
		try {
			this.taskQueue.put(task);
		} catch (InterruptedException e) {
			SDFSLogger.getLog().warn( "thread interrupted",e);
		}
	}

	public synchronized void stop() {
		this.isStopped = true;
		for (RestorePoolThread thread : threads) {
			thread.exit();
		}
	}
}
