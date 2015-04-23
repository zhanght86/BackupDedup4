package xjtu.dedup.fingerprint;
import java.io.*;


public class fingerprint implements Serializable{
	private byte[] hash;
	
	public fingerprint(int hashLength)
	{
		hash=new byte[hashLength];
	}
	public void setHash(byte[] hash)
	{
		this.hash=hash;
	}
	public byte[] getHash()
	{
		return this.hash;
	}
}
