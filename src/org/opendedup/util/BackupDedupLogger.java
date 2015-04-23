package org.opendedup.util;

import java.io.IOException;
import java.util.logging.SimpleFormatter;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.opendedup.sdfs.Main;

import com.sleepycat.je.util.FileHandler;

public class BackupDedupLogger {
	private static Logger backuplog = Logger.getLogger("BackupDedup_backup");
	private static Logger restorelog = Logger.getLogger("BackupDedup_restore");
	private static Logger runstatelog = Logger.getLogger("BackupDedup_runstate");
	static {
		ConsoleAppender bapp = new ConsoleAppender(new PatternLayout("%m%n"));
		backuplog.addAppender(bapp);
		restorelog.addAppender(bapp);
		runstatelog.addAppender(bapp);
		
		backuplog.setLevel(Level.INFO);
		restorelog.setLevel(Level.INFO);
		runstatelog.setLevel(Level.INFO);
		
		RollingFileAppender app = null;
		try {

			app = new RollingFileAppender(new PatternLayout(
					"%d [%t] %p %c %x - %m%n"), Main.backuplogPath, true);
			app.setMaxBackupIndex(2);
			app.setMaxFileSize("10MB");
		} catch (IOException e) {
			backuplog.debug("unable to change appender", e);
		}
		backuplog.addAppender(app);
		backuplog.setLevel(Level.INFO);
		
		try {

			app = new RollingFileAppender(new PatternLayout(
					"%d [%t] %p %c %x - %m%n"), Main.restorelogPath, true);
			app.setMaxBackupIndex(2);
			app.setMaxFileSize("10MB");
		} catch (IOException e) {
			restorelog.debug("unable to change appender", e);
		}
		restorelog.addAppender(app);
		restorelog.setLevel(Level.INFO);
		
		try {

			app = new RollingFileAppender(new PatternLayout(
					"%d [%t] %p %c %x - %m%n"), Main.runstatelogPath, true);
			app.setMaxBackupIndex(2);
			app.setMaxFileSize("10MB");
		} catch (IOException e) {
			runstatelog.debug("unable to change appender", e);
		}
		runstatelog.addAppender(app);
		runstatelog.setLevel(Level.INFO);
	}
	
	public static Logger getbackupLog() {
		return backuplog;
	}
	public static Logger getrestoreLog() {
		return restorelog;
	}
	public static Logger getrunstateLog() {
		return runstatelog;
	}
}
