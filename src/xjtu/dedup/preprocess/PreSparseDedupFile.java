package xjtu.dedup.preprocess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.map.AbstractLinkedMap;
import org.apache.commons.collections.map.LRUMap;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.hashing.AbstractHashEngine;
import org.opendedup.hashing.HashFunctionPool;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.BufferClosedException;
import org.opendedup.sdfs.io.DedupChunk;
import org.opendedup.sdfs.io.FileClosedException;
import org.opendedup.util.SDFSLogger;
public class PreSparseDedupFile {
	private static HashFunctionPool hashPool = new HashFunctionPool(
			Main.writeThreads + 1);
	protected static transient final PreThreadPool pool = new PreThreadPool(
			Main.writeThreads + 1, 8192);
	private long currentPosition = 0;
	private final ReentrantLock writeBufferLock = new ReentrantLock();
	private int maxWriteBuffers = ((Main.maxWriteBuffers * 1024 * 1024) / Main.CHUNK_LENGTH) + 1;
	protected transient ConcurrentHashMap<Long, SimpleWritableCacheBuffer> flushingBuffers = new ConcurrentHashMap<Long, SimpleWritableCacheBuffer>(
			4096, .75f, Main.writeThreads + 1);
	@SuppressWarnings("serial")
	private transient LRUMap writeBuffers = new LRUMap(maxWriteBuffers + 1,
			false) {
		protected boolean removeLRU(AbstractLinkedMap.LinkEntry eldest) {
			if (size() >= maxWriteBuffers) {
				SimpleWritableCacheBuffer swriteBuffer = (SimpleWritableCacheBuffer) eldest
						.getValue();
				if (swriteBuffer != null) {
					try {
						swriteBuffer.flush();
					} catch (Exception e) {
						SDFSLogger.getLog().debug(
								"while closing position "
										+ swriteBuffer.getFilePosition(), e);
					}
				}
			}
			return false;
		}
	};
	
	/**
	 * writes cdcChunk data to the DedupFile
	 * 
	 * @param bbuf
	 *            the bytes to write
	 * @param len
	 *            the length of data to write
	 * @param pos
	 *            the position within the file to write the data to
	 * @param offset
	 *            the offset within the bbuf to start the write from
	 * @param blocknum
	 *            the blocknum of cdcchunk in the DedupFile
	 * @throws java.io.IOException
	 */
	public void writeFile(ByteBuffer buf, int len, int pos, long offset,int blocknum)
			throws java.io.IOException {
		// this.addAio();
		try {
			buf.position(pos);
//			this.writtenTo = true;
			long _cp = offset;
			// ByteBuffer buf = ByteBuffer.wrap(bbuf, pos, len);
			int bytesLeft = len;
			int write = 0;
			while (bytesLeft > 0) {
				// Check to see if we need a new Write buffer
				// WritableCacheBuffer writeBuffer = df.getWriteBuffer(_cp);
				// Find out where to write to in the buffer
				long filePos = _cp;
				int startPos = 0;
				int endPos = startPos + bytesLeft;
					boolean newBuf = true;
					SimpleWritableCacheBuffer writeBuffer = null;
					byte[] b = new byte[len];
					try {
						buf.get(b);
					} catch (java.nio.BufferUnderflowException e) {
						buf.get(b, 0, buf.capacity() - buf.position());
						SDFSLogger.getLog().info(
								"ss buffer underflow writing "
										+ (buf.capacity() - buf.position())
										+ " instead of " + bytesLeft);
					}
					while (writeBuffer == null) {
						try {
							writeBuffer = getCDCWriteBuffer(filePos,len,blocknum,newBuf);
							writeBuffer.write(b, startPos);
						} catch (BufferClosedException e) {
							writeBuffer = null;
							SDFSLogger.getLog().info("trying to write again");
						}
					}
					write = write + bytesLeft;
					_cp = _cp + bytesLeft;
					bytesLeft = 0;
				} 
			this.currentPosition=_cp;
		} catch (Exception e) {
			SDFSLogger.getLog().fatal(
					"error while writing to " 
							+ e.toString(), e);
			throw new IOException("error while writing to " 
					+ " " + e.toString());
		} finally {
			// this.removeAio();
		}
	}
	
