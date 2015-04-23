package xjtu.dedup.backupRestore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.opendedup.util.OSValidator;

import xjtu.dedup.backupmngt.BackupApp;

public class Restore {

	public static Options buildOptions() {
		Options options = new Options();
		options.addOption("p", true,
				"the restore config file path \n e.g. \'C:\\Program Files\\backupdedup\\restoreconfig\\restoreconfig.xml\'");
		options
				.addOption(
						"n",
						true,
						"the restore config file name. \n e.g. restoreconfig.xml \n");
		options.addOption("h", false, "display available options");
		return options;
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, IOException, ParseException {
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
				System.out.println("restore config file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			configfile= f.getPath();
		} 
		else if (cmd.hasOption("n")) {
			File f = new File(OSValidator.getRestoreConfigPath()+cmd.getOptionValue("n").trim());
			if (!f.exists()) {
				System.out.println("restore config file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			configfile = f.getPath();
		} else {
			File f = new File(OSValidator.getRestoreConfigPath() + args[0].trim());
			if (!f.exists()) {
				System.out.println("restore config file " + f.getPath()
						+ " does not exist");
				System.exit(-1);
			}
			configfile= f.getPath();
		}

		BackupApp.startrestore(configfile);

	}
	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter
				.printHelp(
						"Restore -[p|n]<restore config file path or file name> ",
						options);
	}

}
