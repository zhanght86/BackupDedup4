package disms.SISStore.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;



public class ClientDelete{
		private  String userID = null;
		private  String LUID;
		private  String path = "D:\\SIS\\";
		private  String infoPath = null;
		private  String info = null;
		private  String[] infoTmp = null;
		private  String MSG = null;
		private  UUID res_UUID = null;
		private  String returnInfo = null;
		private String host="localhost";
		private int port=8000;
		private Socket socket;
		BufferedReader br;
	    PrintWriter pw;
		
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
		public  void processMSG(String[] commands) throws Exception{
			userID = commands[0];
			infoPath = path + userID +"\\FileInfo\\"+ commands[3]+".txt";
			LUID = commands[2];
			res_UUID = UUID.randomUUID();
			System.out.println(userID+infoPath+LUID+res_UUID);
			System.out.println(info);
			info = readInfo(LUID,infoPath);
			System.out.println(info);
			if(!info.equalsIgnoreCase("")){
				infoTmp = info.split("[*]");
				info ="";
				for(int count = 2;count < infoTmp.length; count++){
					info = info + infoTmp[count]+'*';
				}
				MSG = userID+'#'+"delete"+'#'+info;
				System.out.println(MSG);
			}
			else{
				System.out.println("wrong message!!!");
			}
				
		}
		private PrintWriter getWriter(Socket socket)throws IOException{
		    OutputStream socketOut = socket.getOutputStream();
		    return new PrintWriter(socketOut,true);
		}
		private BufferedReader getReader(Socket socket)throws IOException{
		    InputStream socketIn = socket.getInputStream();
		    return new BufferedReader(new InputStreamReader(socketIn));
		}
		public  void transInfo() throws IOException{
			BufferedReader br;
		    PrintWriter pw;
		    br=getReader(socket);
		    pw=getWriter(socket);
		    System.out.println("Start");
		    pw.println(MSG);
		    System.out.println("end");
//		    returnInfo = br.readLine();
		}
		public  void getInfo() throws IOException{
			
		 
//			comTO.in = new BufferedReader(new InputStreamReader(comTO.socket.getInputStream()));			
//			returnInfo = comTO.in.readLine().toString();
//			System.out.println("ClientDelete-->getInfo:the server has sent the results to the client");	
//			System.out.println(returnInfo);	
		}
//		public  void createCom(){
//			comTO = new ClientTransInfo();			
//			comTO.conn();
//		}
		public ClientDelete(String[] commands) throws Exception{
			socket=new Socket(host,port);
		    br=getReader(socket);
		    pw=getWriter(socket);
			processMSG(commands);
//			createCom();
			transInfo();
			getInfo();
			new DeleteLine(LUID,infoPath);
//			comTO.sendMSG("done");
//			comTO.close();
		}
		public  void main(String[] args) throws Exception{
			new ClientDelete(args);
		}
}
