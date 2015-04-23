package disms.SISStore.client2;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class Chunking {
	public static void printHexString( byte[] b) {  
		   for (int i = 0; i < b.length; i++) { 
		     String hex = Integer.toHexString(b[i] & 0xFF); 
		     if (hex.length() == 1) { 
		       hex = '0' + hex; 
		     } 
		     System.out.print(hex.toUpperCase() ); 
		   } 

		}
    public static void readfile(String file) throws IOException{
    	File f = new File(file);
        long size = f.length();
        Vector<String> pos = new Vector<String>();
        Vector<String> len = new Vector<String>();
        FileInputStream fis = new FileInputStream(file);        		 
    	byte[] buffer = new byte[1024*1024];
    	long temp = 0;
    	long i = 0;
    	int j = 0;
//    	for(long i = 0;i<10;i++){
//    		fis.read(buffer);
//    		printHexString(buffer);
//    		System.out.println();
//    	}
    	System.out.println(size);
    	System.out.println(System.currentTimeMillis());
        for(i = 0;i<size;i=i+1024*1024){
    		fis.read(buffer);
    		for (j = 0; j < 1024*1024-1; j++) { 
//	   		    String hex1 = Integer.toHexString(buffer[j] & 0xFF);
//			    String hex2 = Integer.toHexString(buffer[j+1] & 0xFF); 
//			    if (hex1.length() == 1) { 
//			    	hex1 = '0' + hex1; 
//	    		}
//			    if (hex2.length() == 1) { 
//			    	hex2 = '0' + hex2; 
//	    		}
//			    String hex = hex1.toUpperCase()+hex2.toUpperCase();
    			
	    		if((i+j<=size)&&(buffer[j]==-34)&&(buffer[j+1]==67)){
//	    			if((i+j-temp>64*1024)){
		    			len.addElement(String.valueOf((i+j-temp)));	    			
		    	        pos.addElement(String.valueOf(i+j));
		    	        temp = i+j;
//	    			}
	    		}	    		
    		}
        }
    	System.out.println(System.currentTimeMillis());
        len.addElement(String.valueOf((size-temp)));
        int ss[] = new int[len.size()];
        System.out.println(len.size());
    	System.out.println("块大小");
        for(int c = 0;c<len.size();c++){
//        	System.out.println(len.elementAt(c));
        	ss[c]=Integer.valueOf(len.elementAt(c));
        	System.out.println(ss[c]);
        }
//        for(int d = 0;d<pos.size();d++){
//        	System.out.println(pos.elementAt(d));        	
//        }
    	long avesize = size/len.size();
    	System.out.println("平均块大小");
    	System.out.println(avesize);
    	long pow = 0;
    	for(int e = 0;e<ss.length;e++)
    		pow += (int)Math.pow((avesize-ss[e]), 2);
//    	System.out.println(pow);
    	pow=pow/(ss.length-1);
    	System.out.println("块标准差");
    	System.out.println((int)(Math.sqrt(pow)));
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
//    		     if(j%16==0){
//     				System.out.println();
//     				System.out.print(Long.toHexString(j/16)+":");
//     			 }
//    		     System.out.print(hex.toUpperCase()+' '); 
//    		String hex = Integer.toHexString(buffer[0] & 0xFF); 
//    		if (hex.length() == 1) { 
//    			hex = '0' + hex; 
//    		}
//    		if(hex.equalsIgnoreCase("AC")){  
    			
//        		fis.read(buffer);
//        		hex = Integer.toHexString(buffer[0] & 0xFF);
//        		if(hex.equalsIgnoreCase("ED")){
//                	System.out.print(i);	
//        		pos.addElement(String.valueOf(i));
//        		}
//        		i++;        		    
    		}
//        }
//        for(int j = 0;j<pos.size();j++){
//        	if(j%20==0)
//            	System.out.println();
//        	System.out.print(pos.elementAt(j));
//        	
//        }
//        
//    			if(i%16==0){
//    				System.out.println();
//    				System.out.print(Integer.toHexString(i/16)+":");
//    			}
//    			System.out.print(hex.toUpperCase()+' '); .


  
    public static void main(String[] args) throws IOException {
      readfile("e:\\cc.pdf");
//    	byte i = -128;
//    	String hex = Byte.toHexString(i);
//    	System.out.println(hex);
    }
}
/*
 long HalfFileSize = file.length()/(int)(Math.random()*100);
		     FileInputStream fis = new FileInputStream(file);
		     byte[] buffer = new byte[2];
		     fis.skip(HalfFileSize);
		     fis.read(buffer);
		     byte[] HashValue = messageDigest.digest(buffer);
		     return HashValue;
 */
