package xjtu.dedup.fileutils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.opendedup.sdfs.filestore.MetaFileStore;
import org.opendedup.sdfs.io.DedupFileChannel;
import org.opendedup.sdfs.io.MetaDataDedupFile;
import org.opendedup.util.SDFSLogger;

import xjtu.dedup.backupmngt.BackupJob;

public class FileInfoUtil {
	private static Logger log = SDFSLogger.getLog();
	private static SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");//备份的日期，由于备份是在同一天完成，所以可以采用该方法重设。
	//元数据文件路径：主机名+备份时间+文件路径
	public static String getFilePath(File file){
		StringBuffer filepath = new StringBuffer();
		filepath.append(BackupJob.backupClientHostName).append(File.separator).append(BackupJob.creatTime.substring(0,10).replaceAll("-", ""));
//		String filepath=BackupJob.backupClientHostName+File.separator+BackupJob.creatTime.substring(0,10).replaceAll("-", "");
		String filep=file.getParent();
		String[] path=filep.substring(3).split("\\\\");
		for(int i=0;i<path.length;i++){
//			filepath+=(File.separator+path[i]);
			filepath.append(File.separator).append(path[i]);
		}
		return filepath.toString();
	}
	public static String getSDFSFilePath(File file){
//		String filepath=BackupJob.backupClientHostName+File.separator+BackupJob.creatTime.replaceAll("-", "");
		StringBuffer filepath = new StringBuffer();
		filepath.append(BackupJob.backupClientHostName).append(File.separator).append(BackupJob.creatTime.replaceAll("-", ""));
		String filep=file.getParent();
		String[] path=filep.substring(3).split("\\\\");
		for(int i=0;i<path.length;i++){
//			filepath+=(File.separator+path[i]);
			filepath.append(File.separator).append(path[i]);
		}
		return filepath.toString();
	}
	public static DedupFileChannel getFileChannel(String path ) {
		DedupFileChannel ch = null;
		
			File f = resolvePath(path);
			try {
				MetaDataDedupFile mf = MetaFileStore.getMF(f.getPath());
				ch = mf.getDedupFile().getChannel(-1);
				}
			
			catch (IOException e) {
				log.error("unable to open file" + f.getPath(), e);

				}
		
		return ch;
	}
	
	//设置文件的全局唯一名
	public static String getFileName(File file){
//		String filename;
		StringBuffer filename = new StringBuffer();
//		filename=System.getProperty("user.name")+"_"+sdf.format(new Date())+"_"+file.getAbsolutePath().replaceAll(":\\\\", "_").replace("\\", "_").toLowerCase();
		filename.append(System.getProperty("user.name")).append("_").append(sdf.format(new Date())).append("_").append(file.getAbsolutePath().replaceAll(":\\\\", "_").replace("\\", "_").toLowerCase());
		return filename.toString();
	}
	//恢复文件时保持文件的原文件状态
	public static String parseFileName(String filepath){
		String[] s=filepath.split("_");	
		String ss ="";
		for(int i=3;i<s.length-1;i++)
			ss+=(s[i]+File.separator);
		ss+=s[s.length-1];
		return ss;
	}
	
	private static File resolvePath(String path) {
		//	File _f = new File(Main.volume.getPath() + File.separator + path);
			File _f = new File(path);
			if (!_f.exists()) {
				_f = null;
				log.debug("No such node " + path);

			}
			return _f;
		}
}
