package disms.SISStore.client;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import com.planetj.math.rabinhash.*;

public class CDC{
	public CDC(String file) throws IOException{
		RabinHashFunction32 rhf = new RabinHashFunction32(36524);
		File f = new File(file);
        long size = f.length();
        Vector<String> pos = new Vector<String>();
        Vector<String> len = new Vector<String>();
        FileInputStream fis = new FileInputStream(file);        		 
    	byte[] buffer = new byte[1024*1024];
    	byte[] cal = new byte[16];
    	long temp = 0;
    	long i = 0;
    	int j = 0;
    	int tail;
    	System.out.println(size);
    	System.out.println(System.currentTimeMillis());
        for(i = 0;i<size;i=i+1024*1024){
    		fis.read(buffer);
    		for (j = 0; j < 1024*1024-15; j++) {  
    			cal = Arrays.copyOfRange(buffer, j, j+15);
//    			for(int k = 0;k<cal.length;k++){
//    				String hex1 = Integer.toHexString(cal[k] & 0xFF);
//    				System.out.print(hex1+"*");
//    			}
//    			
//    			System.out.println(rhf.hash(cal));
    			tail = rhf.hash(cal);
	    		if((i+j<=size)&&(tail%67425==0)&&(tail!=0)){
		    			len.addElement(String.valueOf((i+j-temp)));	    			
		    	        pos.addElement(String.valueOf(i+j));
		    	        temp = i+j;
	    		}	    		
    		}
        }
    
    	System.out.println(System.currentTimeMillis());
        len.addElement(String.valueOf((size-temp)));
        int ss[] = new int[len.size()];
        System.out.println("块数量"+'\n'+len.size());
    	System.out.println("块大小");
        for(int c = 0;c<len.size();c++){
        	System.out.println(len.elementAt(c));
//        	ss[c]=Integer.valueOf(len.elementAt(c));
//        	System.out.println(ss[c]);
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
    	/**/
	}
	public static void main(String[] args) throws IOException{
		String str="abc";
		str=1+2+str+3+4;
		System.out.println(str);
	}
}