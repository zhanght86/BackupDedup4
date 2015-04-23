package xjtu.dedup.preprocess;

import java.io.IOException;
import java.util.ArrayList;


import org.opendedup.util.SDFSLogger;

import org.opendedup.collections.HashtableFullException;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.AbstractChunkStore;
import org.opendedup.sdfs.filestore.DSECompaction;
import org.opendedup.sdfs.filestore.FileChunkStore;
import org.opendedup.sdfs.filestore.HashChunk;
import org.opendedup.sdfs.filestore.HashStore;
import org.opendedup.sdfs.filestore.gc.ChunkStoreGCScheduler;
import org.opendedup.sdfs.network.HashClient;
import org.opendedup.sdfs.network.HashClientPool;
import org.opendedup.sdfs.notification.SDFSEvent;
import org.opendedup.sdfs.servers.HCServer;

public class PreHashChunkService {

	private static double kBytesRead;
	private static double kBytesWrite;
	private static final long KBYTE = 1024L;
	private static long chunksRead;
	private static long chunksWritten;
	private static long chunksFetched;
	private static double kBytesFetched;
	private static int unComittedChunks;
	private static int MAX_UNCOMITTEDCHUNKS = 100;
	private static HashStore hs = null;
	private static AbstractChunkStore fileStore = null;
	private static ChunkStoreGCScheduler csGC = null;
	private static HashClientPool hcPool = null;

	/**
	 * @return the chunksFetched
	 */
	public static long getChunksFetched() {
		return chunksFetched;

	}

	static {
		try {
			hs = new HashStore();
			
			if (!Main.chunkStoreLocal && Main.enableNetworkChunkStore) {
				csGC = new ChunkStoreGCScheduler();
			}
		} catch (Exception e) {
			SDFSLogger.getLog().fatal("unable to start hashstore", e);
			System.exit(-1);
		}
		if (Main.upStreamDSEHostEnabled) {
			try {
				hcPool = new HashClientPool(new HCServer(
						Main.upStreamDSEHostName, Main.upStreamDSEPort, false,
						false), "upstream", 24);
			} catch (IOException e) {
				System.err.println("warning unable to connect to upstream server " + Main.upStreamDSEHostName + ":" +Main.upStreamDSEPort);
				SDFSLogger.getLog().error("warning unable to connect to upstream server " + Main.upStreamDSEHostName + ":" +Main.upStreamDSEPort, e);
				System.err.println("Disabling upstream host " + Main.upStreamDSEHostName + ":" +Main.upStreamDSEPort);
				Main.upStreamDSEHostEnabled = false;
				SDFSLogger.getLog().warn("Disabling upstream host " + Main.upStreamDSEHostName + ":" +Main.upStreamDSEPort);
				//e.printStackTrace();
				//System.exit(-1);
			}
		}
	}

	private static long dupsFound;
	
	// write cdc chunk 
	public static boolean preWriteCDCChunk(byte[] hash, byte[] aContents,
			int position, int len, boolean compressed) throws IOException, HashtableFullException {
		boolean written = hs.preAddHashChunk(new HashChunk(hash, 0, len,
				aContents, compressed));
		if (written) {//添加成功，该块分冗余块
			return false;
		} else {//添加失败，该块为冗余块
			return true;
		}
	}
	public static boolean localHashExists(byte[] hash) throws IOException {
		return hs.hashExists(hash);
	}
	
	/*fetch cdcchunk
	 * @param hash
	 * 			cdcchunk hash
	 * @param chunklen
	 * 			cdchunk length
	 * return HashChunk
	 * */
	public static HashChunk fetchCDCChunk(byte[] hash,int chunklen) throws IOException {
		HashChunk hashChunk = hs.getHashCDCChunk(hash,chunklen);
		byte[] data = hashChunk.getData();
		kBytesFetched = kBytesFetched + (data.length / KBYTE);
		chunksFetched++;
		return hashChunk;
	}
	
	/*fetch sischunk
	 * @param hash
	 * 			SISchunk hash
	 * @param chunklen
	 * 			SIShunk length
	 * return HashChunk
	 * */
	public static HashChunk fetchSISChunk(byte[] hash,int sischunklen) throws IOException {
		HashChunk hashChunk = hs.getHashCDCChunk(hash,sischunklen);// the most different between cdcchunk and sischunk is the length of their.
		byte[] data = hashChunk.getData();
		kBytesFetched = kBytesFetched + (data.length / KBYTE);
		chunksFetched++;
		return hashChunk;
	}
	public static byte getHashRoute(byte[] hash) {
		byte hashRoute = (byte) (hash[1] / (byte) 16);
		if (hashRoute < 0) {
			hashRoute += 1;
			hashRoute *= -1;
		}
		return hashRoute;
	}

	public static void processHashClaims(SDFSEvent evt) throws IOException {
		hs.processHashClaims(evt);
	}

	public static long removeStailHashes(long ms, boolean forceRun,SDFSEvent evt)
			throws IOException {
		return hs.evictChunks(ms, forceRun,evt);
	}

	public static void commitChunks() {
		// H2HashStore.commitTransactions();
		unComittedChunks = 0;
	}

	public static long getSize() {
		return hs.getEntries();
	}

	public static long getFreeBlocks() {
		return hs.getFreeBlocks();
	}

	public static long getMaxSize() {
		return hs.getMaxEntries();
	}

	public static int getPageSize() {
		return Main.chunkStorePageSize;
	}

	public static long getChunksRead() {
		return chunksRead;
	}

	public static long getChunksWritten() {
		return chunksWritten;
	}

	public static double getKBytesRead() {
		return kBytesRead;
	}

	public static double getKBytesWrite() {
		return kBytesWrite;
	}

	public static long getDupsFound() {
		return dupsFound;
	}

	public static void close() {
		if (csGC != null)
			csGC.stopSchedules();
		hs.close();

	}

	public static void init() throws IOException {
		
	}

}

