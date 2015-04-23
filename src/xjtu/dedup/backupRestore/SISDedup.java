package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.IOException;

import xjtu.dedup.berkeleyDB.BerkeleyDB;
import xjtu.dedup.methodinterf.DedupMethod;
import xjtu.dedup.restoremngt.RestoreJob;

public class SISDedup implements DedupMethod{

	@Override
	public void dedup(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dedup(BerkeleyDB bdb,File file, long filenum) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restore(BerkeleyDB bdb,String filepath) {
		// TODO Auto-generated method stub
		String time=RestoreJob.backupdate.substring(0, 10).replaceAll("-", ".");
//		try {
//			new SISTaskRestore(RestoreJob.backupJobID,RestoreJob.restoreClientHostName,time,RestoreJob.restorePath);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}	
	}

	@Override
	public void restore(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		
	}

}
