package disms.SISStore.server;
import java.io.*;
import java.util.UUID;

public class CreateUUIDDIR{
	private String filePath = "E:\\SISServer\\";
	public CreateUUIDDIR(){
		
	}
	public boolean createFolder(String uuid,int scale) throws IOException{
		for(int i = 0; i<scale; i++){
			filePath = filePath + uuid.substring(i*2, i*2+2)+"\\";
		}
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
	public static void main(String[] args) throws IOException{
		CreateUUIDDIR ctd = new CreateUUIDDIR();
		ctd.createFolder("3bf9697c-c402-46e9-a6e4-8d420adb182d", 4);
	}
}