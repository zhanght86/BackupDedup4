package org.opendedup.sdfs.servers;

import java.io.File;

import org.opendedup.collections.threads.SyncThreadPool;
import org.opendedup.sdfs.Config;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.DedupFileStore;
import org.opendedup.sdfs.filestore.FileChunkStore;
import org.opendedup.sdfs.filestore.MetaFileStore;
import org.opendedup.sdfs.filestore.gc.SDFSGCScheduler;
import org.opendedup.sdfs.filestore.gc.StandAloneGCScheduler;
import org.opendedup.sdfs.io.VolumeConfigWriterThread;
import org.opendedup.sdfs.mgmt.MgmtWebServer;
import org.opendedup.sdfs.mgmt.NewMgmtWebServer;
import org.opendedup.sdfs.network.NetworkDSEServer;
import org.opendedup.sdfs.notification.SDFSEvent;
import org.opendedup.util.BackupDedupLogger;
import org.opendedup.util.OSValidator;
import org.opendedup.util.SDFSLogger;

import xjtu.dedup.SWUtils.SWHashService;
import xjtu.dedup.preprocess.PreHashChunkService;

public class SDFSService {
	String configFile;

	private SDFSGCScheduler gc = null;
	private StandAloneGCScheduler stGC = null;
	private String routingFile;
	private NetworkDSEServer ndServer = null;
	//added 2013/1/23 for continous backup
	private NewHashChunkService nhcService = null;
	private NewMgmtWebServer nmwServer = null;

	public SDFSService(String configFile, String routingFile) {

		this.configFile = configFile;
		this.routingFile = routingFile;
//		System.out.println("Running SDFS Version " + Main.version);
		BackupDedupLogger.getrunstateLog().info("Running BackupDedup Version " + Main.version);
		if (routingFile != null)
			SDFSLogger.getLog().info(
					"reading routing config file = " + this.routingFile);
//		System.out.println("reading config file = " + this.configFile);
		BackupDedupLogger.getrunstateLog().info("reading config file = " + this.configFile);
		
	}

	public void start() throws Exception {
		Config.parseSDFSConfigFile(this.configFile);
//		nmwServer = new NewMgmtWebServer();
//		nmwServer.start();
		MgmtWebServer.start();
		Main.mountEvent = SDFSEvent.mountInfoEvent("SDFS Version [" + Main.version
				+ "] Mounting Volume from " + this.configFile);
		if (this.routingFile != null)
			Config.parserRoutingFile(routingFile);
		else if (!Main.chunkStoreLocal) {
			Config.parserRoutingFile(OSValidator.getConfigPath()
					+ File.separator + "routing-config.xml");
		}
//		try {
//			if (Main.volume.getName() == null)
//				Main.volume.setName(configFile);
//			Main.volume.setClosedGracefully(false);
//			Config.writeSDFSConfigFile(configFile);
//		} catch (Exception e) {
//			SDFSLogger.getLog().error("Unable to write volume config.", e);
//		}
//		Main.wth = new VolumeConfigWriterThread(configFile);
//		nhcService = new NewHashChunkService();
		if (Main.chunkStoreLocal) {
			try {
				if(Main.issw)
					SWHashService.init();
				else
					HashChunkService.init();
//					nhcService.init();
//				HashChunkService.init();
				if (Main.enableNetworkChunkStore && !Main.runCompact) {
					ndServer = new NetworkDSEServer();
					new Thread(ndServer).start();
				}
			} catch (Exception e) {
				SDFSLogger.getLog().error("Unable to initialize volume ", e);
				System.err.println("Unable to initialize Hash Chunk Service");
				e.printStackTrace();
				System.exit(-1);
			}
			if (Main.runCompact) {
				this.stop();
				System.exit(0);
			}
		if(Main.issdfs )//	|| Main.isfsp
			this.stGC = new StandAloneGCScheduler();
		}

		if (!Main.chunkStoreLocal || Main.issdfs) {
			gc = new SDFSGCScheduler();
		}
//		Main.mountEvent.endEvent("Volume Mounted");
		SDFSEvent.mountInfoEvent("Mounted Volume Successfully");
	}

