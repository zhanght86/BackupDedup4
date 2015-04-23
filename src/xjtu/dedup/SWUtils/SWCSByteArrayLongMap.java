package xjtu.dedup.SWUtils;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.opendedup.collections.AbstractMap;
import org.opendedup.collections.ByteArrayLongMap;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.collections.threads.SyncThread;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.ChunkData;
import org.opendedup.util.NextPrime;
import org.opendedup.util.SDFSLogger;

public class SWCSByteArrayLongMap implements AbstractMap {
	RandomAccessFile kRaf = null;
	FileChannel kFc = null;
	private long size = 0;
	private ReentrantReadWriteLock arlock = new ReentrantReadWriteLock();
	private ReentrantLock iolock = new ReentrantLock();
	private byte[] FREE = new byte[4];
	private byte[] REMOVED = new byte[4];
	private byte[] BLANKCM = new byte[4];
	private long resValue = -1;
	private long freeValue = -1;
	private String fileName;
	private List<SWChunkData> kBuf = Collections
			.synchronizedList(new ArrayList<SWChunkData>());
	private SWByteArrayLongMap[] maps = null;
	private boolean removingChunks = false;
	private String fileParams = "rw";
	private static int freeSlotsLength = 1000000;
	// The amount of memory available for free slots.
	private boolean closed = true;
	long kSz = 0;
	long ram = 0;
	private long maxSz = 0;
	private int hashRoutes = 0;
	// TODO change the kBufMazSize so it not reflective to the pageSize
	private static final int kBufMaxSize = 10485760 / Main.chunkStorePageSize;
	TLongHashSet freeSlots = new TLongHashSet(freeSlotsLength);
	TLongIterator iter = null;
	private boolean firstGCRun = true;
	private long maxPos = 0;
	private ReentrantLock freeSlotLock = new ReentrantLock();

	public SWCSByteArrayLongMap(long maxSize, String fileName)
			throws IOException, HashtableFullException {
		if (Main.compressedIndex)
			maps = new SWByteArrayLongMap[65535];
		else
			maps = new SWByteArrayLongMap[256];
		this.size = (long) (maxSize);
		this.maxSz = maxSize;
		this.fileName = fileName;
		FREE = new byte[4];
		REMOVED = new byte[4];
		Arrays.fill(FREE, (byte) 0);
		Arrays.fill(BLANKCM, (byte) 0);
		Arrays.fill(REMOVED, (byte) 1);
		this.setUp();
		this.closed = false;
		new SyncThread(this);
	}

	public SWCSByteArrayLongMap(long maxSize, String fileName, String fileParams)
			throws IOException, HashtableFullException {
		if (Main.compressedIndex)
			maps = new SWByteArrayLongMap[65535];
		else
			maps = new SWByteArrayLongMap[256];
		this.fileParams = fileParams;
		this.size = (long) (maxSize * 1.125);
		this.maxSz = maxSize;
		this.fileName = fileName;
		FREE = new byte[4];
		REMOVED = new byte[4];
		Arrays.fill(FREE, (byte) 0);
		Arrays.fill(BLANKCM, (byte) 0);
		Arrays.fill(REMOVED, (byte) 1);
		this.setUp();
		this.closed = false;
		new SyncThread(this);
	}
	/**
	 * Searches the set for <tt>obj</tt>
	 * 
	 * @param obj
	 *            an <code>Object</code> value
	 * @return a <code>boolean</code> value
	 * @throws IOException
	 */
	public boolean containsKey(int key) throws IOException {
		if (this.isClosed()) {
			throw new IOException("hashtable [" + this.fileName + "] is close");
		}
		return this.getMap(key).containsKey(key);
	}
	public SWByteArrayLongMap getMap(int hash) throws IOException {
		int hashb = 0;
		hashb = hash%256;
		int hashRoute = 0;
		if(hashb<0){
			hashRoute=-hashb;
		}else{
			hashRoute=hashb;
		}
		SWByteArrayLongMap m = maps[hashRoute];
		if (m == null) {
			iolock.lock();
			arlock.writeLock().lock();
			try {
				m = maps[hashRoute];
				if (m == null) {

					int propsize = (int) (size / maps.length);
					int sz = NextPrime
							.getNextPrimeI((int) (size / maps.length));
					 SDFSLogger.getLog().debug("will create byte array of size "
					 + sz + " propsize was " + propsize);
					ram = ram + (sz * (32 + 8));
					m = new SWByteArrayLongMap(sz, (short) (FREE.length));
					maps[hashRoute] = m;
			//      System.out.println("Creating map at " + hashb +" for total of ");// + this.mapsCreated);
				}
				hashRoutes++;
				 SDFSLogger.getLog().debug("hashroute [" + hashRoute +
				 "] created hr=" + this.hashRoutes);
			} catch (Exception e) {
				SDFSLogger.getLog().fatal(
						"unable to create hashmap. " + maps.length, e);
				throw new IOException(e);
			} finally {
				arlock.writeLock().unlock();
				iolock.unlock();
			}
		}
		return m;
	}
	
