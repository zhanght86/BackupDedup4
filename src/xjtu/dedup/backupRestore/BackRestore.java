package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opendedup.collections.HashtableFullException;
import org.opendedup.sdfs.Main;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;


import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.berkeleyDB.BerkeleyDB;
import xjtu.dedup.methodinterf.DedupMethod;

public class BackRestore {
	private DedupMethod dm;
	private BerkeleyDB bdb;
	public BackRestore(){
		bdb=new BerkeleyDB();
		if(Main.issis){
			dm=new SISDedup();
		}else if(Main.iscdc){
			dm=new CDCDedup();
		}else if(Main.issw){
			dm=new SWDedup();
		}else{
//			dm=new FSPDedup();
			dm=new SDFSDedup();
		}
		bdb.initdb(0);
	}
	public BackRestore(int deduptype){
		bdb=new BerkeleyDB();
		bdb.initdb(deduptype);
		// 0 represent FSP,1 represent CDC,2 represent SB,3 represent SIS,4 represent SDFS
		if(deduptype == 0){
			dm = new FSPDedup();
		}else if(deduptype == 1){
			dm = new CDCDedup();
//			dm = new NewCDCDedup();
//			dm = new FastCDCDedup();
//			dm = new NewFastCDCDedup();
		}else if(deduptype == 2){
			dm = new SWDedup();
		}else if(deduptype == 3){
			dm = new SISDedup();
		}else if(deduptype == 4){
			dm = new SDFSDedup();
		}
//		if(Main.issis){
//			dm=new SISDedup();
//		}else if(Main.iscdc){
//			dm=new CDCDedup();
////			dm=new NewCDCDedup();
//		}else if(Main.issw){
//			dm=new SWDedup();
//		}else if(Main.isfsp){
//			dm=new FSPDedup();
//		}else if(Main.issdfs){
//			dm=new SDFSDedup();
//		}
	}
	public void backup(File file,long filenum){
		dm.dedup(bdb,file, filenum);
	}
	public void restore(String filepath){
		dm.restore(bdb,filepath);
	}
	public void finalize(){
		bdb.finalize();
	}
	public void closedb(){
		bdb.closedb();
	}
	public String backupJobInfoFromDB(String key){
		return bdb.backupJobInfoFromDB(key)	;
	}
	