	public void prestart() throws Exception {

		Config.parseSDFSConfigFile(this.configFile);
		MgmtWebServer.start();
		Main.mountEvent = SDFSEvent.mountInfoEvent("SDFS Version [" + Main.version
				+ "] Mounting Volume from " + this.configFile);
		if (this.routingFile != null)
			Config.parserRoutingFile(routingFile);
		else if (!Main.chunkStoreLocal) {
			Config.parserRoutingFile(OSValidator.getConfigPath()
					+ File.separator + "routing-config.xml");
		}
		if (Main.chunkStoreLocal) {
			try {
				if(Main.issw)
					SWHashService.init();
				else
					PreHashChunkService.init();
				if (Main.enableNetworkChunkStore && !Main.runCompact) {
					ndServer = new NetworkDSEServer();
					new Thread(ndServer).start();
				}
			} catch (Exception e) {
				SDFSLogger.getLog().error("Unable to initialize volume ", e);
				System.err.println("Unable to initialize Hash Chunk Service");
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	public void stop() {
		SDFSEvent.mountWarnEvent("Unmounting Volume");
		SDFSLogger.getLog().info("Shutting Down SDFS");
		SDFSLogger.getLog().info("Stopping FDISK scheduler");
		if (!Main.chunkStoreLocal) {
			gc.stopSchedules();
		} else {
			try {
				this.stGC.close();
			}catch(Exception e) {}
		}
		SDFSLogger.getLog().info("Flushing and Closing Write Caches");
		DedupFileStore.close();
		SDFSLogger.getLog().info("Write Caches Flushed and Closed");
		SDFSLogger.getLog().info("Committing open Files");
		MetaFileStore.close();
		SDFSLogger.getLog().info("Open File Committed");
		SDFSLogger.getLog().info("Writing Config File");

		/*
		 * try { MD5CudaHash.freeMem(); } catch (Exception e) { }
		 */
		MgmtWebServer.stop();
		try {
			Main.wth.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Process p = Runtime.getRuntime().exec(
					"umount " + Main.volumeMountPoint);
			p.waitFor();
		} catch (Exception e) {
		}
		if (Main.chunkStoreLocal) {
			SDFSLogger.getLog().info(
					"######### Shutting down HashStore ###################");
			HashChunkService.close();
			if (Main.enableNetworkChunkStore && !Main.runCompact) {
				ndServer.close();
			} else {

			}
		}
		try {
			Main.volume.setClosedGracefully(true);
			Config.writeSDFSConfigFile(configFile);
		} catch (Exception e) {
			SDFSLogger.getLog().error("Unable to write volume config.", e);
		}
		SDFSEvent evt = SDFSEvent.mountInfoEvent("Unmounting Volume");
		SDFSLogger.getLog().info("SDFS is Shut Down");
	}
	public void stopbackupservice() {
//		SDFSEvent.umountEvent("Unmounting Volume");
//		SDFSLogger.getLog().info("Shutting Down SDFS");
//		SDFSLogger.getLog().info("Stopping FDISK scheduler");
		BackupDedupLogger.getrunstateLog().info("Shutting Down BackupDedup");
		if (!Main.chunkStoreLocal) {
			if(gc != null)
				gc.stopSchedules();
		} else {
			try {
				if(Main.issdfs || Main.isfsp)
					this.stGC.close();
			}catch(Exception e) {}
		}
		if(Main.issdfs){
//			SDFSLogger.getLog().info("Flushing and Closing Write Caches");
			BackupDedupLogger.getrunstateLog().info("Flushing and Closing Write Caches");
			DedupFileStore.close();
//			SDFSLogger.getLog().info("Write Caches Flushed and Closed");
			BackupDedupLogger.getrunstateLog().info("Write Caches Flushed and Closed");
//			SDFSLogger.getLog().info("Committing open Files");
			BackupDedupLogger.getrunstateLog().info("Committing open Files");
			MetaFileStore.close();
//			SDFSLogger.getLog().info("Open File Committed");
			BackupDedupLogger.getrunstateLog().info("Open File Committed");
//			SDFSLogger.getLog().info("Writing Config File");
			BackupDedupLogger.getrunstateLog().info("Writing Config File");
		}
//		System.out.println("Writing Config File");
//		try {
//			Config.writeSDFSConfigFile(configFile);
//		} catch (Exception e) {
//
//		}
		//remote or local
//		MgmtWebServer.stop();
		try {
			if(Main.wth!=null)
				Main.wth.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}

//		try {
//			Process p = Runtime.getRuntime().exec(
//					"umount " + Main.volumeMountPoint);
//			p.waitFor();
//		} catch (Exception e) {
//		}
		
		if (Main.chunkStoreLocal) {
			SDFSLogger.getLog().info(
					"######### Shutting down HashStore ###################");
			HashChunkService.close();
//			nhcService.close();
			if (Main.enableNetworkChunkStore) {
				ndServer.close();
			} else {

			}
		}
		//销毁线程池
//		SyncThreadPool.getInstance().destroy();
//		try {
//			Main.volume.setClosedGracefully(true);
//			Config.writeSDFSConfigFile(configFile);
//		} catch (Exception e) {
//			SDFSLogger.getLog().error("Unable to write volume config.", e);
//		}
//		BackupDedupLogger.getrunstateLog().info("SDFS is Shut Down");
		SDFSLogger.getLog().info("SDFS is Shut Down");
	}
	
	public void prestop() {	
		MgmtWebServer.stop();		
		if (Main.chunkStoreLocal) {
			SDFSLogger.getLog().info(
					"######### Shutting down HashStore ###################");
			PreHashChunkService.close();
			if (Main.enableNetworkChunkStore) {
				ndServer.close();
			} else {

			}
		}
	}
}
