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
