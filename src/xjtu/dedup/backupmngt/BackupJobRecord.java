package xjtu.dedup.backupmngt;


import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

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

import org.opendedup.sdfs.Main;
import org.opendedup.util.BackupDedupLogger;
import org.opendedup.util.OSValidator;
import org.opendedup.util.SDFSLogger;
import org.opendedup.util.date.WeekPolicy;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import frame.TempStorage;




/*
 * 备份任务记录类，用来记录备份任务配置文件，以供备份客户端和服务器使用。
 * */
public class BackupJobRecord {
	String configpath=OSValidator.getBackupConfigPath();
	String backupsrc="D:\\etc1,D:\\etc2,D:\\etc3";
	String backupdest="E:\\tempdir\\backup";//需要备份的数据，供压缩测试使用
	String zipdir="D:\\tempdir";
	Boolean iscompressed=false;
	int type=1;
	int client_threads=10;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss"); 
	SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd"); 
    String currentDate = sdf.format(new Date()); 
    String currentDateformat=sdf1.format(new Date());
	String backupID="backupjob_"+currentDate;
	String backup_volume_name="sdfs_vol1";
	String backupconfigname=backup_volume_name+currentDateformat+"backupjob.xml";
	//存储备份任务的hash表
	private Hashtable<String,BackupJob> jobs;
	//存储备份任务文件
	private  File jobsFile;
	

public void init() throws ParserConfigurationException, IOException, SAXException{
	/*if(!this.getbackupjobconfigfile())
	    this.writebackupjobconfig();
	else
		this.appendbackupjobtoconfig();*/
	   this.writebackupjobconfig();
}

public void writebackupjobconfig()throws ParserConfigurationException,IOException{
	File dir = new File(configpath);
	if(!dir.exists()) {
		System.out.println("making " + dir.getAbsolutePath());
		dir.mkdirs();
	}
	File file = new File(configpath+backupconfigname);
	// Create XML DOM document (Memory consuming).
	Document xmldoc = null;
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder = factory.newDocumentBuilder();
	DOMImplementation impl = builder.getDOMImplementation();
	// Document.
	xmldoc = impl.createDocument(null, "backupjob-config", null);
	// Root element.
	Element root = xmldoc.getDocumentElement();
	root.setAttribute("version", "0.0.0");
	Element backupjobs=xmldoc.createElement("backupjobs");
	Element backupjob = xmldoc.createElement("backupjob");
	backupjob.setAttribute("backupID", this.backupID);
	backupjob.setAttribute("backupsrc", this.backupsrc);
	backupjob.setAttribute("backupdest", this.backupdest);
	backupjob.setAttribute("type", Integer.toString(this.type));
	backupjob.setAttribute("iscompressed",Boolean.toString(this.iscompressed));
	backupjob.setAttribute("zipdir", this.zipdir);
	backupjob.setAttribute("createTime", this.currentDate);
	backupjob.setAttribute("client_threads", Integer.toString(this.client_threads));
	backupjob.setAttribute("backup_volume_name",this.backup_volume_name);
	backupjob.setAttribute("lastRun", this.currentDate);
	backupjobs.appendChild(backupjob);
	root.appendChild(backupjobs);
	try {
		// Prepare the DOM document for writing
		Source source = new DOMSource(xmldoc);

		Result result = new StreamResult(file);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance()
				.newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.transform(source, result);
	} catch (TransformerConfigurationException e) {
		e.printStackTrace();
	} catch (TransformerException e) {
		e.printStackTrace();
	}
}
/*
 * 向xml写用户提交的备份作业记录——测试
 * */
public void writeBackupConfigToXML(String backupjobname,String backupsrc,int type,boolean isCompressed,int client_threads,String backup_volume_name)throws ParserConfigurationException,IOException, ParseException{
	SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd"); 
    String currentDate = sdf.format(new Date()); 
    String currentDateformat=sdf1.format(new Date());
	String backupID=backupjobname+"_"+currentDate;
	File dir = new File(configpath);
	if(!dir.exists()) {
		System.out.println("making " + dir.getAbsolutePath());
		dir.mkdirs();
	}
	File file = new File(configpath+System.getProperty("user.name")+"_bakcupjob.xml");
	jobsFile = file;
	jobs = new Hashtable<String,BackupJob>();
	if (jobsFile.exists() && jobsFile.isFile()) {
		readXMLFile();
		//appendXMLFile(backupjobname,backupsrc,type,isCompressed,client_threads,backup_volume_name);
	}else{
		//writeXMLFile();
	}
}
public void writeBackupConfigToXML(String backupjobname,String backupsrc,WeekPolicy wp,int client_threads,String backup_volume_name,long length,long count)throws ParserConfigurationException,IOException, ParseException{
	SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd"); 
    String currentDate = sdf.format(new Date()); 
    String currentDateformat=sdf1.format(new Date());
	String backupID=backupjobname+"_"+currentDate;
	File dir = new File(configpath);
	if(!dir.exists()) {
		System.out.println("making " + dir.getAbsolutePath());
		dir.mkdirs();
	}
	File file = new File(configpath+System.getProperty("user.name")+"_bakcupjob.xml");
	System.out.println("making "+file.getAbsolutePath());
	jobsFile = file;
	jobs = new Hashtable<String,BackupJob>();
	
//	TempStorage.backupName=backupjobname;
//	TempStorage.backupSrc=backupsrc;
//	TempStorage.backupVolume=backup_volume_name;
	
	if(jobsFile.exists()&&jobsFile.isFile()){
		appendXMLFile(backupjobname,backupsrc,wp,client_threads,backup_volume_name,length,count);
	}else{
		writeXMLFile(backupjobname,backupsrc,wp,backup_volume_name,length,count);
	}
}
public void readXMLFile() throws ParseException{
		if(!jobsFile.exists()){
			return;
		}
		try {
			Document doc = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().parse(jobsFile);
			doc.getDocumentElement().normalize();
			Element backupjobs=(Element)doc.getElementsByTagName("backupjobs").item(0);
			NodeList backupjobList=backupjobs.getElementsByTagName("backupjob");
			if(backupjobList.getLength() != 0) {
				for(int s = 0; s < backupjobList.getLength(); s++) {
					String backupjobid, source, dest_volume,lastrun;
					int backup_type,compress_type,encrypt_type;
					Node node = backupjobList.item(s);
					
					if(node.getNodeType() == Node.ELEMENT_NODE) {
						Element elem = (Element) node;
						backupjobid=elem.getAttribute("backupID").trim();
						source= elem.getAttribute("backupsrc").trim();
						BackupJob.backupdest= elem.getAttribute("backupdest").trim();
						BackupJob.zipdir = elem.getAttribute("zipdir").trim();
						compress_type=Integer.parseInt(elem.getAttribute("compresstype"));
					//	BackupJob.iscompressed=Boolean.parseBoolean(backupjobElement.getAttribute("iscompressed"));
						encrypt_type=Integer.parseInt(elem.getAttribute("encrypttype"));
						backup_type=Integer.parseInt(elem.getAttribute("type"));
						BackupJob.creatTime=elem.getAttribute("createTime");
						lastrun=elem.getAttribute("lastRun");
						BackupJob.client_threads=Integer.parseInt(elem.getAttribute("client_threads"));
						dest_volume=elem.getAttribute("backup_volume_name");
						if(backupjobid.equals("")) {
							System.out.println("Parse error: empty backup job name!");
							continue;//跳出本次循环，继续下一次循环
						}
						
						jobs.put(backupjobid, new BackupJob(backupjobid,source,dest_volume,lastrun,backup_type,compress_type,encrypt_type));
					}
				}
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
public void writeXMLFile(String backupjobname,String backupsrc,int type,int compressetype,int encrypttype,String backup_volume_name) throws ParserConfigurationException{
		Document xmldoc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation impl = builder.getDOMImplementation();
		// Document.
		xmldoc = impl.createDocument(null, "backupjob-config", null);
		// Root element.
		Element root = xmldoc.getDocumentElement();
		root.setAttribute("version", "0.0.0");
		Element backupjobs=xmldoc.createElement("backupjobs");
		Element backupjob = xmldoc.createElement("backupjob");
		backupjob.setAttribute("backupID", backupjobname);
		backupjob.setAttribute("backupsrc", backupsrc);
		backupjob.setAttribute("backupdest", this.backupdest);
		backupjob.setAttribute("type", Integer.toString(type));
		backupjob.setAttribute("compresstype",Integer.toString(compressetype));
		backupjob.setAttribute("encrypttype", Integer.toString(encrypttype));
		backupjob.setAttribute("zipdir", this.zipdir);
		backupjob.setAttribute("createTime", this.currentDate);
		backupjob.setAttribute("client_threads", Integer.toString(client_threads));
		backupjob.setAttribute("backup_volume_name",backup_volume_name);
		backupjob.setAttribute("lastRun", this.currentDate);
		backupjobs.appendChild(backupjob);
		root.appendChild(backupjobs);
		try {
			// Prepare the DOM document for writing
			Source source = new DOMSource(xmldoc);

			Result result = new StreamResult(jobsFile);

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	public void writeXMLFile(String backupjobname,String backupsrc,WeekPolicy wp,String backup_volume_name,long length,long count) throws ParserConfigurationException, DOMException{
		Document xmldoc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation impl = builder.getDOMImplementation();
		// Document.
		xmldoc = impl.createDocument(null, "backupjob-config", null);
		// Root element.
		Element root = xmldoc.getDocumentElement();
		root.setAttribute("version", "0.0.0");
		Element backupjobs=xmldoc.createElement("backupjobs");
		Element backupjob = xmldoc.createElement("backupjob");
		backupjob.setAttribute("backupID", backupjobname);
		backupjob.setAttribute("backupsrc", backupsrc);
		backupjob.setAttribute("backupdest", this.backupdest);
		backupjob.setAttribute("user.name", System.getProperty("user.name"));
//		TempStorage.backupUserName=System.getProperty("user.name");
		backupjob.setAttribute("backupstate", "ready");
		try {
			backupjob.setAttribute("hostname", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		backupjob.setAttribute("backupFilesLength", Long.toString(length));
		backupjob.setAttribute("backupJobFilesCount", Long.toString(count));
		if(wp.Monday!=-1){
			backupjob.setAttribute("Monday.backuptype", Integer.toString(wp.Monday));
		}else{
			backupjob.setAttribute("Monday.backuptype", "-1");
		}
		if(wp.Tuesday!=-1){
			backupjob.setAttribute("Tuesday.backuptype", Integer.toString(wp.Tuesday));
		}else{
			backupjob.setAttribute("Tuesday.backuptype", "-1");
		}
		if(wp.Wednesday!=-1){
			backupjob.setAttribute("Wednesday.backuptype", Integer.toString(wp.Wednesday));
		}else{
			backupjob.setAttribute("Wednesday.backuptype", "-1");
		}
		if(wp.Thursday!=-1){
			backupjob.setAttribute("Thursday.backuptype", Integer.toString(wp.Thursday));
		}else{
			backupjob.setAttribute("Thursday.backuptype", "-1");
		}
		if(wp.Friday!=-1){
			backupjob.setAttribute("Friday.backuptype", Integer.toString(wp.Friday));
		}else{
			backupjob.setAttribute("Friday.backuptype", "-1");
		}
		if(wp.Saturday!=-1){
			backupjob.setAttribute("Saturday.backuptype", Integer.toString(wp.Saturday));
		}else{
			backupjob.setAttribute("Saturday.backuptype", "-1");
		}
		if(wp.Sunday!=-1){
			backupjob.setAttribute("Sunday.backuptype", Integer.toString(wp.Sunday));
		}else{
			backupjob.setAttribute("Sunday.backuptype", "-1");
		}
		backupjob.setAttribute("zipdir", this.zipdir);
		backupjob.setAttribute("createTime", this.currentDate);
		backupjob.setAttribute("client_threads", Integer.toString(client_threads));
		backupjob.setAttribute("backup_volume_name",backup_volume_name);
		backupjob.setAttribute("lastRun", this.currentDate);
		backupjobs.appendChild(backupjob);
		root.appendChild(backupjobs);
		try {
			// Prepare the DOM document for writing
			Source source = new DOMSource(xmldoc);

			Result result = new StreamResult(jobsFile);

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	public void appendXMLFile(String backupjobname,String backupsrc,int type,int compresstype,int encrypttype,String backup_volume_name){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		String filepath=this.getbackupjobconfigpath();
		Document doc = null;
		try{
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc=builder.parse(new File(filepath));
		doc.normalize();
//		Element backupjobs=doc.getDocumentElement();
		NodeList nl = doc.getElementsByTagName("backupjobs");
		Node nod = nl.item(0);
		Element backupjob=doc.createElement("backupjob");
		backupjob.setAttribute("backupID", backupjobname);
		backupjob.setAttribute("backupsrc", backupsrc);
		backupjob.setAttribute("backupdest", this.backupdest);
		backupjob.setAttribute("type", Integer.toString(this.type));
		backupjob.setAttribute("compresstype",Integer.toString(compresstype));
		backupjob.setAttribute("encrypttype", Integer.toString(encrypttype));
		backupjob.setAttribute("zipdir", this.zipdir);
		backupjob.setAttribute("createTime", this.currentDate);
		backupjob.setAttribute("client_threads", Integer.toString(this.client_threads));
		backupjob.setAttribute("backup_volume_name", backup_volume_name);
		backupjob.setAttribute("lastRun", this.currentDate);
		nod.appendChild(backupjob);
		}catch (ParserConfigurationException e) {
			// TODO: handle exception
			System.out.println("Construct document builder error:"+e);
		}catch (SAXException e) {
			// TODO: handle exception
			System.out.println("Parse xml file error:"+e);
		}catch(IOException e){
			System.out.println("Read xml file error:"+e);
		}

		try{
			// Prepare the DOM document for writing
			Source source = new DOMSource(doc);

			Result result = new StreamResult(new File(filepath));

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	public void appendXMLFile(String backupjobname,String backupsrc,WeekPolicy wp,int client_threads,String backup_volume_name,long length,long count){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		String filepath=this.getbackupjobconfigpath();
		Document doc = null;
		try{
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc=builder.parse(new File(filepath));
		doc.normalize();
//		Element backupjobs=doc.getDocumentElement();
		NodeList nl = doc.getElementsByTagName("backupjobs");
		Node nod = nl.item(0);
		Element backupjob=doc.createElement("backupjob");
		backupjob.setAttribute("backupID", backupjobname);
		backupjob.setAttribute("backupsrc", backupsrc);
		backupjob.setAttribute("backupdest", this.backupdest);
		backupjob.setAttribute("backupFilesLength", Long.toString(length));
		backupjob.setAttribute("backupJobFilesCount",Long.toString(count));
		backupjob.setAttribute("user.name", System.getProperty("user.name"));
//		TempStorage.backupUserName=System.getProperty("user.name");
		backupjob.setAttribute("backupstate", "ready");
		try{
		backupjob.setAttribute("hostname", InetAddress.getLocalHost().getHostName());
		}catch (UnknownHostException e1) {
			// TODO: handle exception
			e1.printStackTrace();
		}
		if(wp.Monday!=-1){
			backupjob.setAttribute("Monday.backuptype", Integer.toString(wp.Monday));
		}else{
			backupjob.setAttribute("Monday.backuptype", "-1");
		}
		if(wp.Tuesday!=-1){
			backupjob.setAttribute("Tuesday.backuptype", Integer.toString(wp.Tuesday));
		}else{
			backupjob.setAttribute("Tuesday.backuptype", "-1");
		}
		if(wp.Wednesday!=-1){
			backupjob.setAttribute("Wednesday.backuptype", Integer.toString(wp.Wednesday));
		}else{
			backupjob.setAttribute("Wednesday.backuptype", "-1");
		}
		if(wp.Thursday!=-1){
			backupjob.setAttribute("Thursday.backuptype", Integer.toString(wp.Thursday));
		}else{
			backupjob.setAttribute("Thursday.backuptype", "-1");
		}
		if(wp.Friday!=-1){
			backupjob.setAttribute("Friday.backuptype", Integer.toString(wp.Friday));
		}else{
			backupjob.setAttribute("Friday.backuptype", "-1");
		}
		if(wp.Saturday!=-1){
			backupjob.setAttribute("Saturday.backuptype", Integer.toString(wp.Saturday));
		}else{
			backupjob.setAttribute("Saturday.backuptype", "-1");
		}
		if(wp.Sunday!=-1){
			backupjob.setAttribute("Sunday.backuptype", Integer.toString(wp.Sunday));
		}else{
			backupjob.setAttribute("Sunday.backuptype", "-1");
		}
		backupjob.setAttribute("zipdir", this.zipdir);
		backupjob.setAttribute("createTime", this.currentDate);
		backupjob.setAttribute("client_threads", Integer.toString(this.client_threads));
		backupjob.setAttribute("backup_volume_name", backup_volume_name);
		backupjob.setAttribute("lastRun", this.currentDate);
		nod.appendChild(backupjob);
		}catch (ParserConfigurationException e) {
			// TODO: handle exception
			System.out.println("Construct document builder error:"+e);
		}catch (SAXException e) {
			// TODO: handle exception
			System.out.println("Parse xml file error:"+e);
		}catch(IOException e){
			System.out.println("Read xml file error:"+e);
		}

		try{
			// Prepare the DOM document for writing
			Source source = new DOMSource(doc);

			Result result = new StreamResult(new File(filepath));

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
/*
 * 界面显示
 * */
public void writeBackupConfigToXML(String backupjobname,String backupsrc,int type,int compressetype,int encrypttype,String backup_volume_name)throws ParserConfigurationException,IOException, ParseException{
	SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd"); 
    String currentDate = sdf.format(new Date()); 
    String currentDateformat=sdf1.format(new Date());
	String backupID=backupjobname+"_"+currentDate;
	File dir = new File(configpath);
	if(!dir.exists()) {
		System.out.println("making " + dir.getAbsolutePath());
		dir.mkdirs();
	}
	File file = new File(configpath+System.getProperty("user.name")+"_bakcupjob.xml");
	if(file.exists())
	{
		file.delete();
		file.createNewFile();
	}
	jobsFile = file;
	writeXMLFile(backupjobname,backupsrc,type,compressetype, encrypttype,backup_volume_name);
	
}
/*
 * 向配置文件内增加新的备份任务
 * */
public void appendbackupjobtoconfig() throws ParserConfigurationException, SAXException, IOException{
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	String filepath=this.getbackupjobconfigpath();
	Document doc = null;
	try{
	DocumentBuilder builder = factory.newDocumentBuilder();
	doc=builder.parse(new File(filepath));
	doc.normalize();
//	Element backupjobs=doc.getDocumentElement();
	NodeList nl = doc.getElementsByTagName("backupjobs");
	Node nod = nl.item(0);
	Element backupjob=doc.createElement("backupjob");
	backupjob.setAttribute("backupID", this.backupID);
	backupjob.setAttribute("backupsrc", this.backupsrc);
	backupjob.setAttribute("backupdest", this.backupdest);
	backupjob.setAttribute("type", Integer.toString(this.type));
	backupjob.setAttribute("iscompressed",Boolean.toString(this.iscompressed));
	backupjob.setAttribute("zipdir", this.zipdir);
	backupjob.setAttribute("createTime", this.currentDate);
	backupjob.setAttribute("client_threads", Integer.toString(this.client_threads));
	nod.appendChild(backupjob);
	}catch (ParserConfigurationException e) {
		// TODO: handle exception
		System.out.println("Construct document builder error:"+e);
	}catch (SAXException e) {
		// TODO: handle exception
		System.out.println("Parse xml file error:"+e);
	}catch(IOException e){
		System.out.println("Read xml file error:"+e);
	}

	try{
		// Prepare the DOM document for writing
		Source source = new DOMSource(doc);

		Result result = new StreamResult(new File(filepath));

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance()
				.newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.transform(source, result);
	} catch (TransformerConfigurationException e) {
		e.printStackTrace();
	} catch (TransformerException e) {
		e.printStackTrace();
	}
}
/*
 * 更新备份作业内备份状态
 * */
public void updateBackupState(String backupjobid,String configfile) throws ParserConfigurationException, SAXException, IOException{
		File file = new File(configfile);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();
		Element backupjobs = (Element) doc.getElementsByTagName("backupjobs").item(0);
		NodeList nl=backupjobs.getElementsByTagName("backupjob");
		for(int bj=0;bj<nl.getLength();bj++){
			Element backupjobElement = (Element) nl.item(bj);
			if(backupjobid.equals(backupjobElement.getAttribute("backupID"))){
//				SDFSLogger.getLog().info("Update the backup state of the "+backupjobid);
				BackupDedupLogger.getbackupLog().info("Update the backup state of the "+backupjobid);
				backupjobElement.setAttribute("backupstate", "done");
			}
		}

		try {
			// Prepare the DOM document for writing
			Source source = new DOMSource(doc);

			Result result = new StreamResult(file);

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
public String getbackupjobconfigpath(){
	File file = new File(configpath +System.getProperty("user.name")+"_bakcupjob.xml");
	if(!file.exists())
		return null;
	else
		return file.getAbsolutePath();
}
public boolean getbackupjobconfigfile(){
	File file=new File(configpath+backupconfigname);
	if(file.exists())
		return true;
	else 
		return false;
}
}