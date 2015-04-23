package xjtu.dedup.restoremngt;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.opendedup.util.SDFSLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import xjtu.dedup.backupmngt.BackupServiceProxy;
import xjtu.dedup.multithread.RestoreClient;
import xjtu.dedup.multithread.RestoreClientPool;

public class RestoreJobInfoParser {
	public void parserestorejobconfigfile(String restoreconfigfilepath) throws IOException{
		try {
			File file = new File(restoreconfigfilepath);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			
			Element backupjobs=(Element)doc.getElementsByTagName("restorejobs").item(0);
			NodeList nl=backupjobs.getElementsByTagName("restorejob");
	        for(int rj=0;rj<nl.getLength();rj++)
	        {
	        Element restorejobElement = (Element) nl.item(rj);
			RestoreJob.restoreJobID=restorejobElement.getAttribute("restoreID").trim();
			RestoreJob.backupJobID=restorejobElement.getAttribute("backupJobID").trim();
		//	RestoreJob.restoreClientHostName= restorejobElement.getAttribute("restoreClientHostName").trim();
			RestoreJob.restorePath= restorejobElement.getAttribute("restorePath").trim();
			RestoreJob.restoredFileName = restorejobElement.getAttribute("restoredFileName").trim();
			RestoreJob.createTime=restorejobElement.getAttribute("createTime");
			RestoreJob.client_threads=Integer.parseInt(restorejobElement.getAttribute("client_threads"));
			RestoreJob.volume_name=restorejobElement.getAttribute("volume_name").trim();
			RestoreJob.backupJobFilesCount=Long.parseLong(restorejobElement.getAttribute("BackupJobFilesCount").trim());
			RestoreJob.backupdate=restorejobElement.getAttribute("backupdate").trim();
			RestoreJob.restoreClientHostName=restorejobElement.getAttribute("hostname").trim();
			RestoreJob.BackupClientHostName=restorejobElement.getAttribute("BackupClientHostName").trim();
	//		RestoreJob.compType=Integer.parseInt(restorejobElement.getAttribute("compType").trim());
			//恢复客户端的并发控制模块
//			System.out.println("Add the restore object to threadpool!");
			RestoreClient rc=new RestoreClient(RestoreJob.restoreClientHostName,RestoreJob.BackupClientHostName,RestoreJob.restorePath,
					RestoreJob.restoredFileName,RestoreJob.createTime,RestoreJob.volume_name,RestoreJob.backupJobFilesCount,RestoreJob.backupdate,RestoreJob.compType);
			BackupServiceProxy.restoreclients.put(RestoreJob.restoreJobID, rc);
	        }

		} catch (Exception e) {
			SDFSLogger.getLog().fatal("unable to parse config file ["+restoreconfigfilepath+"]", e);
		}
	}
}
