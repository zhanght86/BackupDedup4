package disms.SISStore.client2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class SISTaskDelete{
	private String path = "D:\\SIS\\";
	private Vector txt = new Vector();
	private Vector strs = new Vector();
	
	public SISTaskDelete(String taskNO,String user,String time) throws IOException{
		String filePath = path+user+"\\"+"Task\\"+time+"\\"+taskNO+".txt";
		System.out.println(filePath);
		File delFile = new File(filePath);
		delFile.delete();
		SISCLIENT bcp = new SISCLIENT();
		for(int i = 0;i<txt.size();i++){
			String[] tmp = txt.elementAt(i).toString().split("[*]");
			String[] tmp2 = tmp[1].split("[.]");
			String tmp3 = tmp2[0]+'.'+tmp2[1]+'.'+tmp2[2];
			String cmd = user+"*delete*"+tmp[0]+'*'+tmp3;
			String[] L1 = cmd.split("[*]");	
			try {
				bcp.initService(L1);
			//	Thread.sleep(200);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		
	}
	public static void main(String[] args) throws IOException{	
		new SISTaskDelete("task001","NO.002","2012.04.21");
	}
}