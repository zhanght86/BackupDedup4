package disms.SISStore.client2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

import frame.progressbar.progressWindow;

public class SISTaskRestore{
	private String path = "E:\\backupdedup\\SIS\\Task\\";
	private Vector txt = new Vector();
	private Vector strs = new Vector();
	private String user = "Administrator";
	
	public SISTaskRestore(String time,String desFolder, progressWindow pw) throws IOException{
		String filePath =  org.opendedup.sdfs.Main.volume.getPath()+"\\Task\\"+time+".txt";
		BufferedReader in = null;
		in = new BufferedReader(new FileReader(filePath));
		String line = null;
		try {
			line = in.readLine();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(line != null){
			txt.addElement(line);
			try {
				line = in.readLine();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		SISCLIENT bcp = new SISCLIENT();
		for(int i = 0;i<txt.size();i++){
			String[] tmp = txt.elementAt(i).toString().split("[*]");
			String[] tmp2 = tmp[1].split("[.]");
			String tmp3 = tmp2[0]+'.'+tmp2[1]+'.'+tmp2[2];
			SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd.HH.mm.ss.SSS");      
			Date curDate = new Date(System.currentTimeMillis());//获取当前时间      
			String strDATE = formatter.format(curDate);
			String finalPath = desFolder +'['+strDATE+']'+tmp[2];
			String cmd = user+"*restore*"+tmp[0]+'*'+tmp3+'*'+finalPath;
			String[] L1 = cmd.split("[*]");	
			try {
				bcp.initService(L1);
				//Thread.sleep(200);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			pw.setProgress(pw.getProgress() + i);
		}		
	}
	public static void main(String[] args) throws IOException{
		
//		new SISTaskRestore("2012-12-27_10-07-55","C:\\");
	}
}