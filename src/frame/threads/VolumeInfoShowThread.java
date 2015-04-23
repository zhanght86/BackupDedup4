package frame.threads;

import frame.VolumeInfoShow;

public class VolumeInfoShowThread extends Thread{
	public void run(){
		VolumeInfoShow vis=new VolumeInfoShow();
		vis.setVisible(true);
	}
}
