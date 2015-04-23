package xjtu.dedup.DB;

import java.sql.*;

public class MySQLDB {
	//用户名
	private String user = "";
	//密码
	private String password = "";
	//主机
	private String host = "";
	//数据库名字
	private String database = "";
	
	private static MySQLDB db=null;
	/*
	 * private String url="jdbc:mysql://"+host+"/"+"useUnicode=true&characterEncoding=GB2312";
	 */
	private String url ="";
	private Connection con = null;
	private Connection conn=null;
	Statement stmt;
	PreparedStatement pstmt;
	/**
	* 私有的构造方法，保证外部不能实例化，只能由MySQLDB自己能提供自
	* 己的实例，并且只能有一个。
	* 根据主机、数据库名称、数据库用户名、数据库用户密码取得连接。
	* @param host String
	* @param database String
	* @param user String
	* @param password String
	*/
	public MySQLDB(){
		
	}
	private MySQLDB(String host, String database, String user, String password) {
	this.host = host;
	this.database = database;
	this.user = user;
	this.password = password;
	//显示中文
	this.url = "jdbc:mysql://" + host + "/" + database;
	
	try {
	     Class.forName("com.mysql.jdbc.Driver");
	}catch (ClassNotFoundException e) {
	     System.err.println("class not found:" + e.getMessage());
	}
	try {
	  con = DriverManager.getConnection(this.url, this.user, this.password);
	  //连接类型为ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY
	  stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
	  ResultSet.CONCUR_READ_ONLY);
	}catch (SQLException a) {
	System.err.println("sql exception:" + a.getMessage());
	}
	}
	/*
	 * 静态工厂方法，来获得一个MySQLDB实例
	 */

	public static MySQLDB getInstance(String host, String database, String user, String password){
	if(db==null){
	   db=new MySQLDB(host,database,user,password);
	}
	   return db;
	}
    
	/**
	 * 返回取得的连接
	 */
	public Connection getCon() {
	   return con;
	}
	
	public Connection getConnetction(String url,String user,String password)throws SQLException{
		try{
			Class.forName("com.mysql.jdbc.Driver");
			conn=DriverManager.getConnection(url, user, password);
		}catch (ClassNotFoundException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return conn;
	}
	public void excuteUpdate(String sql){
		try{
			pstmt=conn.prepareStatement(sql);
			pstmt.executeUpdate();
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	/**
	 * 执行一条简单的查询语句
	 * 返回取得的结果集
	 */

	public ResultSet executeQuery(String sql) {
	ResultSet rs = null;
	try {
	     rs = stmt.executeQuery(sql);
	}catch (SQLException e) {
	       e.printStackTrace();
	}
	  return rs;
	}

	/**
	 * 执行一条简单的更新语句
	 * 执行成功则返回true
	 */
	@SuppressWarnings("finally")
	public boolean executeUpdate(String sql) {
	boolean v = false;
	try {
	    v = stmt.executeUpdate(sql) > 0 ? true : false;
	}catch (SQLException e) {
	       e.printStackTrace();
	}finally { 
		return v;
	}
	}
    
	public void close(){
		try{
			//pstmt.close();
			stmt.close();
		    con.close();
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println("unable to close formally!");
		}
	}
}
