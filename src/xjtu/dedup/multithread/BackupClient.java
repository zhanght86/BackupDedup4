package xjtu.dedup.multithread;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.servers.SDFSService;
import org.opendedup.util.BackupDedupLogger;
import org.opendedup.util.OSValidator;
import org.opendedup.util.SDFSLogger;
import org.opendedup.util.date.WeekPolicy;

import xjtu.dedup.DB.IPreparedStatementCallback;
import xjtu.dedup.DB.JDBCTemplate;
import xjtu.dedup.backupRestore.BackRestore;
import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.backupmngt.VolumeInfoParser;
import xjtu.dedup.preprocess.CDCParameter;
import xjtu.dedup.preprocess.PrepareDedup;

import frame.progressbar.*;
import disms.SISStore.client2.*;
import disms.SISStore.server2.*;

public class BackupClient implements IBackupRestoreClient {
	private String backupjobid;
	private String backupClientHostName;
	private int clientPort;
	private String backupServerHostNaem;
	private int serverPort;
	private String backupsrc;
	private WeekPolicy weekp=new WeekPolicy();
	private boolean iscompressed;
	private int compressetype;
	private int encrypttype;
	private int type;
	private String createTime;
	private String backup_volume_name;
	private String old_backup_volume_name;
	private String backup_volume_type;
	private static long backupSize=0l;
	private long filescount=0l;
	private long backupjobfilescount=0l;
	
    private StringBuffer strBuf = null; 
    private static StringBuffer fileNameBuf = new StringBuffer(); 
    private static StringBuffer fileInfoBuf=new StringBuffer();
    private static StringBuffer filesbuf=new StringBuffer();// 备份文件信息暂存，为写入数据库做准备
    static final int buffer = 2048; 
    static int c=0;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
    String currentDate = sdf.format(new Date()); 
    
    private  BackRestore bR=null;
    private  PrepareDedup pd = new PrepareDedup();
    
    private  progressWindow progressWindow;
    //抽样概率选择自学习文件集
    Random rd = new Random();
    int probality = 4;
    
    ShutdownHook shutdownHook = null;
    private static SDFSService sdfsService=null;
    
    public BackupClient(){
    	
    }
    
	public BackupClient(String backupClientHostName,int clientPort,String backupServerHostName,int serverPort,
			String backupsrc,int compressetype,int encrypttype,int type,String createTime,String backup_volume_name
			) throws SQLException{
		this.backupClientHostName=backupClientHostName;
		this.clientPort=clientPort;
		this.backupServerHostNaem=backupServerHostName;
		this.serverPort=serverPort;
		this.backupsrc=backupsrc;
		//this.iscompressed=iscompressed;
		this.compressetype=compressetype;
		this.encrypttype=encrypttype;
		this.type=type;
		this.createTime=createTime;
		this.backup_volume_name=backup_volume_name;
		System.out.println("start sdfs service ......");
		initsdfs();
		this.dojob();		
	}
	public BackupClient(String backupjobid,String backupClientHostName,int clientPort,String backupServerHostName,int serverPort,
			String backupsrc,WeekPolicy wp,String createTime,String backup_volume_name,long length,long filescount) throws SQLException{
		this.backupjobid=backupjobid;
		this.backupClientHostName=backupClientHostName;
		this.clientPort=clientPort;
		this.backupServerHostNaem=backupServerHostName;
		this.serverPort=serverPort;
		this.backupsrc=backupsrc;
		//this.iscompressed=iscompressed;
		this.weekp=wp;
		this.createTime=createTime;
		this.backup_volume_name=backup_volume_name;
//		BackupJob.backup_volume_name = this.backup_volume_name;
		backupSize=length;
		this.filescount=filescount;
	}

