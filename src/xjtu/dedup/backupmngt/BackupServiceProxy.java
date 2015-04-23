package xjtu.dedup.backupmngt;

import java.util.HashMap;

import org.opendedup.sdfs.network.HashClientPool;

import xjtu.dedup.multithread.BackupClient;
import xjtu.dedup.multithread.BackupClientPool;
import xjtu.dedup.multithread.RestoreClient;
import xjtu.dedup.multithread.RestoreClientPool;

public class BackupServiceProxy {
//	public static HashMap<String, BackupClientPool> backupclients = new HashMap<String, BackupClientPool>();
	public static HashMap<String, BackupClient> backupclients = new HashMap<String, BackupClient>();
//	public static HashMap<String, RestoreClientPool> restoreclients = new HashMap<String, RestoreClientPool>();
	public static HashMap<String, RestoreClient> restoreclients = new HashMap<String, RestoreClient>();
}
