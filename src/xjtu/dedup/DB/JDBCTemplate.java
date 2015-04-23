package xjtu.dedup.DB;
import java.sql.*;
public class JDBCTemplate {
	 public Object execute(IStatementCallback action) {   
	        Connection conn = null;   
	        Statement stmt = null;   
	        Object result = null;          
	        try {   
	            conn=this.getConnection();   
	            conn.setAutoCommit(false);             
	            stmt=conn.createStatement();   
	               
	            //注意这一句   
	            result=action.doInStatement(stmt);   
	               
	            conn.commit();   
	            conn.setAutoCommit(true);              
	        } catch (SQLException e) {   
	            transactionRollback(conn);//进行事务回滚   
	            e.printStackTrace();   
	            throw new RuntimeException(e);   
	        }finally{   
	            this.closeStatement(stmt);   
	            this.closeConnection(conn);   
	        }   
	   
	        return result;   
	    }   
	 public Object execute(IPreparedStatementCallback action,String sql) {   
	        Connection conn = null;   
	        PreparedStatement pstmt = null;   
	        Object result = null;          
	        try {   
	            conn=this.getConnection();   
	            conn.setAutoCommit(false);             
	            pstmt=conn.prepareStatement(sql);
	            //注意这一句   
	            result=action.doInPreparedStatement(pstmt);   
	               
	            conn.commit();   
	            conn.setAutoCommit(true);              
	        } catch (SQLException e) {   
	            transactionRollback(conn);//进行事务回滚   
	            e.printStackTrace();   
	            throw new RuntimeException(e);   
	        }finally{   
	            this.closePreparedStatement(pstmt);   
	            this.closeConnection(conn);   
	        }   
	   
	        return result;   
	    }  
	       
	    /*  
	     * 当发生异常时进行事务回滚  
	     */   
	    private void transactionRollback(Connection conn){   
	        if(conn!=null){   
	            try {   
	                conn.rollback();   
	            } catch (SQLException e) {   
	                // TODO Auto-generated catch block   
	                e.printStackTrace();   
	            }   
	        }   
	           
	    }   
	    //关闭打开的Statement   
	    private void closeStatement(Statement stmt){   
	        if(stmt!=null){   
	            try {   
	                stmt.close();   
	                stmt=null;   
	            } catch (SQLException e) {   
	                e.printStackTrace();   
	            }   
	        }   
	    }   
	    
	    //关闭打开的PreparedStatement   
	    private void closePreparedStatement(PreparedStatement pstmt){   
	        if(pstmt!=null){   
	            try {   
	                pstmt.close();   
	                pstmt=null;   
	            } catch (SQLException e) {   
	                e.printStackTrace();   
	            }   
	        }   
	    }  
	    //关闭打开的Connection    
	    private void closeConnection(Connection conn){   
	        if(conn!=null){   
	            try {   
	                conn.close();   
	                conn=null;   
	            } catch (SQLException e) {   
	                e.printStackTrace();   
	            }   
	        }   
	    }   
	   
	    //取得一个Connection   
	    private Connection getConnection() {           
	        String driver = "com.mysql.jdbc.Driver";   
	        String url = "jdbc:mysql://127.0.0.1/backupmetadata";           
	        Connection conn=null;   
	        try {   
	            Class.forName(driver);   
	            conn = DriverManager.getConnection(url, "root", "123456");   
	        } catch (ClassNotFoundException e) {   
	            e.printStackTrace();   
	        } catch (SQLException e) {   
	            e.printStackTrace();   
	        }   
	        return conn;   
	    }   
	   

}