    int backuptype = -1;
    public boolean execBackupJob() throws Exception{
		backuptype = weekp.getbackupType();
		boolean isstop = false;
//		if(!BackupJob.backup_volume_name.equals(this.backup_volume_name)){
	    	BackupJob.backupjobid=this.backupjobid;
			BackupJob.backupClientHostName=this.backupClientHostName;
			BackupJob.creatTime=this.createTime;
			BackupJob.backupjobguid=this.backupClientHostName+this.backupjobid+this.createTime;
			BackupJob.backup_volume_name = this.backup_volume_name;
			this.old_backup_volume_name = BackupJob.backup_volume_name;	
//		else{
//			removehook();
//		}
		if(backuptype !=-1){
			initsdfs();	
//			preProcessBackupJob();
			new Thread(){
				public void run(){
//					if(Main.iscdc || Main.issw)
//						initsdfs();
					BackupDedupLogger.getbackupLog().info("start backup......"+" backup job id : "+backupjobid
							+" backup time : "+createTime+" backup volume name : "+backup_volume_name+"");
			    	/**/int volumetype=VolumeInfoParser.getTypeOfVolume(backup_volume_name);
					progressWindow = new progressWindow();
					progressWindow.setMax((int)backupSize/1024);
					progressWindow.setProgress(0);
					progressWindow.setVisible(true);
			    	long start=System.currentTimeMillis();
			    	if(volumetype==-1)
			    		BackupDedupLogger.getrunstateLog().info("The volume of"+backup_volume_name+"isn't exist.");
			    	if(volumetype==3){
			        	System.out.println("SIS task.....");
			        	try {
							new SISTaskBackup1(createTime,backupsrc,progressWindow);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			        	System.out.println("SIS end.....");		
			    	}else{
			    		bR=new BackRestore(volumetype);
				    	backup(backupsrc);
				    	bR.closedb();//close the bekerley DB
				    }	
			    	
			    	long end=System.currentTimeMillis();
			    	BackupDedupLogger.getbackupLog().info("Backup Job use "+(end-start));
//				    if(!Main.issis)
//						 stopsdfs();
			    	progressWindow.dispose();
			    	BackupDedupLogger.getbackupLog().info("Backup Job End !");
				}
			}.start();	
			 if(!Main.issis )
				 stopsdfs();
			return true;
		}else{
			JOptionPane.showMessageDialog(null, "The current backup job was made on "+weekp.getWeekofDayWhenBackup()+" and should be backuped on that day!\nToday is "+getDayOfWeek()+".");
			return false;
		}		
//		preProcessBackupJob();
//		if(Main.iscdc || Main.issw)
//			initsdfs();
//    	SDFSLogger.getLog().info("Start backup the uncompressed files......");
//    	System.out.println("Start backup the uncompressed files......");
//    	long start=System.currentTimeMillis();
//    	/**/int volumetype=VolumeInfoParser.getTypeOfVolume(backup_volume_name);
//    	if(volumetype==-1)
//    		SDFSLogger.getLog().info("The volume of"+backup_volume_name+"isn't exist.");
//    	if(volumetype==3){
//        	System.out.println("SIS task.....");
//        	new SISTaskBackup1(BackupJob.creatTime,this.backupsrc);
//        	System.out.println("SIS end.....");		
//    	}else{
//    		bR=new BackRestore(volumetype);
//	    	backup(this.backupsrc);
//	    	bR.closedb();//close the bekerley DB
//	    }	
//    	long end=System.currentTimeMillis();
//	    SDFSLogger.getLog().info("Backup Job use "+(end-start));
//	    System.out.println("Backup Job use "+(end-start));
//	    if(!Main.issis)
//			 stopsdfs();
//    	progressWindow.dispose();
//	    SDFSLogger.getLog().info("Backup job end!");
//	    System.out.println("Backup job end!");
    	
    	
    }
    
	public void preProcessBackupJob(){
		preinitsdfs();
		if(Main.iscdc || Main.issw){
//			System.out.println("Start preprocess backup ......");
			BackupDedupLogger.getbackupLog().info("preprocess backup ......");
	    	prebackup(this.backupsrc);
	    	pd.setCDC_MandCDC_R();
//	    	System.out.println("CDC_M: "+CDCParameter.CDC_M+ " CDC_R: "+CDCParameter.CDC_R);
	    	BackupDedupLogger.getbackupLog().info("CDC_M: "+CDCParameter.CDC_M+ " CDC_R: "+CDCParameter.CDC_R);
	    	prestopsdfs();
	    	BackupDedupLogger.getbackupLog().info("Finish preprocess backup.");
//	    	System.out.println("Finish preprocess backup.");
		}	
	}
    
    /*
     * 获取备份文件元数据信息
     * */
    public void getBackupFilesInfo(){
        try { 
            String backupsrc=this.backupsrc;
            String[] src = backupsrc.split("\n"); 
            for (int i = 0; i < src.length; i++) { 
                File file = new File(src[i]); 
                saveBackupFilesInfo(file); 
            } 
        } 
        catch (Exception e) { 
            System.out.println(e); 
        } 
    }
    /*
     * 压缩多个文件或目录
     * */
    public void compress(){
        try { 
            String destDir = BackupJob.backupsrc;
            //压缩即将备份的文件
            String[] destDirSrc=destDir.split("\n");
            for(int i=0;i<destDirSrc.length;i++)
            {
            	compress(destDirSrc[i]);
            }
            }catch (Exception e) {
				// TODO: handle exception
            	e.printStackTrace();
			}
    }
    public String compress1(){
        String zipfilename=null;
        try { 
            String destDir = BackupJob.backupsrc;
            //压缩即将备份的文件
            String[] destDirSrc=destDir.split("\n");
            for(int i=0;i<destDirSrc.length;i++)
            {
            	zipfilename=compress1(destDirSrc[i]);
            }
            }catch (Exception e) {
				// TODO: handle exception
            	e.printStackTrace();
			}
			return zipfilename;
    }
    /*
     * 压缩单个文件或目录
     **/
    public void compress(String zipfile) { 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
        String currentDate = sdf.format(new Date()); 
        boolean flag=false; 
        try { 
            String zipDir =BackupJob.zipdir;
            String zip=OSValidator.getBackupFilesMetaDataPath();
            File filein=new File(zipfile); 
            File[] file=filein.listFiles(); 
            for(int i=0;i<file.length;i++){ 
                if(file[i].isDirectory()||file[i].length()>0){ 
                    flag=true; 
                    break; 
                } 
            } 
            if(flag==true){ 
            File fileout = new File(zipDir); 
            if(fileout.exists()==false){ 
                fileout.mkdir(); 
            } 
            String strAbsFilename = fileout.getAbsolutePath() + File.separator + currentDate +"_"+filein.getName()+ "_backup.zip"; 
            OutputStream os = new FileOutputStream(strAbsFilename); 
            BufferedOutputStream bs = new BufferedOutputStream(os); 
            ZipOutputStream zo = new ZipOutputStream(bs); 
            zip(zipfile, new File(zipfile), zo, true, true); 
            zo.closeEntry(); 
            zo.close(); 
            saveAll(zip); 
            } 
        } 
        catch (Exception e) { 
            e.printStackTrace(); 
        } 

    } 
    public String compress1(String zipfile) { 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
        String currentDate = sdf.format(new Date()); 
        boolean flag=false; 
        File filein=null;
        try { 
            String zipDir =BackupJob.zipdir;
            String zip=OSValidator.getBackupFilesMetaDataPath();
            filein=new File(zipfile); 
            File[] file=filein.listFiles(); 
            for(int i=0;i<file.length;i++){ 
                if(file[i].isDirectory()||file[i].length()>0){ 
                    flag=true; 
                    break; 
                } 
            } 
            if(flag==true){ 
            File fileout = new File(zipDir); 
            if(fileout.exists()==false){ 
                fileout.mkdir(); 
            } 
            String strAbsFilename = fileout.getAbsolutePath() + File.separator + currentDate +"_"+filein.getName()+ "_backup.zip"; 
            OutputStream os = new FileOutputStream(strAbsFilename); 
            BufferedOutputStream bs = new BufferedOutputStream(os); 
            ZipOutputStream zo = new ZipOutputStream(bs); 
            zip(zipfile, new File(zipfile), zo, true, true); 
            zo.closeEntry(); 
            zo.close(); 
            saveAll(zip); 
            } 
        } 
        catch (Exception e) { 
            e.printStackTrace(); 
        } 
        return currentDate+"_"+filein.getName()+"_backup.zip";
    } 
    
    public void zip(String path, File basePath, ZipOutputStream zo, boolean isRecursive, boolean isOutBlankDir) 
    throws Exception { 
    File inFile = new File(path); 
    File[] files = new File[0]; // 什么意思？
    if (inFile.isDirectory()) { 
        files = inFile.listFiles(); 
    } 
    else if (inFile.isFile()) { 
        files = new File[1]; 
        files[0] = inFile; 
    } 
    byte[] buf = new byte[1024]; 
    int len; 
	System.out.println("++++++ "+files.length);
    for (int i = 0; i < files.length; i++) { 
        String pathName = ""; 
        if (basePath != null) { 
            if (basePath.isDirectory()) { 
                pathName = files[i].getPath().substring(basePath.getPath().length() + 1); 
            } 
            else { 
                pathName = files[i].getPath().substring(basePath.getParent().length() + 1); 

            } 
           // System.out.println("-------"+pathName);
        } 
        else { 
            pathName = files[i].getName(); 
        } 
        if (files[i].isDirectory()) { 
            if (isOutBlankDir && basePath != null) { 
                zo.putNextEntry(new ZipEntry(pathName + "/")); //可以使空目录也放进去 
            } 
            if (isRecursive) { 
                zip(files[i].getPath(), basePath, zo, isRecursive, isOutBlankDir); 
            } 
        } 
        else { 
            FileInputStream fin = new FileInputStream(files[i]); 
            zo.putNextEntry(new ZipEntry(pathName)); 
            while ((len = fin.read(buf)) > 0) { 
                zo.write(buf, 0, len); 
            } 
            fin.close(); 
        } 
    } 

} 

    /*
     * 初始化sdfs卷，设置备份数据存储位置
     * */
    public void initsdfs(){
    	String volumeConfigFile =OSValidator.getConfigPath()+BackupJob.backup_volume_name+"-volume-cfg.xml";//;
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
     * 初始化sdfs卷，设置备份数据存储位置
     * */
    public void preinitsdfs(){
    	String volumeConfigFile =OSValidator.getConfigPath()+this.backup_volume_name+"-volume-cfg.xml";//;
		String routingConfigFile = null;//"C:\\Program Files\\sdfs\\etc\\routing-config.xml";   //"E:\\deduptest\\etc\\routing-config.xml";
		sdfsService = new SDFSService(volumeConfigFile,
				routingConfigFile);

		try {
			sdfsService.prestart();
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
    	shutdownHook = new ShutdownHook(sdfsService);
    	Runtime.getRuntime().addShutdownHook(shutdownHook);
//    	sdfsService.stopbackupservice();
    }
    
    /*
     * 删除相同钩子
     * */
    public void removehook(){
    	Runtime.getRuntime().removeShutdownHook(shutdownHook);
//    	sdfsService.stopbackupservice();
    }
    
    public void prestopsdfs(){
		try {
			sdfsService.prestop();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("Exiting because " + e1.toString());
			System.exit(-1);
		}

    }
    
    public void mkdir() { 
        try { 
            String dest=BackupJob.backupdest;
            String zipdir=BackupJob.zipdir;
            File f = new File(dest); 
            File zipFile = new File(zipdir); 
            if (f.exists() == false) { 
                f.mkdirs(); 
            } 
            if (zipFile.exists() == false) { 
                f.mkdirs(); 
            } 
        } 
        catch (Exception e) { 
            System.out.println(e); 
        } 
    } 
    
    public void copy() { 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss"); 
        String currentDate = sdf.format(new Date()); 
        try { 
            String backupsrc=BackupJob.backupsrc;
            String[] src = backupsrc.split(","); 
            String dest = BackupJob.backupdest; 
            for (int i = 0; i < src.length; i++) { 
                File file = new File(src[i]); 
                getAllFiles(file, currentDate, dest, src[i],BackupJob.type); 
            } 
        } 
        catch (Exception e) { 
            System.out.println(e); 
        } 
    } 
    
    public void delete() { 
        try { 
            String dest = BackupJob.backupdest; 
            File f = new File(dest); 
            if (f.exists()) { 
                File[] allFiles = f.listFiles(); 
                for (int i = 0; i < allFiles.length; i++) { 
                    if (allFiles[i].isFile()) { 
                        allFiles[i].delete(); 
                    } 
                    else { 
                        deleteFolder(allFiles[i]); 
                    } 

                    System.out.println(allFiles[i].getAbsolutePath()); 
                } 
            } 
        } 
        catch (Exception e) { 
            System.out.println(e); 
        } 
    } 

    public void deleteFolder(File folder) { 
        String childs[] = folder.list(); 
        if (childs == null || childs.length <= 0) { 
            folder.delete(); 
        } 
        for (int i = 0; i < childs.length; i++) { 
            String childName = childs[i]; 
            String childPath = folder.getPath() + File.separator + childName; 
            File filePath = new File(childPath); 
            if (filePath.exists() && filePath.isFile()) { 
                filePath.delete(); 
            } 
            else if (filePath.exists() && filePath.isDirectory()) { 
                deleteFolder(filePath); 
            } 
        } 

        folder.delete(); 
    } 

    public String getAllFiles(File dir, String modifiedDate, String dest, String src,int type) throws Exception { 
        strBuf = new StringBuffer(); 
        searchDirectory(dir, modifiedDate, dest, src,type); 
        save(dest); 
        return strBuf.toString(); 
    } 
/*
 * 增量文件复制，只备份修改的文件
 * */
    public String searchDirectory(File dir, String modifiedDate, String dest,String src,int type) throws Exception { 
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
        File[] dirs = dir.listFiles(); 
        System.out.println(dirs.length);
        File fileout = new File(dest); 
        if (!fileout.exists()) { 
            fileout.mkdirs(); 
        } 
        for (int i = 0; i < dirs.length; i++) { 
        	System.out.println(dirs[i]);
            if (dirs[i].isDirectory()) { 
                searchDirectory(dirs[i], modifiedDate, dest, src,type); 
            } 
            else { 
                try { 
                     if (type==0&&!(df.format(new Date(dirs[i].lastModified())).equals(df.format(df.parse(modifiedDate))))) { 
                    	System.out.println("Full backuping......");
                        File file = new File(dirs[i].toString()); 
                        FileInputStream fileIn = new FileInputStream(file); 
                        String destDir = 
                            file.getAbsolutePath().substring(src.length(), file.getAbsolutePath().lastIndexOf("\\")); 
                        File temp = new File(dest + destDir); 
                        if (!temp.exists()) { 
                            temp.mkdirs(); 
                        } 
                        String strAbsFilename = fileout.getAbsolutePath() + destDir + File.separator + file.getName(); 
                        FileOutputStream fileOut = new FileOutputStream(strAbsFilename); 
                        String pathName = strAbsFilename.substring(dest.length()); 
                        fileNameBuf.append(pathName); 
                        fileNameBuf.append("\r\n"); 
                        byte[] br = new byte[1024]; 
                        while (fileIn.read(br) > 0) { 
                            fileOut.write(br); 
                            fileOut.flush(); 
                        } 
                        fileIn.close(); 
                    } 
                   else if(type==1&&(df.format(new Date(dirs[i].lastModified())).equals(df.format(df.parse(modifiedDate))))){
                	   System.out.println("incremental backuping......");
                       File file = new File(dirs[i].toString()); 
                       FileInputStream fileIn = new FileInputStream(file); 
                       String destDir = 
                           file.getAbsolutePath().substring(src.length(), file.getAbsolutePath().lastIndexOf("\\")); 
                       File temp = new File(dest + destDir); 
                       if (!temp.exists()) { 
                           temp.mkdirs(); 
                       } 
                       String strAbsFilename = fileout.getAbsolutePath() + destDir + File.separator + file.getName(); 
                       FileOutputStream fileOut = new FileOutputStream(strAbsFilename); 
                       String pathName = strAbsFilename.substring(dest.length()); 
                       fileNameBuf.append(pathName); 
                       fileNameBuf.append("\r\n"); 
                       byte[] br = new byte[1024]; 
                       while (fileIn.read(br) > 0) { 
                           fileOut.write(br); 
                           fileOut.flush(); 
                       } 
                       fileIn.close(); 
                   }
                   else if(type==2){
                	   System.out.println("different backuping......");
                	   
                   }
                } 
                catch (Exception e) { 
                    e.printStackTrace(); 
                } 
            } 
        } 
        return strBuf.toString(); 
    } 
    
    public void saveBackupFilesInfo(File dir){
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
    	if(dir.isFile())
    	{
    		 File file = dir;
             String filepath=file.getAbsolutePath();
             String filename=file.getName();
             long filesize=file.length();
             long lastmodified=file.lastModified();
             fileInfoBuf.append("FilePath:"+filepath+" FileName:"+filename+" FileSize:"+filesize+" LastModified:"+lastmodified+" BackupCreateTime:"+df.format(new Date()));
             fileInfoBuf.append("\r\n");
             if(c==0){
              filesbuf.append(filepath+","+filename+","+filesize+","+df.format(lastmodified)+","+df.format(new Date()));
             }
             else{
             	filesbuf.append("~"+filepath+","+filename+","+filesize+","+df.format(lastmodified)+","+df.format(new Date()));
             }
             c++;
             backupSize+=filesize;
    	}
    	else{//若为目录，则列出目录下所有文件
        File[] dirs = dir.listFiles(); 
        for (int i = 0; i < dirs.length; i++) { 
        	System.out.println(dirs[i]);
            if (dirs[i].isDirectory()) { 
                saveBackupFilesInfo(dirs[i]); 
            } 
            else { 
                        File file = new File(dirs[i].toString()); 
                        String filepath=file.getAbsolutePath();
                        String filename=file.getName();
                        long filesize=file.length();
                        long lastmodified=file.lastModified();
                        fileInfoBuf.append("FilePath:"+filepath+" FileName:"+filename+" FileSize:"+filesize+" LastModified:"+lastmodified+" BackupCreateTime:"+df.format(new Date()));
                        fileInfoBuf.append("\r\n");
                        if(c==0){
                         filesbuf.append(filepath+","+filename+","+filesize+","+df.format(lastmodified)+","+df.format(new Date()));
                        }
                        else{
                        	filesbuf.append("~"+filepath+","+filename+","+filesize+","+df.format(lastmodified)+","+df.format(new Date()));
                        }
                        c++;
                        backupSize+=filesize;
            }
        }
    	}
    }
    //以 1/4 概率选择自学习文件集
    public void prebackup(String backupsrcs){
   	 try { 
   		 	String backupsrc=backupsrcs;
            String[] src = backupsrc.split("\n"); 
            for (int i = 0; i < src.length; i++) { 
                File file = new File(src[i]); 
                if(file.isFile())
                {
               	 //changed on 2012.10.26
                	//单个文件不需要进行抽样
                	pd.selfLearningCDCDedup(file);
                }else{
               	// doRecursivebackupFiles(file); 
               	 	preparedoIteratorBackupFiles(file);
                }
            } 
        } 
        catch (Exception e) { 
            System.out.println("PreProcess Backup Failure: "+e); 
        } 
   }
    
    public void backup(String backupsrcs){

    	 try { 
    		 String backupsrc=backupsrcs;
             String[] src = backupsrc.split("\n"); 
             for (int i = 0; i < src.length; i++) { 
                 File file = new File(src[i]); 
                 if(file.isFile())
                 {
                	 //changed on 2012.10.26
                	 bR.backup(file, backupjobfilescount);
                	 BackupDedupLogger.getbackupLog().info("number "+backupjobfilescount+":"+file.getAbsolutePath());
                	 progressWindow.setCurrentFile(file.getAbsolutePath());
 					 progressWindow.setProgress(progressWindow.getProgress() + (int)Math.round(FileUtils.sizeOf(file)/1024));
//             			  try {
//             				  Runtime.getRuntime().exec("attrib -A "+file.getAbsolutePath());
//             			  } catch (IOException e) {
//         				// TODO Auto-generated catch block
//         				e.printStackTrace();
//         				}//清除文件归档属性
 					
         			backupjobfilescount++;
                 }else{
                	// doRecursivebackupFiles(file); 
                	 doIteratorBackupFiles(file);
                 }
             } 
         } 
         catch (Exception e) { 
             System.out.println("Backup Failure: "+e); 
         } 
         BackupJob.filesCount=backupjobfilescount;
         backupjobfilescount=0;
    }
  //非递归广度优先算法 进行遍历文件备份
    private void doIteratorBackupFiles(File rootFile) throws Exception {  
        int level = 1;  
        LinkedList<WrapperFileNodeLevel> list = new LinkedList<WrapperFileNodeLevel>();  
         File[] childs = rootFile.listFiles();  
         if (childs != null) {  
             for (File child : childs) { // 1.先添加第1层的子节点到迭代列表里  
                list.add(new WrapperFileNodeLevel(child, level + 1));  
            }  
        }  
        WrapperFileNodeLevel wrap;  
         while (!list.isEmpty()){ // 2. 开始迭代  
             wrap = list.removeFirst(); // 移除并返回此列表的第一个元素  
             if (!wrap.file.isDirectory()) {  
            	 try { 
                     	Calendar   calendar   =   Calendar.getInstance(); 
                     	if(backuptype == 0){//凡是备份的文件都取消其归档属性
                     //		SDFSLogger.getLog().info("Full backuping......");
                     		//changed on 2012.10.26
									// TODO Auto-generated method stub
									progressWindow.setCurrentFile(wrap.file.getAbsolutePath());
		         					progressWindow.setProgress(progressWindow.getProgress() + (int)Math.round(FileUtils.sizeOf(wrap.file)/1024));								                   		
                     		bR.backup(wrap.file, backupjobfilescount);
                     		BackupDedupLogger.getbackupLog().info("number "+backupjobfilescount+":"+wrap.file.getAbsolutePath());
                     	//	SDFSLogger.getLog().info("FilePath:"+wrap.file.getAbsolutePath()+" FileSize:"+wrap.file.length()+" LastModified:"+wrap.file.lastModified());
                     	//	Runtime.getRuntime().exec("attrib -A "+wrap.file.getAbsolutePath());//清除文件归档属性
                     	}
                     	else if(backuptype == 1){
                     		//凡是上一次备份后修改过的文件都进行备份
                     	//	SDFSLogger.getLog().info("Incremental backuping......");
                     		if(sdf.format(new Date(wrap.file.lastModified())).equals(currentDate))
                     		{
    									// TODO Auto-generated method stub
    									progressWindow.setCurrentFile(wrap.file.getAbsolutePath());
    		         					progressWindow.setProgress(progressWindow.getProgress() + (int)Math.round(FileUtils.sizeOf(wrap.file)/1024));								
                     			bR.backup(wrap.file, backupjobfilescount);
                     			BackupDedupLogger.getbackupLog().info("number "+backupjobfilescount+":"+wrap.file.getAbsolutePath());
                     		//	SDFSLogger.getLog().info("FilePath:"+wrap.file.getAbsolutePath()+" FileSize:"+wrap.file.length()+" LastModified:"+wrap.file.lastModified());
                     		}
                     	//	Runtime.getRuntime().exec("attrib -A "+dirs[i].getAbsolutePath());//清除文件归档属性
                     	}
                     	else if(backuptype == 2)
                     	{//凡是存在归档属性的文件都进行备份
                     	//	SDFSLogger.getLog().info("Different backuping......");
                     		if(IsArchive(wrap.file.getAbsolutePath()))//该文件是归档文件
                     		{
//                     			final File file = wrap.file;
//                         		SwingUtilities.invokeLater(new Runnable(){
//    								public void run() {
//    									// TODO Auto-generated method stub
//    									progressWindow.setCurrentFile(file.getAbsolutePath());
//    		         					progressWindow.setProgress(progressWindow.getProgress() + (int)Math.round(FileUtils.sizeOf(file)/1024));								
//    								}                    			
//                         		}); 
                     			progressWindow.setCurrentFile(wrap.file.getAbsolutePath());
	         					progressWindow.setProgress(progressWindow.getProgress() + (int)Math.round(FileUtils.sizeOf(wrap.file)/1024));								
                     			bR.backup(wrap.file, backupjobfilescount);
                     			BackupDedupLogger.getbackupLog().info("number "+backupjobfilescount+":"+wrap.file.getAbsolutePath());
                     			//	SDFSLogger.getLog().info("FilePath:"+wrap.file.getAbsolutePath()+" FileSize:"+wrap.file.length()+" LastModified:"+wrap.file.lastModified());
                     		}
                     	}
                    } 
                    catch (Exception e) { 
                        e.printStackTrace(); 
                    } 
                    backupjobfilescount++;
             }  
            childs = wrap.file.listFiles();  
             if (childs != null) {  
                 for (File child : childs) {  
                      list.add(new WrapperFileNodeLevel(child, wrap.level + 1)); // 3.有子节点则加入迭代列表  
                 }  

              }  

          }    
      }  
  
  //非递归广度优先算法 进行遍历文件备份
    private void preparedoIteratorBackupFiles(File rootFile) throws Exception {  
        int level = 1;  
        LinkedList<WrapperFileNodeLevel> list = new LinkedList<WrapperFileNodeLevel>();  
         File[] childs = rootFile.listFiles();  
         if (childs != null) {  
             for (File child : childs) { // 1.先添加第1层的子节点到迭代列表里  
                list.add(new WrapperFileNodeLevel(child, level + 1));  
            }  
        }  
        WrapperFileNodeLevel wrap;  
         while (!list.isEmpty()){ // 2. 开始迭代  
             wrap = list.removeFirst(); // 移除并返回此列表的第一个元素  
             if (!wrap.file.isDirectory()) {  
            	 try { 
                     	Calendar   calendar   =   Calendar.getInstance(); 
                     	if((weekp.Monday==0 && calendar.get(Calendar.DAY_OF_WEEK)==2)||(weekp.Tuesday==0&&calendar.get(Calendar.DAY_OF_WEEK)==3)
                     		||(weekp.Wednesday==0&&calendar.get(Calendar.DAY_OF_WEEK)==4)
                     			||(weekp.Thursday==0&&calendar.get(Calendar.DAY_OF_WEEK)==5)||
                     			(weekp.Friday==0&&calendar.get(Calendar.DAY_OF_WEEK)==6)||(weekp.Saturday==0&&
                     					calendar.get(Calendar.DAY_OF_WEEK)==7)
                     			||(weekp.Sunday==0 && calendar.get(Calendar.DAY_OF_WEEK)==1)){//凡是备份的文件都取消其归档属性
                     		//changed on 2012.10.26
                     		if(rd.nextInt(probality) == 1){
                     			pd.selfLearningCDCDedup(wrap.file);
                     		}
                     	}
                     	else if((weekp.Monday==1&&calendar.get(Calendar.DAY_OF_WEEK)==2)||(weekp.Tuesday==1&&calendar.get(Calendar.DAY_OF_WEEK)==3)
                     		||(weekp.Wednesday==1&&calendar.get(Calendar.DAY_OF_WEEK)==4)
                     			||(weekp.Thursday==1&&calendar.get(Calendar.DAY_OF_WEEK)==5)||
                     			(weekp.Friday==1&&calendar.get(Calendar.DAY_OF_WEEK)==6)||(weekp.Saturday==1&&
                     					calendar.get(Calendar.DAY_OF_WEEK)==7)
                     			||(weekp.Sunday==1&&calendar.get(Calendar.DAY_OF_WEEK)==1)){
                     		//凡是上一次备份后修改过的文件都进行备份
                     	//	SDFSLogger.getLog().info("Incremental backuping......");
                     		if(sdf.format(new Date(wrap.file.lastModified())).equals(currentDate))
                     		{
                     			if(rd.nextInt(probality) == 1){
                         			pd.selfLearningCDCDedup(wrap.file);
                         		} 
                     		}        
                     	}
                     	else if((weekp.Monday==2&&calendar.get(Calendar.DAY_OF_WEEK)==2)||(weekp.Tuesday==2&&calendar.get(Calendar.DAY_OF_WEEK)==3)
                     		||(weekp.Wednesday==2&&calendar.get(Calendar.DAY_OF_WEEK)==4)
                     			||(weekp.Thursday==2&&calendar.get(Calendar.DAY_OF_WEEK)==5)||
                     			(weekp.Friday==2&&calendar.get(Calendar.DAY_OF_WEEK)==6)||(weekp.Saturday==2&&
                     					calendar.get(Calendar.DAY_OF_WEEK)==7)
                     			||(weekp.Sunday==2&&calendar.get(Calendar.DAY_OF_WEEK)==1))
                     	{//凡是存在归档属性的文件都进行备份
                     		if(IsArchive(wrap.file.getAbsolutePath()))//该文件是归档文件
                     		{
                     			if(rd.nextInt(probality) == 1){
                         			pd.selfLearningCDCDedup(wrap.file);
                         		}
                     		}
                     	}
                    } 
                    catch (Exception e) { 
                        e.printStackTrace(); 
                    } 
             }  
            childs = wrap.file.listFiles();  
             if (childs != null) {  
                 for (File child : childs) {  
                      list.add(new WrapperFileNodeLevel(child, wrap.level + 1)); // 3.有子节点则加入迭代列表  
                 }  

              }  

          }    
      }  
    
    /*
     * 把备份文件元数据信息写入到backupmetadata数据库中，backupfiles表记录着这些备份文件信息
     * */

    public void writetobackupfiles()throws SQLException{	
    	JDBCTemplate jt=new JDBCTemplate();
    	String sql="insert into backupmetafiles(FilePath,FileName,FileSize,LastModified,BackupCreatTime) values(?,?,?,?,?)";
    	int count=(Integer)jt.execute(new IPreparedStatementCallback() {
			
			@Override
			public Object doInPreparedStatement(PreparedStatement pstmt)
					throws RuntimeException, SQLException {
				// TODO Auto-generated method stub
				int result=0;
		    //	String filepath,filename,filesize,lastModified,backupCreateTime;
		    	String str=filesbuf.toString();
		    	String[] ss=str.split("~");
		    	String[] s;
		    	for(int i=0;i<ss.length;i++)
		    	{
		    		s=ss[i].split(",");
		    		pstmt.setString(1, s[0]);
		    		pstmt.setString(2, s[1]);
		    		pstmt.setString(3, s[2]);
		    		pstmt.setString(4, s[3]);
		    		pstmt.setString(5, s[4]);
		            result=pstmt.executeUpdate();
		    	}
				return result;
			}
		},sql);

    }
    
    public void writeBackupConfigToSQL(String backupJobname,String backupSrc,String backupDate,String backupVolume,int compresstype,long length)
    {
    	final String bjobname=backupJobname;
    	final String bsrc=backupSrc;
    	final String bdate=backupDate;
    	final String bvolume=backupVolume;
    	final long backupSize=length;
    	final String username=System.getProperty("user.name");
    	final int comptype=compresstype;
    	JDBCTemplate jt=new JDBCTemplate();
    	String sql="insert into backupjobrecord(backupjobID,backupSrc,backupDate,backupSize,backupVolume,userName,compType) values(?,?,?,?,?,?,?)";
    	int count=(Integer)jt.execute(new IPreparedStatementCallback() {
			
			@Override
			public Object doInPreparedStatement(PreparedStatement pstmt)
					throws RuntimeException, SQLException {
				// TODO Auto-generated method stub
				int result=0;

		    		pstmt.setString(1,bjobname);
		    		pstmt.setString(2, bsrc);
		    		pstmt.setString(3, bdate);
		    		pstmt.setLong(4, backupSize);
		    		pstmt.setString(5, bvolume);
		    		pstmt.setString(6, username);
		    		pstmt.setInt(7, comptype);
		            result=pstmt.executeUpdate();
				return result;
			}
		},sql);
    }
    public void save(String dest) { 
        try { 
           
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss"); 
            String currentDate = sdf.format(new Date()); 
            File f = new File(dest + "//" + "filelist_"+currentDate+".txt"); 
            FileWriter fout = new FileWriter(f); 
            BufferedWriter buf = new BufferedWriter(fout); 
            buf.write(fileNameBuf.toString()); 
          //  buf.flush();
            buf.close(); 
            fout.close(); 
        } 
        catch (Exception e) { 
            System.out.println(e); 
        } 
    } 
    public void saveAll(String zip) { 
        try {     
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-kk"); 
            String currentDate = sdf.format(new Date()); 
            File file = new File(zip); 
            if(!file.exists())
            {
            	file.mkdirs();
            }
            File f=new File(file+File.separator+currentDate+"filelist.txt");
            FileWriter fout = new FileWriter(f,true); 
            BufferedWriter buf = new BufferedWriter(fout); 
            buf.write("\r\n"); 
            buf.write(currentDate); 
            buf.write("\r\n"); 
            buf.write(fileInfoBuf.toString()); 
            buf.close(); 
            fout.close(); 
        } 
        catch (Exception e) { 
            System.out.println(e); 
        } 
    }
    /*
     * 保存文件元数据信息，写入到本地磁盘
     * windows 下写入默认路径是：C:\Program Files\backupdedup\backupfilesmeta\
     * linux 下写入默认路径是:/etc/backupdedup/backupfilesmeta/
     * */
    public void savefilesmetadata(){
            try {     
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-kk"); 
                String currentDate = sdf.format(new Date()); 
                File dir=new File(OSValidator.getBackupFilesMetaDataPath());
                if(!dir.exists())
                {
                	dir.mkdirs();
                }
                File f = new File(OSValidator.getBackupFilesMetaDataPath()+ File.separator +currentDate+"filelist.txt");            
                FileWriter fout = new FileWriter(f,true); 
                BufferedWriter buf = new BufferedWriter(fout); 
                buf.write("\r\n"); 
                buf.write(currentDate); 
                buf.write("\r\n"); 
                buf.write(fileInfoBuf.toString()); 
                buf.close(); 
                fout.close(); 
            } 
            catch (Exception e) { 
                System.out.println(e); 
            } 
    }
    //get the size of backup files.
    public long getBackupSize(){
    	return backupSize;
    }
    public long getBackupSize(String strsrc){
    	try { 
            String backupsrc=strsrc;
            String[] src = backupsrc.split("\n"); 
            for (int i = 0; i < src.length; i++) { 
                File file = new File(src[i]); 
                getBackupFilesLength(file); 
            } 
        } 
        catch (Exception e) { 
            System.out.println(e); 
        } 
        return backupSize;
    }
    public void getBackupFilesLength(File dir){
    	if(dir.isFile()){
    		 File file = dir;
             long filesize=file.length();
             backupSize+=filesize;
             backupjobfilescount++;
    	}else{//若为目录，则列出目录下所有文件
    		File[] dirs = dir.listFiles(); 
    		for (int i = 0; i < dirs.length; i++) { 
    			if (dirs[i].isDirectory()) { 
    				getBackupFilesLength(dirs[i]); 
    			}else{ 
    				File file = new File(dirs[i].toString()); ;
    				long filesize=file.length();
    				backupSize+=filesize;
    				backupjobfilescount++;
    			}
    		}
    	}
    }
    public void initBackupSize(){
    	backupSize=0l;
    }
    
    public long getBackupJobFilesCount(){
    	return backupjobfilescount;
    }
    public void setBackupJobFilesCount(long filesnum){
    	this.backupjobfilescount=filesnum;
    }
	@Override
	public void restore(String filename) {
		// TODO Auto-generated method stub
		
	} 
	
	public boolean IsArchive(String filepath){
		boolean isArchived = false;
		String cmddir="cmd.exe /c dir /aa "+filepath;
		try {
			Process dir_proc=Runtime.getRuntime().exec(cmddir);
			BufferedReader br=new BufferedReader(new InputStreamReader(dir_proc.getInputStream()));
			for(int i=0;i<6;i++)
				br.readLine();
			String strbuff=br.readLine();
			if(!strbuff.isEmpty())
				isArchived=true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isArchived;
		
	}
	
	
	 private class WrapperFileNodeLevel {  
	        final File file;  
	        final int level;  
	        /**  
	         * @param file  
	         * @param level  
	        */ 
	       WrapperFileNodeLevel(File file, int level) {  
	           this.file = file;  
	           this.level = level;  
	        }  
	   }


	@Override
	public void backup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doRecursivebackupFiles(File dir) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dojob() throws SQLException {
		// TODO Auto-generated method stub
		
	}  
    public String getDayOfWeek(){
    	Calendar   calendar   =   Calendar.getInstance(); 
    	int index = calendar.get(Calendar.DAY_OF_WEEK);
    	String dayofweek = null;
    	if(index == 1){
    		dayofweek = "Sunday";
    	}else if(index == 2){
    		dayofweek = "Monday";
    	}else if(index == 3){
    		dayofweek = "Tuesday";
    	}else if(index == 4){
    		dayofweek = "Wednsday";
    	}else if(index == 5){
    		dayofweek = "Thursday";
    	}else if(index == 6){
    		dayofweek = "Friday";
    	}else if(index == 7){
    		dayofweek ="Saturday";
    	}
    	return dayofweek;
    }
}
