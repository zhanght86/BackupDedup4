package xjtu.dedup.multithread;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.WritableCacheBuffer;
import org.opendedup.util.SDFSLogger;
import org.opendedup.util.ThreadPool;

import xjtu.dedup.backupmngt.BackupJob;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

public class BackupClientPool {
private BackupClient bc;
private String  backupjobid;
private int poolSize;
private long c;
private LinkedBlockingQueue<BackupClient> passiveObjects = null;
private static transient BackupRestoreThreadPool pool = null;
private Vector vector;

private transient HashMap<Long, BackupClient> flushingIBackupRestoreClients = new HashMap<Long, BackupClient>();

/*
 private transient ConcurrentLinkedHashMap<Long, BackupClient> concurrentjobs=new Builder<Long, BackupClient>()
.concurrencyLevel(BackupJob.client_threads)
.initialCapacity(101)
.maximumWeightedCapacity(101)
.listener(new EvictionListener<Long, BackupClient>() {
	// This method is called just after a new entry has been
	// added
	public void onEviction(Long key, BackupClient backupClient) {

		if (backupClient != null) {
			try {
				flushingIBackupRestoreClients.put(key, backupClient);
			} catch (Exception e) {

				// TODO Auto-generated catch block
				SDFSLogger.getLog().error(
						"issue adding for IBackupRestoreClient", e);
			} finally {
			}

			pool.execute(backupClient);
		}

	}
}

).build();

*/
	public BackupClientPool(BackupClient bc,String backupjobid, int client_threads) throws IOException, SQLException{
		this.backupjobid=backupjobid;
		this.bc=bc;
		this.poolSize=client_threads;
		passiveObjects=new LinkedBlockingQueue<BackupClient>(this.poolSize);
		passiveObjects.add(bc);
		System.out.println("init the thread pool size!");
		pool=new BackupRestoreThreadPool(this.poolSize+1, this.poolSize);
		pool.add(bc);
		pool.execute();
		System.out.println("Backup ********************");
	}
	
	public void populatePool()throws IOException{
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
	
	public BackupClient makeObject() throws SQLException{
		BackupClient backupClient=bc;
	//	IBackupRestoreClient.dobackupjob();
		return backupClient;
	}
	
	public void runBackupJobPool(){
	//	pool.execute(this.bc);
	}
	
}
