package xjtu.dedup.DB;

import java.sql.SQLException;
import java.sql.Statement;

public interface IStatementCallback {
	public Object doInStatement(Statement stmt) throws RuntimeException,SQLException; 
}
