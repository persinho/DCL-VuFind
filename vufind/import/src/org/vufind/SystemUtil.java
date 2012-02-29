package org.vufind;

import java.io.*;

import org.apache.log4j.Logger;

public class SystemUtil {
	// Used to identify the windows platform.
	private static final String	WIN_ID	= "Windows";

	/**
	 * Try to determine whether this application is running under Windows or some
	 * other platform by examing the "os.name" property.
	 * 
	 * @return true if this application is running under a Windows OS
	 */
	public static boolean isWindowsPlatform() {
		String os = System.getProperty("os.name");
		if (os != null && os.startsWith(WIN_ID)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isMacintoshPlatform() {
		if (System.getProperty("mrj.version") == null) {
			return false;
		} else {
			return true;
		}
	}

	public static String executeCommand(String commandName, String parameters, Logger logger) throws IOException {
		if (isWindowsPlatform() == false) {
			return "cannot execute commands on non-windows machines";
		} else {
			String cmd = getCommandName() + " /C " + commandName + " " + parameters;
			Process process = Runtime.getRuntime().exec(cmd);
			return getResultsOfProcess(process, logger);
		}
	}

	public static String getResultsOfProcess(Process process, Logger logger) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		// Read and print the output
		String results = "";
		String line = null;
		while ((line = in.readLine()) != null) {
			results += (line + "\r\n");
			if (logger != null) logger.info(line);
		}
		return results;
	}

	public static String executeCommand(String cmd, Logger logger) throws IOException {
		logger.info("Running: " + cmd);
		Process process = Runtime.getRuntime().exec(cmd);
		return getResultsOfProcess(process, logger);
	}
	
	public static String executeCommand(String[] cmdArray, Logger logger) throws IOException {
		StringBuffer cmd = new StringBuffer();
		for (String curCmd : cmdArray){
			cmd.append(curCmd + " ");
		}
		logger.info("Running command \r\n" + cmd);
		Process process = Runtime.getRuntime().exec(cmdArray);
		return getResultsOfProcess(process, logger);
	}

	public static String changeDirectory(String newDirectory) throws IOException {
		return executeCommand("cd", "\"" + newDirectory + "\"", null);
	}

	public static String startCommand(String url, Logger logger) throws IOException {
		/*
		 * if (url.startsWith("file://") && (url.indexOf(" ") != -1)){ url =
		 * url.substring(7, url.length()); File urlFile = new File(url); File
		 * urlPath = urlFile.getParentFile(); String urlName = urlFile.getName();
		 * String[] parameters = {"/c", "start", "\"\"", url}; //Title String
		 * results = executeCommand("start", parameters , urlPath); }else{ String
		 * results = executeCommand("start", "\"" +url + "\""); }
		 */
		String results;
		if (isWindows9x() == false) {
			String[] cmdArray = { getCommandName(), "/c", "start", "\"\"", "\"" + url + "\"" }; // 3rd
																																													// parameter
																																													// is
																																													// Title
			results = executeCommand(cmdArray, logger);
		} else {
			String[] cmdArray = { getCommandName(), "/C", "start", "\"" + url + "\"" }; // Windows
																																									// 9x
																																									// does
																																									// not
																																									// take
																																									// title
																																									// parameter
			results = executeCommand(cmdArray, logger);
		}
		return results;
	}

	public static boolean isWindows9x() {
		String osName = System.getProperty("os.name");
		osName = osName.toLowerCase();
		if (osName.indexOf("windows 9") > -1) {
			return true;
		} else {
			return false;
		}
	}

	private static String getCommandName() {
		if (isWindows9x()) {
			return "command";
		} else {
			return "cmd";
		}
	}
}