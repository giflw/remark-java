package com.overzealous.remark.util;

import org.apache.commons.io.FileUtils;

import java.io.IOException;

/**
 * @author Phil DeJarnett
 */
public class TestUtils {

	/**
	 * Reads a resource into a string.
	 * @param path Path to resource
	 * @return String contents of resource
	 */
	public static String readResourceToString(String path) {
		String result = "";
		try {
			result =  FileUtils.readFileToString(FileUtils.toFile(StringUtils.class.getResource(path)));
		} catch(IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
