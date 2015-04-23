package disms.SISStore.client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.util.*;

public class SISCLIENT implements Runnable{
/*
 *	private static String serialNO = "1.0.0";	//版本号
 *	private String exeuteStatus = "";			//系统执行状态
 *	private String[] executeResults;			//每一部执行的结果
 */	
	private String commandLines[] = null;		//存储用户的命令
	private String command = null;
	private String info = null;
	private String userID = null;				//用户的ID号写入到文件之中
	private String[] paras = null;
	public static ServerSocket serSocket;
	private final static int poolSize = 6;
	public static ThreadPool filePool = new ThreadPool(Runtime.getRuntime().availableProcessors()*poolSize);
	//new
//	private String host="localhost";
//	private int port=8000;
//	private Socket socket;
//	public BufferedReader br;
//    public PrintWriter pw;
	//new
	public SISCLIENT(String[] paras){
		this.paras = paras;
	}
	public SISCLIENT() throws UnknownHostException, IOException{
		
//	     socket=new Socket(host,port); 
//	     br=getReader(socket);
//	     pw=getWriter(socket);
		
	}
	
	//new
	private PrintWriter getWriter(Socket socket)throws IOException{
	    OutputStream socketOut = socket.getOutputStream();
	    return new PrintWriter(socketOut,true);
	}
	private BufferedReader getReader(Socket socket)throws IOException{
	    InputStream socketIn = socket.getInputStream();
	    return new BufferedReader(new InputStreamReader(socketIn));
	}
	//new
/*
 * 	检查版本号
 *	public static void checkVersion(){
 *	}
 */
	
/*
 * 解析命令:将commands进行处理，保存入commandLines，此时为已声明commandLineS分配空间
 */
	public void resolveCommand(String[] commands){		
		commandLines = commands;
		userID = commandLines[0];
		command = commandLines[1];
		info = commandLines[2];
	}
/*
 * 启动客户端的服务
 */
	public String initService(String[] commands) throws Exception{
		resolveCommand(commands);
		if (command.equalsIgnoreCase("backup"))
		{
			initBackup(commandLines);
			System.out.println("SISCLIENT-->initService:client backup mission comleted");
			return "success";
		}
		else if(command.equalsIgnoreCase("restore"))
		{
			initRestore(commandLines);
			System.out.println("SISCLIENT-->initService:client restore mission comleted");
			return "success";
		}
		else if(command.equalsIgnoreCase("delete"))
		{
//			for(int m = 0;m < commandLines.length;m++)
//				System.out.println(commandLines[m]);
			initDelete(commandLines);
			
			System.out.println("SISCLIENT-->initService:client delete mission comleted");
			return "success";
		}
		else
			return null;
	}
	//启动备份服务
	public static void initBackup(String[] commands) throws Exception{
		System.out.println("SISCLIENT-->initBackup:start the backup process");
		new ClientBackup(commands);
	}
	//启动还原服务
	public static void initRestore(String[] commands) throws Exception{
		System.out.println("SISCLIENT-->initRestore:start the restore process");
		new ClientRestore(commands);
	}
	//启动删除服务
	public static void initDelete(String[] commands) throws Exception{
		System.out.println("SISCLIENT-->initRestore:start the delete process");
		for(int m = 0;m < commands.length;m++)
			System.out.println(commands[m]);
		new ClientDelete(commands);
	}

