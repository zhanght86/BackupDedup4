package org.opendedup.sdfs.io;

import java.io.IOException;

import java.nio.ByteBuffer;

import org.opendedup.hashing.HashFunctionPool;
import org.opendedup.sdfs.Main;

public class SparseDataChunk {

	private boolean doop;
	private byte[] hash;
	private boolean localData = false;
	private long timeAdded = 0;
	public static final int RAWDL = 1 + HashFunctionPool.hashLength + 1 + 8;
	private int chunklen=0;// cdcchunk length
	public static final int CDCRAWDL=1 + HashFunctionPool.hashLength+  1 + 8 + 4;
//	public static final int CDCRAWDL=1+Main.hashLength+1+8+4;

	public SparseDataChunk(byte[] rawData) throws IOException {
		if (rawData.length != RAWDL)
			throw new IOException(
					"possible data corruption: byte array length "
							+ rawData.length + " does not equal " + RAWDL);
		ByteBuffer buf = ByteBuffer.wrap(rawData);
		byte b = buf.get();
		if (b == 0)
			doop = false;
		else
			doop = true;
		hash = new byte[HashFunctionPool.hashLength];
		buf.get(hash);
		b = buf.get();
		if (b == 0)
			this.localData = false;
		else
			this.localData = true;
		this.timeAdded = buf.getLong();
	}
	public SparseDataChunk(byte[] rawData,boolean iscdc) throws IOException {
		if (rawData.length != CDCRAWDL)
			throw new IOException(
					"possible data corruption: byte array length "
							+ rawData.length + " does not equal "
							+ CDCRAWDL);
		ByteBuffer buf = ByteBuffer.wrap(rawData);
		byte b = buf.get();
		if (b == 0)
			doop = false;
		else
			doop = true;
		hash = new byte[HashFunctionPool.hashLength];
		buf.get(hash);
		b = buf.get();
		if (b == 0)
			this.localData = false;
		else
			this.localData = true;
		this.timeAdded = buf.getLong();
		this.chunklen=buf.getInt();
	}
	public SparseDataChunk(boolean doop, byte[] hash, boolean localData,
			long timeAdded) {
		this.doop = doop;
		this.hash = hash;
		this.localData = localData;
		this.timeAdded = timeAdded;
	}

	public SparseDataChunk(boolean doop, byte[] hash, boolean localData,
			long timeAdded,int length) {
		this.doop = doop;
		this.hash = hash;
		this.localData = localData;
		this.timeAdded = timeAdded;
		this.chunklen=length;
	}
	
	public boolean isDoop() {
		return doop;
	}

	public byte[] getHash() {
		return hash;
	}

	public int getChunkLen(){
		return chunklen;
	}
	
	public byte[] getBytes() {
		ByteBuffer buf ;
		if(Main.iscdc)
			 buf= ByteBuffer.wrap(new byte[CDCRAWDL]);
		else
			 buf= ByteBuffer.wrap(new byte[RAWDL]);
		if (doop)
			buf.put((byte) 1);
		else
			buf.put((byte) 0);
		buf.put(hash);
		if (localData)
			buf.put((byte) 1);
		else
			buf.put((byte) 0);
		buf.putLong(this.timeAdded);
		if(Main.iscdc)
			buf.putInt(this.chunklen);
		return buf.array();
	}

	public boolean isLocalData() {
		return localData;
	}

	public void setLocalData(boolean local) {
		this.localData = local;
	}

	public long getTimeAdded() {
		return timeAdded;
	}

}
