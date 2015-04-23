package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.hashing.AbstractHashEngine;
import org.opendedup.hashing.HashFunctionPool;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.DedupFileChannel;
import org.opendedup.sdfs.io.MetaDataDedupFile;
import org.opendedup.sdfs.servers.HashChunkService;
import org.opendedup.util.Adler32_Checksum;
import org.opendedup.util.ByteUtils;
import org.opendedup.util.SDFSLogger;

import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.berkeleyDB.BerkeleyDB;
import xjtu.dedup.fileutils.FileInfoUtil;
import xjtu.dedup.methodinterf.DedupMethod;
import xjtu.dedup.restoremngt.RestoreJob;

public class SWDedup implements DedupMethod{
	private static Logger log = SDFSLogger.getLog();
	private static HashFunctionPool hashPool = new HashFunctionPool(
			Main.writeThreads + 1);
	public static void main(String args[]){
		File file = new File("D:\\test.txt");
		BerkeleyDB bdb = null;
		long filenum = 1;
		SWDedup swd = new SWDedup();
		swd.dedup(bdb,file,filenum);
		//restore
		swd.restore(bdb, file);
		
	}
	@Override
	public void dedup(BerkeleyDB bdb,File file) {
		// TODO Auto-generated method stub
	  
	}

	@Override
	public void dedup(BerkeleyDB bdb,File file, long filenum) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
 	    String fileName = file.getName();
		String filepath=FileInfoUtil.getFilePath(file);// 设置全局唯一文件元数据文件名
		String backupjobid=BackupJob.backupClientHostName+BackupJob.backupjobid+BackupJob.creatTime+filenum;// backupjob key(一次备份任务记录)
		String backupfileid=Main.volume.getPath()+File.separator+filepath+File.separator+fileName;//backupjob file path (备份任务对应文件路径)
		//记录备份任务文件信息.key:backupjobid,value:backupfileid
		bdb.backupJobInfoToDB(backupjobid, backupfileid);
		Long length = 0l;	
		FileInputStream in=null;
		int BUF_MAX_SIZE=131072;//readbuf最大长度128kb
		int MaxChunkSize=32768;//最大分块大小32kb
		int exp_rwsize=BUF_MAX_SIZE;//预期读文件大小4kb
		int g_block_size=4096;
		int slide_sz=0;
		int bcsize=0;
		int bpos=0;
		int rwsize=0;
		int hkey=0;
		int head=0,tail=0;//readbuf中字节的开始位置和结束位置
		int bflag=0;
		long offset=0l;// 写文件偏移量
		int blocknum=0;//块数
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		length = file.length();
		MetaDataDedupFile mf=bdb.getMetaDataDedupFile(file,backupfileid);
		
