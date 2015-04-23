/*服务器端接收文件*/
package disms.SISStore.server2;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFileChooser;


/**
* 该类用到的绑定端口初始为10000，如果绑定不成功则试另外的端口；
* 绑定次数用tryBindTimes变量，表示如果绑定失败会对它加一；
* 当前绑定端口由DefaultBindPort+tryBindTimes决定；
* 外界系统(调用此程序的对象)可以获取当前的帮定端口；
* 并告诉客户端服务的端口号以使其能正确连接到该端口上；
* @author
*
*/
public class GetFile extends Thread {

    // 服务套接字等待对方的连接和文件发送
    static ServerSocket serSocket;
    // 由服务套接字产生的 套接字
    Socket tempSocket;
    // 用于读取
    InputStream inSocket;
    // 随机访问文件
    RandomAccessFile inFile = null;
    // 临时缓寸区
    byte byteBuffer[] = new byte[1024];
    // 默认用10000端口监听请求
    static int defaultBindPort = 10000;
    static boolean binded = false;
    private ThreadPool filePool;
    private ThreadPool msgpool;
    private final int poolSize = 3;
 
    /**
    * @构造方法
    * @抛出异常的原因是无法绑定服务的端口
    * */
    public GetFile(String filePath) throws Exception {
        serSocket = new ServerSocket(defaultBindPort);  
        filePool = new ThreadPool(Runtime.getRuntime().availableProcessors()*poolSize);
    }
   
   /**
    * @监控线程
    */
    public void fileService() {
    	while(true){
        try {
	            System.out.println("等待对方接入");	            
	            // 等待对方的连接
	            tempSocket = serSocket.accept();
	            filePool.execute(new HandlerFile(tempSocket));
	        } catch (Exception ex) {
	            System.out.println(ex.toString());
	            ex.printStackTrace();
	            return;
	        }
    	}
    }
    public void msgService() {
    	while(true){
            try {
    	            System.out.println("等待对方接入");	            
    	            // 等待对方的连接
    	            tempSocket = serSocket.accept();
    	            filePool.execute(new HandlerMsg(tempSocket));
    	        } catch (Exception ex) {
    	            System.out.println(ex.toString());
    	            ex.printStackTrace();
    	            return;
    	        }
        	}
        }

    
    /**
    * @测试方法
    * @param args
     * @throws Exception 
    */
    public static void main(String args[]) throws Exception {
         String filePath1 = "D:/kk";
         GetFile  getFile1 = new GetFile(filePath1);
         getFile1.fileService();

    }

}
class HandlerMsg implements Runnable{
	private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String msg;
    public HandlerMsg(Socket socket) throws IOException{
    	this.socket = socket;
    	in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
    	
    }
	public void run() {
		// TODO Auto-generated method stub
    	String line =null;
		try {
			
			line=in.readLine();
            //System.out.println("Client send is:"+line);
            
            //将服务器端信息发往客户端               
            out=new PrintWriter(socket.getOutputStream(),true);
            out.println("ServerGetInfo-->getInfo:Your Message Received!");
            System.out.println(line);
            out.close();
            in.close();
            socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}       
		
	}
}
class HandlerFile implements Runnable{
	private Socket socket;
	InputStream inSocket;
    byte byteBuffer[] = new byte[1024];
    RandomAccessFile inFile = null;
    public static int i = 0;
	public HandlerFile(Socket soc) throws IOException{
		socket = soc;
	    inSocket = socket.getInputStream();          
	    File savedFile = new File("D:\\kk"+i);
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

