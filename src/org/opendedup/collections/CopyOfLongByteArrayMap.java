package org.opendedup.collections;

import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import org.opendedup.collections.threads.SyncThread;
import org.opendedup.sdfs.Main;
import org.opendedup.util.NextPrime;
import org.opendedup.util.SDFSLogger;

public class CopyOfLongByteArrayMap implements AbstractMap {

	// RandomAccessFile bdbf = null;
	int arrayLength = 0;
	String filePath = null;
	private ReentrantLock hashlock = new ReentrantLock();
	private boolean closed = true;
	public byte[] FREE = new byte[16];
	public int iterPos = 0;
	// private int maxReadBufferSize = Integer.MAX_VALUE;
	// private int eI = 1024 * 1024;
	// private long endPos = maxReadBufferSize;
	File dbFile = null;
	Path bdbf = null;
	TLongHashSet locks = null;

	// private boolean smallMemory = false;
	public CopyOfLongByteArrayMap(int arrayLength, String filePath)
			throws IOException {
		if (Runtime.getRuntime().maxMemory() < 1610612736) {
			SDFSLogger.getLog().debug("Preparing for smaller memory footprint");
			// smallMemory = true;
			// this.maxReadBufferSize = 50 * 1024 * 1024;
			// endPos = maxReadBufferSize;
		}
		this.arrayLength = arrayLength;
		this.filePath = filePath;
		FREE = new byte[arrayLength];
		Arrays.fill(FREE, (byte) 0);
		this.openFile();
		new SyncThread(this);
		try {
			locks = new TLongHashSet(NextPrime.getNextPrimeI(2048));
		}catch(Exception e) {
			SDFSLogger.getLog().error(e);
		}
	}

	public CopyOfLongByteArrayMap(int arrayLength, String filePath, String fileParams)
			throws IOException {
		if (Runtime.getRuntime().maxMemory() < 1610612736) {
			SDFSLogger.getLog().debug("Preparing for smaller memory footprint");
			// smallMemory = true;
			// this.maxReadBufferSize = 50 * 1024 * 1024;
			// endPos = maxReadBufferSize;
		}
		this.arrayLength = arrayLength;
		this.filePath = filePath;
		FREE = new byte[arrayLength];
		Arrays.fill(FREE, (byte) 0);
		this.openFile();
		new SyncThread(this);
		try {
			locks = new TLongHashSet(NextPrime.getNextPrimeI(2048));
		}catch(Exception e) {
			SDFSLogger.getLog().error(e);
		}
	}

	public void iterInit() {
		this.iterPos = 0;
	}

	public long nextKey() throws IOException {
		File f = new File(this.filePath);
		long pos = (long) iterPos * (long) Main.CHUNK_LENGTH;
		long fLen = ((f.length() * (long) Main.CHUNK_LENGTH) / arrayLength);
		if (iterPos == 0)
			SDFSLogger.getLog().info("fLen = " + fLen);
		while (pos <= fLen) {
			try {
				try {
					this.hashlock.lock();
					pos = (long) iterPos * (long) Main.CHUNK_LENGTH;
					iterPos++;
				} catch (Exception e1) {
				} finally {
					this.hashlock.unlock();
				}
				byte[] b = this.get(pos);
				if (b != null)
					return pos;

			} catch (Exception e) {

			} finally {

			}

		}
		if (pos == fLen)
			SDFSLogger.getLog().info("length end " + pos);

		return -1;
	}

