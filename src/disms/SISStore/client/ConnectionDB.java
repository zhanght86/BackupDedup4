package disms.SISStore.client;
import java.sql.DriverManager;   
import java.sql.Connection;   
import java.sql.ResultSet;
import java.sql.SQLException; 
import java.sql.Statement;

public class ConnectionDB{
	
	private static String url = "jdbc:MySQL://127.0.0.1:3306/sis";
	private static String user = "root";
	private static String password = "sa";
	private static Connection conn = null;
	private static ResultSet rs = null;
	private static Statement statement = null;
	private static String sql =null; 
	public static void createConnection(){
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();;
			conn = DriverManager.getConnection(url, user, password);
			if(!conn.isClosed())   
				System.out.println("Succeeded connecting to the Database!");
			statement = conn.createStatement();
		}
		catch(ClassNotFoundException e) {   
			System.out.println("Sorry,can't find the Driver!");   
			e.printStackTrace();   
		}
		catch(SQLException e) {   
			e.printStackTrace();   
		}
		catch(Exception e) {   
			e.printStackTrace();   
		}  
	}
	public static void closeConnection() throws SQLException{
		rs.close();  
		conn.close();
	}
	public static void hasTable(){
		new ConnectionDB();
		ConnectionDB.createConnection();
	}
	
	public static void WriteSpecialFiles(){
		
	}
	
	public static void main(String[] args)
	{
		new ConnectionDB();
		ConnectionDB.createConnection();
	}
}
/*
		
			sql = "select * from global_file";
			rs = statement.executeQuery(sql);
			while(rs.next()) {
				String name = null;
				name = rs.getString("SNAME");
				name = new String(name.getBytes("ISO-8859-1"),"GB2312");
				System.out.println(rs.getString("SNO") + "\t" + name);  
			}  
			rs.close();  
			conn.close();
			
*/