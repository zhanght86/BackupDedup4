package xjtu.dedup.multithread;
/*
 * backup and restore interface,for polymorphism.
 * */
import java.io.File;
import java.sql.SQLException;
import java.util.zip.ZipOutputStream;

public interface IBackupRestoreClient {
	/*
	 * start backup or restore job.
	 * */
	public abstract void dojob() throws SQLException;
	/*
	 * get meta data of backup files.
	 * */
	public abstract void getBackupFilesInfo();
	/*
	 * compress the backup files
	 * */
	public abstract void compress();
	/*
	 * java.util.zip:zip
	 * */
	public abstract void zip(String path, File basePath, ZipOutputStream zo, boolean isRecursive, boolean isOutBlankDir) throws Exception;
	/*
	 * init sdfs volume.
	 * */
	public abstract void initsdfs();
	/*
	 * make directory
	 * */
	public abstract void mkdir();
	/*
	 * copy file from src to dst.
	 * */
	public abstract void copy();
	/*
	 * delete files.
	 * */
	public abstract void delete();
	/*
	 * delete file folder
	 * */
	public abstract void deleteFolder(File folder);
	/*
	 * get information of backup files.
	 * */
	public abstract String getAllFiles(File dir, String modifiedDate, String dest, String src,int type) throws Exception;
	/*
	 * incremental backup files. 
	 * */
	public abstract String searchDirectory(File dir, String modifiedDate, String dest,String src,int type) throws Exception;
	/*
	 * store backup files information.
	 * */
	public abstract void saveBackupFilesInfo(File dir);
	/*
	 * simple backup file from src to dst.
	 * */
	public abstract void backup();
	/*
	 * restore the backup files.
	 * */
	public abstract void restore(String filename);
	/*
	 * parsing src files and backup them.
	 * */
	public abstract void backup(String backupsrcs);
	/*
	 * backup files to DSE(SDFS volume).
	 * */
	public abstract void doRecursivebackupFiles(File dir);
	/*
	 * Write the meta data of backup files to mysql.
	 * */
	public abstract void writetobackupfiles() throws SQLException;
	
	public abstract void save(String dest);
	
	public abstract void saveAll(String zipdir);
	
}
