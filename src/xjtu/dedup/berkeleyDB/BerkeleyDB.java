package xjtu.dedup.berkeleyDB;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.opendedup.hashing.HashFunctionPool;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.MetaDataDedupFile;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

public class BerkeleyDB {
	private  Environment myDbEnvironment = null;
	private  Environment myBackupDbEnvironment=null;
	private  Environment myHashDbEnvironment=null;
	private  Database myDatabase = null;
	private  Database myBackupDatabase=null;
	private  Database myHashDatabase=null;
	private  Database myClassDb=null;
	private  StoredClassCatalog classCatalog=null;
	private  EntryBinding<MetaDataDedupFile> dataBinding=null;
	
	private DatabaseEntry key=new DatabaseEntry();
	private DatabaseEntry data=new DatabaseEntry();
	
	private static HashFunctionPool hashPool = new HashFunctionPool(
			Main.writeThreads + 1);
	/*
	 * @param deduptype
	 * 			the type of deduplication method.
	 * 0 represent FSP(SDFS),1 represent CDC,2 represent SB,3 represent SIS
	 * */
	public void initdb(int deduptype){
		if(deduptype==0){
			//fsp backup
				String backupdatabasepath=Main.volume.getPath();
				String databasePath=Main.dedupDBStore;
				String hashdatabasepath=Main.hashDBStore;
				try {
				//  打开一个环境，如果不存在则创建一个
				    EnvironmentConfig envConfig = new EnvironmentConfig();
				    envConfig.setAllowCreate(true);//如果不存在则创建一个
				    envConfig.setTransactional(true); // 支持事务
				    File file=new File(databasePath);
				    File file1=new File(backupdatabasepath);
				    File file2=new File(hashdatabasepath);
				    if(!file.exists()){
				    	file.mkdirs();
				    }
				    if(!file1.exists())
				    	file1.mkdirs();
				    if(!file2.exists())
				    	file2.mkdirs();
				    myDbEnvironment = new Environment(file, envConfig);
				    myBackupDbEnvironment=new Environment(file1,envConfig);
				    if(Main.issw)
				    	myHashDbEnvironment=new Environment(file2,envConfig);
				//  打开一个数据库，如果数据库不存在则创建一个 
				    DatabaseConfig dbConfig = new DatabaseConfig();
				    dbConfig.setAllowCreate(true);
				    myDatabase = myDbEnvironment.openDatabase(null, 
				    		"FSPDatabase", dbConfig); //打开一个数据库，数据库名为
				                                   //FSPDatabase,数据库的配置为dbConfig
				    myBackupDatabase=myBackupDbEnvironment.openDatabase(null, "FSPBackupDatabase", dbConfig);
				    if(Main.issw)
				    	myHashDatabase=myHashDbEnvironment.openDatabase(null, "PrimaryHashDatabase", dbConfig);
				    //2)打开用来存储类信息的库 
				    myClassDb = myDbEnvironment.openDatabase(null, "classDb", dbConfig); 
				 // 3）创建catalog 
				    classCatalog = new StoredClassCatalog(myClassDb); 
				    // 4）绑定数据和类 
				    dataBinding = new SerialBinding<MetaDataDedupFile>(classCatalog,MetaDataDedupFile.class); 
				} catch (DatabaseException dbe) {
				    // 错误处理
					System.err.println(dbe.toString());
					System.exit(1);
				}
				}
				else if(deduptype==1){
					// CDC database
					String databasePath=Main.dedupDBStore;//备份文件数据库，记录文件及对应分块hash值信息
					String backupdatabasepath=Main.volume.getPath();//备份任务数据库，记录一个备份任务和相应文件信息
					try {
					//  打开一个环境，如果不存在则创建一个
					    EnvironmentConfig envConfig = new EnvironmentConfig();
					    envConfig.setAllowCreate(true);//如果不存在则创建一个
					    envConfig.setTransactional(true); // 支持事务
					    File file=new File(databasePath);
					    File backupinfofile=new File(backupdatabasepath);
					    if(!file.exists())
					    	file.mkdirs();
					    if(!backupinfofile.exists())
					    	backupinfofile.mkdirs();
					    myDbEnvironment = new Environment(file, envConfig);
					    myBackupDbEnvironment=new Environment(backupinfofile,envConfig);
					//  打开一个数据库，如果数据库不存在则创建一个 
					    DatabaseConfig dbConfig = new DatabaseConfig();
					    dbConfig.setAllowCreate(true);
					    myDatabase = myDbEnvironment.openDatabase(null, 
					    		"CDCDatabase", dbConfig); //打开一个数据库，数据库名为
					                                   //CDCDatabase,数据库的配置为dbConfig
					    myBackupDatabase=myBackupDbEnvironment.openDatabase(null, "CDCBackupInfoDatabase", dbConfig);
					    //2)打开用来存储类信息的库 
					    myClassDb = myDbEnvironment.openDatabase(null, "classDb", dbConfig); 
					 // 3）创建catalog 
					    classCatalog = new StoredClassCatalog(myClassDb); 
					    // 4）绑定数据和类 
					    dataBinding = new SerialBinding<MetaDataDedupFile>(classCatalog,MetaDataDedupFile.class); 

					} catch (DatabaseException dbe) {
					    // 错误处理
						System.err.println(dbe.toString());
						System.exit(1);
					}
				}
				else if(deduptype==2){
					// SB database
				}
				else if(deduptype==3){
					// SIS database
				}else if(deduptype==4){
					//sdfs nodatabase
				}
	}
	/**
	 * using berkeley db to store the backup job infomation 
	 * */
	public void backupJobInfoToDB(String key,String data){
		try{
			try {
				DatabaseEntry thekey=new DatabaseEntry(key.getBytes("UTF-8"));
				DatabaseEntry thedata=new DatabaseEntry();
				if(myBackupDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS)
				{
					myBackupDatabase.delete(null,thekey);
				}
				thedata=new DatabaseEntry(data.getBytes("UTF-8"));
				myBackupDatabase.put(null, thekey, thedata);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}catch (DatabaseException dbe) {
			// TODO: handle exception
			dbe.printStackTrace();
		}
	}
	public String backupJobInfoFromDB(String key){
		String data = null;
		DatabaseEntry thekey = null;
		DatabaseEntry thedata=null;
		try {		
			try {
				thekey = new DatabaseEntry(key.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			thedata=new DatabaseEntry();
			if(myBackupDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS){
				// 如果文件名已经存在，则读入元数据文件对象，并重新写入更新
			   try {
				data=new String(thedata.getData(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//   mf.setLength(length, false);
			}
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
		return data;		
	}
	
	public  MetaDataDedupFile getMetaDataDedupFile(File file){
		MetaDataDedupFile mf=null;
		Long length = file.length();
		int blockNum=(length.intValue()/Main.chunkStorePageSize)+1;
		String path = "E:\\deduptest\\fspvolumes\\FSPfiles\\" ;
		String key = path+file.getName();//File ID
		DatabaseEntry thekey = null;
		DatabaseEntry thedata=null;
		try {		
			try {
				thekey = new DatabaseEntry(key.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			thedata=new DatabaseEntry();
			if(myDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS){
				// 如果文件名已经存在，则读入元数据文件对象，并重新写入更新
			   mf=(MetaDataDedupFile)dataBinding.entryToObject(thedata);
			//   mf.setLength(length, false);
			}else{
				//如果不存在，则需要重新创建一个新的元数据文件对象，写入该文件去重元数据信息
				 mf=new MetaDataDedupFile();		
			}
			mf.setPath(key,false);
			mf.setblocknum(blockNum, false);
			mf.setLength(length, false);
			if(mf.getDfGuid()==null)
				mf.setDfGuid(UUID.randomUUID().toString());
			dataBinding.objectToEntry(mf, thedata);
			myDatabase.put(null, thekey, thedata);	
		//	mf.setblocknum(4, false);
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}

		return mf;
	}
	public  MetaDataDedupFile getMetaDataDedupFile(File file,String fileID){
		MetaDataDedupFile mf = null;
		String key = fileID;
		Long length = file.length();		
		int blockNum=0;
		if(Main.isfsp)
			blockNum=(length.intValue()/Main.chunkStorePageSize)+1;
		DatabaseEntry thekey = null;
		DatabaseEntry thedata=null;
		try {		
			try {
				thekey = new DatabaseEntry(key.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			thedata=new DatabaseEntry();
			if(myDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS){
				// 如果文件名已经存在，则读入元数据文件对象，并重新写入更新
			   mf=(MetaDataDedupFile)dataBinding.entryToObject(thedata);
			//   mf.setLength(length, false);
			}else{
				//如果不存在，则需要重新创建一个新的元数据文件对象，写入该文件去重元数据信息
				 mf=new MetaDataDedupFile();	
				 mf.setPath(key,false);
				 if(Main.isfsp)
					 mf.setblocknum(blockNum, false);
				 mf.setLength(length, false);
				 if(mf.getDfGuid()==null)
				     mf.setDfGuid(UUID.randomUUID().toString());
//				 mf.sync();
				dataBinding.objectToEntry(mf, thedata);
				myDatabase.put(null, thekey, thedata);	
			}
		//	mf.setblocknum(4, false);
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}

		return mf;
	}
	// 获取去重文件元数据文件
	public  MetaDataDedupFile getMetaDataDedupFile(String fileID){
		MetaDataDedupFile mf = null;
		String key = fileID;
		DatabaseEntry thekey = null;
		DatabaseEntry thedata=null;
		try {		
			try {
				thekey = new DatabaseEntry(key.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			thedata=new DatabaseEntry();
			if(myDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS){
				// 如果文件名已经存在，则读入元数据文件对象，并重新写入更新
			   mf=(MetaDataDedupFile)dataBinding.entryToObject(thedata);
			//   mf.setLength(length, false);
			}
		//	mf.setblocknum(4, false);
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
		return mf;
	}
	// 记录去重文件数据块数
	public  void putMetaDataDedupFile(String fileID, MetaDataDedupFile mf,int blocknum){
		String key = fileID;
		DatabaseEntry thekey = null;
		DatabaseEntry thedata=null;
		try {		
			try {
				thekey = new DatabaseEntry(key.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			thedata=new DatabaseEntry();
			if(myDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS){
				// 如果文件名已经存在，则删除原元数据文件对象，并重新写入新mf
			   if(myDatabase.delete(null, thekey)==OperationStatus.SUCCESS){
				   mf.setblocknum(blocknum, false);
				   dataBinding.objectToEntry(mf, thedata);
				   myDatabase.put(null, thekey, thedata);  
			   }
			}
			else{
				mf.setblocknum(blocknum, false);
				dataBinding.objectToEntry(mf, thedata);
				myDatabase.put(null, thekey, thedata);  
			}
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
	}
	public  boolean SWAdler32HashExists(int hkey){
		String hashdata=Integer.toString(hkey);// 恢复时new String(byte[] bytes,String charset);
		DatabaseEntry thekey = null;
		DatabaseEntry thedata=null;
	    boolean isExist=false;
		try{
			thekey=new DatabaseEntry(hashdata.getBytes("UTF-8"));
			thedata=new DatabaseEntry();
			if(myHashDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS){
				    byte[] theclaimsdata=thedata.getData();
				    String thedatastr=new String(theclaimsdata,"UTF-8");
				    int claimsnum=Integer.parseInt(thedatastr);
				    claimsnum+=1;
				    thedatastr=Integer.toString(claimsnum);
				    thedata=new DatabaseEntry(thedatastr.getBytes("UTF-8"));
				    myHashDatabase.put(null, thekey, thedata);
				    
					isExist=true;
			}
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return isExist;
	}
	public  boolean addSWAdler32Hash(int hkey){
		String hashdata=Integer.toString(hkey);
		DatabaseEntry thekey = null;
		DatabaseEntry thedata=null;
	    String thenewdata=null;
	    boolean isSuccess=false;
		try{
			thekey=new DatabaseEntry(hashdata.getBytes("UTF-8"));
			thedata=new DatabaseEntry();
			if(myHashDatabase.get(null, thekey, thedata, LockMode.DEFAULT)==OperationStatus.SUCCESS){
				 byte[] theclaimsdata=thedata.getData();
				 String thedatastr=new String(theclaimsdata,"UTF-8");
				 int claimsnum=Integer.parseInt(thedatastr);
				 claimsnum+=1;
				 thedatastr=Integer.toString(claimsnum);
				 thedata=new DatabaseEntry(thedatastr.getBytes("UTF-8"));
			}else{
				thedata=new DatabaseEntry("1".getBytes("UTF-8"));
			}
			myHashDatabase.put(null, thekey, thedata);
			isSuccess=true;
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return isSuccess;
	}
	/**
	 * clear the locale information of db
	 * */
	public void finalize() {
		try {
			if (myDatabase != null) {
	            myDatabase.close();
	            myClassDb.close();
	        }

			if (myDbEnvironment != null) {
		        myDbEnvironment.cleanLog(); // 在关闭环境前清理下日志
		        myDbEnvironment.close();
		    } 
			//close backupdatabase
			if(myBackupDatabase!=null)
				myBackupDatabase.close();
			
			if(myBackupDbEnvironment!=null){
				myBackupDbEnvironment.cleanLog();
				myBackupDbEnvironment.close();
			}
			//close hashdatabase
			if(myHashDatabase!=null)
				myHashDatabase.close();
			if(myHashDbEnvironment!=null){
				myHashDbEnvironment.cleanLog();
				myHashDbEnvironment.close();
			}

		    
		} catch (DatabaseException dbe) {
		    // Exception handling goes here
		} 


	}
	/**
	 * close the locale information of db
	 * */
	public void closedb() {
		try {
			if (myDatabase != null) {
	            myDatabase.close();
	            myClassDb.close();
	        }

			if (myDbEnvironment != null) {
		        myDbEnvironment.cleanLog(); // 在关闭环境前清理下日志
		        myDbEnvironment.close();
		    } 
			//close backupdatabase
			if(myBackupDatabase!=null)
				myBackupDatabase.close();
			
			if(myBackupDbEnvironment!=null){
				myBackupDbEnvironment.cleanLog();
				myBackupDbEnvironment.close();
			}
			//close hashdatabase
			if(myHashDatabase!=null)
				myHashDatabase.close();
			if(myHashDbEnvironment!=null){
				myHashDbEnvironment.cleanLog();
				myHashDbEnvironment.close();
			}	    
		} catch (DatabaseException dbe) {
		    // Exception handling goes here
		} 


	}
}
