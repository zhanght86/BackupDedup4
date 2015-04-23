package xjtu.dedup.methodinterf;

import java.io.File;

import xjtu.dedup.berkeleyDB.BerkeleyDB;

public interface DedupMethod {
	/**
	 * dedup method 
	 * @param file: filepath
	 * */
	public void dedup(BerkeleyDB bdb,File file);
	/**
	 * dedup method 
	 * @param file: filepath
	 * @param filenum: file number in the dedup file sequence
	 * */
	public void dedup(BerkeleyDB bdb,File file,long filenum);
	
	/**
	 * restore the dedup file
	 * @param filepath: the file name to be restored
	 * */
	public void restore(BerkeleyDB bdb,String filepath);
	/**
	 * restore the dedup file
	 * @param file: the file name to be restored
	 * */
	public void restore(BerkeleyDB bdb,File file);
}
