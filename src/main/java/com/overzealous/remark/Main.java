/*
 * Copyright 2011 OverZealous Creations, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.overzealous.remark;

import org.apache.commons.cli.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the starting point to begin remarking HTML to Markdown from the command line.
 * @author Phil DeJarnett
 */
public class Main {

	private static class Args {
		File fileInput = null;
		URL urlInput = null;
		int inputTimeout = 15;
		String baseURL = "";
		String charset = null;
		File output = null;
		Options options = null;
	}
	
	public static void main(String[] args) throws Exception {
		new Main().start(args);
	}
	
	private void start(String[] args) {
		Args myArgs = processArgs(args);
		if(myArgs != null) {
			Remark remark = new Remark(myArgs.options);
			if(myArgs.output != null) {
				FileOutputStream fos = null;
				OutputStreamWriter osw = null;
				BufferedWriter bw = null;
				try {
					fos = new FileOutputStream(myArgs.output);
					//noinspection IOResourceOpenedButNotSafelyClosed
					osw = new OutputStreamWriter(fos, "UTF-8");
					//noinspection IOResourceOpenedButNotSafelyClosed
					bw = new BufferedWriter(osw);
					remark = remark.withWriter(bw);
					convert(remark, myArgs);
					
				} catch(IOException ex) {
					System.err.println("Error reading from input or writing to output file:");
					System.err.println("  " + ex.getMessage());
				} finally {
					try {
						if(bw != null) {
							bw.close();
						}
						if(osw != null) {
							osw.close();
						}
						if(fos != null) {
							fos.close();
						}
					} catch(IOException ex) {
						ex.printStackTrace();
					}
				}
			} else {
				try {
					System.out.println(convert(remark, myArgs));
				} catch(IOException ex) {
					System.err.println("Error reading from input:");
					System.err.println("  " + ex.getMessage());
				}
			}
		}
	}
	
	private String convert(Remark remark, Args myArgs) throws IOException {
		String ret;
		if(myArgs.fileInput != null) {
			ret = remark.convert(myArgs.fileInput, myArgs.charset, myArgs.baseURL);
		} else {
			ret = remark.convert(myArgs.urlInput, myArgs.inputTimeout * 1000);
		}
		return ret;
	}
	
	private Args processArgs(String[] args) {
		Args result = new Args();
		List<String> error = new ArrayList<String>();
		org.apache.commons.cli.Options opts = makeOptions();
		CommandLineParser clp = new PosixParser();
		
		try {
			CommandLine cl = clp.parse(opts, args, true);
			if(cl.hasOption('h')) {
				printHelp(opts);
				result = null;
			} else {
				checkType(cl, result, error);
				checkOutput(cl, result, error);
				checkTimeout(cl, result, error);
				checkBaseURL(cl, result, error);
				checkCharset(cl, result, error);
				checkInput(cl, result, error);
			}
		} catch(ParseException ex) {
			System.err.println("Unexpected error parsing the command line.");
			System.err.println();
			printHelp(opts);
			result = null;
		}
		
		if(!error.isEmpty()) {
			System.err.println("Error in your options:");
			for(final String err : error) {
				System.err.print(" - ");
				System.err.println(err);
			}
			System.err.println();
			result = null;
		}
		
		return result;
	}
	
	private void printHelp(org.apache.commons.cli.Options opts) {
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp("remark [options] [-o <outputfile>] <input_file_or_url>", opts);
		System.err.println();
	}
	
	private org.apache.commons.cli.Options makeOptions() {
		org.apache.commons.cli.Options opts = new org.apache.commons.cli.Options();
		opts.addOption("t", "type", true, "Type of markdown to target: markdown, markdownextra, multimarkdown, pegdown, pegdownall, or github");
		opts.addOption("o", "output", true, "Name of file to output to; defaults to system out");
		opts.addOption("timeout", true, "Timeout in seconds for downloading from URLs only");
		opts.addOption("baseurl", true, "Base URL for file inputs");
		opts.addOption("charset", true, "Character set for file inputs");
		opts.addOption("h", "help", false, "Displays the command line help");
		return opts;
	}
	
	private void checkType(CommandLine cl, Args result, List<String> error) {
		if(cl.hasOption('t')) {
			String type = cl.getOptionValue('t');
			if("markdown".equalsIgnoreCase(type)) {
				result.options = Options.markdown();
			} else if("markdown".equalsIgnoreCase(type)) {
				result.options = Options.markdown();
			} else if("multimarkdown".equalsIgnoreCase(type)) {
				result.options = Options.multiMarkdown();
			} else if("markdownextra".equalsIgnoreCase(type)) {
				result.options = Options.markdownExtra();
			} else if("pegdown".equalsIgnoreCase(type)) {
				result.options = Options.pegdownBase();
			} else if("pegdownall".equalsIgnoreCase(type)) {
				result.options = Options.pegdownAllExtensions();
			} else if("github".equalsIgnoreCase(type)) {
				result.options = Options.github();
			} else {
				error.add("Invalid type specified");
			}
		} else {
			result.options = Options.markdown();
		}
	}
	
	private void checkOutput(CommandLine cl, Args result, List<String> error) {
		if(cl.hasOption('o')) {
			File output = new File(cl.getOptionValue('o')).getAbsoluteFile();
			result.output = output;
			if(!output.exists()) {
				// check for parent path
				File parent = output.getParentFile();
				if(parent.exists() && !parent.isDirectory()) {
					error.add("Output does is not a valid path.");
				} else {
					if(!parent.exists() && !parent.mkdirs()) {
						error.add("Output path could not be created.");
					} else if(!parent.canWrite()) {
						error.add("Output directory cannot be written to.");
					}
				}
			} else if(!output.isFile()) {
				error.add("Output file exists and is not a file");
			} else if(!output.canWrite()) {
				error.add("Output file cannot be written to.");
			}
		}
	}
	
	private void checkTimeout(CommandLine cl, Args result, List<String> error) {
		if(cl.hasOption("timeout")) {
			try {
				Integer timeout = Integer.parseInt(cl.getOptionValue("timeout"));
				if(timeout < 1) {
					error.add("Invalid timeout specified.");
				} else {
					result.inputTimeout = timeout;
				}
			} catch(NumberFormatException ex) {
				error.add("Invalid timeout specified.");
			}
		}
	}
	
	@SuppressWarnings({"UnusedParameters"})
	private void checkBaseURL(CommandLine cl, Args result, List<String> error) {
		if(cl.hasOption("baseurl")) {
			result.baseURL = cl.getOptionValue("baseurl");
		}
	}
	
	private void checkCharset(CommandLine cl, Args result, List<String> error) {
		if(cl.hasOption("charset")) {
			String charset =  cl.getOptionValue("charset");
			if(Charset.isSupported(charset)) {
				result.charset = charset;
			} else {
				error.add("Unsupported charset.");
			}
		}
	}
	
	private void checkInput(CommandLine cl, Args result, List<String> error) {
		List leftover = cl.getArgList();
		if(leftover.isEmpty()) {
			error.add("No input file or URL specified.");
		} else if(leftover.size() > 1) {
			error.add("Too many arguments.");
		} else {
			String arg = (String)leftover.get(0);
			if(arg.contains("://")) {
				try {
					result.urlInput = new URL(arg);
				} catch(MalformedURLException ex) {
					error.add("Malformed URL: "+ex.getMessage());
				}
			} else {
				File input = new File(arg);
				if(input.isFile() && input.canRead()) {
					result.fileInput = input;
				} else {
					error.add("Unable to read input file.");
				}
			}
		}
	}

}