	/**
	 * initializes the Object set of this hash table.
	 * 
	 * @param initialCapacity
	 *            an <code>int</code> value
	 * @return an <code>int</code> value
	 * @throws HashtableFullException
	 * @throws FileNotFoundException
	 */
	public long setUp() throws IOException, HashtableFullException {
		File _fs = new File(fileName);
		boolean exists = new File(fileName).exists();
		if (!_fs.getParentFile().exists()) {
			_fs.getParentFile().mkdirs();
		}
		kRaf = new RandomAccessFile(fileName, this.fileParams);
		// kRaf.setLength(ChunkMetaData.RAWDL * size);
		kFc = kRaf.getChannel();
		this.freeSlots.clear();
		long start = System.currentTimeMillis();
		int freeSl = 0;
		if (exists) {
			this.closed = false;
			SDFSLogger.getLog().info(
					"This looks an existing hashtable will repopulate with ["
							+ size + "] entries.");
			SDFSLogger
					.getLog()
					.info("##################### Loading Hash Database #####################");
			kRaf.seek(0);
			int count = 0;
			System.out.print("Loading ");
			while (kFc.position() < kRaf.length()) {
				count++;
				if (count > 500000) {
					count = 0;
					System.out.print("#");
				}

				byte[] raw = new byte[4];		//32位(4 bytes)弱hash校验，粗略判断一个块是否存在
				try {
					long currentPos = kFc.position();
					kRaf.read(raw);
					if (Arrays.equals(raw, BLANKCM)) {
						SDFSLogger
								.getLog()
								.debug("found free slot at "
										+ ((currentPos / raw.length) * Main.chunkStorePageSize));
						this.addFreeSlot((currentPos / raw.length)
								* Main.chunkStorePageSize);
						freeSl++;
					} else {
						SWChunkData cm = new SWChunkData(raw);
						boolean corrupt = false;
						if (!corrupt) {
							boolean foundFree = Arrays.equals(raw,
									FREE);
							boolean foundReserved = Arrays.equals(raw,
									REMOVED);
//							long value = cm.getcPos();
//							if (!cm.ismDelete()) {
//								if (foundFree) {
//									this.freeValue = value;
//									SDFSLogger.getLog().info(
//											"found free  key  with value "
//													+ value);
//								} else if (foundReserved) {
//									this.resValue = value;
//									SDFSLogger.getLog().info(
//											"found reserve  key  with value "
//													+ value);
//								} else {
//									if (cm.getHash().length > 0) {
//										if(cm.getcPos() > this.maxPos)
//											this.maxPos = cm.getcPos() + Main.chunkStorePageSize;
//										boolean added = this.put(cm, false);
//										if (added)
//											this.kSz++;
//									} else {
//										SDFSLogger
//												.getLog()
//												.debug("found free slot at "
//														+ ((currentPos / raw.length) * Main.chunkStorePageSize));
//										this.addFreeSlot((currentPos / raw.length)
//												* Main.chunkStorePageSize);
//										freeSl++;
//									}
//								}
//							}
						}
					}
				} catch (BufferUnderflowException e) {

				}
			}
		}
		System.out.println(" Done Loading ");
		SDFSLogger.getLog().info(
				"########## Finished Loading Hash Database in ["
						+ (System.currentTimeMillis() - start) / 100
						+ "] seconds ###########");
		SDFSLogger.getLog().info(
				"loaded [" + kSz + "] into the hashtable [" + this.fileName
						+ "] free slots available are [" + freeSl + "] free slots added ["
						+ this.freeSlots.size() + "]");
		return size;
	}
	
