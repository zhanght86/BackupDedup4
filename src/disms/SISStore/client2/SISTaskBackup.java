package disms.SISStore.client2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class SISTaskBackup{
	public static Vector vec = new Vector();
	private Vector strs = new Vector();
	private String path = "D:\\SIS\\";
	
	public SISTaskBackup(String taskNO,String user,String[] files) throws IOException{
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd");      
		Date curDate = new Date(System.currentTimeMillis());//获取当前时间      
		String strDATE = formatter.format(curDate);
		String filePath = path+user+"\\"+"Task\\"+strDATE+"\\"+taskNO+".txt";
		File dir = new File(path+user+"\\"+"Task\\"); 
        if(!dir.exists())
        	dir.mkdir();
		File dirFile = new File(path+user+"\\"+"Task\\"+strDATE); 
        if(!dirFile.exists())
        	dirFile.mkdir();
		File f = new File(filePath);
		System.out.println(filePath);
		if(!f.exists())
			f.createNewFile();
		for(int i = 0; i < files.length ; i++){
			strs.addElement(user+"*backup*"+files[i]);
		}
		SISCLIENT bcp = new SISCLIENT();
		for(int i = 0;i<strs.size();i++){
			String[] c = strs.get(i).toString().split("[*]");
			try {
				bcp.initService(c);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		FileWriter fosTempCONS = new FileWriter(filePath,true);
		for(int i = 0; i < vec.size();i++ )
			fosTempCONS.write(vec.get(i).toString()+'\n');
		fosTempCONS.close();
	}
	public static void main(String[] args) throws IOException{
		String[] files = {"D:\\kk1","D:\\kk2","D:\\kk3","D:\\kk0"};
		new SISTaskBackup("task002","NO.002",files);
	}
}
