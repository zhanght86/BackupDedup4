package xjtu.dedup.SWUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.bouncycastle.util.Arrays;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.AbstractChunkStore;
import org.opendedup.sdfs.filestore.FileChunkStore;
import org.opendedup.sdfs.filestore.S3ChunkStore;
import org.opendedup.util.ByteUtils;
import org.opendedup.util.HashFunctions;
import org.opendedup.util.SDFSLogger;
/**
 * 
 * @author Guofeng zhu Chunk block meta data is as follows: [checksum(4 bytes)]// 
 * @param
 *      keyvalue:the checksum of Chunk block.
 * 
 */
public class SWChunkData {
	public static final int RAWDL = 4;
	private int checksum=0;
	public static final int CLAIMED_OFFSET = 1 + 2 + 32 + 8;
	private static byte[] BLANKCM = new byte[RAWDL];
	private boolean mDelete = false;
	private short hashLen = 0;
	private byte[] hash = null;
	private long added = 0;
	private long lastClaimed = 0;
	private long numClaimed = 0;
	private int cLen = 0;
	private long cPos = 0;
	private byte[] chunk = null;
	private static byte[] blankHash = null;;

	public SWChunkData(int key) {
		checksum= key;
	}
	public SWChunkData(byte[] keys){
		checksum=ByteUtils.bytesToInt(keys);
	}
	public int getMetaDataInt() {
		return checksum;
	}
	public ByteBuffer getMetaDataBytes() {
		return ByteBuffer.wrap(intToByteArray(checksum));
	}
   public  byte[] intToByteArray(int value){
		    byte[] b = new byte[4];
		     for (int i = 0; i < 4; i++) {
		            int offset = (b.length - 1 - i) * 8;
		             b[i] = (byte) ((value >>> offset) & 0xFF);
		      }
		      return b;
	}
	public  int bytesToInt(byte[] bytes){
		int length = 4;
		int intValue = 0;
		        for (int i = 0; i < length; i++) {
		         int offset = (length-1-i) * 8; //24, 16, 8
		         intValue += (bytes[i] & 0xFF) << offset;//<< 优先级高于&
		        }
		       return intValue;
	}
}
