package xjtu.dedup.backupmngt;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Hashtable;

import org.opendedup.util.date.WeekPolicy;


/*
 * 备份任务相关信息
 * */
public class BackupJob {
	public static String backupjobguid;
	public static String backupjobid;
/*
 * 备份类型
 * 全备份：0；增量备份：1；差异备份：2
 * */
	public static int type=0;
/*
 * 备份状态
 * 就绪：0；进行中：1；完成：2
 * */
	public static int status=0;
	/*
	 * 备份过程中是否压缩
	 * 就绪：0；进行中：1；完成：2
	 * */
	public static boolean iscompressed=true;
	/*
	 * 压缩算法
	 * */
	public static int compresstype=0;
	/*
	 * 加密算法
	 * */
	public static int encrypttype=0;
/*
 * 备份客户端主机名
 * */
	public static String backupClientHostName="127.0.0.1";
/*
 * 备份客户端端口
 * */
	public static int clientPort=3001;
/*
 * 备份数据源（备份源文件路径）
 * */
	public static String backupsrc="";
	/*
	 * 备份数据大小
	 * */
	public static long backupSize=0l;
/*
 * 备份服务器主机名
 * */
	public static String backupServerHostName="0.0.0.0";
/*
 * 备份服务器端口
 * */
	public static int serverPort=3002;
	/*
	 * 备份数据目的地址
	 * */
	public static String backupdest="";
	/*
	 * 备份数据选择的卷
	 * */
	public static String backup_volume_name = null;
	/*
	 * 备份数据压缩路径
	 * */
	public static String zipdir="";
/*
 * 备份任务创建时间
 * */
	public static String creatTime;
	/*
	 * 备份任务开始运行的时间
	 * **/
	public static String lastRun;
/*
 * 备份任务结束时间
 * */
	public static String endTime;
/*
 * 备份线程数
* */
		public static int client_threads;
	/*
	 * 备份策略类
	 * */
		public static WeekPolicy weekp;
/*
 * 备份数据中包括的总文件数
 * */
	public static long filesCount=0l;
/*
 * 备份数据总大小
 * */
	public static long filesSize=0;
/*
 * 备份过程中已完成的文件
 * */
	public static StringBuffer finishedStringbuffer;
/*
 * 备份过程中已完成的文件的大小
 * */
	public static long finishedSize=0;
/*
 * 备份完成的百分比
 * */
	public static double percent=0.0;
/*
 * 备份过程使用时间
 * */
	public static long usedTime;
	/*
	 * 
	 * */
	private Collection<String> hashes;
	/*
	 * 存储备份任务的hash表
	 * */
	private Hashtable<String, String> hashtable; 
	
	public  BackupJob(String backupjobid,String source,String dest_volume,String lastrun,int backup_type,int compress_type,int encrypt_type){
		this.backupjobid=backupjobid;
		this.backupsrc=source;
		this.backup_volume_name=dest_volume;
		this.lastRun=lastrun;
		this.type=backup_type;
		this.compresstype=compress_type;
		this.encrypttype=encrypt_type;	
	}
	/*
	 * 设置备份任务的全局唯一标示符
	 * */
	public static void setbackupjobguid(String jobguid){
		backupjobguid=jobguid;
	}
	/*
	 * 返回备份任务的全局唯一标示符
	 * */
	public String getbackupjobguid(){
		return backupjobguid;
	}
	/*
	 * 返回备份任务名
	 * */
	public String getbackupjobid(){
		return this.backupjobid;
	}
	/*
	 * 返回备份源地址
	 * */
	public String getbackupsrc(){
		return this.backupsrc;
	}
	/*
	 * 返回备份目的地址——卷名
	 * */
	public String getbackupdest(){
		return this.backup_volume_name;
	}
	/*
	 * 返回备份类型：0：全备份 1：增量备份 2：差异备份
	 * */
	public int getbackuptype(){
		return this.type;
	}
	/*
	 * 返回压缩类型：0：zip 1：zip64 2：无压缩方式
	 * */
	public int getcompresstype(){
		return this.compresstype;
	}
	/*
	 * 返回加密类型：0：AES128 1：AES256 0：无加密方式
	 * */
	public int getencrypttype(){
		return this.encrypttype;
	}
	
}

