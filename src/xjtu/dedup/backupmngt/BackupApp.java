package xjtu.dedup.backupmngt;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.omg.CORBA.PUBLIC_MEMBER;
import org.opendedup.util.BackupDedupLogger;
import org.opendedup.util.OSValidator;
import org.opendedup.util.SDFSLogger;
import org.opendedup.util.date.WeekPolicy;
import org.xml.sax.SAXException;

import xjtu.dedup.multithread.BackupClient;
import xjtu.dedup.multithread.RestoreClient;
import xjtu.dedup.restoremngt.RestoreJobInfoParser;
import xjtu.dedup.restoremngt.RestoreJobRecord;


public class BackupApp { 
	private static String backupjobname="myfristbackupjob";
	private static String backupsrc="D:\\etc1,D:\\etc2,D:\\etc3";
	private static boolean iscompressed=true;
	private static int type=1;
	private static int client_threads=10;
	SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd"); 
    String currentDateformat=sdf1.format(new Date());
	private static String backup_volume_name="sdfs_vol1";
	static //恢复配置
	String restorePath="D:\\restore\\";
	static String restoredFileName="2012-01-08_etc2_backup.zip";
	static int clt_threads=10;
	static String volume_name="sdfs_vol1";
	
    static boolean isbackup=false;//true;
    static boolean isrestore=true;//false;
    //备份预处理
    public void preProcessBackupJob(String configfile) throws IOException{
    	if(configfile==null)
    		return;
    	else{
    			BackupJobInfoParser bjip=new BackupJobInfoParser();
    			SDFSLogger.getLog().info("parsing the config file: "+configfile);
    			bjip.parsebackupjobconfig(configfile);
    		}
        }
    //获取备份信息
    public static String[][] getBackupInfo(String configfile){
    	BackupJobInfoParser bjip=new BackupJobInfoParser();
    	return bjip.getBackupInfo(configfile);
    }
    //更新XML备份任务记录中备份状态
    public  void updateBackupStateToXML(String backupjobid,String configfile) throws ParserConfigurationException, SAXException, IOException{
    	BackupJobRecord bjr=new BackupJobRecord();
    	bjr.updateBackupState(backupjobid,configfile);
    	
    }
    //删除一条备份任务记录
    public static void delBackupTaskRecord(String configfile,String backupTaskID,String backupSrc){
    	BackupJobInfoParser bjip=new BackupJobInfoParser();
    	try {
			bjip.updateBackupJobConfigtoDelete(configfile, backupTaskID, backupSrc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //恢复预处理
    public static String[][] preProcessRestoreJob(String configfile){
    	if(configfile==null){
    		return null;
    	}
    	else {
    		BackupJobInfoParser bjip=new BackupJobInfoParser();
    		SDFSLogger.getLog().info("parsing the config file: "+configfile);
    		return bjip.parserestorejobconfig(configfile);
    	}
    }
    
    public static void startbackup(String configfile) throws IOException {
	//	BackupJobRecord bjr=new BackupJobRecord();
	//	bjr.init();
	//	String configfile=bjr.getbackupjobconfigpath();
		BackupJobInfoParser bjip=new BackupJobInfoParser();
		System.out.println("parsing the config file "+configfile);
	//	SDFSLogger.getLog().info("parsing the config file: "+configfile);
		bjip.parsebackupjobconfig(configfile);
    }
    
    public static void startrestore(String configfile) throws ParserConfigurationException, IOException{
    	String restoreconfigfile=configfile;
		RestoreJobInfoParser rjip=new RestoreJobInfoParser();
//		SDFSLogger.getLog().info("parsing the restore config file : "+restoreconfigfile);
		BackupDedupLogger.getrestoreLog().info("parsing the restore config file : "+restoreconfigfile);
		rjip.parserestorejobconfigfile(restoreconfigfile);
    }
    /*
     * 测试数据
     * */
    public static String writeBackupConfigToXML(String backupjobname,String backupsrc,int type,boolean isCompressed,int client_threads,String backup_volume_name) throws ParserConfigurationException, IOException, ParseException{   	
		BackupJobRecord bjr=new BackupJobRecord();
		String bjobname=backupjobname;
		String bsrc=backupsrc;
		int t=type;
		boolean isComp=isCompressed;
		int c_threads=client_threads;
		String b_volume_name=backup_volume_name;	
		bjr.writeBackupConfigToXML(bjobname,bsrc, t, isComp, c_threads, b_volume_name);
		return bjr.getbackupjobconfigpath();
    }
    
    public String writeBackupConfigToXML(String backupjobname,String backupsrc,WeekPolicy wp,String backup_volume_name,long length,long count) throws ParserConfigurationException, IOException, ParseException{   	
		BackupJobRecord bjr=new BackupJobRecord();
		String bjobname=backupjobname;
		String bsrc=backupsrc;
		int c_threads=client_threads;
		String b_volume_name=backup_volume_name;	
		bjr.writeBackupConfigToXML(bjobname,bsrc,wp, c_threads, b_volume_name,length,count);
		return bjr.getbackupjobconfigpath();
    }
    /*
     * 界面显示
     * */
    public  String writeBackupConfigToXML(String backupjobname,String backupsrc,int type,int compresstype,int encrypttype,String backup_volume_name) throws ParserConfigurationException, IOException, ParseException{   	
		BackupJobRecord bjr=new BackupJobRecord();
		String bjobname=backupjobname;
		String bsrc=backupsrc;
		int t=type;
		int ct=compresstype;
		int et=encrypttype;
		String b_volume_name=backup_volume_name;	
		bjr.writeBackupConfigToXML(backupjobname, backupsrc, type, ct, et, b_volume_name);
		return bjr.getbackupjobconfigpath();
    }
    
    public static String writeRestoreConfigToXML(String backupjobid,String backuphostname,String restoreDest,String backupsrc,int clients,String vol_name,long filescount,String backupdate) throws ParserConfigurationException{
    	RestoreJobRecord rjr=new RestoreJobRecord();
		rjr.writeRestoreConfigToXML(backupjobid,backuphostname,restoreDest, backupsrc, clients, vol_name,filescount,backupdate);
    	
		return rjr.getrestorejobconfigpath();
    	
    }
    public static void main(String[] args) throws Exception { 
    	
    	String configfile;
    	if(isbackup){
    		configfile=writeBackupConfigToXML(backupjobname,backupsrc, type, iscompressed, client_threads, backup_volume_name);
    	    startbackup(configfile);
    	/*	BackupJobRecord bjr=new BackupJobRecord();
    		bjr.init();
    		BackupClient bc=new BackupClient();
        	System.out.println("sdfs volume initalizing......");
    		bc.initsdfs();
    		String configfile=bjr.getbackupjobconfigpath();
    		BackupJobInfoParser bjip=new BackupJobInfoParser();
    		System.out.println("parsing the config file");
    		bjip.parsebackupjobconfig(configfile);*/
    	}
    	if(isrestore){
    		String username=System.getProperty("user.name");
    		String backupdate="2012-02-28";
    		int comptype=2;
    		configfile=writeRestoreConfigToXML(null,null,restorePath, restoredFileName, clt_threads, volume_name,0,backupdate);
    		startrestore(configfile);
    	/*	RestoreJobRecord rjr=new RestoreJobRecord();
    		rjr.init();
    		System.out.println("sdfs volume initalizing......");
    		RestoreClient rc=new RestoreClient();
        	rc.initsdfs();
    		String restoreconfigfile=rjr.getrestorejobconfigpath();
    		RestoreJobInfoParser rjip=new RestoreJobInfoParser();
    		System.out.println("parsing the config file");
    		rjip.parserestorejobconfigfile(restoreconfigfile);*/
    	}
    	
       
        
        /*
         * *
        if(BackupJob.iscompressed){
        	System.out.println("Get information of backup files......"); 
        	app.getBackupFilesInfo();
        	System.out.println("Start compress dst folder......"); 
        	app.compress(); 
        	System.out.println("sdfs volume initalizing......");
        	app.initsdfs();
        	System.out.println("Start backup the compressed files......");
        	app.backup();
        	System.out.println("restoring......");
        	String filename="2011-11-28_backup.zip";
        	app.restore(filename);
        	System.out.println("End!"); 
        }
        else{
        	System.out.println("Get information of backup files......"); 
        	app.getBackupFilesInfo();
        	System.out.println("Output the backup files information......");
        	System.out.println(fileInfoBuf); 
        	System.out.println(filesbuf);
        	app.writetobackupfiles();
        	System.out.println("sdfs volume initalizing......");
        	app.initsdfs();
        	System.out.println("Start backup the uncompressed files......");
        	app.backup(BackupJob.backupsrc);
        	System.out.println("restoring......");
        	String filename="B.txt";//"2011-11-28_backup.zip";
        	app.restore(filename);
        	System.out.println("End!"); 
        }
         	System.out.println("Start......"); 
        	System.out.println("Start delete the dst folder......"); 
        	app.delete(); 
        	System.out.println("Start create the dst folder......"); 
        	app.mkdir(); 
        	System.out.println("Start copy src folder to dst folder......"); 
        	app.copy(); 
        	System.out.println(fileNameBuf); 
        	System.out.println("sdfs volume initalizing......");
        	app.initsdfs();
        	System.out.println("Start backup the compressed file folders......");
        	app.backup();
        	System.out.println("restoring......");
        	String filename="2011-11-28_backup.zip";
        	app.restore(filename);
        	System.out.println("End!"); 
         * */
    } 
} 