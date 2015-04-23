package disms.SISStore.client2;
import java.io.*;
public class CreateTimeDIR{
	private String filePath = "E:\\SISServer\\";
	public CreateTimeDIR(){
		
	}
	public  boolean createFolder(String timeStamp,String accuracy) throws IOException{
		String[] dateFields = timeStamp.split("[.]");
		String[] timeFields = dateFields[dateFields.length-1].split("[:]");
		filePath = filePath + dateFields[0] +"\\"+ dateFields[1] +"\\"+ dateFields[2] +"\\";
		if(accuracy.equalsIgnoreCase("day")){
			return createDIR(filePath);
		}
		if(accuracy.equalsIgnoreCase("hour")){
			filePath = filePath + timeFields[0] +"\\";
			return createDIR(filePath);			
		}
		if(accuracy.equalsIgnoreCase("minute")){
			filePath = filePath + timeFields[0] +"\\" + timeFields[1] +"\\";
			return createDIR(filePath);			
		}
		if(accuracy.equalsIgnoreCase("second")){
			filePath = filePath + timeFields[0] +"\\" + timeFields[1] +"\\" + timeFields[2] +"\\";
			return createDIR(filePath);
		}
		if(accuracy.equalsIgnoreCase("millisecond")){
			filePath = filePath + timeFields[0] +"\\" + timeFields[1] +"\\" + timeFields[2] +"\\" + timeFields[3] +"\\";
			return createDIR(filePath);
		}
		return false;
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
		String ts = "2012.03.16.21:18:19:453";
		String ac = "millisecond";
		CreateTimeDIR cf = new CreateTimeDIR();
		cf.createFolder(ts,ac);
	}
}