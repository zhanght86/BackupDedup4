package disms.SISStore.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;



public class ClientRestore{
	private  String userID = null;
	private  String path = "D:\\SIS\\";
	private  String filePath = null;
	private  String LUID = null;
	private  String infoPath = null;
	private  String info = null;
	private  String[] infoTmp = null;
	private  String MSG = null;
	private  UUID res_UUID = null;
	private  String returnInfo = null;
	private String host="localhost";
	private int port=8000;
	private Socket socket;

	private Socket fileSoc;
	
	public  String readInfo(String info,String path) throws Exception{	       
	       FileReader fr = new FileReader(path);
	       BufferedReader buf = new BufferedReader(fr);
	       String str = null;
	       while((str = buf.readLine()) != null){
	           if(str.contains(info)){
	    	       return str;
	           }
	       }
	       str = null;
	       return str;
	}

	public  void main(String[] args) throws Exception{
		new ClientRestore(args);
	}
	public  void processMSG(String[] commands) throws Exception{
		userID = commands[0];
		infoPath = path + userID +"\\FileInfo\\"+ commands[3]+".txt";
		filePath = commands[4];
		LUID = commands[2];
		res_UUID = UUID.randomUUID();
		//MSG = userID+'#'+"restore"+'#'+LUID;
		System.out.println(infoPath);
		info = readInfo(LUID,infoPath);
		System.out.println(info);
		if(!info.equalsIgnoreCase("")){
			infoTmp = info.split("[*]");
			info ="";
			for(int count = 2;count < infoTmp.length; count++){
				info = info + infoTmp[count]+'*';
			}
			MSG = userID+'#'+"restore"+'#'+info;
			System.out.println(MSG);
		}
		else
			System.out.println("wrong message!!!");
	}

	public  void createCom(){
	
	}
	private PrintWriter getWriter(Socket socket)throws IOException{
	    OutputStream socketOut = socket.getOutputStream();
	    return new PrintWriter(socketOut,true);
	}
	private BufferedReader getReader(Socket socket)throws IOException{
	    InputStream socketIn = socket.getInputStream();
	    return new BufferedReader(new InputStreamReader(socketIn));
	}
	public void transInfo() throws IOException{
//		System.out.println("here and there"+MSG);
//		comTO.sendMSG(MSG);
		BufferedReader br;
	    PrintWriter pw;
	    br=getReader(socket);
	    pw=getWriter(socket);
	    pw.println(MSG);
	    returnInfo = br.readLine();
	}

	public  void getFile() throws IOException{
		File createFile = new File(filePath);
		if(!createFile.exists())
			createFile.createNewFile();
		if(SISCLIENT.serSocket==null)
			SISCLIENT.serSocket = new ServerSocket();
		if(!SISCLIENT.serSocket.isBound()){
			SISCLIENT.serSocket.bind(new InetSocketAddress(20000));
		}
		System.out.println(SISCLIENT.serSocket.getLocalPort());
		fileSoc = SISCLIENT.serSocket.accept();
//		System.out.println("into sendInfo");
		SISCLIENT.filePool.execute(new HandlerRES(fileSoc,filePath));
//		System.out.println("into sendInfo");
	}

	public ClientRestore(String[] commands) throws Exception{
		socket=new Socket(host,port); 	   
		processMSG(commands);
//		createCom();
		transInfo();
//		getInfo();
		if(returnInfo.equalsIgnoreCase("FOUND")){
			getFile();
		}
		else
		{
			System.out.println("ClientRestore-->ClientRestore:the infomation of the file is not right");
		}
	}
}