		DedupFileChannel ch = null;		
		try {
			ch = mf.getDedupFile().getChannel(-1);
			}catch (Exception e) {
				// TODO: handle exception
			}		
		byte win_buf[]=new byte[MaxChunkSize*2];
		byte readbuf[]=new byte[BUF_MAX_SIZE];
		byte tempreadbuf[] =new byte[(int)BUF_MAX_SIZE];
		byte blockChunk[]=new byte[MaxChunkSize*2];	//碎片块缓存，用来记录滑动过程碎片块数据
		byte lastblockChunk[]=new byte[BUF_MAX_SIZE];
		int lastblocklen=0;
		byte hash[]=new byte[Main.hashLength];
		int count=0;
		AbstractHashEngine hc = null;
		try {
			hc = hashPool.borrowObject();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		Adler32_Checksum adler32Checksum=new Adler32_Checksum();
		RandomAccessFile rf = null;
		try {
			rf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} 
		byte adler_pre_char = 0;
		boolean doop=false;// 强hash是否存在
		try {
			while((rwsize=rf.read(readbuf,bpos,exp_rwsize))!=-1){
				if((rwsize+bpos+slide_sz)<g_block_size)
					break;
				head=0;
				tail=bpos+rwsize;
				while((head+g_block_size)<tail){
					System.arraycopy(readbuf, head, win_buf, 0, g_block_size);
					hkey= (slide_sz == 0) ? adler32Checksum.adler32_checksum(win_buf, g_block_size) : 
						adler32Checksum.adler32_rolling_checksum(hkey, g_block_size, adler_pre_char, readbuf[head+g_block_size-1]);
					
					/* bflag: 
					  0, both CRC and MD5 are not idenitical
			          1, both CRC and MD5 are identical
				  	  2, CRC is identical and MD5 is not
					*/
					bflag = 0;
					/* this block maybe is duplicate */
					if (bdb.SWAdler32HashExists(hkey))
					{	
						try {
							hash = hc.getHash(win_buf);
						} catch (Exception e) {
							try {
								throw new IOException(e);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						} 
						//insert the framgment
						ByteBuffer bbuf;
						try {
							try {
								doop=HashChunkService.hashExists(hash,(short) 4);
							} catch (HashtableFullException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						if(doop){
							if(slide_sz!=0){
								try{
									bbuf=ByteBuffer.wrap(blockChunk);
									ch.writeFile(bbuf,slide_sz,0,offset,blocknum);
									log.debug("wrote " + offset);//new String(b));
								}catch (Exception e) {
									// TODO: handle exception
									log.error("unable to write to file " + fileName, e);
								}
								blocknum++;
								offset+=slide_sz;
								slide_sz=0;
							}
							//insert the fixed-size block
						    try {
								bbuf=ByteBuffer.wrap(win_buf);// add.
								ch.writeFile(bbuf, g_block_size, 0, offset,blocknum);// blockData changed to bbuf.
								log.debug("wrote " + offset);//new String(b));
							} catch (IOException e) {
								log.error("unable to write to file " + fileName, e);

							}
							blocknum++;//块数
							offset += g_block_size;	//g_block_size=4096
							head += g_block_size;
							slide_sz = 0;
							bflag=1;
						}
						
					}
					// the block is not duplicate
					if(bflag!=1){	
							blockChunk[slide_sz++] = readbuf[head++];
							if (slide_sz ==g_block_size)
							{
								bcsize = g_block_size;
								/* calculate checksum and check in */
								hkey =adler32Checksum.adler32_checksum(blockChunk, bcsize);
								if(bdb.addSWAdler32Hash(hkey)){
								try {
										ByteBuffer bbuf=ByteBuffer.wrap(blockChunk);// add.
										ch.writeFile(bbuf, g_block_size, 0, offset,blocknum);// blockData changed to bbuf.
										log.debug("wrote " + offset);//new String(b));
									} catch (IOException e) {
										log.error("unable to write to file " + fileName, e);
									}
								}
								offset+=g_block_size;
								blocknum++;//块数
								slide_sz = 0;
							}
					}
					adler_pre_char = readbuf[head - 1];
				}
				/* read expected data from file to full up buf */
				bpos = tail - head;
				exp_rwsize = BUF_MAX_SIZE - bpos;
				if(head!=0)
					adler_pre_char = readbuf[head - 1];
				System.arraycopy(readbuf, head, tempreadbuf, 0, bpos);
				System.arraycopy(tempreadbuf, 0, readbuf, 0, bpos);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		/* last chunk */
		lastblocklen = ((bpos + slide_sz) > 0) ? bpos + slide_sz : 0;
		if (lastblocklen > 0)
		{
			System.arraycopy(blockChunk, 0, lastblockChunk, 0, slide_sz);
			System.arraycopy(readbuf, 0, lastblockChunk, slide_sz, bpos);
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
		bdb.putMetaDataDedupFile(backupfileid, mf, blocknum);
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
		try{
			in.close();
		}catch (IOException e) {
			// TODO: handle exception
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
			length=mf.length();
			String dfguid=mf.getDfGuid();
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
			long pos=0l;
			int chunklen=0;
			byte[] sparsecdcdatachunk=new byte[30];
			byte[] b;
			byte[] cdcrawlchunks=ch.getDedupFile().getCDCRAWLChunk(dfguid);
			while (true&&i<chunkNum) {
				int read;
				try {
					chunklen=this.getSWChunkLen(cdcrawlchunks, i);
					sparsecdcdatachunk=this.getSparseSWDataChunk(cdcrawlchunks,i);
					//chunklen=this.getDedupFile().getChunkLen(pos,i);
					b = new byte[chunklen];
					ByteBuffer bbuf=ByteBuffer.wrap(b);
					//read = ch.read(bbuf, 0, b.length, offset,i,Main.iscdc);
					read = ch.read(bbuf, 0, b.length,offset,i,sparsecdcdatachunk, Main.iscdc);
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
	//返回第chunnum个swchunk的长度,类似于CDC
	public int getSWChunkLen(byte[] swrawlchunks,int chunknum){
		int swarrayLength = 1 + Main.hashLength + 1 + 8+4;
		byte[] buflen=new byte[4];
		for(int i=26;i<30;i++)
			buflen[i-26]=swrawlchunks[chunknum*swarrayLength+i];
		return ByteUtils.bytesToInt(buflen);
	}
	//返回第chunnum个SWchunk的hash值
	public byte[] getSparseSWDataChunk(byte[] SWrawlchunks,int chunknum){
		int swarrayLength = 1 + Main.hashLength + 1 + 8+4;
		byte[] sparseswdatachunk=new byte[swarrayLength];
		for(int i=0;i<swarrayLength;i++)
			sparseswdatachunk[i]=SWrawlchunks[chunknum*swarrayLength+i];
		return sparseswdatachunk;
	}
}