	public  void run(){
		try {
			initService(paras);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) throws Exception{
//		String line1 = "NO.001 restore 8b82a27a-d272-42b0-900a-340dc252f089 2012.04.05 D:\\found2.rar";
//		String[] L1 = line1.split("[*]");	
//		String line2 = "NO.001 backup D:\\(free)bo3d120328.exe";
//		String[] L2 = line2.split("[*]");	
//		String line3 = "NO.001 delete 8b82a27a-d272-42b0-900a-340dc252f089 2012.04.05";
//		String[] L3 = line3.split("[*]");	
//		SISCLIENT test1 = new SISCLIENT();
//		SISCLIENT test2 = new SISCLIENT();
//		SISCLIENT test3 = new SISCLIENT();
//		test1.initService(L1);
//		test2.initService(L2);
//		test3.initService(L3);
//		String line1 = "NO.001*backup*C:\\1.mp3";
//		String[] L1 = line1.split("[*]");
//		SISCLIENT test1 = new SISCLIENT(L1);
//		String line2 = "NO.001*backup*C:\\1 (5).mp3";
//		String[] L2 = line2.split("[*]");
//		SISCLIENT test2 = new SISCLIENT(L2);
//		String line3 = "NO.001*backup*C:\\1 (6).mp3";
//		String[] L3 = line3.split("[*]");
//		SISCLIENT test3 = new SISCLIENT(L3);
//		String line4 = "NO.001*backup*C:\\1 (7).mp3";
//		String[] L4 = line4.split("[*]");
//		SISCLIENT test4 = new SISCLIENT(L4);
//		String line1 = "NO.001*restore*eb5029a8-3e11-4ad9-b71b-d84f98b0547a*2012.05.02*C:\\7 (1).mp3";
//		String[] L1 = line1.split("[*]");
//		SISCLIENT test1 = new SISCLIENT(L1);
//		String line2 = "NO.001*restore*609a702a-fe86-4589-9cb6-ac2709d79996*2012.05.02*C:\\7 (2).mp3";
//		String[] L2 = line2.split("[*]");
//		SISCLIENT test2 = new SISCLIENT(L2);
//		String line3 = "NO.001*restore*4170a529-7794-48ad-9542-48be27648840*2012.05.02*C:\\7 (3).mp3";
//		String[] L3 = line3.split("[*]");
//		SISCLIENT test3 = new SISCLIENT(L3);
//		String line4 = "NO.001*restore*6476a1c0-7365-4f0f-aab3-99d8b67ef7ec*2012.05.02*C:\\7 (4).mp3";
//		String[] L4 = line4.split("[*]");
//		SISCLIENT test4 = new SISCLIENT(L4);
//		test1.run();
//		test2.run();
//		test3.run();
//		test4.run();
//		Thread t1 = new Thread(test1);
//		Thread t2 = new Thread(test2);
//		Thread t3 = new Thread(test3);
//		Thread t4 = new Thread(test4);
//		t1.start();
//		t2.start();
//		t3.start();
//		t4.start();
//		Thread.sleep(10000);

		String line1 = "NO.001*restore*89022a22-222e-43e7-a2d0-38c6901f75fb*2012.05.07*D:\\yyy.zip";
		String[] L1 = line1.split("[*]");
		SISCLIENT test1 = new SISCLIENT(L1);
		Thread t1 = new Thread(test1);
		t1.start();
	}
}
class HandlerRES implements Runnable{
	private Socket socket;
	InputStream inSocket;
    byte byteBuffer[] = new byte[1024];
    RandomAccessFile inFile = null;
    public static int i = 0;
	public HandlerRES(Socket soc,String filePath) throws IOException{
		System.out.println("into HandlerRES");
		socket = soc;
	    inSocket = socket.getInputStream();          
	    File savedFile = new File(filePath);
	    i++;
        inFile = new RandomAccessFile(savedFile, "rw");    
	}
	public void run() {
		// TODO Auto-generated method stub
		int amount;
	    try {
			while ((amount = inSocket.read(byteBuffer)) != -1) {
	            inFile.write(byteBuffer, 0, amount);
	        }
	        // 关闭流
	        inSocket.close();
	        System.out.println("Get OK");
	        System.out.println("接收完毕!");
	        // 关闭文件
	        inFile.close();
	        // 关闭临时套接字
	        socket.close();      
	       
	    } catch (IOException e) {
	        System.out.println(e.toString());
	        e.printStackTrace();
	    }
	}
	
}
/*
 		SISCLIENT sendFileTest1 = new SISCLIENT();
		String line = "N0.001 delete 6ed7daa1-ceaf-48d8-8636-db7d733cedf1";
		String[] L = line.split("[ ]");
		sendFileTest1.initService(L);		
		String line1 = "NO.001 backup D:\\SIS\\files\\1.zip";
		String[] L1 = line1.split("[ ]");	
		String line2 = "NO.001 backup D:\\SIS\\files\\2.zip";
		String[] L2 = line1.split("[ ]");	
		String line3 = "NO.001 backup D:\\SIS\\files\\3.zip";
		String[] L3 = line1.split("[ ]");	
		String line4 = "NO.001 backup D:\\SIS\\files\\4.zip";
		String[] L4 = line1.split("[ ]");
		SISCLIENT test1 = new SISCLIENT(L1);
		SISCLIENT test2 = new SISCLIENT(L2);
		SISCLIENT test3 = new SISCLIENT(L3);
		SISCLIENT test4 = new SISCLIENT(L4);
		Thread t1 = new Thread(test1);
		Thread t2 = new Thread(test2);
		Thread t3 = new Thread(test3);
		Thread t4 = new Thread(test4);
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		//sendFileTest2.initService(L);
*/