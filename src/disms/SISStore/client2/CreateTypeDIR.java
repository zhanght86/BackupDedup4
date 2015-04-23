package disms.SISStore.client2;
import java.io.*;

public class CreateTypeDIR{
	private String filePath = "E:\\SISServer\\";
	private long first = 100*1024*1024;
	private long second = 1024*1024;
	private long third = 10*1024;
	private long fourth = 100;
	public CreateTypeDIR(){
		
	}
	public void adjustPARA(long one,long two,long three,long four){
		if(one*two*three*four!=0){
			first = one;
			second = two;
			third = three;
			fourth = four;
		}
	}
	public boolean createFolder(String fileType,long fileSize,int accuracy) throws IOException{
		long fs = fileSize;
		long one;
		long two;
		long three;
		long four;
		filePath = filePath + fileType + "\\";
		if((first!=0)&&(accuracy>0)){
			one = fs/first;
			filePath =filePath + one + "\\";
			if((second!=0)&&(accuracy>1)){				
				two = (fs%first)/second;
				filePath =filePath + two + "\\";
				if((third!=0)&&(accuracy>2)){
					three = ((fs%first)%second)/third;
					filePath =filePath + three + "\\";
					if(fourth!=0&&(accuracy>3)){
						four = (((fs%first)%second)%third)/fourth;
						filePath =filePath + four + "\\";
					}
				}
			}
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
		CreateTypeDIR ctd = new CreateTypeDIR();
		ctd.createFolder("pdf", 5*100*1024*1024+56*1024*1024+7*10*1024+600, 4);
	}
}