package org.opendedup.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

public class ByteUtils {

	public static byte[] serializeHashMap(HashMap<String, String> map) {
		StringBuffer keys = new StringBuffer();
		Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			keys.append(key);
			if (iter.hasNext())
				keys.append(",");
		}
		StringBuffer values = new StringBuffer();
		iter = map.values().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			values.append(key);
			if (iter.hasNext())
				values.append(",");
		}
		byte[] kb = keys.toString().getBytes();
		byte[] vb = values.toString().getBytes();
		byte[] out = new byte[kb.length + vb.length + 8];
		ByteBuffer buf = ByteBuffer.wrap(out);
		buf.putInt(kb.length);
		buf.put(kb);
		buf.putInt(vb.length);
		buf.put(vb);
		return buf.array();
	}

	public static HashMap<String, String> deSerializeHashMap(byte[] b) {
		ByteBuffer buf = ByteBuffer.wrap(b);
		byte[] kb = new byte[buf.getInt()];
		buf.get(kb);
		byte[] vb = new byte[buf.getInt()];
		buf.get(vb);
		String[] keys = new String(kb).split(",");
		String[] values = new String(vb).split(",");
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < keys.length; i++) {
			map.put(keys[i], values[i]);
		}
		return map;
	}

	public static int bytesToInt(byte[] bytes){
		int length = 4;
		int intValue = 0;
		for (int i = 0; i < length; i++) {
		    int offset = (length-1-i) * 8; //24, 16, 8
		    intValue += (bytes[i] & 0xFF) << offset;//<< 优先级高于&
	    }
		return intValue;
	}
	public static long bytesToLong (byte[] bytes){
		int length = 8;
		long longValue = 0;
		        for (int i = length - 1; i >= 0; i--) {
		         int offset = i * 8; //56, 48, 40, 32, 24, 16, 8
		         longValue |= (long)(bytes[i] & 0xFF) << offset; //一定要先强制转换成long型再移位, 因为0xFF为int型
		        }
		       return longValue;
	}
	public static float bytesToFloat(byte[] bytes) {
		return Float.intBitsToFloat(bytesToInt(bytes));
	}
	public static double bytesToDouble(byte[] bytes) {
		return Double.longBitsToDouble(bytesToLong(bytes));
	}
	public static String bytes2HexString(byte[] b) { 
		String ret = ""; 
		for (int i = 0; i < b.length; i++) { 
		String hex = Integer.toHexString(b[ i ]&0xFF); 
		if (hex.length() == 1) {
		  hex = '0' + hex; 
		  } 
		  ret += hex.toUpperCase(); 
		  } 
		  return ret; 
	} 
}
