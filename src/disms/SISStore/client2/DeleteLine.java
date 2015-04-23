package disms.SISStore.client2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


public class DeleteLine
{
	DeleteLine(String delLine,String filePath) throws Exception
	{
		File file = new File(filePath);
		String rl = null;
		String special = delLine;
		StringBuffer bf = new StringBuffer();
		BufferedReader br = new BufferedReader(new FileReader(file));
		while(( rl = br.readLine()) != null)
		{
			rl = rl.trim();
			if(rl.indexOf(special) == -1){
			bf.append(rl).append("\r\n");
			}
		}
		br.close();
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write(bf.toString());
		out.flush();
		out.close();
	}
	public static void main(String[] args) throws Exception{
		new DeleteLine("63","D:\\clientSIStemp.txt");
	}
}
