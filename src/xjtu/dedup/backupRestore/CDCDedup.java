package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.hashing.HashFunctionPool;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.MetaFileStore;
import org.opendedup.sdfs.io.DedupFileChannel;
import org.opendedup.sdfs.io.MetaDataDedupFile;
import org.opendedup.util.ByteUtils;
import org.opendedup.util.SDFSLogger;

import com.planetj.math.rabinhash.RabinHashFunction32;
import com.planetj.math.rabinhash.RabinHashFunction64;

import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.berkeleyDB.BerkeleyDB;
import xjtu.dedup.fileutils.FileInfoUtil;
import xjtu.dedup.methodinterf.DedupMethod;
import xjtu.dedup.preprocess.CDCParameter;
import xjtu.dedup.restoremngt.RestoreJob;
/*
 * 
 * */
public class CDCDedup implements DedupMethod {
	private static Logger log = SDFSLogger.getLog();
	private static HashFunctionPool hashPool = new HashFunctionPool(
			Main.writeThreads + 1);
	private static RabinHashFunction32 rabinFunction=new RabinHashFunction32(753743835);//不可约多项式
	private static Adler32 adler32Function=new Adler32();
	private String dedupfileguid=null;
	String mainvolumepath="E:\\deduptest\\cdcvolumes\\cdcfiles";
	@Override
	public void dedup(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		String filepath=FileInfoUtil.getFilePath(file);// 设置全局唯一文件元数据文件名
		String fileName = file.getName();
		//File filePath=new File(Main.volume.getPath()+File.separator+filepath);
		File filePath=new File(mainvolumepath+File.separator+filepath);
		if(!filePath.exists()&&filePath.isDirectory())
		{
			filePath.mkdir();
		}
		String path = filePath + File.separator + fileName;//元数据文件路径：主机名+备份时间+文件路径+文件名
		Long length = 0l;
		FileInputStream in=null;
		int BUF_MAX_SIZE=131072;//readbuf最大长度128kb
		int MaxChunkSize=12288;//最大分块大小12kb
		int exp_rwsize=131072;//预期读文件大小128kb
		int MinChunkSize=4096;//最小分块大小，实际数据块取值范围在(MinChunkSize,MaxChunkSize).4kb
		int winChunkSize=48;//滑动窗口大小.2kb
		int chunk_cdc_M=4096;//chunk_cdc_M=block_size 4096
		int chunk_cdc_r=13;
		int block_sz=0,old_block_sz=0;//block_sz:blockChunk中的字节长度
		int head=0,tail=0;//readbuf中字节的开始位置和结束位置
		long offset=0l;// 写文件偏移量
		int blocknum=0;//块数
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//FileChannel fc=in.getChannel();
		length = file.length();
		int byteRead=0;
		try {
			log.debug("creating " + fileName);
		//	MetaDataDedupFile mf = MetaFileStore.getMF(path+fileName);
			MetaDataDedupFile mf = MetaFileStore.getMF(path);
			mf.setLength(length, true);
			mf.sync();
		} catch (Exception e) {
			log.error("unable to create file " + path, e);
		}
		DedupFileChannel ch = FileInfoUtil.getFileChannel(path);
		ch.mf.setLength(length, true);		
		byte windowChunk[]=new byte[winChunkSize+1];
		byte readbuf[]=new byte[(int)BUF_MAX_SIZE];
		int bpos=0;
		byte blockChunk[]=new byte[MaxChunkSize*2];
		byte lastblockChunk[]=new byte[MaxChunkSize*2];
		byte[] cdcChunk;
		int lastblocklen=0;
		byte hash[]=new byte[Main.hashLength];
		int count=0;
		RandomAccessFile rf = null;
		try {
			rf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		int rabinHash;
		byte adler_pre_char;
		try {
			while((byteRead=rf.read(readbuf,bpos,exp_rwsize))!=-1)
			{
				//last chunk
				if(bpos+byteRead+block_sz<MinChunkSize){
					break;
				}
				head=0;
				tail=bpos+byteRead;
				// avoid unnecessary computation and comparsion.
				if(block_sz+winChunkSize<MinChunkSize){
					old_block_sz=block_sz;
					block_sz=((block_sz+tail-head)>(MinChunkSize-winChunkSize))?(MinChunkSize-winChunkSize):(block_sz+tail-head);
					System.arraycopy(readbuf, head,blockChunk, old_block_sz,  block_sz-old_block_sz);
					head+=(block_sz-old_block_sz);
				}
				while((head+winChunkSize)<tail){
					System.arraycopy(readbuf, head,windowChunk, 0,  winChunkSize);
					//can use the Rolling Checksum algorithm
					rabinHash=(int) rabinFunction.hash(windowChunk);
					//块的分界点
					if(rabinHash%chunk_cdc_M==chunk_cdc_r){
						System.arraycopy(readbuf, head, blockChunk, block_sz, winChunkSize);
						head+=winChunkSize;
						block_sz+=winChunkSize;
						if(block_sz>MinChunkSize){
							cdcChunk=new byte[block_sz];
							System.arraycopy(blockChunk, 0,cdcChunk, 0,block_sz);
							try{
								//去重处理过程
								ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
								ch.writeFile(bbuf, block_sz, 0, offset,blocknum);
								log.debug("wrote " + offset);//new String(b));
								
							}catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}
							blocknum++;//块数
							offset += block_sz;
							block_sz=0;
						}
					}else{
						blockChunk[block_sz++]=readbuf[head++];
						if(block_sz>=MaxChunkSize){
							cdcChunk=new byte[block_sz];
							System.arraycopy(blockChunk, 0,cdcChunk, 0,block_sz);
							try{
								//去重处理过程
								ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
								ch.writeFile(bbuf, block_sz, 0, offset,(int)blocknum);
								log.debug("wrote " + offset);//new String(b));
							}catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}
							blocknum++;
							offset += block_sz;
							block_sz=0;
						}						
					}
					if(block_sz==0){
						block_sz=((tail-head)>(MinChunkSize-winChunkSize))?(MinChunkSize-winChunkSize):(tail-head);
						System.arraycopy(readbuf, head,blockChunk, 0,  block_sz);
						head = ((tail - head) > (MinChunkSize - winChunkSize)) ? 
								head + (MinChunkSize - winChunkSize) : tail;
					}
					adler_pre_char=readbuf[head-1];
				}
				/* read expected data from file to full up buf */
				bpos=tail-head;
				exp_rwsize=BUF_MAX_SIZE-bpos;
				if(head!=0)
					adler_pre_char=readbuf[head-1];
				System.arraycopy(readbuf, head,readbuf, 0,  bpos);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//last chunk process
		lastblocklen=((byteRead+bpos+block_sz)>=0)?byteRead+bpos+block_sz:0;
		if(lastblocklen>0){
			System.arraycopy(blockChunk, 0,lastblockChunk, 0,  block_sz);
			ByteBuffer bbuf=ByteBuffer.wrap(lastblockChunk);
			try {
				ch.writeFile(bbuf, lastblocklen, 0, offset,(int)blocknum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	// 记录去重文件块数.
		ch.mf.setblocknum(blocknum, true);
	// process writebuffer to cdcChunkStore file	
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
		generateDedupFileGuid(file);
		storeBackupJobInfoToDB(bdb, filenum);//记录备份任务文件信息.key:backupjobid,value:backupfileid
		
		int BUF_MAX_SIZE = 128*1024;//readbuf最大长度128kb
		int MaxChunkSize = 40*1024;//最大分块大小12kb
		int exp_rwsize = BUF_MAX_SIZE;//预期读文件大小128kb
		int MinChunkSize = 8*1024;//最小分块大小，实际数据块取值范围在(MinChunkSize,MaxChunkSize).4kb
		int winChunkSize = 50;//滑动窗口大小.2kb
		int chunk_cdc_M = 216;
		int chunk_cdc_r = 17;
		int block_sz = 0,old_block_sz = 0;//block_sz:blockChunk中的字节长度
		int head = 0,tail = 0;//readbuf中字节的开始位置和结束位置
		long offset = 0l;// 写文件偏移量
		int blocknum = 0;//块数
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file,"r");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		FileChannel fc = raf.getChannel();
		//FileChannel fc=in.getChannel();
		MetaDataDedupFile mf=storeMetaDataOfDedupFile(bdb, file);
		
		DedupFileChannel ch = null;		
		try {
			ch = mf.getDedupFile().getChannel(-1);
			}catch (Exception e) {
				// TODO: handle exception
		}		
		byte windowChunk[]=new byte[winChunkSize];//为什么要+1？
		ByteBuffer readbuf=ByteBuffer.allocate(BUF_MAX_SIZE);
		byte tempreadbuf[] =new byte[BUF_MAX_SIZE];
		int bpos=0;
		int cpos=0;
//		byte blockChunk[]=new byte[MaxChunkSize*2];
		ByteBuffer blockChunk=ByteBuffer.allocate(MaxChunkSize*2);
		byte lastblockChunk[]=new byte[BUF_MAX_SIZE];
		byte[] cdcChunk;
		int lastblocklen=0;
		byte hash[]=new byte[Main.hashLength];
		int count=0;

		int rabinHash;
		byte adler_pre_char;
		int byteRead=0;
		try {
			while((byteRead=fc.read(readbuf,cpos))!=-1){
				//last chunk
				if(byteRead+block_sz<MinChunkSize)
					break;
				
				head=0;
				tail=byteRead + bpos; //the error source
				// avoid unnecessary computation and comparsion.
				if(block_sz+winChunkSize<MinChunkSize){
					old_block_sz=block_sz;
					block_sz = (((block_sz + tail - head) > (MinChunkSize - winChunkSize)) ? (MinChunkSize - winChunkSize) : (block_sz + tail-head));
					blockChunk.position(old_block_sz);
					blockChunk.put(readbuf.array(), head, block_sz-old_block_sz);
					head+=(block_sz-old_block_sz);
				}
				while((head+winChunkSize)<=tail){
					readbuf.position(head);
					readbuf.get(windowChunk, 0, winChunkSize);
					//can use the Rolling Checksum algorithm
					rabinHash=rabinFunction.hash(windowChunk);
					//块的分界点
					// (216,13),(371,19),(407,17),(517,71),(2137,37),(4096,117),
					if(rabinHash % CDCParameter.CDC_M == CDCParameter.CDC_R ){
						blockChunk.position(block_sz);
						blockChunk.put(readbuf.array(), head, winChunkSize);
						head+=winChunkSize;
						block_sz+=winChunkSize;
						if(block_sz>MinChunkSize){
							cdcChunk=new byte[block_sz];
							blockChunk.position(0);
							blockChunk.get(cdcChunk);
							try{
								//去重处理过程
								ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
								ch.writeFile(bbuf, block_sz, 0, offset,blocknum);
//								System.out.println("offset: "+offset+" length: "+block_sz+" blocknum: "+blocknum);
								
							}catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}
							blocknum++;//块数
							offset += block_sz;
							block_sz=0;
						}
					}else{
						readbuf.position(head);
						blockChunk.put(block_sz, readbuf.get());//滑动一个字节,readbuf递增1
						block_sz++;
						head++;
						if(block_sz>=MaxChunkSize){
							cdcChunk=new byte[block_sz];
							blockChunk.position(0);
							blockChunk.get(cdcChunk);
							try{
								//去重处理过程
								ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
								ch.writeFile(bbuf, block_sz, 0, offset,blocknum);
//								System.out.println("offset: "+offset+" length: "+block_sz+" blocknum: "+blocknum);
							}catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}
							blocknum++;
							offset += block_sz;
							block_sz=0;
						}	
					}
					//avoid unnecessary computation and comparsion
					if(block_sz==0){
						block_sz=((tail-head)>(MinChunkSize-winChunkSize))?(MinChunkSize-winChunkSize):(tail-head);
						blockChunk.position(0);
						blockChunk.put(readbuf.array(), head, block_sz);
						head=((tail - head) > (MinChunkSize - winChunkSize)) ? head + (MinChunkSize - winChunkSize) : tail;
//						head+=block_sz;
					}
					adler_pre_char = readbuf.get(head-1);
				}
				/* read expected data from file to full up buf */
				bpos=tail-head;
				if(head!=0)
					adler_pre_char=readbuf.get(head-1);
				readbuf.position(head);
				readbuf.get(tempreadbuf, 0, bpos);
				readbuf.position(0);
				readbuf.put(tempreadbuf,0,bpos);
				readbuf.position(bpos);
				//set the filechannel position
				cpos += byteRead;				

			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}finally{
			try{
				fc.close();
			}catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		//last chunk process
		//changed on 2013.2.27
		lastblocklen=((byteRead+bpos+block_sz)>=0) ? (byteRead == -1 ? bpos+block_sz : byteRead+bpos+block_sz) : 0;// byteRead+bpos+block_sz最后一个块长度,两种情况：1、文件总长度 < 最小块；2、最后一次读文件的长度 < 最小块
		if(lastblocklen>0){
			blockChunk.position(0);
			blockChunk.get(lastblockChunk, 0, block_sz);
			readbuf.position(0);
			readbuf.get(lastblockChunk, block_sz, bpos);
			try {
				ByteBuffer bbuf=ByteBuffer.wrap(lastblockChunk);
				ch.writeFile(bbuf, lastblocklen, 0, offset,blocknum);
//				System.out.println("offset: "+offset+" length: "+lastblocklen+" blocknum: "+blocknum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			offset+=lastblocklen;
			blocknum++;
		}
//		System.out.println("file end pos: "+offset+" Total block number: "+blocknum);
	// 记录去重文件块数.
		storeChunksNumToMetaDataDedupFile(bdb, mf, blocknum);
	// process writebuffer to cdcChunkStore file	
		try {
			String guid=mf.getDfGuid();
			ch.getDedupFile().writeCache(guid,blocknum);
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
	    long offset = 0;
		String dirPath = RestoreJob.restorePath;	
		
		MetaDataDedupFile mf=bdb.getMetaDataDedupFile(filepath);
		DedupFileChannel ch = null;		
		try {
			ch = mf.getDedupFile().getChannel(-1);
			}catch (Exception e) {
				// TODO: handle exception
			}
		int chunkNum=mf.getBlockNum();
		
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
		int chunklen=0;
		byte[] b;
		while (true&&i<chunkNum) {
			int read;
			try {
				chunklen=ch.getDedupFile().getChunkLen(offset, i);
				b = new byte[chunklen];
				ByteBuffer bbuf=ByteBuffer.wrap(b);
				read=ch.read(bbuf, 0, b.length, offset, i, Main.iscdc);
				if (read == -1)
					break;
				out.write(bbuf.array(), 0, read);
				offset += chunklen;
				i++;

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void restore(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		String filepath=mainvolumepath+File.separator+FileInfoUtil.getFilePath(file);// 设置全局唯一文件元数据文件名
		restore(bdb,filepath);
	}
	//返回第chunnum个cdcchunk的长度
	public int getCDCChunkLen(byte[] cdcrawlchunks,int chunknum){
		int cdcarrayLength = 1 + Main.hashLength + 1 + 8+4;
		byte[] buflen=new byte[4];
		for(int i=26;i<30;i++)
			buflen[i-26]=cdcrawlchunks[chunknum*cdcarrayLength+i];
		return ByteUtils.bytesToInt(buflen);
	}
	//返回第chunnum个cdcchunk的hash值
	public byte[] getSparseCDCDataChunk(byte[] cdcrawlchunks,int chunknum){
		int cdcarrayLength = 1 + Main.hashLength + 1 + 8+4;
		byte[] sparsecdcdatachunk=new byte[cdcarrayLength];
		for(int i=0;i<cdcarrayLength;i++)
			sparsecdcdatachunk[i]=cdcrawlchunks[chunknum*cdcarrayLength+i];
		return sparsecdcdatachunk;
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
	 * 记录去重文件块数.
	 * */
	public void storeChunksNumToMetaDataDedupFile(BerkeleyDB bdb,MetaDataDedupFile mf,int blocknum){
		bdb.putMetaDataDedupFile(this.dedupfileguid, mf, blocknum);
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
