package frame.threads;

import frame.CreateBackupJob;

public class CreateBackupJobThread extends Thread {
	public void run(){
		CreateBackupJob cbj=new CreateBackupJob();
		cbj.setVisible(true);
	}
}
