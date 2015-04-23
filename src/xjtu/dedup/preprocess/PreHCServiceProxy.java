package xjtu.dedup.preprocess;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.opendedup.collections.HashtableFullException;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.network.HashClient;
import org.opendedup.sdfs.network.HashClientPool;
import org.opendedup.sdfs.servers.ByteCache;
import org.opendedup.sdfs.servers.HashChunkService;
import org.opendedup.util.SDFSLogger;
import org.opendedup.util.StringUtils;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

public class PreHCServiceProxy {
	public static HashMap<String, HashClientPool> dseServers = new HashMap<String, HashClientPool>();
	public static HashMap<String, HashClientPool> dseRoutes = new HashMap<String, HashClientPool>();
	private static int cacheLenth = 10485760 / Main.CHUNK_LENGTH;
	private static ConcurrentLinkedHashMap<String, ByteCache> readBuffers = new Builder<String, ByteCache>()
			.concurrencyLevel(Main.writeThreads).initialCapacity(cacheLenth)
			.maximumWeightedCapacity(cacheLenth)
			.listener(new EvictionListener<String, ByteCache>() {
				// This method is called just after a new entry has been
				// added
				public void onEviction(String key, ByteCache writeBuffer) {
				}
			}

			).build();

	private static HashMap<String, byte[]> readingBuffers = new HashMap<String, byte[]>();
	// private static LRUMap existingHashes = new
	// LRUMap(Main.systemReadCacheSize);
	private static ReentrantLock readlock = new ReentrantLock();
	public static boolean writeChunk(byte[] hash, byte[] aContents,
			int position, int len, boolean sendChunk) throws IOException,
			HashtableFullException {
		boolean doop = false;
		if (Main.chunkStoreLocal) {
			// doop = HashChunkService.hashExists(hash);
			if (!doop && sendChunk) {
				if(Main.iscdc)
//					doop=HashChunkService.preWriteCDCChunk(hash, aContents, position, len, false);
					doop=PreHashChunkService.preWriteCDCChunk(hash, aContents, position, len, false);
			}
		} else {
			byte[] hashRoute = { hash[0] };
			String db = StringUtils.getHexString(hashRoute);
			HashClient hc = null;
			try {
				hc = getWriteHashClient(db);
				doop = hc.hashExists(hash,(short)0);
				if (!doop && sendChunk) {
					try {
						hc.writeChunk(hash, aContents, 0, len);
					} catch (Exception e) {
						SDFSLogger.getLog().warn("unable to use hashclient", e);
						hc.close();
						hc.openConnection();
						hc.writeChunk(hash, aContents, 0, len);
					}
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				SDFSLogger.getLog().fatal("Unable to write chunk " + hash, e1);
				throw new IOException("Unable to write chunk " + hash);
			} finally {
				if (hc != null)
					returnObject(db, hc);
			}
		}
		return doop;
	}
	private static HashClient getWriteHashClient(String name) throws Exception {
		HashClient hc = (HashClient) dseRoutes.get(name).borrowObject();
		return hc;
	}
	
	private static void returnObject(String name, HashClient hc) throws IOException {
		dseRoutes.get(name).returnObject(hc);
	}
}
