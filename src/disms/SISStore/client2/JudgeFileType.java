package disms.SISStore.client2;

import java.io.*;

/*************************************
1. 通过后缀名，如exe,jpg,bmp,rar,zip等等。

2. 通过读取文件，获取文件的Content-type来判断。

3. 通过读取文件流，根据文件流中特定的一些字节标识来区分不同类型的文件。

4. 若是图片，则通过缩放来判断，可以缩放的为图片，不可以的则不是。
***************************************/

public class JudgeFileType{
	public static String getEXTName(String FileName){
		File file =new File(FileName);
		String fileName=file.getName();
		String prefix=fileName.substring(fileName.lastIndexOf(".")+1);
		return prefix;
	}
	public static void main(String args[]){
	    File f =new File("TileTest.java.exe");
	    String fileName=f.getName();
	    String prefix=fileName.substring(fileName.lastIndexOf(".")+1);
	    System.out.println(prefix);
	}
}