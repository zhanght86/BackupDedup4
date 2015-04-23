package xjtu.dedup.bloomfilter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Writer;
import java.util.BitSet;

public   class  SimpleBloomFilter {

    private   static   final   int    DEFAULT_SIZE  =   2   <<   24 ;
    private   static   final   int [] seeds         =   new   int [] {  7 ,  11 ,  13 ,  31 ,  37 ,  61  };

    private   BitSet             bits          =   new  BitSet(DEFAULT_SIZE);
    private  SimpleHash[]       func          =   new  SimpleHash[seeds.length];

    public   static   void  main(String[] args) {
      // System.out.println(DEFAULT_SIZE);
       String value  =   " stone2083@yahoo.cn " ;
       SimpleBloomFilter filter  =   new  SimpleBloomFilter();
       System.out.println(filter.contains(value));
       filter.add(value);
       System.out.println(filter.contains(value));
       filter.store();
       String value1="Startzgf168@126.com";
       System.out.println(filter.contains(value1));
    //   filter.add(value1);
       System.out.println(filter.contains(value1));
       filter.store();
   }

    public  SimpleBloomFilter() {
    	init();
        for  ( int  i  =   0 ; i  <  seeds.length; i ++ ) {
           func[i]  =   new  SimpleHash(DEFAULT_SIZE, seeds[i]);
       }
   }
    
    public  void init(){
    	String bitstr=null;
    	StringBuffer sb=new StringBuffer();
    	//byte[] bitbytes=null;
    	try{
        	File file=new File("C:\\bits.txt");
        	if(!file.exists())
        		return;
        	FileInputStream fis=new FileInputStream(file);
        	DataInputStream dis=new DataInputStream(fis);
        	while((bitstr=dis.readLine())!=null)
        		sb.append(bitstr);
        //	bitstr=dis.readLine();
        	//dis.read(bitbytes);
        //	System.out.println(sb.toString());
        	bits=fromByteArray(sb.toString().getBytes());
        	System.out.println(bits.toString());
        	fis.close();
        	dis.close();
        //	System.out.println(bitstr);
        	}catch (Exception e) {
    			// TODO: handle exception
        		e.printStackTrace();
    		}
    }
    
    public BitSet fromByteArray(byte[] bytes){
    	BitSet bs=new BitSet();
    	for(int i=0;i<bytes.length*8;i++)
    	{
    		if((bytes[bytes.length-i/8-1] & (1 << (i%8)))>0)
    			bs.set(i);
    	}
    	return bs;
    }
    
    public   void  add(String value) {
        for  (SimpleHash f : func) {
           bits.set(f.hash(value),  true );
       }
   }

    public   boolean  contains(String value) {
        if  (value  ==   null ) {
            return   false ;
       }
        boolean  ret  =   true ;
        for  (SimpleHash f : func) {
           ret  =  ret  &&  bits.get(f.hash(value));
       }
        return  ret;
   }
    	
    public void store(){
    	String bitstr=bits.toString();
    	try{
    	File file=new File("C:\\bits.txt");
    	FileOutputStream ops=new FileOutputStream(file);
    	DataOutputStream dos=new DataOutputStream(ops);
    	dos.write(bitstr.getBytes());
    	ops.close();
    	dos.close();
    	}catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
    }

    public   static   class  SimpleHash {

        private   int  cap;
        private   int  seed;

        public  SimpleHash( int  cap,  int  seed) {
            this .cap  =  cap;
            this .seed  =  seed;
       }

        public   int  hash(String value) {
            int  result  =   0 ;
            int  len  =  value.length();
            for  ( int  i  =   0 ; i  <  len; i ++ ) {
               result  =  seed  *  result  +  value.charAt(i);
           }
            return  (cap  -   1 )  &  result;
       }

   }

} 