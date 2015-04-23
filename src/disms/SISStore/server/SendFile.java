/*文件发送端*/
package disms.SISStore.server;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

import javax.swing.JFileChooser;

/**
* 在服务器端开启的情况下 实例化套接字 并发送文件
*
* @author 
*/
public class SendFile extends Thread {

    // 远程的IP字符串
    String remoteIPString = null;
    // 远程的服务端口
    int port;
    // 临时套接字
    Socket tempSocket;
    // 发送文件用的输出流
    OutputStream outSocket;
    // 欲发送的文件
    RandomAccessFile outFile;
    // 发送文件用的临时缓存区
    byte byteBuffer[] = new byte[1024];

    /**
    * 构造方法仅用于选择发送文件的位置 并从外部接收远程地址和端口
    *
    */
    public SendFile(String remoteIPString, int port,String filePath) {
        try {
            this.remoteIPString = remoteIPString;
            this.port = port;

           // 选择发送的文件位置
//            JFileChooser jfc = new JFileChooser(".");
            File file = new File(filePath);
//            int returnVal = jfc.showOpenDialog(new javax.swing.JFrame());
//            if (returnVal == JFileChooser.APPROVE_OPTION) {
//                file = jfc.getSelectedFile();
//
//            }

            outFile = new RandomAccessFile(file, "r");

        } catch (Exception e) {
        }
    }

    /**
    * 先决条件是服务器端先开启
    *
    */
    public void run() {
        try {
            this.tempSocket = new Socket(this.remoteIPString, this.port);
            System.out.println("与服务器连接成功!");
            outSocket = tempSocket.getOutputStream();

            int amount;
            System.out.println("开始发送文件...");
            while ((amount = outFile.read(byteBuffer)) != -1) {
                outSocket.write(byteBuffer, 0, amount);
//                System.out.println("文件发送中...");
            }
            System.out.println("Send File complete");
//            javax.swing.JOptionPane.showMessageDialog(new javax.swing.JFrame(),
//                    "已发送完毕", "提示!", javax.swing.JOptionPane.PLAIN_MESSAGE);
            outFile.close();
            tempSocket.close();

        } catch (IOException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }

    }

    /**
    * 测试方法
    *
    * @param args
    */
    public static void main(String args[]) {
        SendFile sf1 = new SendFile("127.0.0.1", 10000,"C:\\1(1).mp3");
        SendFile sf2 = new SendFile("127.0.0.1", 10000,"C:\\1(2).mp3");
        SendFile sf3 = new SendFile("127.0.0.1", 10000,"C:\\1(3).mp3");
        SendFile sf4 = new SendFile("127.0.0.1", 10000,"C:\\1(4).mp3");
        Thread t1 = new Thread(sf1);
        Thread t2 = new Thread(sf2);
        Thread t3 = new Thread(sf3);
        Thread t4 = new Thread(sf4);
        t1.start();
        t2.start();
        t3.start();
        t4.start();

    }
}