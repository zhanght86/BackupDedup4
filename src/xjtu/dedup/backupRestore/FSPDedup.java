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
import org.opendedup.sdfs.io.DedupFileChannel;
import org.opendedup.sdfs.io.MetaDataDedupFile;
import org.opendedup.util.SDFSLogger;

import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.berkeleyDB.BerkeleyDB;
import xjtu.dedup.fileutils.FileInfoUtil;
import xjtu.dedup.methodinterf.DedupMethod;
import xjtu.dedup.restoremngt.RestoreJob;

public class FSPDedup implements DedupMethod {
	private static Logger log = SDFSLogger.getLog();
	private String dedupfileguid=null;
	@Override
	public void dedup(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dedup(BerkeleyDB bdb,File file, long filenum) {
		// TODO Auto-generated method stub
		FileInputStream in=null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Long length = file.length();
		int blockNum=(length.intValue()/Main.chunkStorePageSize)+1;
		int byteRead=0;
		generateDedupFileGuid(file);
		storeBackupJobInfoToDB(bdb, filenum);//记录备份任务文件信息.key:backupjobid,value:backupfileid
		MetaDataDedupFile mf=storeMetaDataOfDedupFile(bdb, file);////记录备份文件元数据信息.key:backupfileid,value:guid
		byte blockData[] = new byte[Main.chunkStorePageSize];
		DedupFileChannel ch = null;		
		try {
			ch = mf.getDedupFile().getChannel(-1);
			}catch (Exception e) {
				// TODO: handle exception
			}
		long offset = 0;
	    try {
				while ((byteRead = in.read(blockData)) != -1) {
					log.debug(byteRead + "char has been read");
					try {
						ByteBuffer bbuf=ByteBuffer.wrap(blockData);// add.
						ch.writeFile(bbuf, byteRead, 0, offset);// blockData changed to bbuf.
						log.debug("wrote " + offset);//new String(b));
					} catch (IOException e) {
						log.error("unable to write to file " + file.getName(), e);

					}
					offset += byteRead;
					blockData = new byte[Main.chunkStorePageSize];
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try{
				in.close();
			}catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			try {
				String guid=mf.getDfGuid();
				ch.getDedupFile().writeCache(guid,blockNum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (HashtableFullException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	@Override
	public void restore(BerkeleyDB bdb,String filepath) {
		// TODO Auto-generated method stub
		String dirPath = RestoreJob.restorePath;
		MetaDataDedupFile mf=bdb.getMetaDataDedupFile(filepath);
		DedupFileChannel ch = null;		
		try {
			ch = mf.getDedupFile().getChannel(-1);
			}catch (Exception e) {
				// TODO: handle exception
			}
		int blocknum=mf.getBlockNum();
		long length=mf.length();
		long offset=0l;
		byte[] b = new byte[Main.chunkStorePageSize];
		FileOutputStream out = null;
		File outFile=new File(dirPath+filepath.substring(Main.volume.getPath().length()+24));
		if(!outFile.exists())
			outFile.getParentFile().mkdirs();
		try {
			out = new FileOutputStream(outFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int i=0;
		while (true&&i<blocknum) {
			int read;
			try {
				ByteBuffer bbuf=ByteBuffer.wrap(b);
				read = ch.read(bbuf, 0, b.length, offset);
				if (read == -1)
					break;

				out.write(bbuf.array(), 0, read);
				offset += Main.chunkStorePageSize;
				i++;

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

	@Override
	public void restore(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		restore(bdb,FileInfoUtil.getFilePath(file));
	}
	//返回第chunnum个fspchunk的hash值
	public byte[] getSparseFSPDataChunk(byte[] fsprawlchunks,int chunknum){
		int fsparrayLength = 1 + Main.hashLength + 1 + 8;
		byte[] sparsefspdatachunk=new byte[fsparrayLength];
		for(int i=0;i<fsparrayLength;i++)
			sparsefspdatachunk[i]=fsprawlchunks[chunknum*fsparrayLength+i];
		return sparsefspdatachunk;
	}
	/**
	 * 存储备份任务文件信息
	 * 存储的映射关系（key:value）：备份任务全局id->备份文件全局id
	 * 
	 * */
	public void storeBackupJobInfoToDB(BerkeleyDB bdb, long filenum){
		String backupjobguid=BackupJob.backupjobguid+filenum;// backupjob key(一次备份任务记录)
		//记录备份任务文件信息.key:backupjobid,value:backupfileid
		bdb.backupJobInfoToDB(backupjobguid, this.dedupfileguid);
	}
	/**
	 * 存储去重文件元数据信息
	 * 存储的映射关系（key:value）: 备份文件全局id->去重文件元数据
	 * */
	public MetaDataDedupFile storeMetaDataOfDedupFile(BerkeleyDB bdb,File file){
		return bdb.getMetaDataDedupFile(file,this.dedupfileguid);
	}
	/*
	 * 生产去重文件的全局唯一标识符
	 * */
	public void generateDedupFileGuid(File file){
		String fileName = file.getName();
		String filepath=FileInfoUtil.getFilePath(file);// 设置全局唯一文件元数据文件名
		this.dedupfileguid=Main.volume.getPath()+File.separator+filepath+File.separator+fileName;//backupjob file path (备份任务对应文件路径)	
	}
}
