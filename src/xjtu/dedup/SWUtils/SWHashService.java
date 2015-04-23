package xjtu.dedup.SWUtils;

import java.io.IOException;

import org.opendedup.sdfs.filestore.HashStore;
import org.opendedup.sdfs.filestore.gc.ChunkStoreGCScheduler;
import org.opendedup.util.SDFSLogger;

public class SWHashService {
	private static double kBytesRead;
	private static double kBytesWrite;
	private static final long KBYTE = 1024L;
	private static long chunksRead;
	private static long chunksWritten;
	private static long chunksFetched;
	private static double kBytesFetched;
	private static int unComittedChunks;
	private static int MAX_UNCOMITTEDCHUNKS = 100;
	private static PrimaryHashStore phs = null;
	
	private static ChunkStoreGCScheduler csGC = null;
	
	static {
		try {
			phs = new PrimaryHashStore();
//			csGC = new ChunkStoreGCScheduler();
		} catch (Exception e) {
			SDFSLogger.getLog().fatal( "unable to start hashstore", e);
			System.exit(-1);
		}
	}
	
	public static boolean hashExists(int hash) throws IOException {
		return phs.hashExists(hash);
	}
	public static void init() {

	}
	
}
