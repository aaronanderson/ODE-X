/**
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
package org.apache.ode.data.core.repo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.FileTypeMap;
import javax.inject.Singleton;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

@Singleton
public class RepoFileTypeMap extends FileTypeMap {
	
	public static String DEFAULT_TYPE = "application/octet-stream";

	Map<String,String> fileNamemappings= new ConcurrentHashMap<String, String>();
	Map<String,String> namespaceMappings= new ConcurrentHashMap<String, String>();
	
	
	public void registerFileExtension(String extension, String mimeType) {
		fileNamemappings.put(extension, mimeType);
	}

	public void unRegisterFileExtension(String extension) {
		fileNamemappings.remove(extension);
	}
	
	public boolean isValid(String mimeType){
		return fileNamemappings.values().contains(mimeType) || namespaceMappings.values().contains(mimeType);
	}

	@Override
	public String getContentType(File file) {
		try{
		return getContentType(new FileInputStream(file));
		}catch (FileNotFoundException fe){
			return null;
		}
	}

	@Override
	public String getContentType(String fileName) {

		int index = fileName.indexOf('.');
		if (index == -1 || index > fileName.length()-2){
			return DEFAULT_TYPE;
		}
		return fileNamemappings.get(fileName.substring(index+1));
	}
	
	public void registerNamespace(String namespace, String mimeType) {
		namespaceMappings.put(namespace, mimeType);
	}

	public void unRegisterNamespace(String namespace) {
		namespaceMappings.remove(namespace);
	}
	
	public String getContentType(byte[] contents) {
		return getContentType(new ByteArrayInputStream(contents));
	}
	
	public String getContentType(InputStream contents) {
		try{
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(contents);
			reader.nextTag();
			String ns= namespaceMappings.get(reader.getNamespaceURI());
			contents.close();
			return ns;
		}catch (Exception e){
			return null;
		}
	}

	public String getContentTypeByNS(String namespace) {
		return namespaceMappings.get(namespace);
	}


}
