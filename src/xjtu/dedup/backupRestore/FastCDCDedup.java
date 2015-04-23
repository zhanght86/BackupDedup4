package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.hashing.AbstractHashEngine;
import org.opendedup.hashing.HashFunctionPool;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.DedupFileChannel;
import org.opendedup.sdfs.io.MetaDataDedupFile;
import org.opendedup.util.ByteUtils;
import org.opendedup.util.SDFSLogger;

import com.planetj.math.rabinhash.RabinHashFunction32;

import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.berkeleyDB.BerkeleyDB;
import xjtu.dedup.fileutils.FileInfoUtil;
import xjtu.dedup.methodinterf.DedupMethod;
import xjtu.dedup.preprocess.CDCParameter;
import xjtu.dedup.restoremngt.RestoreJob;

public class FastCDCDedup implements DedupMethod{
	private static Logger log = SDFSLogger.getLog();
	private String dedupfileguid=null;
	private static HashFunctionPool hashPool = new HashFunctionPool(
			Main.writeThreads + 1);
	private static RabinHashFunction32 rabinFunction=new RabinHashFunction32(141);
	private static Adler32 adler32Function=new Adler32();
	@Override
	public void dedup(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dedup(BerkeleyDB bdb,File file, long filenum) {
		// TODO Auto-generated method stub
		generateDedupFileGuid(file);
		storeBackupJobInfoToDB(bdb, filenum);//记录备份任务文件信息.key:backupjobid,value:backupfileid
		
		FileInputStream in=null;
		int BUF_MAX_SIZE=131072;//readbuf最大长度128kb
		int MaxChunkSize=40*1024;//最大分块大小16kb
		int exp_rwsize=BUF_MAX_SIZE;//预期读文件大小128kb
		int MinChunkSize=8*1024;//最小分块大小，实际数据块取值范围在(MinChunkSize,MaxChunkSize).4kb
		int winChunkSize=48;//滑动窗口大小.2kb
		int chunk_cdc_M=216;
		int chunk_cdc_r=17;
		int block_sz=0,old_block_sz=0;//block_sz:blockChunk中的字节长度
		int readyblock_sz=0;//备选参数
		int head=0,tail=0;//readbuf中字节的开始位置和结束位置
		int readyhead=0,readytail=0;//备选参数
		long offset=0l;// 写文件偏移量
		int blocknum=0;//块数
		int flag=0; //跳出标记,flag=1代表中断滑动窗口操作
		int backupBreak=0;//边界点标记
		int fixedChunkSize=16*1024; //加速因子
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int byteRead=0;
		MetaDataDedupFile mf=storeMetaDataOfDedupFile(bdb, file);		
		DedupFileChannel ch = null;		
		try {
			ch = mf.getDedupFile().getChannel(-1);
			}catch (Exception e) {
				// TODO: handle exception
			}		
		byte windowChunk[]=new byte[winChunkSize];//为什么要+1？
		byte readbuf[]=new byte[BUF_MAX_SIZE];
		byte tempreadbuf[] =new byte[BUF_MAX_SIZE];
		int bpos=0;
		byte blockChunk[]=new byte[MaxChunkSize*2];
		byte readyblockChunk[]=new byte[MaxChunkSize*2];
		byte lastblockChunk[]=new byte[BUF_MAX_SIZE];
		byte[] cdcChunk,readycdcChunk = null;
		int lastblocklen=0;
		byte hash[]=new byte[Main.hashLength];
		int cdcbreakpointcount=0;
		try {
			AbstractHashEngine hc = hashPool.borrowObject();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		RandomAccessFile rf = null;
//		try {
//			rf = new RandomAccessFile(file, "r");
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} 
		int rabinHash;
		byte adler_pre_char;
	//	ByteBuffer blockbuf=ByteBuffer.allocate(MaxChunkSize);
		try {
			while((byteRead=in.read(readbuf,bpos,exp_rwsize))!=-1)
			{
				//last chunk
				if(bpos+byteRead+block_sz<MinChunkSize)
					break;				
				head=0;
				tail=bpos+byteRead;
				//judge if the fixed-size chunking
				if(flag == 1){
					int remindchunksize = tail - head;
					if(fixedChunkSize <= remindchunksize){
						cdcChunk=new byte[fixedChunkSize];
						System.arraycopy(readbuf, head, cdcChunk, 0, fixedChunkSize);//直接读入后续fixedChunkSize(8KB)大小数据
						head += fixedChunkSize;
//						System.out.println("offset: "+offset+" block_sz:"+fixedChunkSize+" blocknum: "+blocknum);
						try{
							//去重处理过程
							ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
							ch.writeFile(bbuf, cdcChunk.length, 0, offset,blocknum);
							log.debug("wrote " + offset);//new String(b));
							
						}catch (Exception e) {
							// TODO: handle exception
							e.printStackTrace();
						}
						blocknum++;//块数
						offset += cdcChunk.length;				
					}else{
						cdcChunk=new byte[remindchunksize];
						System.arraycopy(readbuf, head, cdcChunk, 0, remindchunksize);//直接读入后续fixedChunkSize(8KB)大小数据
						head += remindchunksize;
//						System.out.println("offset: "+offset+" last_block_sz:"+remindchunksize+" blocknum: "+blocknum);
						try{
							//去重处理过程
							ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
							ch.writeFile(bbuf, cdcChunk.length, 0, offset,blocknum);
							log.debug("wrote " + offset);//new String(b));
							
						}catch (Exception e) {
							// TODO: handle exception
							e.printStackTrace();
						}
						blocknum++;//块数
						offset += cdcChunk.length;	
						
						break;
					}				
				}
				// avoid unnecessary computation and comparsion.
				if(block_sz + winChunkSize < MinChunkSize){
					old_block_sz=block_sz;
					block_sz=((block_sz+tail-head)>(MinChunkSize-winChunkSize))?(MinChunkSize-winChunkSize):(block_sz+tail-head);
					System.arraycopy(readbuf, head, blockChunk, old_block_sz,  block_sz-old_block_sz);
					head+=(block_sz-old_block_sz);
				}
				while((head+winChunkSize)<=tail){
					System.arraycopy(readbuf, head, windowChunk, 0, winChunkSize);
					//can use the Rolling Checksum algorithm
					rabinHash=rabinFunction.hash(windowChunk);
					//备选边界点限制条件
//				if(rabinHash%97==11){
//					System.arraycopy(readbuf, head, readyblockChunk, readyblock_sz, winChunkSize);
//					readyhead=head+winChunkSize;
//					readyblock_sz=block_sz+winChunkSize;
//					if(readyblock_sz>MinChunkSize){
//						readycdcChunk=new byte[readyblock_sz];
//						System.arraycopy(readyblockChunk, 0,readycdcChunk, 0,readyblock_sz);
//						System.out.println(readycdcChunk.length);
//						backupBreak=1;//断点标记
//					 }
//				}
					//块的分界点
					if(rabinHash % CDCParameter.CDC_M == CDCParameter.CDC_R){
						System.arraycopy(readbuf, head, blockChunk, block_sz, winChunkSize);
						head+=winChunkSize;
						block_sz+=winChunkSize;
						if(block_sz>MinChunkSize){
							cdcChunk=new byte[block_sz];
							System.arraycopy(blockChunk, 0,cdcChunk, 0, block_sz);
//							System.out.println("offset: "+offset+" block_sz:"+block_sz+" blocknum: "+blocknum);
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
							cdcbreakpointcount++;
							// 找到第二个cdc分界点,读取后续fixedChunkSize文件内容
//						fixedChunkSize=(fixedChunkSize<=(tail-head))?fixedChunkSize:(tail-head);								
							if(cdcbreakpointcount == 2){
								cdcbreakpointcount = 0;
								int remindchunksize = tail - head;
								if(fixedChunkSize <= remindchunksize){
									cdcChunk=new byte[fixedChunkSize];
									System.arraycopy(readbuf, head, cdcChunk, 0, fixedChunkSize);//直接读入后续fixedChunkSize(8KB)大小数据
									head += fixedChunkSize;
//									System.out.println("offset: "+offset+" block_sz:"+fixedChunkSize+" blocknum: "+blocknum);
									try{
										//去重处理过程
										ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
										ch.writeFile(bbuf, cdcChunk.length, 0, offset,blocknum);
										log.debug("wrote " + offset);//new String(b));
										
									}catch (Exception e) {
										// TODO: handle exception
										e.printStackTrace();
									}
									blocknum++;//块数
									offset += cdcChunk.length;				
								}else{
									flag = 1;
									break;
								}
							}
						  }
						}else{
							flag = 2;
							blockChunk[block_sz++]=readbuf[head++]; //滑动一个字节
							if(block_sz>=MaxChunkSize){
								cdcbreakpointcount++; // As a cdc chunk;
								if(backupBreak==1){//备选匹配条件
									try{
										//去重处理过程
										ByteBuffer bbuf=ByteBuffer.wrap(readycdcChunk);
										ch.writeFile(bbuf, readyblock_sz, 0, offset,blocknum);
										log.debug("wrote " + offset);//new String(b));
										
									}catch (Exception e) {
										// TODO: handle exception
										e.printStackTrace();
									}
									blocknum++;//块数
									offset += readyblock_sz;
									readyblock_sz=0;
								}else{
									cdcChunk=new byte[block_sz];
									System.arraycopy(blockChunk, 0,cdcChunk, 0, block_sz);
//									System.out.println("offset: "+offset+" block_sz:"+block_sz+" blocknum: "+blocknum);
									try{
										//去重处理过程
										ByteBuffer bbuf=ByteBuffer.wrap(cdcChunk);
										ch.writeFile(bbuf, block_sz, 0, offset,blocknum);
										log.debug("wrote " + offset);//new String(b));
									}catch (Exception e) {
										// TODO: handle exception
										e.printStackTrace();
									}
									blocknum++;
									offset += block_sz;
									block_sz=0;
									flag=0;
								}
							}	
						}
					//avoid unnecessary computation and comparsion
					if(block_sz==0){
						block_sz=((tail-head)>(MinChunkSize-winChunkSize))?(MinChunkSize-winChunkSize):(tail-head);
						System.arraycopy(readbuf, head, blockChunk, 0, block_sz);
						head = ((tail - head) > (MinChunkSize - winChunkSize)) ? 
							head + (MinChunkSize - winChunkSize) : tail;
//						head+=block_sz;
					}
					adler_pre_char=readbuf[head-1];
				 }
				/* read expected data from file to full up buf */
				bpos=tail-head;
				exp_rwsize=BUF_MAX_SIZE-bpos;
				if(head!=0)
					adler_pre_char=readbuf[head-1];
				System.arraycopy(readbuf, head, tempreadbuf, 0, bpos);
				System.arraycopy(tempreadbuf, 0,readbuf, 0, bpos);
			 }
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try{
			in.close();
		}catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		//last chunk process
		//changed on 2013.2.27
		lastblocklen=((byteRead+bpos+block_sz)>=0) ? (byteRead == -1 ? bpos+block_sz : byteRead+bpos+block_sz) : 0;// byteRead+bpos+block_sz最后一个块长度,两种情况：1、文件总长度 < 最小块；2、最后一次读文件的长度 < 最小块
		if(lastblocklen>0){
			System.arraycopy(blockChunk, 0,lastblockChunk, 0, block_sz);
			System.arraycopy(readbuf, 0,lastblockChunk, block_sz,bpos);
//			System.out.println("offset: "+offset+" lastblocklength:"+lastblocklen+" blocknum: "+blocknum);
			ByteBuffer bbuf=ByteBuffer.wrap(lastblockChunk);
			try {
				ch.writeFile(bbuf, lastblocklen, 0, offset,blocknum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			blocknum++;			
		}
		
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
	    long length, offset = 0;
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
		while (true && i<chunkNum) {
			int read;
			try {
				chunklen=ch.getDedupFile().getChunkLen(offset, i);
				b = new byte[chunklen];
				ByteBuffer bbuf=ByteBuffer.wrap(b);
				read = ch.read(bbuf, 0, b.length,offset,i, Main.iscdc);
				if (read == -1)
					break;

				out.write(bbuf.array(), 0, read);
				offset += read;
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
		restore(bdb,FileInfoUtil.getFilePath(file));
	}
	//返回第chunnum个cdcchunk的长度
	public int getCDCChunkLen(byte[] cdcrawlchunks,int chunknum){
		int cdcarrayLength = 1 + Main.hashLength + 1 + 8 + 4;
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

