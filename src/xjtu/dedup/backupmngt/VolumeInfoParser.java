package xjtu.dedup.backupmngt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.opendedup.util.OSValidator;
/**
 * 解析volume的信息，包括卷名、卷大小、分块大小、是否本地卷等
 * */

public class VolumeInfoParser {
	public static int getTypeOfVolume(String backup_volume_name){
		String backup_path=OSValidator.getBackupConfigPath();
		File volumesfile=new File(backup_path+"volumesfile.info");
		if(!volumesfile.exists())
			try {
				volumesfile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		BufferedReader bReader = null;
		try {
			bReader = new BufferedReader(new FileReader(volumesfile));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String arecord="";
		String[] str;
		int volumetype=-1;
		try{
			while((arecord=bReader.readLine())!=null){
				str=arecord.split(" ");
				if(str[0].equals(backup_volume_name)){
					volumetype=typeParser(str[3]);
					break;
				}
			}
			bReader.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return volumetype;
	}
	/*
	 * @param deduptype
	 * 			the type of deduplication method.
	 * 0 represent FSP,1 represent CDC,2 represent SB,3 represent SIS,4 represent SDFS
	 * */
	public static int typeParser(String volumetype){
		int vtype=-1;;
		if(volumetype.equals("fsp"))
			vtype=0;
		else if(volumetype.equals("cdc"))
			vtype=1;
		else if(volumetype.equals("sw"))
			vtype=2;
		else if(volumetype.equals("sis"))
			vtype=3;
		else if(volumetype.equals("sdfs"))
			vtype=4;
		return vtype;
	}
	public static void main(String args[]) throws FileNotFoundException{
		System.out.println(getTypeOfVolume("sis_vol1"));
	}
}
