package disms.SISStore.client2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.swing.JFileChooser;

import org.apache.commons.io.FileUtils;
import org.opendedup.sdfs.*;

import frame.progressbar.progressWindow;

public class SISTaskBackup1{
	public static Vector vec = new Vector();
	private Vector fs = new Vector();
	private String path1 = "E:\\backupdedup\\SIS\\";
	
	public SISTaskBackup1(){
		
	}
	
	public SISTaskBackup1(final String createTime,final String files,progressWindow pw) throws IOException{
				path1 = org.opendedup.sdfs.Main.volume.getPath()+"\\";
				String pathd = path1+"Task\\";
				final String filePath = path1+"Task\\"+createTime+".txt";
				try {
					createDIR(pathd,filePath);
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}

				String[] paths = files.split("\n");
				final Vector fs = new Vector();
				chooseFile(fs, paths);
				pw.setMax(fs.size());
				pw.setProgress(0);
				pw.setVisible(true);
		        SISCLIENT bcp = null;
				try {
					bcp = new SISCLIENT();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for(int i = 0;i<fs.size();i++){
					String[] c = fs.get(i).toString().split("[*]");
					try {
						bcp.initService(c);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					pw.setCurrentFile(c[2]);
					pw.setProgress(pw.getProgress() + i);
				}
				
				FileWriter fosTempCONS = null;
				try {
					fosTempCONS = new FileWriter(filePath,true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for(int i = 0; i < vec.size();i++ )
					try {
						fosTempCONS.write(vec.get(i).toString()+'\n');
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				try {
					fosTempCONS.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				pw.dispose();	        
		
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
//		SISTaskBackup1 stb =  new SISTaskBackup1("2012.12.12",path);
		//stb.chooseFile(fs, paths);
//		for(int i = 0;i<fs.size();i++)
//		{
//			System.out.println(fs.elementAt(i));
//		}
		
	}
}