package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.MetaFileStore;
import org.opendedup.sdfs.io.DedupFileChannel;
import org.opendedup.sdfs.io.MetaDataDedupFile;
import org.opendedup.util.SDFSLogger;

import xjtu.dedup.berkeleyDB.BerkeleyDB;
import xjtu.dedup.fileutils.FileInfoUtil;
import xjtu.dedup.methodinterf.DedupMethod;
import xjtu.dedup.restoremngt.RestoreJob;

public class SDFSDedup implements DedupMethod {
	private static Logger log = SDFSLogger.getLog();
	@Override
	public void dedup(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		String filepath=FileInfoUtil.getSDFSFilePath(file);// 设置全局唯一文件元数据文件名
		String fileName = file.getName();
		File filePath=new File(Main.volume.getPath()+File.separator+filepath);
		if(!filePath.exists()&&filePath.isDirectory())
		{
			filePath.mkdir();
		}
		String path = filePath + File.separator + fileName;//元数据文件路径：主机名+备份时间+文件路径+文件名
		Long length = 0l;
		FileInputStream in=null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//FileChannel fc=in.getChannel();
		length = file.length();
		int byteRead;
		try {
			log.debug("creating " + fileName);
			MetaDataDedupFile mf = MetaFileStore.getMF(path);
			mf.sync();
		} catch (Exception e) {
			log.error("unable to create file " + path, e);
		}
		byte blockData[] = new byte[Main.chunkStorePageSize];
		long offset = 0;
		DedupFileChannel ch = FileInfoUtil.getFileChannel(path);
		ch.mf.setLength(length, true);
		try {
			while ((byteRead = in.read(blockData)) != -1) {
				log.debug(byteRead + "char has been read");
				try {
					ByteBuffer bbuf=ByteBuffer.wrap(blockData);// add.
					ch.writeFile(bbuf, byteRead, 0, offset);// blockData changed to bbuf.
					log.debug("wrote " + offset);//new String(b));
				} catch (IOException e) {
					log.error("unable to write to file" + fileName, e);

				}
				offset += byteRead;
				blockData = new byte[Main.chunkStorePageSize];
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			ch.getDedupFile().writeCache();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HashtableFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void dedup(BerkeleyDB bdb,File file, long filenum) {
		// TODO Auto-generated method stub
		dedup(bdb,file);
	}

	@Override
	public void restore(BerkeleyDB bdb,String filepath) {
		// TODO Auto-generated method stub
//		String dirPath = RestoreJob.restorePath;
		String fileprefix=Main.volume.getPath()+File.separator+RestoreJob.restoreClientHostName+File.separator+RestoreJob.backupdate.replaceAll("-", "");
		String path=fileprefix+File.separator+filepath.substring(3);
//		int pathlen=path.length();	
		File dirs=new File(path);
		File[] files = null;
		String filename;
		if(dirs.isDirectory()){
			files=dirs.listFiles();
			int filenum=files.length;
			for(int i=0;i<filenum;i++){
				restore(files[i]);
			}
		}else{
			restore(dirs);
		}
//		restore(filepath);
	}

	@Override
	public void restore(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		long length, offset = 0;
		
		String filepath = null;
		String dirPath = RestoreJob.restorePath;
		String fileprefix=Main.volume.getPath()+File.separator+RestoreJob.restoreClientHostName+File.separator+RestoreJob.backupdate.replaceAll("-", "");
		int fileprefixlen=fileprefix.length();	
		
		int chunkNum = 1;
		filepath=file.getAbsolutePath();
		String fileName=filepath.substring(fileprefixlen-1);
		
		DedupFileChannel ch = FileInfoUtil.getFileChannel(filepath);
		length = ch.mf.length();
		byte[] b = new byte[Main.chunkStorePageSize];
		FileOutputStream out = null;
		File outFile=new File(dirPath+fileName);
		if(!outFile.exists())
			outFile.getParentFile().mkdirs();
		try {
			out = new FileOutputStream(outFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (true) {
			int read;
			try {
				ByteBuffer bbuf=ByteBuffer.wrap(b);
				read = ch.read(bbuf, 0, b.length, offset);
				if (read == -1)
					break;

				out.write(bbuf.array(), 0, read);
				offset += Main.chunkStorePageSize;
				chunkNum++;

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	public void restore(File file){
		// TODO Auto-generated method stub
		String filename;
		long length, offset = 0;
		if(file.isFile()){
			filename=file.getName();
			DedupFileChannel ch = FileInfoUtil.getFileChannel(file.getAbsolutePath());
			length = ch.mf.length();
			byte[] b = new byte[Main.chunkStorePageSize];
			FileOutputStream out = null;
			File outFile=new File(RestoreJob.restorePath+filename);
			if(!outFile.exists())
				outFile.getParentFile().mkdirs();
			try {
				out = new FileOutputStream(outFile);
			} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			while (true) {
				int read;
				try {
					ByteBuffer bbuf=ByteBuffer.wrap(b);
					read = ch.read(bbuf, 0, b.length, offset);
					if (read == -1)
						break;

					out.write(bbuf.array(), 0, read);
					offset += Main.chunkStorePageSize;

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}else{
			File[] files=file.listFiles();
			for(int i=0;i<files.length;i++){
				restore(files[i]);
			}
		}
	}
}
