package xjtu.dedup.restoremngt;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import org.opendedup.util.OSValidator;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RestoreJobRecord {
	String restoreClientHostName="0.0.0.0";
	String restoreJobConfigPath=OSValidator.getRestoreConfigPath();
	String restorePath="D:\\restore\\";
	String restoredFileName="jj.ppt";
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss"); 
    String currentDate = sdf.format(new Date()); 
	String restoreID="restorejob_"+currentDate;
	int client_threads=10;
	
	String volume_name="sdfs_vol1";
	
	public RestoreJobRecord(){
		
		}
	public void init() throws ParserConfigurationException{
		/*if(!this.getrestorejobconfigfile())
			this.writerestorejobfile();
		else
			this.appendrestorejobtoconfigfile();
			*/
		this.writerestorejobfile();
	}
	
	public void writerestorejobfile() throws ParserConfigurationException{
			File dir = new File(restoreJobConfigPath);
			if(!dir.exists()) {
				System.out.println("making " + dir.getAbsolutePath());
				dir.mkdirs();
			}
			File file = new File(restoreJobConfigPath + "restoreconfig.xml");
			// Create XML DOM document (Memory consuming).
			Document xmldoc = null;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation impl = builder.getDOMImplementation();
			// Document.
			xmldoc = impl.createDocument(null, "restorejob-config", null);
			// Root element.
			Element root = xmldoc.getDocumentElement();
			root.setAttribute("version", "0.0.0");
			Element restorejobs=xmldoc.createElement("restorejobs");
			Element restorejob = xmldoc.createElement("restorejob");
			restorejob.setAttribute("restoreID", this.restoreID);
			restorejob.setAttribute("restoreClientHostName", this.restoreClientHostName);
			restorejob.setAttribute("restorePath", this.restorePath);
			restorejob.setAttribute("restoredFileName", this.restoredFileName);
			restorejob.setAttribute("createTime", this.currentDate);
			restorejob.setAttribute("client_threads", Integer.toString(this.client_threads));
			restorejob.setAttribute("volume_name", this.volume_name);
			restorejobs.appendChild(restorejob);
			root.appendChild(restorejobs);
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
	
	public void writeRestoreConfigToXML(String backupjobid,String backuphostname,String restorePath,String restoredFileName,int client_threads,String volume_name,long filescount,String backupdate) throws ParserConfigurationException{
		File dir = new File(restoreJobConfigPath);
		if(!dir.exists()) {
			System.out.println("making " + dir.getAbsolutePath());
			dir.mkdirs();
		}
		File file = new File(restoreJobConfigPath + "restoreconfig.xml");
		// Create XML DOM document (Memory consuming).
		Document xmldoc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation impl = builder.getDOMImplementation();
		// Document.
		xmldoc = impl.createDocument(null, "restorejob-config", null);
		// Root element.
		Element root = xmldoc.getDocumentElement();
		root.setAttribute("version", "0.0.0");
		Element restorejobs=xmldoc.createElement("restorejobs");
		Element restorejob = xmldoc.createElement("restorejob");
		restorejob.setAttribute("restoreID", this.restoreID);
		restorejob.setAttribute("backupJobID", backupjobid);
		restorejob.setAttribute("BackupClientHostName", backuphostname);
		restorejob.setAttribute("restoreClientHostName", this.restoreClientHostName);
		restorejob.setAttribute("restorePath", restorePath);
		restorejob.setAttribute("restoredFileName", restoredFileName);
		restorejob.setAttribute("createTime", this.currentDate);
		restorejob.setAttribute("client_threads", Integer.toString(client_threads));
		restorejob.setAttribute("volume_name", volume_name);
		restorejob.setAttribute("BackupJobFilesCount", Long.toString(filescount));
		restorejob.setAttribute("backupdate", backupdate);
		try {
			restorejob.setAttribute("hostname", InetAddress.getLocalHost().getHostName());
		} catch (DOMException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		restorejobs.appendChild(restorejob);
		root.appendChild(restorejobs);
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
	
	public void appendrestorejobtoconfigfile(){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		String filepath=this.getrestorejobconfigpath();
		Document doc = null;
		try{
		DocumentBuilder builder = factory.newDocumentBuilder();
		doc=builder.parse(new File(filepath));
		doc.normalize();
//		Element backupjobs=doc.getDocumentElement();
		NodeList nl = doc.getElementsByTagName("restorejobs");
		Node nod = nl.item(0);
		Element restorejob=doc.createElement("restorejob");
		restorejob.setAttribute("restoreID", this.restoreID);
		restorejob.setAttribute("restoreClientHostName", this.restoreClientHostName);
		restorejob.setAttribute("restorePath", this.restorePath);
		restorejob.setAttribute("restoredFileName", this.restoredFileName);
		restorejob.setAttribute("createTime", this.currentDate);
		restorejob.setAttribute("client_threads", Integer.toString(this.client_threads));
		restorejob.setAttribute("volume_name", this.volume_name);
		nod.appendChild(restorejob);
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
	
	public String getrestorejobconfigpath(){
		File file = new File(restoreJobConfigPath + "restoreconfig.xml");
		return file.getAbsolutePath();
	}
	public boolean getrestorejobconfigfile(){
		File file=new File(restoreJobConfigPath);
		if(file.exists())
			return true;
		else 
			return false;
	}
}
