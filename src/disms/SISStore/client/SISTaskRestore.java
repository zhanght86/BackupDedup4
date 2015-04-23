package disms.SISStore.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class SISTaskRestore{
	private String path = "D:\\SIS\\";
	private Vector txt = new Vector();
	private Vector strs = new Vector();
	
	public SISTaskRestore(String taskNO,String user,String time,String desFolder) throws IOException{
		String filePath = path+user+"\\"+"Task\\"+time+"\\"+taskNO+".txt";
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
			String des = tmp[1].replace(":", ".");
			String finalPath = desFolder +'['+des+']'+tmp[2];
			String cmd = user+"*restore*"+tmp[0]+'*'+tmp3+'*'+finalPath;
			String[] L1 = cmd.split("[*]");	
			try {
				bcp.initService(L1);
				//Thread.sleep(200);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}		
	}
	public static void main(String[] args) throws IOException{
		
		new SISTaskRestore("task001","NO.002","2012.04.26","C:\\Documents and Settings\\Administrator\\桌面\\2\\");
	}
}