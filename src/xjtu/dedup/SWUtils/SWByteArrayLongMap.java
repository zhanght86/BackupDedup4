package xjtu.dedup.SWUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.opendedup.sdfs.Main;
import org.opendedup.util.SDFSLogger;

import com.ning.compress.lzf.LZFEncoder;

public class SWByteArrayLongMap {
	ByteBuffer values = null;
	ByteBuffer claims = null;
	ByteBuffer keys = null;
	byte[] compValues = null;
	byte[] compClaims = null;
	byte[] compKeys = null;
	private int size = 0;
	private int entries = 0;
	
	private ReentrantReadWriteLock hashlock = new ReentrantReadWriteLock();
	public static byte[] FREE = new byte[4];
	public static byte[] REMOVED = new byte[4];
	private int iterPos = 0;

	static {
		FREE = new byte[4];
		REMOVED = new byte[4];
		Arrays.fill(FREE, (byte) 0);
		Arrays.fill(REMOVED, (byte) 1);
	}

	public SWByteArrayLongMap(int size, short arraySize) throws IOException {
		this.size = size;
		this.setUp();
	}
	/**
	 * initializes the Object set of this hash table.
	 * 
	 * @param initialCapacity
	 *            an <code>int</code> value
	 * @return an <code>int</code> value
	 * @throws IOException
	 */
	public int setUp() throws IOException {
		int kSz = 0;
		keys = ByteBuffer.allocate(size * FREE.length);
		values = ByteBuffer.allocate(size * 8);
		claims = ByteBuffer.allocate(size);

		for (int i = 0; i < size; i++) {
			keys.put(FREE);
			values.putLong(-1);
			claims.put((byte) 0);
			kSz++;
		}
		return size;
	}
	/**
	 * Searches the set for <tt>obj</tt>
	 * 
	 * @param obj
	 *            an <code>Object</code> value
	 * @return a <code>boolean</code> value
	 */
	public boolean containsKey(int key) {
		try {
			this.hashlock.readLock().lock();
			int index = index(key);
			if (index >= 0) {
				int pos = (index / FREE.length);
				this.claims.position(pos);
				this.claims.put((byte) 1);
				return true;
			}
			return false;
		} catch (Exception e) {
			SDFSLogger.getLog().fatal("error getting record", e);
			return false;
		} finally {
			this.hashlock.readLock().unlock();
		}
	}
	/**
	 * Locates the index of <tt>obj</tt>.
	 * 
	 * @param obj
	 *            an <code>Object</code> value
	 * @return the index of <tt>obj</tt> or -1 if it isn't in the set.
	 */
	protected int index(int hashvalue) {
		int hash=hashvalue& 0x7fffffff;
		int index = this.hashFunc1(hash) * FREE.length;////获取一个整型数值（正整数），用来定位一条数据存储
		// int stepSize = hashFunc2(hash);
		byte[] cur = new byte[4];
		keys.position(index);
		keys.get(cur);
		int storehash=(cur[0] << 24)+ ((cur[1] & 0xFF) << 16)+ ((cur[2] & 0xFF) << 8)+ (cur[3] & 0xFF);
		if (storehash==hash) {
			return index;
		}
		if (Arrays.equals(cur, FREE)) {
			return -1;
		}

		// NOTE: here it has to be REMOVED or FULL (some user-given value)
		if (Arrays.equals(cur, REMOVED) || storehash!=hash) {
			// see Knuth, p. 529
			final int probe = (1 + (hash % (size - 2))) * FREE.length;
			int z = 0;
			do {
				z++;
				index += (probe); // add the step
				index %= (size * FREE.length); // for wraparound
				cur = new byte[FREE.length];
				keys.position(index);
				keys.get(cur);
				if (z > size) {
					SDFSLogger.getLog().info(
							"entries exhaused size=" + this.size + " entries="
									+ this.entries);
					return -1;
				}
			} while (!Arrays.equals(cur, FREE)
					&& (Arrays.equals(cur, REMOVED) || storehash!=hash));
		}

		return storehash!=hash ? -1 : index;
	}
	  public  byte[] intToByteArray(int value){
		    byte[] b = new byte[4];
		     for (int i = 0; i < 4; i++) {
		            int offset = (b.length - 1 - i) * 8;
		             b[i] = (byte) ((value >>> offset) & 0xFF);
		      }
		      return b;
	}
	public boolean put(int key) {
		try {
			this.hashlock.writeLock().lock();
			if (entries >= size)
				throw new IOException(
						"entries is greater than or equal to the maximum number of entries. You need to expand "
								+ "the volume or DSE allocation size");
			int pos = this.insertionIndex(key);
			if (pos < 0)
				return false;
			try {
			this.keys.position(pos);
			this.keys.put(intToByteArray(key));
			this.entries = entries + 1;
			return pos > -1 ? true : false;
			}catch(Exception e) {
				throw e;
			}
		} catch (Exception e) {
			SDFSLogger.getLog().fatal("error inserting record", e);
			return false;
		} finally {
			this.hashlock.writeLock().unlock();
		}
	}
	/**
	 * Locates the index at which <tt>obj</tt> can be inserted. if there is
	 * already a value equal()ing <tt>obj</tt> in the set, returns that value's
	 * index as <tt>-index - 1</tt>.
	 * 
	 * @param obj
	 *            an <code>Object</code> value
	 * @return the index of a FREE slot at which obj can be inserted or, if obj
	 *         is already stored in the hash, the negative value of that index,
	 *         minus 1: -index -1.
	 */
	protected int insertionIndex(int key) {
		int hash = key & 0x7fffffff;
		int index = this.hashFunc1(hash) * FREE.length;
		// int stepSize = hashFunc2(hash);
		byte[] cur = new byte[4];
		keys.position(index);
		keys.get(cur);
        int storehash=(cur[0] << 24)+ ((cur[1] & 0xFF) << 16)+ ((cur[2] & 0xFF) << 8)+ (cur[3] & 0xFF);

		if (Arrays.equals(cur, FREE)) {
			return index; // empty, all done
		} else if (storehash==key) {
			return -index - 1; // already stored
		} else { // already FULL or REMOVED, must probe
			// compute the double hash
			final int probe = (1 + (hash % (size - 2))) * FREE.length;

			// if the slot we landed on is FULL (but not removed), probe
			// until we find an empty slot, a REMOVED slot, or an element
			// equal to the one we are trying to insert.
			// finding an empty slot means that the value is not present
			// and that we should use that slot as the insertion point;
			// finding a REMOVED slot means that we need to keep searching,
			// however we want to remember the offset of that REMOVED slot
			// so we can reuse it in case a "new" insertion (i.e. not an update)
			// is possible.
			// finding a matching value means that we've found that our desired
			// key is already in the table
			if (!Arrays.equals(cur, REMOVED)) {
				// starting at the natural offset, probe until we find an
				// offset that isn't full.
				do {
					index += (probe); // add the step
					index %= (size * FREE.length); // for wraparound
					cur = new byte[FREE.length];
					keys.position(index);
					keys.get(cur);
				} while (!Arrays.equals(cur, FREE)
						&& !Arrays.equals(cur, REMOVED)
						&& storehash!=hash);
			}

			// if the index we found was removed: continue probing until we
			// locate a free location or an element which equal()s the
			// one we have.
			if (Arrays.equals(cur, REMOVED)) {
				int firstRemoved = index;
				while (!Arrays.equals(cur, FREE)
						&& (Arrays.equals(cur, REMOVED) || storehash!=hash)) {
					index += (probe); // add the step
					index %= (size * FREE.length); // for wraparound
					cur = new byte[FREE.length];
					keys.position(index);
					keys.get(cur);
				}
				// NOTE: cur cannot == REMOVED in this block
				return (!Arrays.equals(cur, FREE)) ? -index - 1 : firstRemoved;
			}
			// if it's full, the key is already stored
			// NOTE: cur cannot equal REMOVE here (would have retuned already
			// (see above)
			return storehash!=hash ? -index - 1 : index;
		}
	}
	
	private int hashFunc1(int hash) {
		return hash % size;
	}

	public int hashFunc3(int hash) {
		int result = hash + 1;
		return result;
	}
	public void flush(){
		
	}
}
