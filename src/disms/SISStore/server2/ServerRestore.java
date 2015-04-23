package disms.SISStore.server2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import disms.SISStore.client2.*;


public class ServerRestore{
	private  String filePath = "E:\\backupdedup\\SIS\\files\\";
	private  String[] link_Info = null;
	private  String info_Link= null;
	private  String sendfilePath= null;
	private  String returnInfo = null;
//	private Socket socket;
	public  String readInfo(String info,String path) throws Exception{	       
	       FileReader fr = new FileReader(path);
	       BufferedReader buf = new BufferedReader(fr);

	       String str = "";
	       while((str = buf.readLine()) != null){
	           if(str.contains(info)){
	    	       return str;
	           }
	       }
	       return str;
	}
	
	public  void processMSG(String command, String userID) throws Exception{
		
		System.out.println(command);
		link_Info = command.split("[*]");
		System.out.println(link_Info[6]);		
		long hash1 = Long.parseLong(link_Info[6])%128;
		long hash2 = Long.parseLong(link_Info[8])%128;
		long hash3 = Long.parseLong(link_Info[10])%128;
		long hash4 = Long.parseLong(link_Info[12])%128;
		filePath = filePath + hash1 +"\\"+hash2+"\\"+hash3+"\\"+hash4+"\\"+"linkInfo.txt";
		for(int i = 0;i< link_Info.length;i++)
		{
			System.out.println("fileInfo"+link_Info[i]);
		}
        System.out.println("link_Info[0]:"+link_Info[0]);
        System.out.println("filePath:"+filePath);
		info_Link = readInfo(link_Info[0],filePath);
        System.out.println("info_Link:"+info_Link);
	}
	
	public  void transInfo() throws IOException{
		/*
        SISSERVER.out=new PrintWriter(SISSERVER.socket.getOutputStream(),true);
        SISSERVER.out.println(returnInfo);
		 * */
//		BufferedReader br;
//	    PrintWriter pw;
//	    br=getReader(socket);
//	    pw=getWriter(socket);
//      pw.println(returnInfo);
        ClientRestore.returnInfo=returnInfo;
    	/*ServerTransInfo cti = new ServerTransInfo();
		cti.conn();
		cti.sendMSG(returnInfo);
		cti.close();*/
	}
	private PrintWriter getWriter(Socket socket)throws IOException{
	    OutputStream socketOut = socket.getOutputStream();
	    return new PrintWriter(socketOut,true);
	}
	private BufferedReader getReader(Socket socket)throws IOException{
	    InputStream socketIn = socket.getInputStream();
	    return new BufferedReader(new InputStreamReader(socketIn));
	}
	public  void sendObjFile() throws IOException{
		link_Info = info_Link.split("[*]");
		System.out.println(link_Info[5]);
		sendfilePath = link_Info[5]+link_Info[4];
		File createFile = new File(ClientRestore.filePath);
		System.out.println(ClientRestore.filePath);
		System.out.println(createFile.getParent().toString());
		System.out.println(createFile.getName().toString());
		System.out.println(sendfilePath);
		new FileUtil().copyFileToDir(createFile.getParent().toString(), new File(sendfilePath), createFile.getName().toString());
		File fileOBJ = new File(sendfilePath);	
//		System.out.println(socket.getInetAddress());
//		String IP = socket.getInetAddress().toString().replaceAll("/", "");		
//		System.out.println(IP);
//		SendFile sf1 = new SendFile(IP,20000,fileOBJ.toString());
//		sf1.start();
	}
	
	public ServerRestore(String command, String userID) throws Exception{
//		this.socket = socket;
		filePath = org.opendedup.sdfs.Main.volume.getPath()+"\\files\\";
        System.out.println("ServerRestore-->ServerRestore:in to this process"+command+userID);
        System.out.println(command);
		System.out.println(userID);
		processMSG(command, userID);
		
	    if((info_Link.indexOf(link_Info[0]) != -1)&&
	       (info_Link.indexOf(link_Info[4]) != -1)&&
	       (info_Link.indexOf(link_Info[5]) != -1))  
		{
	    	returnInfo = "FOUND";
	        System.out.println(returnInfo);
	    	transInfo();
	        System.out.println("ServerRestore-->ServerRestore:found the matching information");
	        sendObjFile();
		}
	    else{
	    	returnInfo = "UNFOUND";
	    	transInfo();
	        System.out.println("ServerRestore-->ServerRestore:not found the matching information");	    	
	    }
	}
}