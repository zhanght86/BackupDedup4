package xjtu.dedup.multithread;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.servers.SDFSService;
import org.opendedup.util.BackupDedupLogger;
import org.opendedup.util.OSValidator;
import org.opendedup.util.SDFSLogger;
import org.quartz.Job;

import xjtu.dedup.backupRestore.BackRestore;
import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.backupmngt.VolumeInfoParser;
import xjtu.dedup.restoremngt.RestoreJob;

import disms.SISStore.server2.*;
import disms.SISStore.client2.*;
import frame.progressbar.progressWindow;

public class RestoreClient implements IBackupRestoreClient {
	String restoreClientHostName;
	String restorePath;
	String restoredFileName;
	String createTime;
	String volume_name;
	String backup_volume_type;
	String backupdate;
	long filescount;
	//String backupjobid;
	int comptype;
	String hostname;
	String backupClientHostName;
	
	BackRestore backRestore=null;
	
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss"); 
    
	private static SDFSService sdfsService=null;
	private  progressWindow progressWindow;
	
	public RestoreClient(){
		
	}
	public RestoreClient(String restoreclient,String backupClientHostname,String restorepath,String restoredfilename,String createtime,String volume_name,long filescount,String backupdate,int comptype) throws IOException{
	//	this.restoreClientHostName=restoreclient;
		this.hostname=restoreclient;
		this.restorePath=restorepath;
		this.restoredFileName=restoredfilename;
		this.createTime=createtime;
		this.volume_name=volume_name;
		this.filescount=filescount;
		this.backupdate=backupdate;
		this.comptype=comptype;
		this.backupClientHostName=backupClientHostname;
//		System.out.println("start sdfs services......");
		BackupDedupLogger.getrunstateLog().info("start backupdedup service for restore.......");
		initsdfs();
		execRestoreJob();
//		stopsdfs();
	}
	
	public void execRestoreJob() throws IOException{
		new Thread(){
			public void run(){
				int volumetype=VolumeInfoParser.getTypeOfVolume(RestoreJob.volume_name);
				backRestore=new BackRestore(volumetype);
				BackupDedupLogger.getrestoreLog().info("restore job id : "+RestoreJob.backupJobID+
						" restore date : "+sdf.format(new Date())+" restore host : "+
						RestoreJob.BackupClientHostName + " total files : "+RestoreJob.backupJobFilesCount);
				progressWindow = new progressWindow();
				progressWindow.setMax((int)RestoreJob.backupJobFilesCount);
				progressWindow.setProgress(0);
				progressWindow.setVisible(true);
				long start=System.currentTimeMillis();
				String key=RestoreJob.BackupClientHostName+RestoreJob.backupJobID+RestoreJob.backupdate;
				String data=null;
				if(Main.issis)
				{
					try {
						new SISTaskRestore(RestoreJob.backupdate,RestoreJob.restorePath,progressWindow);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{
					if(Main.issdfs){
						//sdfs fixed-size chunking
						backRestore.restore(RestoreJob.restoredFileName);
					}else{
					for(int i=0;i<RestoreJob.backupJobFilesCount;i++)
					{
						data=backRestore.backupJobInfoFromDB(key+i);
						backRestore.restore(data);
						progressWindow.setCurrentFile(data);
	 					progressWindow.setProgress(progressWindow.getProgress() + i);
					}
					}
				}
				long end=System.currentTimeMillis();
				BackupDedupLogger.getrestoreLog().info("Restore Job end use "+(end-start));
				backRestore.finalize();
				progressWindow.dispose();
				BackupDedupLogger.getrestoreLog().info("Finished!");
			}
		}.start();

	}
    /*
     * 初始化sdfs卷，设置备份数据存储位置
     * */
    public void initsdfs(){
    	String volumeConfigFile = OSValidator.getConfigPath()+RestoreJob.volume_name+"-volume-cfg.xml";//"E:\\deduptest\\etc\\sdfs_vol1-volume-cfg.xml";
		String routingConfigFile = null;//"C:\\Program Files\\sdfs\\etc\\routing-config.xml";   //"E:\\deduptest\\etc\\routing-config.xml";
		sdfsService = new SDFSService(volumeConfigFile,
				routingConfigFile);

		try {
			sdfsService.start();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("Exiting because " + e1.toString());
			System.exit(-1);
		}

    }
    
    /*
     * 停止sdfs卷，刷新内存数据存储到磁盘中
     * */
    public void stopsdfs(){
    	ShutdownHook shutdownHook = new ShutdownHook(sdfsService);
    	Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
	@Override
	public void backup() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void backup(String backupsrcs) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void doRecursivebackupFiles(File dir){
		// TODO Auto-generated method stub
		
	}
	@Override
	public void compress() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void copy() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void delete() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void deleteFolder(File folder) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getAllFiles(File dir, String modifiedDate, String dest,
			String src, int type) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void getBackupFilesInfo() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mkdir() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void save(String dest) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void saveAll(String zipdir) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void saveBackupFilesInfo(File dir) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String searchDirectory(File dir, String modifiedDate, String dest,
			String src, int type) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void writetobackupfiles() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void zip(String path, File basePath, ZipOutputStream zo,
			boolean isRecursive, boolean isOutBlankDir) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void dojob() throws SQLException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void restore(String filename) {
		// TODO Auto-generated method stub
		
	}
}
