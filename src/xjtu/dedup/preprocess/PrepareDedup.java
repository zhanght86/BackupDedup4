package xjtu.dedup.preprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.opendedup.collections.HashtableFullException;
import org.opendedup.sdfs.servers.SDFSService;
import org.opendedup.util.OSValidator;
import org.opendedup.util.SDFSLogger;


import com.planetj.math.rabinhash.RabinHashFunction32;


//随机选择几个备份文件进行去重预处理
public class PrepareDedup {
    private static SDFSService sdfsService=null;
	private static RabinHashFunction32 rabinFunction=new RabinHashFunction32(32);
//	private static int blockNumber[] = new int[CDCParameter.CDC_M_Set.length];
//	private static int sumcount[] = new int[CDCParameter.CDC_M_Set.length];
	private static int blockNumber[] = new int[CDCParameter.totalcount];
	private static int sumcount[] = new int[CDCParameter.totalcount];
	private int CDC_M = 216;
	private int CDC_R = 13;
	public static void main(String args[]){
		PrepareDedup pd = new PrepareDedup();
		initsdfs();
    	System.out.println("Start PreProcess ......");
    	long start=System.currentTimeMillis();
    	File file = new File("E:\\BackupDedupTestData\\preprocess\\testfile.pptx");
    	File file1 = new File("E:\\BackupDedupTestData\\preprocess\\testfile1.pptx");
    	File file2 = new File("E:\\BackupDedupTestData\\preprocess\\testfile2.pptx");
    	File file3 = new File("E:\\BackupDedupTestData\\preprocess\\testfile3.pptx");
    	File file4 = new File("E:\\BackupDedupTestData\\preprocess\\testfile4.pptx");
    	int count = pd.preCDCDedup(file,0);
    	int sumcount = count;
    	System.out.println("Duplicate chunks: " + count);
    	count = pd.preCDCDedup(file1,0);
    	sumcount +=count;
    	System.out.println("Duplicate chunks: " + count);
    	count = pd.preCDCDedup(file2,0);
    	sumcount +=count;
    	System.out.println("Duplicate chunks: " + count);
    	count = pd.preCDCDedup(file3,0);
    	sumcount +=count;
    	System.out.println("Duplicate chunks: " + count);
    	count = pd.preCDCDedup(file4,0);
    	sumcount +=count;
    	System.out.println("Duplicate chunks: " + count);
    	
    	System.out.println("Total duplicate chunks: " + sumcount);
    	System.out.println("Total chunks: " + getBlockNum());
    	initBlockNum();
    	long end=System.currentTimeMillis();
    	System.out.println("PreProcess use "+(end-start));
    	stopsdfs();
    	SDFSLogger.getLog().info("PreProcess end!");
    	System.out.println("PreProcess end!");
	}
	public int caculateDedupRateAndDetermine(){
		int indexcount = 0;
		double maxdeduprate = 0.0;
		for(int i=0; i< CDCParameter.CDC_M_Set.length; i++){
			double deduprate = (double)sumcount[i] / blockNumber[i];
			if(maxdeduprate <= deduprate){
				maxdeduprate = deduprate;
				indexcount = i;
			}
		}
		return indexcount;
	}
	
	public void setCDC_MandCDC_R(){
		int index = caculateDedupRateAndDetermine();
		CDCParameter.CDC_M = CDCParameter.CDC_M_Set[index];
		CDCParameter.CDC_R = CDCParameter.CDC_R_Set[index];
	}
	
	public void selfLearningCDCDedup(File file){
		for(int i=0; i< CDCParameter.CDC_M_Set.length; i++){
			this.CDC_M = CDCParameter.CDC_M_Set[i];
			this.CDC_R = CDCParameter.CDC_R_Set[i];
			sumcount[i] += preCDCDedup(file, i);
		}
	}
	//CDC去重预处理，主要目的是确定块边界点条件，即（M,R）值对
	public int preCDCDedup(File file, int index){
		int BUF_MAX_SIZE = 131072;//readbuf最大长度128kb
		int MaxChunkSize = 12288;//最大分块大小12kb
		int exp_rwsize = BUF_MAX_SIZE;//预期读文件大小128kb
		int MinChunkSize = 4096;//最小分块大小，实际数据块取值范围在(MinChunkSize,MaxChunkSize).4kb
		int winChunkSize = 48;//滑动窗口大小.2kb
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

		int rabinHash;
		int byteRead=0;
		PreSparseDedupFile psdf = new PreSparseDedupFile();
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
					if(rabinHash % this.CDC_M == this.CDC_R){
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
								psdf.writeFile(bbuf, block_sz, 0, offset,blocknum);
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
								psdf.writeFile(bbuf, block_sz, 0, offset,blocknum);
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
				}
				/* read expected data from file to full up buf */
				bpos=tail-head;
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
		lastblocklen=((byteRead+bpos+block_sz)>=0) ? (byteRead == -1 ? bpos+block_sz : byteRead+bpos+block_sz) : 0;// byteRead+bpos+block_sz最后一个块长度,两种情况：1、文件总长度 < 最小块；2、最后一次读文件的长度 < 最小块
		if(lastblocklen>0){
			blockChunk.position(0);
			blockChunk.get(lastblockChunk, 0, block_sz);
			readbuf.position(0);
			readbuf.get(lastblockChunk, block_sz, bpos);
			try {
				ByteBuffer bbuf=ByteBuffer.wrap(lastblockChunk);
				psdf.writeFile(bbuf, lastblocklen, 0, offset,blocknum);
//				System.out.println("offset: "+offset+" length: "+lastblocklen+" blocknum: "+blocknum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			offset+=lastblocklen;
			blocknum++;
		}
		addBlockNum(blocknum, index);
	// process writebuffer to cdcChunkStore file	
		try {
			psdf.writeCache();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HashtableFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//返回检测到的冗余数据块总数
		return psdf.getDupsFound();
	}
	//FastCDC去重预处理，主要目的是确定块边界点条件，即（M,R）值对
	public static void preFastCDCDedup(File filepath){
		
	} 
	
	public static void initBlockNum(){
		for(int i=0; i< CDCParameter.CDC_M_Set.length; i++){
			blockNumber[i] = 0;
		}
	}
	public static void addBlockNum(int blocknum ,int index){
		blockNumber[index] += blocknum;
	}
	
	public static int[] getBlockNum(){
		return blockNumber;
	}
    /*
     * 初始化sdfs卷，设置备份数据存储位置
     * */
    public static void initsdfs(){
    	String volumeConfigFile =OSValidator.getConfigPath()+"cdc_big_vol1-volume-cfg.xml";//;
		String routingConfigFile = null;//"C:\\Program Files\\sdfs\\etc\\routing-config.xml";   //"E:\\deduptest\\etc\\routing-config.xml";
		sdfsService = new SDFSService(volumeConfigFile,
				routingConfigFile);

		try {
			sdfsService.prestart();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("Exiting because " + e1.toString());
			System.exit(-1);
		}

    }
    
    /*
     * 停止sdfs卷，刷新内存数据存储到磁盘中
     * */
    public static void stopsdfs(){
		try {
			sdfsService.prestop();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("Exiting because " + e1.toString());
			System.exit(-1);
		}

    }
    
}
