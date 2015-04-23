package org.opendedup.util;

import java.io.File;

public class OSValidator {

	public static void main(String[] args) {
		if (isWindows()) {
			System.out.println("This is Windows " + getProgramBasePath() + " "
					+ getConfigPath());
		} else if (isMac()) {
			System.out.println("This is Mac " + getProgramBasePath() + " "
					+ getConfigPath());
		} else if (isUnix()) {
			System.out.println("This is Unix or Linux " + getProgramBasePath()
					+ " " + getConfigPath());
		} else {
			System.out.println("Your OS is not support!!");
		}
		System.out.println(Runtime.getRuntime().maxMemory());
		System.out.println(Runtime.getRuntime().availableProcessors());
	}

	public static boolean isWindows() {

		String os = System.getProperty("os.name").toLowerCase();
		// windows
		return (os.indexOf("win") >= 0);

	}
	

	public static boolean isMac() {

		String os = System.getProperty("os.name").toLowerCase();
		// Mac
		return (os.indexOf("mac") >= 0);

	}

	public static boolean isUnix() {

		String os = System.getProperty("os.name").toLowerCase();
		// linux or unix
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);

	}

	public static String getProgramBasePath() {
		if (isUnix() || isMac())
			return "/opt/sdfs/";
		else {
			return "E:\\" + File.separator + "sdfs"
					+ File.separator;
		}
	}

	public static String getProgramBasePath2 () {
		if(isUnix() || isMac())
			return "/opt/backupdedup/";
		else {
			return "E:\\" + File.separator + "backupdedup"+ File.separator;
		}	
	}
	
	public static String getConfigPath() {
		if (isUnix() || isMac())
			return "/etc/sdfs/";
		else
			return "E:\\" + File.separator + "backupdedup"
					+ File.separator + "etc" + File.separator;
//			return "E:" + File.separator + "backupdedup" + File.separator + "etc" + File.separator;
	}
	
	public static String getBackupConfigPath(){
		if(isUnix() || isMac())
			return "/etc/backupdedup/backupconfig/";
		else
			//return "E:\\" + File.separator + "backupdedup" + File.separator + "backupconfig" + File.separator;
//			return "E:" + File.separator + "backupdedup" + File.separator + "backupconfig" + File.separator;
			return "E:\\" + File.separator + "backupdedup" + File.separator + "backupconfig" + File.separator;
	}
	
	public static String getRestoreConfigPath(){
		if(isUnix() || isMac())
			return "/etc/backupdedup/restore/";
		else
			return "E:\\" + File.separator + "backupdedup" + File.separator + "restoreconfig" + File.separator;
	}
	
	public static String getBackupFilesMetaDataPath(){
		if(isUnix() || isMac())
			return "/etc/backupdedup/backupfilesmeta/";
		else
			return "E:\\" + File.separator + "backupdedup" + File.separator + "backupfilesmeta" + File.separator;
	}
}
