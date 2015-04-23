package disms.SISStore.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.swing.JFileChooser;

public class SISTaskBackup1{
	public static Vector vec = new Vector();
	private Vector fs = new Vector();
	private String path1 = "E:\\backupdedup\\SIS\\";
	
	public SISTaskBackup1(){
		
	}
	
	public SISTaskBackup1(String createTime,String files) throws IOException{
//		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd");      
//		Date curDate = new Date(System.currentTimeMillis());//获取当前时间      
//		String strDATE = formatter.format(curDate);
		String pathd = path1+"Task\\";
		String filePath = path1+"Task\\"+createTime+".txt";
		createDIR(pathd,filePath);
//		File dir = new File(path+user+"\\"+"Task\\"); 
//        if(!dir.exists())
//        	dir.mkdir();
//		File dirFile = new File(path+user+"\\"+"Task\\"+strDATE); 
//        if(!dirFile.exists())
//        	dirFile.mkdir();
//		File f = new File(filePath);
//		System.out.println(filePath);
//		if(!f.exists())
//			f.createNewFile();
//		for(int i = 0; i < files.length ; i++){
//			strs.addElement(user+"*backup*"+files[i]);
//		}
		String[] paths = files.split("\n");
		Vector fs = new Vector();
		chooseFile(fs, paths);
//		for(int i = 0;i<fs.size();i++)
//		{
//			System.out.println(fs.elementAt(i));
//		}
		SISCLIENT bcp = new SISCLIENT();
		for(int i = 0;i<fs.size();i++){
			String[] c = fs.get(i).toString().split("[*]");
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
	public void createDIR(String fp,String fp2) throws IOException{
		File judgeExist = new File(fp);
//		boolean  creadok1 = true;
//		boolean  creadok2 = true;
//		boolean  creadok3 = true;
//		boolean  creadok4 = true;
		if (!(judgeExist.exists())&&!(judgeExist.isDirectory()))
		{
            judgeExist.mkdirs();
		}
		File filesExist = new File(fp2);
		if (!(filesExist.exists())&&!(filesExist.isDirectory()))
		{
            filesExist.createNewFile();
		} 
//		File fileInfo = new File(fp+"fileInfo.txt");
//		if (!(fileInfo.exists())&&!(fileInfo.isDirectory()))
//		{
//            creadok3  =  fileInfo.createNewFile();
//		}  
//		File linkInfo = new File(fp+"linkInfo.txt");
//		if (!(linkInfo.exists())&&!(linkInfo.isDirectory()))
//		{
//            creadok4  =  linkInfo.createNewFile();
//		} 
//		if(creadok1&&creadok2&&creadok3&&creadok4 == true){
//			System.out.println("the folder has been created successfully");
//			return true;
//		}

        		
	}
	public void chooseFile(Vector fs,String[] paths){		
		for(int i = 0; i < paths.length;i++ ){
			if(!(new File(paths[i])).isDirectory())
				fs.addElement("administrator"+"*backup*"+paths[i]);
			else
				fileList(new File(paths[i]),fs);
		}
		System.out.println(fs);
	}
	public void fileList(File file,Vector vt) {
        File[] files = file.listFiles();
        if (files != null) {
              for (File f : files) {
            	  if(!f.isDirectory())
//                    System.out.println(f.getPath());
            	  	vt.addElement("administrator"+"*backup*"+f.getPath());
                    fileList(f,vt);
              }
        }
   }
	public static void main(String[] args) throws IOException{
		String path = "E:\\test"+'\n'+"E:\\SISServer.zip";
		
		//Vector fs = new Vector();
		SISTaskBackup1 stb =  new SISTaskBackup1("2012.12.12",path);
		//stb.chooseFile(fs, paths);
//		for(int i = 0;i<fs.size();i++)
//		{
//			System.out.println(fs.elementAt(i));
//		}
		
	}
}