package xjtu.dedup.restoremngt;
/*
 * 恢复任务相关信息
 * */
public class RestoreJob {
	/*
	 * 恢复作业ID
	 * */
	public static String restoreJobID; 
	/*
	 * 要恢复的备份作业ID
	 * */
	public static String backupJobID;
	/*
	 * 恢复客户端主机名
	 * */
	public static String restoreClientHostName;
	/*
	 * 备份作业完成客户端主机名
	 * */
	public static String BackupClientHostName;
	/*
	 * 恢复目的路径
	 * */
	public static String restorePath;
	/*
	 * 要恢复的文件
	 * */
	public static String restoredFileName;
	/*
	 * 恢复作业创建时间
	 * */
	public static String createTime;
	/*
	 * 恢复作业并发数
	 * */
	public static int client_threads;
	/*
	 * 恢复目的卷
	 * */
	public static String volume_name;
	/*
	 * 当前系统登录用户名
	 * */
	public static long backupJobFilesCount;
	/*
	 * 要恢复备份任务的创建时间
	 * */
	public static String backupdate;
	/*
	 * 将恢复备份任务时的压缩类型
	 */
	public static int compType;
}
