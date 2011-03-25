/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.spi.repo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.FileTypeMap;
import javax.inject.Singleton;

@Singleton
public class RepoFileTypeMap extends FileTypeMap {
	
	public static String DEFAULT_TYPE = "application/octet-stream";

	Map<String,String> mappings= new ConcurrentHashMap<String, String>();
	
	public void registerExtension(String extension, String mimeType) {
		mappings.put(extension, mimeType);
	}

	public void unRegisterExtension(String extension) {
		mappings.remove(extension);
	}

	@Override
	public String getContentType(File file) {
		//TODO do XML inspection
		return getContentType(file.getName());
	}

	@Override
	public String getContentType(String fileName) {

		int index = fileName.indexOf('.');
		if (index == -1 || index > fileName.length()-2){
			return DEFAULT_TYPE;
		}
		return mappings.get(fileName.substring(index+1));
	}

}
