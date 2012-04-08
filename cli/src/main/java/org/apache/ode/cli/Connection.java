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
package org.apache.ode.cli;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class Connection {
	
	MBeanServerConnection jmxConnection;
   
  @Parameter(names = "--host", description = "JMX Host")
  public String host="localhost";
  
  @Parameter(names = "--port", description = "JMX Port")
  public int port=4848;
  
  @Parameter(names = "--ssl", description = "JMX uses SSL")
  public String version;
  
  @Parameter(names = "--jmxurl", description = "Full JMX Connection URL")
  public String jmxurl;
  
  MBeanServerConnection getConnection() throws IOException{
	  if (jmxConnection ==null){
		  JMXServiceURL url = null;

		  if (jmxurl !=null){
			  url = new JMXServiceURL(jmxurl);
		  }else{ 
			  url = new JMXServiceURL("service:jmx:rmi://"+host+":"+port+"/jndi/rmi://"+host+":"+port+"/jmxrmi");
		  }
		  JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
		  jmxConnection = jmxc.getMBeanServerConnection();
	  }
	  return jmxConnection;
  }
}
