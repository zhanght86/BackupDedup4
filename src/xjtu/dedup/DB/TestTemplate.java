package xjtu.dedup.DB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestTemplate {
	 
	public static void main(String args[]){
		final SimpleDateFormat sdf=new SimpleDateFormat("yyyy-mm-dd");
		//TestTemplate tt=new TestTemplate();
		//tt.testJdbcTemplate();
		JDBCTemplate jt=new JDBCTemplate();
		String sql="INSERT INTO teacher(name,address,year) VALUES(?,?,?)"; 
	//	String[] objArray={"zhuguofeng1,xian,2011-12-06","zhuguofeng2,xian,2011-12-06","zhuguofeng3,xian,2011-12-06"};
		int count=(Integer)jt.execute(new IPreparedStatementCallback() {
			
			@Override
			public Object doInPreparedStatement(PreparedStatement pstmt)
					throws RuntimeException, SQLException {
				// TODO Auto-generated method stub
				int result = 0;
				for(int i=0;i<3;i++)
				{
				 pstmt.setString(1, "zhuguofeng"+i);
				 pstmt.setString(2, "xian");
				 pstmt.setString(3, sdf.format(new Date()));
				 result=pstmt.executeUpdate();
				}
				return result;
			}
		},sql);
		System.out.println("Count:"+count);
	}
	 public void testJdbcTemplate(){   
	        JDBCTemplate jt=new JDBCTemplate();   
	        /*  
	         * 因为IStatementCallback是一个接口，所以我们在这里直接用一个匿名类来实现  
	         * 如果已经正确的插入啦一条数据的话　，它会正确的返回一个　整数　１   
	         * 而我们这里的stmt是从JdbcTemplate中传过来的  
	         */   
	        int count=(Integer)jt.execute(new IStatementCallback(){   
	            public Object doInStatement(Statement stmt) throws RuntimeException, SQLException {   
	   
	                String sql="INSERT INTO teacher(name,address,year) VALUES('zhuguofeng','xian','2011-12-06')";   
	                int result=stmt.executeUpdate(sql);   
	                return new Integer(result);   
	            }              
	        });        
	        System.out.println("Count: "+count);   
	           
	        /*  
	         * 在这里我们就把刚刚插入的数据取出一个数据，直接输出来  
	         *   
	         */   
	        jt.execute(new IStatementCallback(){   
	            public Object doInStatement(Statement stmt) throws RuntimeException, SQLException {   
	   
	                String sql="SELECT name,address FROM teacher WHERE id=1";   
	                ResultSet rs=null;   
	                rs=stmt.executeQuery(sql);   
	                if(rs.next()){   
	                    System.out.println(rs.getString("name"));   
	                    System.out.println(rs.getString("address"));   
	                }   
	                /*  
	                 * 在这里就直接返回一个1啦，如果你愿意的话，你可以再写一个Person类  
	                 * 在if语句中实例化它,赋值再把它返回  
	                 */   
	                return new Integer(1);   
	            }              
	        });        
	    }   


}