	public byte[] nextValue() throws IOException {
		long pos = iterPos * this.arrayLength;
		byte[] val = null;
		File f = new File(this.filePath);
		while (pos < f.length()) {
			val = new byte[this.arrayLength];
			try {
				try {
					this.hashlock.lock();
					pos = (long) iterPos * (long) Main.CHUNK_LENGTH;
					iterPos++;
				} catch (Exception e1) {
				} finally {
					this.hashlock.unlock();
				}
				val = this.get(pos);
				if (val != null)
					return val;
			} catch (Exception e) {
			} finally {
			}
		}
		return null;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.annesam.collections.AbstractMap#isClosed()
	 */
	public boolean isClosed() {
		return this.closed;
	}

	private void openFile() throws IOException {
		if (this.closed) {
			this.hashlock.lock();
			bdbf = Paths.get(filePath);
			try {
				dbFile = new File(filePath);
				boolean fileExists = dbFile.exists();
				if (!dbFile.getParentFile().exists()) {
					dbFile.getParentFile().mkdirs();
				}
				SDFSLogger.getLog().debug("opening [" + this.filePath + "]");

				if (!fileExists) {
					FileChannel bdb = (FileChannel) Files.newByteChannel(bdbf,
							StandardOpenOption.CREATE,
							StandardOpenOption.WRITE, StandardOpenOption.READ,
							StandardOpenOption.SPARSE);
					bdb.position(1024);
					bdb.close();
				}
				// initiall allocate 32k
				this.closed = false;
			} catch (IOException e) {
				SDFSLogger.getLog().error("unable to open file " + filePath);
				throw e;
			} catch (Exception e) {
				throw new IOException(e);
			} finally {
				this.hashlock.unlock();
			}
		}
	}

	private long calcMapFilePos(long fpos) throws IOException {
		long pos = (fpos / Main.CHUNK_LENGTH) * FREE.length;
		/*
		 * if (pos > Integer.MAX_VALUE) throw new IOException(
		 * "Requested file position " + fpos +
		 * " is larger than the maximum length of a file for this file system "
		 * + (Integer.MAX_VALUE * Main.CHUNK_LENGTH) / FREE.length);
		 */
		return pos;
	}

	private long getMapFilePosition(long pos) throws IOException {
		long propLen = this.calcMapFilePos(pos);
		return propLen;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.annesam.collections.AbstractMap#put(long, byte[])
	 */
	public void put(long pos, byte[] data) throws IOException {
		if (this.isClosed()) {
			throw new IOException("hashtable [" + this.filePath + "] is close");
		}
		if (data.length != this.arrayLength)
			throw new IOException("data length " + data.length
					+ " does not equal " + this.arrayLength);
		long fpos = 0;
		FileChannel _bdb = null;
		try {
			fpos = this.getMapFilePosition(pos);
			_bdb = (FileChannel) Files.newByteChannel(bdbf,StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.READ,
					StandardOpenOption.SPARSE);
			this.hashlock.lock();
			_bdb.write(ByteBuffer.wrap(data), fpos);
		} catch (BufferOverflowException e) {
			SDFSLogger.getLog().fatal(
					"trying to write at " + fpos + " but file length is "
							+ dbFile.length());
			throw e;
		} catch (Exception e) {
			// System.exit(-1);
			throw new IOException(e);
		} finally {
			try {
				this.hashlock.unlock();
			}catch(Exception e) {}
			try {
				_bdb.close();
			} catch (Exception e) {
			}
		}
	}

	public void truncate(long length) throws IOException {
		this.hashlock.lock();
		long fpos = 0;
		FileChannel _bdb = null;
		try {
			fpos = this.getMapFilePosition(length);
			_bdb = (FileChannel) Files.newByteChannel(bdbf,StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.READ,
					StandardOpenOption.SPARSE);
			_bdb.truncate(fpos);
		} catch (Exception e) {
			// System.exit(-1);
			throw new IOException(e);
		} finally {
			try {
				_bdb.close();
			} catch (Exception e) {
			}
		}
		this.hashlock.unlock();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.annesam.collections.AbstractMap#remove(long)
	 */
	public void remove(long pos) throws IOException {
		if (this.isClosed()) {
			throw new IOException("hashtable [" + this.filePath + "] is close");
		}

		this.hashlock.lock();
		long fpos = 0;
		FileChannel _bdb = null;
		try {
			fpos = this.getMapFilePosition(pos);
			_bdb = (FileChannel) Files.newByteChannel(bdbf,StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.READ,
					StandardOpenOption.SPARSE);
			_bdb.write(ByteBuffer.wrap(FREE), fpos);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			try {
				_bdb.close();
			} catch (Exception e) {
			}
			this.hashlock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.annesam.collections.AbstractMap#get(long)
	 */
	public byte[] get(long pos) throws IOException {
		if (this.isClosed()) {
			throw new IOException("hashtable [" + this.filePath + "] is close");
		}
		this.hashlock.lock();
		this.hashlock.unlock();
		long fpos = 0;
		FileChannel _bdb = null;
		try {
			fpos = this.getMapFilePosition(pos);
			_bdb = (FileChannel) Files.newByteChannel(bdbf,StandardOpenOption.WRITE,
					StandardOpenOption.READ, StandardOpenOption.SPARSE);
			ByteBuffer buf = ByteBuffer.wrap(new byte[this.arrayLength]);
			_bdb.read(buf, fpos);
			byte[] b = buf.array();
			if (Arrays.equals(b, this.FREE))
				return null;
			return b;
		} catch (BufferUnderflowException e) {
			return null;
		} catch (Exception e) {
			SDFSLogger.getLog().fatal(
					"error getting data at " + fpos + " buffer capacity="
							+ dbFile.length(), e);
			throw new IOException(e);
		} finally {
			try {
				_bdb.close();
			} catch (Exception e) {
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.annesam.collections.AbstractMap#sync()
	 */
	public void sync() throws IOException {
		FileChannel _bdb = null;
		try {
			_bdb = (FileChannel) Files.newByteChannel(bdbf,StandardOpenOption.WRITE,
					StandardOpenOption.READ, StandardOpenOption.SPARSE);
			_bdb.force(true);
		} catch (IOException e) {

		} finally {
			try {
				_bdb.close();
			} catch (Exception e) {
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.annesam.collections.AbstractMap#vanish()
	 */
	public void vanish() throws IOException {
		this.hashlock.lock();
		try {
			if (!this.isClosed())
				this.close();
			File f = new File(this.filePath);
			f.delete();
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			this.hashlock.unlock();
		}
	}

	public void copy(String destFilePath) throws IOException {
		this.hashlock.lock();
		FileChannel srcC = null;
		FileChannel dstC = null;
		try {
			this.sync();
			File dest = new File(destFilePath);
			File src = new File(this.filePath);
			if (dest.exists())
				dest.delete();
			else
				dest.getParentFile().mkdirs();
			srcC = (FileChannel) Files.newByteChannel(
					Paths.get(src.getPath()), StandardOpenOption.READ,
					StandardOpenOption.SPARSE);
			dstC = (FileChannel) Files.newByteChannel(
					Paths.get(dest.getPath()), StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.SPARSE);
			srcC.transferTo(0, src.length(), dstC);
			srcC.transferTo(0, src.length(), dstC);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			try {
				srcC.close();
			} catch (Exception e) {
			}
			try {
				dstC.close();
			} catch (Exception e) {
			}
			this.hashlock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.annesam.collections.AbstractMap#close()
	 */
	public void close() {
		if (!this.isClosed()) {
			this.closed = true;
		}
	}
}
