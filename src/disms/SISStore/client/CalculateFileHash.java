package disms.SISStore.client;
import java.io.*;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class CalculateFileHash{
	public static byte[] MD5HashValueFirst(File file) throws NoSuchAlgorithmException, IOException, DigestException{
		try {
		     MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		     FileInputStream fis = new FileInputStream(file);
		     byte[] buffer = new byte[50];
		     fis.read(buffer);
		     //for(int i=0;i<buffer.length;i++)
			 //   System.out.println(buffer[i]);
		     byte[] HashValue = messageDigest.digest(buffer);
		     return HashValue;
		   } 
		catch (IOException e){
			return null; 
		 }		
	}
	public byte[] MD5HashValueGlobal(File file) throws NoSuchAlgorithmException, IOException, DigestException{	
		try {
			 MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		     byte[] buffer = new byte[2000];
		     for(int i = 0;i<100;i++){
		    	 byte[] buf = new byte[20];
		    	 long HalfFileSize = file.length()/100*i;
			     FileInputStream fis = new FileInputStream(file);
			     fis.skip(HalfFileSize);
			     fis.read(buf);
			     System.arraycopy(buf,0,buffer,i*20,20);
		     }
//		     long HalfFileSize = file.length()/3-1024;
//		     FileInputStream fis = new FileInputStream(file);
//		     fis.skip(HalfFileSize);
//		     fis.read(buffer);
		     byte[] HashValue = messageDigest.digest(buffer);
		     return HashValue;
		   } 
		catch (IOException e){
			return null; 
		 }		
	}
	public byte[] MD5HashValueSecond(File file) throws NoSuchAlgorithmException, IOException, DigestException{
		try {
		     MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		     long HalfFileSize = file.length()/2-1024;
		     FileInputStream fis = new FileInputStream(file);
		     byte[] buffer = new byte[2048];
		     fis.skip(HalfFileSize);
		     fis.read(buffer);
		     byte[] HashValue = messageDigest.digest(buffer);
		     return HashValue;
		   } 
		catch (IOException e){
			return null; 
		 }		
	}
	public byte[] MD5HashValueThird(File file) throws NoSuchAlgorithmException, IOException, DigestException{
		try {
		     MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		     long WholeFileSize = file.length()-2048;
		     FileInputStream fis = new FileInputStream(file);
		     byte[] buffer = new byte[2048];
		     fis.skip(WholeFileSize);
		     fis.read(buffer);
		     byte[] HashValue = messageDigest.digest(buffer);
		     return HashValue;
		   } 
		catch (IOException e){
			return null; 
		 }		
	}

	 public static void main(String args[]) throws Exception
	    {
//	    	String path = "E:\\SISServer6.zip";
//	        File ff = new File(path);
//	        CalculateFileHash cfh = new CalculateFileHash();
//	        byte[] ByteTemp = cfh.MD5HashValueGlobal(ff);
//	        for(int i=0;i<ByteTemp.length;i++)
//	        	System.out.println(ByteTemp[i]);
		 	String path = "E:\\3.rar";
		 	File ff = new File(path);
		 	long size = ff.length();
		 	byte[] buffer = MD5HashValueFirst(ff);
		 
		    for(int i=0;i<buffer.length;i++)
		       System.out.println(buffer[i]);
		 	
	    }
	
}