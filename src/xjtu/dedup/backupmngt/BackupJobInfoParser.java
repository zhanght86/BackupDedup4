package xjtu.dedup.backupmngt;

import java.io.File;
import java.io.IOException;
import java.sql.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendedup.util.SDFSLogger;
import org.opendedup.util.date.WeekPolicy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import xjtu.dedup.multithread.BackupClient;
import xjtu.dedup.multithread.BackupClientPool;
/*
 * 备份任务信息类，用来解析备份配置文件信息
 * */
public class BackupJobInfoParser {
	public void parsebackupjobconfig(String configpath)throws IOException{
		try {
			File file = new File(configpath);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
		
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			
			Element backupjobs=(Element)doc.getElementsByTagName("backupjobs").item(0);
			NodeList nl=backupjobs.getElementsByTagName("backupjob");
			for(int bj=0;bj<nl.getLength();bj++)
			{
				Element backupjobElement = (Element) nl.item(bj);
				BackupJob.backupjobid=backupjobElement.getAttribute("backupID").trim();
				BackupJob.backupsrc = backupjobElement.getAttribute("backupsrc").trim();
				BackupJob.backupdest= backupjobElement.getAttribute("backupdest").trim();
				BackupJob.zipdir = backupjobElement.getAttribute("zipdir").trim();
				BackupJob.backupSize=Long.parseLong(backupjobElement.getAttribute("backupFilesLength"));
				BackupJob.filesCount=Long.parseLong(backupjobElement.getAttribute("backupJobFilesCount"));
				BackupJob.weekp=new WeekPolicy(Integer.parseInt(backupjobElement.getAttribute("Monday.backuptype")),Integer.parseInt(backupjobElement.getAttribute("Tuesday.backuptype")),
						Integer.parseInt(backupjobElement.getAttribute("Wednesday.backuptype")),Integer.parseInt(backupjobElement.getAttribute("Thursday.backuptype")),
						Integer.parseInt(backupjobElement.getAttribute("Friday.backuptype")),Integer.parseInt(backupjobElement.getAttribute("Saturday.backuptype")),
						Integer.parseInt(backupjobElement.getAttribute("Sunday.backuptype")));
				BackupJob.creatTime=backupjobElement.getAttribute("createTime");
				BackupJob.client_threads=Integer.parseInt(backupjobElement.getAttribute("client_threads"));
				BackupJob.backup_volume_name=backupjobElement.getAttribute("backup_volume_name");
				BackupJob.backupClientHostName=backupjobElement.getAttribute("hostname");
				//备份客户端的并发控制模块
			//	BackupClient bc=new BackupClient(BackupJob.backupClientHostName,BackupJob.clientPort,BackupJob.backupServerHostName,BackupJob.serverPort,
			//			BackupJob.backupsrc,BackupJob.compresstype,BackupJob.encrypttype,BackupJob.type,BackupJob.creatTime,BackupJob.backup_volume_name);
				BackupClient bc=new BackupClient(BackupJob.backupjobid,BackupJob.backupClientHostName,BackupJob.clientPort,BackupJob.backupServerHostName,BackupJob.serverPort,
								BackupJob.backupsrc,BackupJob.weekp,BackupJob.creatTime,BackupJob.backup_volume_name,BackupJob.backupSize,BackupJob.filesCount);
				
			//	BackupServiceProxy.backupclients.put(BackupJob.backupjobid, new BackupClientPool(bc,BackupJob.backupjobid,BackupJob.client_threads));
				BackupServiceProxy.backupclients.put(BackupJob.backupjobid, bc);
			}
			
			
		} catch (Exception e) {
			SDFSLogger.getLog().fatal("unable to parse config file ["+configpath+"]", e);
		}
}
//	
public String[][] getBackupInfo(String configfile){
	String[][] backupinfo=null;
	try {
		File file = new File(configfile);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
	
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();
		
		Element backupjobs=(Element)doc.getElementsByTagName("backupjobs").item(0);
		NodeList nl=backupjobs.getElementsByTagName("backupjob");
		backupinfo=new String[nl.getLength()][5];
		for(int bj=0;bj<nl.getLength();bj++)
		{
			Element backupjobElement = (Element) nl.item(bj);
			backupinfo[bj][0]=backupjobElement.getAttribute("backupID").trim();
			backupinfo[bj][1]=backupjobElement.getAttribute("user.name").trim();
			backupinfo[bj][2]=backupjobElement.getAttribute("backupsrc").trim();
			backupinfo[bj][3]=backupjobElement.getAttribute("backup_volume_name").trim();
			backupinfo[bj][4]=backupjobElement.getAttribute("backupstate").trim();
		}

		
	} catch (Exception e) {
		SDFSLogger.getLog().fatal("unable to get backup info from config file ["+configfile+"]", e);
	}
	return backupinfo;
}
public String[][] parserestorejobconfig(String configpath){
		String[][] restoreinfo=null;
		try {
			File file = new File(configpath);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
		
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			
			Element backupjobs=(Element)doc.getElementsByTagName("backupjobs").item(0);
			NodeList nl=backupjobs.getElementsByTagName("backupjob");
			restoreinfo=new String[nl.getLength()][7];
			for(int bj=0;bj<nl.getLength();bj++)
			{
				Element backupjobElement = (Element) nl.item(bj);
				restoreinfo[bj][0]=backupjobElement.getAttribute("backupID").trim();
				restoreinfo[bj][1]=backupjobElement.getAttribute("hostname").trim();
				restoreinfo[bj][2] = backupjobElement.getAttribute("backupsrc").trim();
				restoreinfo[bj][3]=backupjobElement.getAttribute("backupFilesLength").trim();
				restoreinfo[bj][4]=backupjobElement.getAttribute("createTime").trim();
				restoreinfo[bj][5]=backupjobElement.getAttribute("backup_volume_name");
				restoreinfo[bj][6]=backupjobElement.getAttribute("backupJobFilesCount").trim();
			}		
			
		} catch (Exception e) {
			SDFSLogger.getLog().fatal("unable to parse config file ["+configpath+"]", e);
		}
		return restoreinfo;
	}

public void updateBackupJobConfigtoDelete(String configpath,String backupTaskID,String backupSrc)throws IOException{
	try {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		File file = new File(configpath);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();
		
		Element backupjobs=(Element)doc.getElementsByTagName("backupjobs").item(0);
		NodeList nl=backupjobs.getElementsByTagName("backupjob");
		for(int bj=0;bj<nl.getLength();bj++)
		{
			Element backupjobElement = (Element) nl.item(bj);
			if(backupTaskID.equals(backupjobElement.getAttribute("backupID").trim())&& backupSrc.equals(backupjobElement.getAttribute("backupsrc").trim())){
				Node parent=(Node) backupjobElement.getParentNode();
				parent.removeChild(backupjobElement);
			}
		}
		// Prepare the DOM document for writing
		Source source = new DOMSource(doc);

		Result result = new StreamResult(new File(configpath));

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance()
				.newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.transform(source, result);
	} catch (TransformerConfigurationException e) {
		e.printStackTrace();
	} catch (TransformerException e) {
		e.printStackTrace();
	} catch (ParserConfigurationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (SAXException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
}