	//get cdcchunk buf.
	public SimpleWritableCacheBuffer getCDCWriteBuffer(long position,int buflen,int blocknum, boolean newBuff)
		throws IOException {
		try {
			long chunkPos = position;
			this.writeBufferLock.lock();
			SimpleWritableCacheBuffer writeBuffer = (SimpleWritableCacheBuffer) this.writeBuffers
				.get(chunkPos);
			if (writeBuffer == null) {
				writeBuffer = (SimpleWritableCacheBuffer) this.flushingBuffers
				.get(chunkPos);
			}
			if (writeBuffer == null) {
				writeBuffer = marshalCDCWriteBuffer(chunkPos,buflen,blocknum,newBuff);
			}
			writeBuffer.open();
			return writeBuffer;
		} finally {
			this.writeBufferLock.unlock();

		}
	}
	//序列化CDCwriteBuffer,用来写数据
	private SimpleWritableCacheBuffer marshalCDCWriteBuffer(long chunkPos,int buflen,int blocknum,
			boolean newChunk) throws IOException {

		SimpleWritableCacheBuffer writeBuffer = null;
		DedupChunk ck = null;
		if (newChunk)
			ck = createNewCDCChunk(chunkPos,buflen,blocknum);
		
		if (ck.isNewChunk()) {
			writeBuffer = new SimpleWritableCacheBuffer(ck.getHash(), chunkPos,
					ck.getLength(),this,blocknum, true);
		} else {
			writeBuffer = new SimpleWritableCacheBuffer(ck, this, true);
			writeBuffer.setPrevDoop(ck.isDoop());
		}
		// need to fix this
		return writeBuffer;
	}
	
	private DedupChunk createNewCDCChunk(long location,int cdcChunklen,int blocknum) {
		DedupChunk ck = new DedupChunk(new byte[Main.hashLength], location,
				cdcChunklen,blocknum, true);
		return ck;
	}
	
	public int writeCache() throws IOException, HashtableFullException {
		Object[] buffers = null;
		this.writeBufferLock.lock();
		try {
			if (this.writeBuffers.size() > 0) {
				buffers = this.writeBuffers.values().toArray();
			} else {
				return 0;
			}
		} finally {
			this.writeBufferLock.unlock();
		}
		int z = 0;
		for (int i = 0; i < buffers.length; i++) {
			SimpleWritableCacheBuffer buf = (SimpleWritableCacheBuffer) buffers[i];
			try {
				buf.flush();
			} catch (BufferClosedException e) {
				SDFSLogger.getLog().debug(
						"while closing position " + buf.getFilePosition(), e);
			}
			z++;
		}
		try {
			buffers = this.flushingBuffers.values().toArray();
		} finally {
		}
		z = 0;
		for (int i = 0; i < buffers.length; i++) {
			SimpleWritableCacheBuffer buf = (SimpleWritableCacheBuffer) buffers[i];
			try {
				buf.close();
			} catch (Exception e) {
				SDFSLogger.getLog().debug(
						"while closing position " + buf.getFilePosition(), e);
			}
			z++;
		}
		return z;
	}
	public int dupsFound = 0;
	public ReentrantLock dupsCountLock = new ReentrantLock();
	
	public void writeCache(SimpleWritableCacheBuffer writeBuffer) throws IOException,
	HashtableFullException, FileClosedException {
		if (writeBuffer == null)
			return;
		if (writeBuffer.isDirty()) {
			AbstractHashEngine hc = hashPool.borrowObject();
			byte[] hash = null;
			try {
				hash = hc.getHash(writeBuffer.getFlushedBuffer());
			} catch (Exception e) {
				throw new IOException(e);
			} finally {
				hashPool.returnObject(hc);
			}
			boolean doop = false;
			try {
				doop = PreHCServiceProxy.writeChunk(hash,
						writeBuffer.getFlushedBuffer(),
						writeBuffer.getLength(), writeBuffer.capacity(),
						true);	
				if (doop){
//					dupsCountLock.tryLock();
					dupsFound ++;
//					dupsCountLock.unlock();
					}
			} catch (Exception e) {
				SDFSLogger.getLog().fatal(
						"unable to add chunk [" + writeBuffer.getHash()
							+ "] at position "
							+ writeBuffer.getFilePosition(), e);
			} finally {

		}

		}
	}
	
	protected void putBufferIntoFlush(SimpleWritableCacheBuffer wbuffer) {
		// this.writeBufferLock.lock();
		try {
			this.writeBuffers.remove(wbuffer.getFilePosition());
			this.flushingBuffers.put(wbuffer.getFilePosition(), wbuffer);
		} finally {
			// this.writeBufferLock.unlock();
		}
	}
	
	protected void putBufferIntoWrite(SimpleWritableCacheBuffer wbuffer) {
		// this.writeBufferLock.lock();
		try {
			this.flushingBuffers.remove(wbuffer.getFilePosition());
			this.writeBuffers.put(wbuffer.getFilePosition(), wbuffer);
		} finally {
			// this.writeBufferLock.unlock();
		}
	}

	public int getDupsFound(){
		return dupsFound;
	}
}
