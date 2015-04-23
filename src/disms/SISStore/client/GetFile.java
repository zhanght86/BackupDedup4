package disms.SISStore.client;
/*服务器端接收文件*/
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFileChooser;

/**
* 该类用到的绑定端口初始为10002，如果绑定不成功则试另外的端口；
* 绑定次数用tryBindTimes变量，表示如果绑定失败会对它加一；
* 当前绑定端口由DefaultBindPort+tryBindTimes决定；
* 外界系统(调用此程序的对象)可以获取当前的帮定端口；
* 并告诉客户端服务的端口号以使其能正确连接到该端口上；
* @author
*
*/
//public class GetFile extends Thread {
public class GetFile{

    // 服务套接字等待对方的连接和文件发送
    ServerSocket serSocket;
    // 由服务套接字产生的 套接字
    Socket tempSocket;
    // 用于读取
    InputStream inSocket;
    // 随机访问文件
    RandomAccessFile inFile = null;
    // 临时缓寸区
    byte byteBuffer[] = new byte[1024];
    // 默认用10002端口监听请求
    int defaultBindPort = 10002;
    // 初始的绑定端口次数为0
    int tryBindTimes = 0;
    // 当前绑定的端口号是10002默认端口
    int currentBindPort = defaultBindPort + tryBindTimes;

    /**
    * @构造方法
    * @抛出异常的原因是无法绑定服务的端口
    * */
    public void getTheFile(int port,String filePath) throws Exception {
        try {
            // 绑定服务的端口
            this.bindToServerPort();

        } catch (Exception e) {
            e.printStackTrace();
            // 绑定不成功重试
            System.out.println(e.toString());
            throw new Exception("绑定端口不成功!");

        }
        // 文件选择器以当前的目录打开
//        JFileChooser jfc = new JFileChooser(".");
//        jfc.showSaveDialog(new javax.swing.JFrame());
//        // 获取当前的选择文件引用
//        File savedFile = jfc.getSelectedFile();

        // 已经选择了文件
        File savedFile = new File(filePath);
        if (savedFile != null) {
            // 读取文件的数据，可以每次以快的方式读取数据
            inFile = new RandomAccessFile(savedFile, "rw");

        }
    }
   
   /**
    * @监控线程
    */
    public void run() {
        try {
            if (this.inFile == null) {
                System.out.println("没有选择文件");
                // 关闭服务方套接字
                this.serSocket.close();
                // 没有选择文件
                return;
            }
           
            System.out.println("wait for..." + '\n' + "等待对方接入");
            // 等待对方的连接
            tempSocket = serSocket.accept();
            // 五秒钟连不上将抛出异常
            this.serSocket.setSoTimeout(5000);
            // 获取输入流
            this.inSocket = tempSocket.getInputStream();
           
        } catch (Exception ex) {
            System.out.println(ex.toString());
            ex.printStackTrace();
            return;
        }
       
        // 以下为传送文件代码和 套接字清理工作
        int amount;
        try {
            while ((amount = inSocket.read(byteBuffer)) != -1) {
                inFile.write(byteBuffer, 0, amount);
            }
            // 关闭流
            inSocket.close();
 //           javax.swing.JOptionPane.showMessageDialog(new javax.swing.JFrame(),"已接收成功", "提示!", javax.swing.JOptionPane.PLAIN_MESSAGE);
            System.out.println("Get OK");
            System.out.println("接收完毕!");
            // 关闭文件
            inFile.close();
            // 关闭临时套接字
            tempSocket.close();
            // 关闭服务方套接字
            this.serSocket.close();
           
        } catch (IOException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }

    }

    /**
    * @绑定端口
    * @throws Exception 抛出异常的原因是无法绑定服务的端口
    */
    private void bindToServerPort() throws Exception {
        try {
            // 输出绑定的端口号到当前的控制台上
            System.out.println("试绑定的端口号是:" + this.currentBindPort);
            // 在自己的机器上开一个服务类套接字并等待发送者的连接
           // serSocket.setReuseAddress(true);
            serSocket = new ServerSocket(this.currentBindPort);

        } catch (Exception e) {
            e.printStackTrace();
            // 绑定不成功重试
            System.out.println(e.toString());
            // 试了不止一次了
            this.tryBindTimes = this.tryBindTimes + 1;
           // 可查看试的次数getTryBindedTimes
            this.currentBindPort = this.defaultBindPort + this.tryBindTimes;

            // 如果试的次数超过20次 退出
            if (this.tryBindTimes >= 20) {
                throw new Exception("无法绑定到指定端口" + '\n' + "试了太多次了!");

            }
            // 递归的绑定
            this.bindToServerPort();
        }

        // 输出绑定的端口号到当前的控制台上
        System.out.println("成功绑定的端口号是: " + this.currentBindPort);

    }

    // 获取试绑定的端口
    public int getTryBindedTimes() {
        return this.tryBindTimes;
    }

    // 获取已经绑定的端口
    public int getCurrentBindingPort() {
        return this.currentBindPort;
    }
   

    /**
    * @测试方法
    * @param args
    */
    public static void main(String args[]) {
        GetFile getFile = new GetFile();
        try {

            getFile.getTheFile(10002, "E:\\lib.zip");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("无法传送文件!");
            System.exit(1);
        }
        getFile.run();
        try {

            getFile.getTheFile(10002, "E:\\lib2.zip");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("无法传送文件!");
            System.exit(1);
        }
        getFile.run();

    }

}