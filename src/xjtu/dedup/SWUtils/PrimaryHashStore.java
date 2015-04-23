package xjtu.dedup.SWUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.opendedup.collections.CSByteArrayLongMap;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.ChunkData;
import org.opendedup.sdfs.filestore.HashChunk;
import org.opendedup.util.HashFunctions;
import org.opendedup.util.SDFSLogger;
import org.opendedup.util.StringUtils;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

public class PrimaryHashStore {
	// A lookup table for the specific hash store based on the first byte of the
	// hash.
	static SWCSByteArrayLongMap bdb = null;
	// the name of the hash store. This is usually associate with the first byte
	// of all possible hashes. There should
	// be 256 total hash stores.
	private String name;
	// Lock for hash queries
	//private ReentrantLock cacheLock = new ReentrantLock();
	int mapSize = (Main.chunkStorePageCache * 1024*1024)/Main.chunkStorePageSize;
	
	private transient HashMap<String, HashChunk> readingBuffers = new HashMap<String, HashChunk>(mapSize);
	private transient ConcurrentLinkedHashMap<String, HashChunk> cacheBuffers = new Builder<String, HashChunk>()
			.concurrencyLevel(Main.writeThreads).initialCapacity(mapSize)
			.maximumWeightedCapacity(mapSize).listener(
					new EvictionListener<String, HashChunk>() {
						// This method is called just after a new entry has been
						// added
						public void onEviction(String key, HashChunk buffer) {
						}
					}

			).build();

	// The chunk store used to store the actual deduped data;
	// private AbstractChunkStore chunkStore = null;
	// Instanciates a FileChunk store that is shared for all instances of
	// hashstores.

	// private static ChunkStoreGCScheduler gcSched = new
	// ChunkStoreGCScheduler();
	private boolean closed = true;
	private static byte[] blankHash = null;
	private static byte[] blankData = null;
	static {
		blankData = new byte[Main.chunkStorePageSize];
		try {
			blankHash = HashFunctions.getTigerHashBytes(blankData);
		} catch (Exception e) {
			SDFSLogger.getLog().fatal("unable to hash blank hash", e);
		}
	}

	/**
	 * Instantiates the TC hash store.
	 * 
	 * @param name
	 *            the name of the hash store.
	 * @throws IOException
	 */
	public PrimaryHashStore() throws IOException {
		this.name = "sdfs";	
		try {
			this.connectDB();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// this.initChunkStore();
	//	SDFSLogger.getLog().info("Cache Size = " +  Main.chunkStorePageSize + " and Dirty Timeout = " + Main.chunkStoreDirtyCacheTimeout);
	//	SDFSLogger.getLog().info("Total Entries " + +bdb.getSize());
	//	SDFSLogger.getLog().info("Added " + this.name);
		this.closed = false;
	}
	
	/**
	 *method used to determine if the hash already exists in the database
	 * 
	 * @param hash
	 *            the md5 or sha hash to lookup
	 * @return returns true if the hash already exists.
	 * @throws IOException
	 */
	public static boolean hashExists(int hash) throws IOException {
		return bdb.containsKey(hash);
	}
	
	/**
	 * The method used to open and connect to the TC database.
	 * 
	 * @throws IOException
	 * @throws HashtableFullException
	 */
	private void connectDB() throws IOException, HashtableFullException {
		File directory = new File(Main.hashDBStore + File.separator);
		if (!directory.exists())
			directory.mkdirs();
		File dbf = new File(directory.getPath() + File.separator + "primaryhashstore-"
				+ this.getName());
		long entries = ((Main.chunkStoreAllocationSize / (long) Main.chunkStorePageSize)) + 8000;
		bdb = new SWCSByteArrayLongMap(entries, dbf
				.getPath());
	}
	
	/**
	 * returns the name of the TCHashStore
	 * 
	 * @return the name of the hash store
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Adds a block of data to the TC hash store.
	 * 
	 * @return true returns true if the data was written. Data will not be
	 *         written if the hash already exists in the db.
	 * @throws IOException
	 * @throws HashtableFullException
	 */
	public static boolean addSWAdler32Hash(int adler32hash) throws IOException,
			HashtableFullException {
		boolean written = false;
		if (!bdb.containsKey(adler32hash)) {
			try {
				if (bdb.put(adler32hash)) {
					written = true;
				} else {
					//written=false;
				}

			} catch (IOException e) {
				SDFSLogger.getLog().fatal(
						"Unable to commit chunk "
								+ adler32hash, e);
				throw e;
			} catch (HashtableFullException e) {
				SDFSLogger.getLog().fatal(
						"Unable to commit chunk "
								+ adler32hash, e);
				throw e;
			}
			finally {
				
			}
		}
		return written;
	}
}
