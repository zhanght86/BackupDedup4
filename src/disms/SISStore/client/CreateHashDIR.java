package disms.SISStore.client;
import java.io.*;
public class CreateHashDIR{
	private String filePath = "E:\\backupdedup\\SIS\\files\\";
	private int scale = 128;
	public CreateHashDIR(){
		
	}
	public void setScale(int sc){
		scale = sc;
	}
	public void resetScale(int sc){
		scale = 256;
	}
	public String getFilePath(){
		return filePath;
	}
	public  boolean createFolder(long HASH1) throws IOException{
		long tail1 = HASH1%scale;
		filePath = filePath + tail1 +"\\";		
		return createDIR(filePath);
	}
	public  boolean createFolder(long HASH1,long HASH2) throws IOException{
		long tail1 = HASH1%scale;
		long tail2 = HASH2%scale;
		filePath = filePath + tail1 +"\\"+tail2+"\\";
		return createDIR(filePath);
	}
	public  boolean createFolder(long HASH1,long HASH2,long HASH3) throws IOException{
		long tail1 = HASH1%scale;
		long tail2 = HASH2%scale;
		long tail3 = HASH3%scale;
		filePath = filePath + tail1 +"\\"+tail2+"\\"+tail3+"\\";
		return createDIR(filePath);
	}
	public  boolean createFolder(long HASH1,long HASH2,long HASH3,long HASH4) throws IOException{
		long tail1 = HASH1%scale;
		long tail2 = HASH2%scale;
		long tail3 = HASH3%scale;
		long tail4 = HASH4%scale;
		filePath = filePath + tail1 +"\\"+tail2+"\\"+tail3+"\\"+tail4+"\\";
		return createDIR(filePath);
	}
	private boolean createDIR(String fp) throws IOException{
		File judgeExist = new File(fp);
		boolean  creadok1 = true;
		boolean  creadok2 = true;
		boolean  creadok3 = true;
		boolean  creadok4 = true;
		if (!(judgeExist.exists())&&!(judgeExist.isDirectory()))
		{
            creadok1  =  judgeExist.mkdirs();
		}
		File filesExist = new File(fp+"files\\");
		if (!(filesExist.exists())&&!(filesExist.isDirectory()))
		{
            creadok2  =  filesExist.mkdirs();
		} 
		File fileInfo = new File(fp+"fileInfo.txt");
		if (!(fileInfo.exists())&&!(fileInfo.isDirectory()))
		{
            creadok3  =  fileInfo.createNewFile();
		}  
		File linkInfo = new File(fp+"linkInfo.txt");
		if (!(linkInfo.exists())&&!(linkInfo.isDirectory()))
		{
            creadok4  =  linkInfo.createNewFile();
		} 
		if(creadok1&&creadok2&&creadok3&&creadok4 == true){
			System.out.println("the folder has been created successfully");
			return true;
		}
		else
			return false;
        		
	}
	public static void main (String[] args) throws IOException{
		long hash1 = 128;
		long hash2 = -254;
		long hash3 = -253;
		long hash4 = -252;
		long hash5 = hash1%128;
		System.out.println(hash5);
		//CreateHashDIR chd = new CreateHashDIR();
		//chd.createFolder(hash1,hash2,hash3,hash4);
	}
}