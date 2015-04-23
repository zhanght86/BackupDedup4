package xjtu.dedup.DB;

import java.sql.*;

/*
 * 测试连接mysql数据库
 * */
public class Test {
public static void main(String args[]) throws SQLException{
	String host="localhost:3306";
	String database="backupmetadata";
	String user="root";
	String passwd="123456";
    String url="jdbc:mysql://" + host + "/" + database;
	/*
	String sql="select * from backupjobrecord";
	try{
	System.out.println("connecting database......");	
	MySQLDB sqldb=MySQLDB.getInstance(host, database, user, passwd);
	ResultSet rs=sqldb.executeQuery(sql);
	String backupjobname;
	String createTime;
	String srcfiles;
	while(rs.next()){
		backupjobname=rs.getString("backupjobname");
		createTime=rs.getDate("createTime").toString();
		srcfiles=rs.getString("srcfiles");
		System.out.println("backupjobname:"+backupjobname+" createTime:"+createTime+" srcfiles:"+srcfiles);
	}
	sqldb.close();
	}catch (SQLException e) {
		// TODO: handle exception
		e.printStackTrace();
	}*/
   /* try{
    	MySQLDB mysqldb=MySQLDB.getInstance(host, database, user, passwd);//new MySQLDB();
    	//Connection con=mysqldb.getConnetction(url, user, passwd);
    	String filepath,filename,filesize,lastModified,backupCreateTime;
    	filepath="D://etc1//a.txt";
    	filename="a.txt";
    	filesize="123";
    	lastModified=backupCreateTime="2011-12-1";
        	String sql="insert into backupfiles(FilePath,FileName,FileSize,LastModified,BackupCreateTime) values('"+filepath+"','"+filename+"','"+filesize+"','"+lastModified+"','"+backupCreateTime+"')";
        	boolean b=mysqldb.executeUpdate(sql);
        	mysqldb.close();
    	}catch (Exception e) {
    		// TODO: handle exception
    		e.printStackTrace();
    	}
}*/
   /* String[] volume_name=null;
	int c=0;
	String sql = "select * from volumesinfo";
	try{
		System.out.println("connecting database......");	
		MySQLDB sqldb=MySQLDB.getInstance(host, database, user, passwd);
		ResultSet rs=sqldb.executeQuery(sql);
		String vol_name;
		String vol_cap;
		String chunk_size;
		String vol_type;
		while(rs.next()){
			vol_name=rs.getString("volume_name");
			vol_cap=rs.getString("volume_capacity");
			chunk_size=rs.getString("chunk_size");
			vol_type=rs.getString("volume_type");
			System.out.println("vol_name:"+vol_name+" volume_capacity:"+vol_cap+" chunk_size:"+chunk_size+" volume_type:"+vol_type);
		}
		sqldb.close();
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}*/
    String sql = "select * from backupjobrecord";
	//Object[][] backupinfo=new Object[][]{};
	int i=0;
	int rownum=0;
	Object[][] backupinfo=null;
	try{
//		System.out.println("connecting database......");	
		MySQLDB sqldb=MySQLDB.getInstance(host, database, user, passwd);
		ResultSet rs=sqldb.executeQuery(sql);
		String backupid;
		String backupsrc;
		String backupdate;
		String backupsize;
		String backupvolume;
		while(rs.next())
		{
			rownum++;
		}
		rs.close();
		rs=sqldb.executeQuery(sql);
		backupinfo=new Object[rownum][5];
		while(rs.next()){
			backupid=rs.getString("backupjobID");
			backupsrc=rs.getString("backupSrc");
			backupdate=rs.getString("backupDate");
			backupsize=rs.getString("backupSize");
			backupvolume=rs.getString("backupVolume");
			backupinfo[i][0]=backupid;
			backupinfo[i][1]=backupsrc;
			backupinfo[i][2]=backupdate;
			backupinfo[i][3]=backupsize;
			backupinfo[i][4]=backupvolume;
			i++;
		//	System.out.println("backupjobID:"+backupid+" backupSrc:"+backupsrc+" backupDate:"+backupdate+" backupSize:"+backupsize+" backupVolume:"+backupvolume);
		}
		sqldb.close();
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		for(int c=0;c<rownum;c++)
		{
			for(int d=0;d<5;d++)
			 System.out.print(backupinfo[c][d]);
			System.out.println();
		}
}
}
