package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.opendedup.util.OSValidator;

import xjtu.dedup.backupmngt.BackupApp;

public class Backup {

	public static Options buildOptions() {
		Options options = new Options();
		options.addOption("p", true,
				"the backup config file path \n e.g. \'C:\\Program Files\\backupdedup\\backupconfig\\sdfs_vol12011-12-26backupjob.xml\'");
		options
				.addOption(
						"n",
						true,
						"the backup config file name. \n e.g. sdfs_vol12011-12-26backupjob.xml \n");
		options.addOption("h", false, "display available options");
		return options;
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		// TODO Auto-generated method stub
        String configfile=null;
		CommandLineParser parser = new PosixParser();
		Options options = buildOptions();
		CommandLine cmd = parser.parse(options, args);
		ArrayList<String> fal = new ArrayList<String>();
		fal.add("-f");
		if (cmd.hasOption("h")) {
			printHelp(options);
			System.exit(1);
		}
		if (cmd.hasOption("p")) {
			File f = new File(cmd.getOptionValue("p").trim());
			if (!f.exists()) {
				System.out.println("backup config file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			configfile= f.getPath();
		} 
		else if (cmd.hasOption("n")) {
			File f = new File(OSValidator.getBackupConfigPath()+cmd.getOptionValue("n").trim());
			if (!f.exists()) {
				System.out.println("backup config file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			configfile = f.getPath();
		} else {
			File f = new File(OSValidator.getBackupConfigPath() + args[0].trim());
			if (!f.exists()) {
				System.out.println("backup config file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			configfile= f.getPath();
		}

        BackupApp.startbackup(configfile);
	}
	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"Backup -[p|n]<backup config file path or file name> ",
						options);
	}

}