	private void addFreeSlot(long position) {
		if (this.removingChunks)
			return;
		freeSlotLock.lock();
		try {
		if (this.freeSlots.size() < freeSlotsLength) {
			try {
				if (!this.freeSlots.contains(position)) {
					this.freeSlots.add(position);
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				
			}
		}
		}catch(Exception e) {
			
		}finally {
			this.freeSlotLock.unlock();
		}
	}
	

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return this.closed;
	}

	@Override
	public void vanish() throws IOException {
		// TODO Auto-generated method stub
		
	}

	public synchronized void sync() throws IOException {
		if (this.isClosed()) {
			throw new IOException("hashtable [" + this.fileName + "] is close");
		}
		this.flushBuffer(true);
		this.kRaf.getFD().sync();
	}
	private synchronized void flushBuffer(boolean lock) throws IOException {
		List<SWChunkData> oldkBuf = null;
		try {
			if (lock)
				this.arlock.writeLock().lock();
			oldkBuf = kBuf;
			if (this.isClosed())
				kBuf = null;
			else {
				kBuf = Collections.synchronizedList(new ArrayList<SWChunkData>());
			}
		} catch (Exception e) {
		} finally {
			if (lock)
				this.arlock.writeLock().unlock();
		}
		Iterator<SWChunkData> iter = oldkBuf.iterator();
		while (iter.hasNext()) {
			SWChunkData cm = iter.next();
			if (cm != null) {
				long pos = ((long)cm.getMetaDataInt() / (long) Main.chunkStorePageSize)
				* (long) SWChunkData.RAWDL;
				this.iolock.lock();
				try {
					kFc.position(pos);
					kFc.write(cm.getMetaDataBytes());
				} catch (Exception e) {
				} finally {
					cm = null;
					this.iolock.unlock();
				}
				cm = null;
			}
		}
		oldkBuf.clear();
		oldkBuf = null;

	}

	public boolean put(int key) throws IOException, HashtableFullException {
		if (this.kSz >= this.maxSz)
			throw new IOException(
					"entries is greater than or equal to the maximum number of entries. You need to expand"
							+ "the volume or DSE allocation size");
		if (this.isClosed()) {
			throw new IOException("hashtable [" + this.fileName + "] is close");
		}
		boolean added = false;
		this.kBuf.add(new SWChunkData(key));
		added = this.put(key, true);

		return added;
	}
	private boolean put(int key, boolean persist) throws IOException,HashtableFullException {
		if (this.isClosed())
			throw new HashtableFullException("Hashtable " + this.fileName
					+ " is close");
		if (kSz >= this.maxSz)
			throw new HashtableFullException("maximum sized reached");
		boolean added = false;
		if (persist) {
			added = this.getMap(key).put(key);
		} else {
			added = this.getMap(key).put(key);
		}

		return added;
	}
	
	public void close() {
		this.closed = true;
		try {
			this.flushBuffer(true);
			this.kRaf.getFD().sync();
			this.kRaf.close();
			this.kRaf = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Hashtable [" + this.fileName + "] closed");
	}

}
