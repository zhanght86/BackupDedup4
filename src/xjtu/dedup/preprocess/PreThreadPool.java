package xjtu.dedup.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.opendedup.util.PoolThread;
import org.opendedup.util.SDFSLogger;

public class PreThreadPool {
	private ArrayBlockingQueue<SimpleWritableCacheBuffer> taskQueue = null;
	private List<PrePoolThread> threads = new ArrayList<PrePoolThread>();
	private boolean isStopped = false;

	public PreThreadPool(int noOfThreads, int maxNoOfTasks) {
		taskQueue = new ArrayBlockingQueue<SimpleWritableCacheBuffer>(maxNoOfTasks);

		for (int i = 0; i < noOfThreads; i++) {
			threads.add(new PrePoolThread(taskQueue));
		}
		for (PrePoolThread thread : threads) {
			thread.start();
		}
	}

	public void execute(SimpleWritableCacheBuffer task) {
		if (this.isStopped) {
			SDFSLogger.getLog().warn(
					"threadpool is stopped will not execute task");
			return;
		}

		try {
			this.taskQueue.put(task);
		} catch (InterruptedException e) {
			SDFSLogger.getLog().warn("thread interrupted", e);
		}
	}

	public synchronized void stop() {
		this.isStopped = true;
		for (PrePoolThread thread : threads) {
			thread.exit();
		}
	}
}
