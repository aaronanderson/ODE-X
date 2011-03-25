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
package org.apache.ode.repo;

import java.util.Observable;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.FileTypeMap;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.namespace.QName;

import org.apache.ode.spi.repo.Repository;


public class RepositoryImpl implements Repository {
	
	@PersistenceContext(unitName = "repo")
	private EntityManager mgr;

	@Inject
	private CommandMap commandMap;
	
	@Inject
	private FileTypeMap typeMappings;
	
	public <C> C load(QName qname, String version, String type, Class<C> javaType){
		return null;
	}
	
	/*
DataHandler load(QName qname, String version, String type){
		
	}*/
	 
   
    public <C> void store(QName qname, String version, String type, C content){
    	String mime_type = null;  //ObjectType <=> mimtype mappingqo.getType();
    	DataHandler my_dh = new DataHandler(content, mime_type);
    	//my_dh.getInputStream()
    }
  /*  
    void store(QName qname, String version, String type, DataHandler content){
    	String mime_type = null;  //ObjectType <=> mimtype mappingqo.getType();
    	DataHandler my_dh = new DataHandler(content, mime_type);
    	//my_dh.getInputStream()
    }
	*/

	@Override
	public Observable watch(QName qname, String version, String type) {
		// TODO Auto-generated method stub
		return null;
	}

}
