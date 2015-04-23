package xjtu.dedup.DB;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface IPreparedStatementCallback {
	public Object doInPreparedStatement(PreparedStatement pstmt) throws RuntimeException,SQLException; 
}