	public  String bytesToHexString(byte[] src){   
	    StringBuilder stringBuilder = new StringBuilder("");   
	   if (src == null || src.length <= 0) {   
	       return null;   
	    }   
	    for (int i = 0; i < src.length; i++) {   
	        int v = src[i] & 0xFF;   
	       String hv = Integer.toHexString(v);   
	       if (hv.length() < 2) {   
	            stringBuilder.append(0);   
	       }   
	       stringBuilder.append(hv);   
	    }   
	   return stringBuilder.toString();   
	}   
	public  byte[] hexStringToBytes(String hexString) {   
	   if (hexString == null || hexString.equals("")) {   
	       return null;   
	   }   
	   hexString = hexString.toUpperCase();   
	   int length = hexString.length() / 2;   
	   char[] hexChars = hexString.toCharArray();   
	   byte[] d = new byte[length];   
	   for (int i = 0; i < length; i++) {   
	        int pos = i * 2;   
	   d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));   
	   }   
	   return d;   
	}   
	private byte charToByte(char c) {   
	   return (byte) "0123456789ABCDEF".indexOf(c);   
	}
	public static void main(String args[]) throws NoSuchAlgorithmException, IOException{	
		BackRestore br=new BackRestore();
		Main.dedupDBStore="E:\\deduptest\\cdcvolumes\\ddb";
		File file=new File("E:\\deduptest\\testdata\\test\\t2.pdf");
		File file1=new File("E:\\deduptest\\testdata\\test\\t3.pdf");
		Main.chunkStore="E:\\deduptest\\cdcvolumes\\cdcChunkStore\\chunks";
		Main.chunkStoreLocal=true;
		Main.hashDBStore="E:\\deduptest\\cdcvolumes\\cdcChunkStore\\hdb";
		Main.dedupFiles=true;
//		Main.iscdc=true;
		Main.issw=true;
		Main.preAllocateChunkStore=false;
		Main.safeSync=false;
		
		SimpleDateFormat sdf1=new SimpleDateFormat("yyyy-MM-dd");
		/*
		Main.dedupDBStore="E:\\deduptest\\fspvolumes\\ddb";
		File file=new File("E:\\deduptest\\testdata\\test\\p1.jpg");
		File file1=new File("E:\\deduptest\\testdata\\test\\t3.pdf");
		Main.chunkStore="E:\\deduptest\\fspvolumes\\fspChunkStore\\chunks";
		Main.chunkStoreLocal=true;
		Main.hashDBStore="E:\\deduptest\\fspvolumes\\fspChunkStore\\hdb";
		Main.dedupFiles=true;
		Main.iscdc=false;
		Main.preAllocateChunkStore=false;
		Main.safeSync=false;
		RestoreJob.restorePath="E:\\deduptest\\restore\\";
		*/
		BackupJob.creatTime=sdf1.format(new Date());
	//	br.FSPBackup(file,1);
	//	br.FSPBackup(file1);	
	//	br.SISBackup(file, 1);
//		br.cdcBackup(file1);
//		try {
//			br.SWbackup(file1);
//		} catch (HashtableFullException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		br.SDFSBackup(file1);
		br.finalize();

		
		System.out.println("----------------------------");
		String filepath="E:\\deduptest\\fspvolumes\\FSPfiles\\t2.pdf";
   // 	br.FSPRestore(filepath);
    //	br.FSPRestore("E:\\deduptest\\fspvolumes\\FSPfiles\\test.pdf");
	//	br.finalize();
	/*	
		String akey="myfirstkey";
		String adata="myfirstdata";
		
		String databasePath="E:\\deduptest\\database\\FSPdatabase";
		Database myDb = null;
		Environment myDbEnv=null;
		try {
		//  打开一个环境，如果不存在则创建一个
		    EnvironmentConfig envConfig = new EnvironmentConfig();
		    envConfig.setAllowCreate(true);//如果不存在则创建一个
		    envConfig.setTransactional(true); // 支持事务
		    myDbEnv = new Environment(new File(databasePath), envConfig);
		//  打开一个数据库，如果数据库不存在则创建一个 
		    DatabaseConfig dbConfig = new DatabaseConfig();
		    dbConfig.setAllowCreate(true);
		    myDb = myDbEnv.openDatabase(null, 
		    		"FSPDatabase", dbConfig); //打开一个数据库，数据库名为
		                                   //FSPDatabase,数据库的配置为dbConfig
		    //2)打开用来存储类信息的库 
		    Database myClassDb = myDbEnv.openDatabase(null, "classDb", dbConfig); 
		} catch (DatabaseException dbe) {
		    // 错误处理
			System.err.println(dbe.toString());
			System.exit(1);
		}
		*/
	/*	try{
		    DatabaseEntry thekey=new DatabaseEntry(akey.getBytes());
		    DatabaseEntry thedata=new DatabaseEntry(adata.getBytes());
		    myDb.put(null, thekey, thedata);
		    if(myDb!=null){
		    	myDb.close();
		    }
		    if(myDbEnv!=null){
		    	myDbEnv.cleanLog();
		    	myDbEnv.close();
		    }
		}catch (Exception e) {
			// TODO: handle exception
		}
		*/
		/*
		try{
			DatabaseEntry thekey=new DatabaseEntry(akey.getBytes());
		    DatabaseEntry thedata=new DatabaseEntry();
		    if(myDb.get(null, thekey, thedata,LockMode.DEFAULT)==OperationStatus.SUCCESS){
		    	byte[] retData=thedata.getData();
		    	String founddata=new String(retData,"UTF-8");
		    	System.out.println("For key: "+akey+" found data: "+founddata+" .");
		    }else{
		    	System.out.println("For key: "+akey+" found data: null.");
		    }
		}catch (Exception e) {
			// TODO: handle exception
		}
		*/
	/*
	//	Main.volume="cdcvolumes";
		try {
			try {
				br.cdcBackup(file,true);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator it = hash_list.iterator();
		while(it.hasNext()){
			System.out.println(it.next().toString());
		}
	*/
	/*
		// restore test
		RestoreJob.restorePath="D:\\restore\\";
		File metafile=new File("E:\\deduptest\\cdcvolumes\\cdcfiles\\p1.jpg");
		br.cdcRestore(metafile,true);
	*/
	}
	
}
