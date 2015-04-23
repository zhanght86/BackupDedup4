package org.opendedup.sdfs;

import java.io.File;
import java.io.IOException;

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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.opendedup.util.OSValidator;
import org.opendedup.util.StringUtils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RoutingConfigWriter {
	String servername="server1";
	String hostname="202.117.49.253";
	int network_port=2222;
	boolean enable_udp=false;
	boolean compress=false;
	int network_threads=8;
	
	public void writeConfigFile(String name,String host,int port,boolean enable_udp,boolean compress,int threads){
		this.servername=name;
		this.hostname=host;
		this.network_port=port;
		this.enable_udp=enable_udp;
		this.compress=compress;
		this.network_threads=threads;
		try {
			this.writeConfigFile();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeConfigFile() throws ParserConfigurationException,
	IOException {
		File dir = new File(OSValidator.getConfigPath());
		if(!dir.exists()) {
			System.out.println("making" + dir.getAbsolutePath());
			dir.mkdirs();
		}
		File file = new File(OSValidator.getConfigPath() +"routing1-config.xml");
		// Create XML DOM document (Memory consuming).
		Document xmldoc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation impl = builder.getDOMImplementation();
		// Document.
		xmldoc = impl.createDocument(null, "routing-config", null);
		// Root element.
		Element root = xmldoc.getDocumentElement();

		Element servers= xmldoc.createElement("servers");
		Element server=xmldoc.createElement("server");
		server.setAttribute("name", this.servername);
		server.setAttribute("host", this.hostname);
		server.setAttribute("port", Integer.toString(this.network_port));
		server.setAttribute("enable-udp", Boolean.toString(this.enable_udp));
		server.setAttribute("compress", Boolean.toString(this.compress));
		server.setAttribute("network-threads", Integer.toString(this.network_threads));
		servers.appendChild(server);
		root.appendChild(servers);
		Element chunks= xmldoc.createElement("chunks");
		for(int i=0;i<256;i++){
			Element chunk=xmldoc.createElement("chunk");
			chunk.setAttribute("name",Integer.toHexString(i));
			chunk.setAttribute("server", this.servername);
			chunks.appendChild(chunk);
		}

		root.appendChild(chunks);

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
	public static void main(String[] args) {
		try {
			System.out.println("Attempting to create Routing Config ...");
			File f = new File(OSValidator.getConfigPath());
			if (!f.exists())
				f.mkdirs();
			RoutingConfigWriter wr = new RoutingConfigWriter();
			wr.writeConfigFile();
			System.out.println("Routing Config finished.");
		} catch (Exception e) {
			System.err.println("ERROR : Unable to create volume because "
					+ e.toString());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(175);
		formatter
				.printHelp(
						"mkfs.sdfs --dse-name=sdfs --dse-capacity=100GB",
						options);
	}
}
