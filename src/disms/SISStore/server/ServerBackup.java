package disms.SISStore.server;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class ServerBackup{
	private String fileInfoPath = "E:\\backupdedup\\SIS\\files\\";
	private String[] fileInfoList = null;
	private String fileInfo = null;
	private long fileSize1 = 50*1024*1024;
	private String fileSize1STR = "50M";
	private long fileSize2 = 100*1024*1024;
	private String fileSize2STR = "100M";
	private String getInfo = null;
	private String[] getInfoList = null;
	private String userID = null;
	private UUID link_NO = null;
	private UUID file_UUID = null;
	private String strDATE = null;
	private String userInfoPath = null;
	private String tempPath = null;
	private String txtInfoPath = null;
	private String returnInfo = null;
	//////////////////////////////////////////////

//	private String host="localhost";
	private Socket fileSoc;
//	private int port=8000;
	private Socket socket;
//	public BufferedReader br;
//    public PrintWriter pw;

//	public ServerSocket serSocket;
	//////////////////////////////////////////////
	/*
	 * 获取时间
	 
	public static void getTime(){
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd.HH:mm:ss:SSS");      
		Date curDate = new Date(System.currentTimeMillis());//获取当前时间      
		strDATE = formatter.format(curDate);		
	}*/
	/*
	 * 保存用户的信息
	 */
	public void saveUserFileInfo(String userInfoPath) throws IOException{
		FileWriter fosTempCONS = new FileWriter(userInfoPath,true);
		fosTempCONS.write(userID+'*');
		fosTempCONS.write(strDATE+'*');
		fosTempCONS.write(link_NO.toString()+'*');
		fosTempCONS.write(getInfoList[1]+'*');
		fosTempCONS.write(getInfoList[2]+'*');
		fosTempCONS.write(getInfoList[3]+'*');
		fosTempCONS.write(getInfoList[4]+'*');
		fosTempCONS.write(getInfoList[5]+'\n');
		fosTempCONS.close();	
	}
	/*
	 * 
	 */
	public void saveInfoFile(String infoFilePath) throws IOException
	{
		FileWriter fosTempCONS = new FileWriter(infoFilePath,true);
		fosTempCONS.write('#');
		fosTempCONS.write(strDATE+'*');
		fosTempCONS.write(file_UUID.toString()+'*');
		fosTempCONS.write(file_UUID.toString()+'.'+getInfoList[0]+'*');
		fosTempCONS.write(tempPath+'*');
		fosTempCONS.write(fileInfo+'\n');
		fosTempCONS.close();	
	}
	
	/*
	 * 创建对应的文件夹
	 */
	public void createFile() throws IOException{
		//给文件分类
		fileInfoList = fileInfo.split("[*]");
		for(int i = 0;i< fileInfoList.length;i++)
		{
			System.out.println("fileInfo"+fileInfoList[i]);
		}
		CreateHashDIR chd = new CreateHashDIR();
		long hash1 = Long.parseLong(fileInfoList[2]);
		long hash2 = Long.parseLong(fileInfoList[4]);
		long hash3 = Long.parseLong(fileInfoList[6]);
		long hash4 = Long.parseLong(fileInfoList[8]);
		chd.createFolder(hash1, hash2, hash3, hash4);
		fileInfoPath = chd.getFilePath();
		//对应的文件夹
		/*
		 * fileInfoPath =fileInfoPath+fileInfoList[0]+"\\";
		System.out.println(fileInfoPath);
		if(Long.parseLong(fileInfoList[1])<fileSize1){
			fileInfoPath =fileInfoPath + "0M-"+fileSize1STR+"\\";
			//System.out.println(fileInfoPath);
		}
		else if((Long.parseLong(fileInfoList[1])>=fileSize1)&&(Long.parseLong(fileInfoList[1])<fileSize2)){
			fileInfoPath = fileInfoPath +fileSize1STR+"-"+fileSize2STR+"\\";
			//System.out.println(fileInfoPath);
		}
		else if(Long.parseLong(fileInfoList[1])>=fileSize2){
			fileInfoPath = fileInfoPath +fileSize2STR+"-"+"\\";
			//System.out.println(fileInfoPath);
		}
		//判断文件是否存在，不存在则创建它
		File judgeExist = new File(fileInfoPath); 
		if (!(judgeExist.exists())&&!(judgeExist.isDirectory()))
		{
            boolean  creadok  =  judgeExist.mkdirs();
            if (creadok)  {
               System.out.println("ServerBackup-->createFile:the folder has been created successfully");
           } else  {
               System.out.println("ServerBackup-->createFile:fail to create the folder");                    
           } 
		} 
		
		File filesExist = new File(fileInfoPath+"files\\");
		if (!(filesExist.exists())&&!(filesExist.isDirectory()))
		{
            boolean  creadok  =  filesExist.mkdirs();
            if (creadok)  {
               System.out.println("ServerBackup-->createFile:the folder has been created successfully");
           } else  {
               System.out.println("ServerBackup-->createFile:fail to create the folder");                    
           } 
		} 
		File InfoFile = new File(fileInfoPath+"fileInfo.txt");
		if (!(InfoFile.exists())&&!(InfoFile.isDirectory()))
		{
            boolean  creadok  =  InfoFile.createNewFile();
            if (creadok)  {
               System.out.println("ServerBackup-->createFile:the info file has been created successfully");
           } else  {
               System.out.println("ServerBackup-->createFile:fail to create the info file");                    
           } 
		} */
	}
	public void processMSG(String command,String ID,String time){
		userID = ID;
		fileInfo = command;
		strDATE = time;
	}
	public void findDedup(){
		new ServerLocalDedup();
		getInfo = ServerLocalDedup.findLocalDedup(fileInfo, fileInfoPath);
	}
	public void getUserInfo() throws IOException{
		//userInfoPath = userInfoPath + userID+".txt";
		userInfoPath = fileInfoPath + "linkInfo.txt";
		File InfoUser = new File(userInfoPath);
		if (!(InfoUser.exists())&&!(InfoUser.isDirectory()))
		{
            boolean  creadok  =  InfoUser.createNewFile();
            if (creadok)  {
               System.out.println("ServerBackup-->createFile:the user file has been created successfully");
           } else  {
               System.out.println("ServerBackup-->createFile:fail to create the user file");                    
           } 
		} 
		if(!getInfo.equalsIgnoreCase(""))
			getInfoList = getInfo.split("[*]");
		else 
			System.out.println("wrong message!");
	}
	public void getLink(){
		link_NO = UUID.randomUUID();	
	}
	private PrintWriter getWriter(Socket socket)throws IOException{
	    OutputStream socketOut = socket.getOutputStream();
	    return new PrintWriter(socketOut,true);
	}
	private BufferedReader getReader(Socket socket)throws IOException{
	    InputStream socketIn = socket.getInputStream();
	    return new BufferedReader(new InputStreamReader(socketIn));
	}
	public void sendInfo() throws IOException{
//        SISSERVER.out=new PrintWriter(SISSERVER.socket.getOutputStream(),true);
//        SISSERVER.out.println(returnInfo);
		System.out.println("into sendInfo");
		BufferedReader br;
	    PrintWriter pw;
	    br=getReader(socket);
	    pw=getWriter(socket);
        pw.println(returnInfo);
		/*sendTo = new ServerTransInfo();			
		sendTo.sendInfo(returnInfo);
		sendTo.close();*/
	}
	public void getFileInfo(){

		if(!fileInfo.equalsIgnoreCase(""))
			getInfoList = fileInfo.split("[*]");
		else 
			System.out.println("wrong message!");
		file_UUID = UUID.randomUUID();	
		txtInfoPath = fileInfoPath + "fileInfo.txt";
		tempPath = fileInfoPath+"files\\"+file_UUID+'.'+getInfoList[0];		
	}
	public void getFile() throws IOException{
		File createFile = new File(tempPath);
		if(!createFile.exists())
			createFile.createNewFile();
		
//		try {
//		SISSERVER.gf.getTheFile(10004,tempPath);
//		} catch (Exception e) {
//		e.printStackTrace();
//		System.out.println("ServerBackup-->getFile:can't get the file");
//		System.exit(1);
//		}
//		SISSERVER.gf.run();
//		System.out.println("into sendInfo");
//		serSocket = new ServerSocket(10000);
		if(!SISSERVER.serSocket.isBound()){
			SISSERVER.serSocket.bind(new InetSocketAddress(11000));
		}
		System.out.println(SISSERVER.serSocket.getLocalPort());
		fileSoc = SISSERVER.serSocket.accept();
//		System.out.println("into sendInfo");
	    SISSERVER.filePool.execute(new HandlerTrans(fileSoc,tempPath));
//		System.out.println("into sendInfo");
	}
	ServerBackup(String command,String ID,String time,Socket socket) throws Exception{
		this.socket= socket; 	   
//		serSocket = new ServerSocket(10000);
		System.out.println("ServerBackup-->ServerBackup:into the server backup service");
		processMSG(command,ID,time);
		createFile();
		findDedup();		
		//getTime();
		if(getInfo != null){
			getUserInfo();
			getLink();
			saveUserFileInfo(userInfoPath);
			returnInfo = "completed"+'*'+link_NO.toString()+'*'+getInfoList[1];
			sendInfo();
		}
		else
		{
			getFileInfo();
			getLink();
			returnInfo = "undone"+'*'+link_NO.toString()+'*'+file_UUID.toString();
			sendInfo();
			getFile();
			saveInfoFile(txtInfoPath);
			findDedup();
			getUserInfo();
			saveUserFileInfo(userInfoPath);
		}
	}
}