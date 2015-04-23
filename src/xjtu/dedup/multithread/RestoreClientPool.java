package xjtu.dedup.multithread;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendedup.util.SDFSLogger;

import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.restoremngt.RestoreJob;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

public class RestoreClientPool {
	private RestoreClient rc;
	private String restoreJobID;
	private int poolSize;
	private long d;
	private LinkedBlockingQueue<RestoreClient> passiveObjects = null;
	
	private static transient RestoreThreadPool pool = null;
	
	private transient HashMap<Long, RestoreClient> flushingIBackupRestoreClients = new HashMap<Long, RestoreClient>();
	private transient ConcurrentLinkedHashMap<Long, RestoreClient> concurrentjobs=new Builder<Long, RestoreClient>()
	.concurrencyLevel(RestoreJob.client_threads)
	.initialCapacity(101)
	.maximumWeightedCapacity(101)
	.listener(new EvictionListener<Long, RestoreClient>() {
		// This method is called just after a new entry has been
		// added
		public void onEviction(Long key, RestoreClient restoreClient) {

			if (restoreClient != null) {
				try {
					flushingIBackupRestoreClients.put(key, restoreClient);
				} catch (Exception e) {

					// TODO Auto-generated catch block
					SDFSLogger.getLog().error(
							"issue adding for IBackupRestoreClient", e);
				} finally {
				}

				pool.execute(restoreClient);
			}

		}
	}

	).build();
	
	public RestoreClientPool(RestoreClient rc,String restoreJobID, int client_threads) throws IOException{
		this.rc=rc;
		this.restoreJobID=restoreJobID;
		this.poolSize=client_threads;
		passiveObjects=new LinkedBlockingQueue<RestoreClient>(this.poolSize);	
		System.out.println("init the threadpool size");
		pool=new RestoreThreadPool(this.poolSize+1, this.poolSize);
		concurrentjobs.put(d++,this.rc);

		
		
	//	this.populatePool();
	//	rc.dojob();
	}
	
	public synchronized void populatePool() throws IOException{
		for (int i = 0; i < poolSize; i++) {
			try {
				this.passiveObjects.add(this.makeObject());
			} catch (Exception e) {
				SDFSLogger.getLog().error("Unable to get object out of pool ",e);
				throw new IOException(e.toString());

			} finally {
			}
		}
	}

	private RestoreClient makeObject() {
		// TODO Auto-generated method stub
		RestoreClient restoreClient=rc;
	//	restoreClient.dorestorejob();
		return restoreClient;
	}
}
