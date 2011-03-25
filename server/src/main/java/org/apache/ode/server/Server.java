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
package org.apache.ode.server;

import org.jboss.weld.environment.se.ShutdownManager;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

//import org.apache.webbeans.lifecycle.LifecycleFactory;
//import org.apache.webbeans.spi.ContainerLifecycle;

public class Server {
	//ContainerLifecycle lifecycle;
	Weld weld;
	WeldContainer container;
	
	public static void main (String [] args){
		Server server = new Server();
		server.start();
		server.stop();
		
		
	}
	
	
	public void start() {    
		//ServiceLoader<ContentType> sl = ServiceLoader.load(ContentType.class);
		//.getSystemResources(fullName);
	    //else
		//configs = Thread.currentThread().getclassloader().getResources(fullName);
	
		//TODO register RepoFileType mapper and RepoCommandMap with guice
		//org.jboss.weld.environment.se.StartMain.main(new String[]{"JSR","299"});
		weld = new Weld();
		container =weld.initialize();
		//container.instance().select(WebServer.class).get();
		/*lifecycle = LifecycleFactory.getInstance().getLifecycle(); 
		try {
			lifecycle.startApplication(null);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		//Set<Bean<?>> beans  = lifecycle.getBeanManager().getBeans("webServer");
		Set<Bean<?>> beans =lifecycle.getBeanManager().getBeans(Object.class,new AnnotationLiteral<Any>(){});
		System.out.println("All beans: "+beans.size());
		Iterator<Bean<?>> i = beans.iterator();
		while(i.hasNext()){
			Bean<?> b = i.next();
			System.out.println("name: "+ b.getName() + " type: " + b.getBeanClass().getCanonicalName());
			
		}*/
		/*
		if (beans.size()>0){
			 Bean<?> bean =beans.iterator().next();
	        
	        WebServer webServer = (WebServer) lifecycle.getBeanManager().getReference(bean, WebServer.class, lifecycle.getBeanManager().createCreationalContext(bean));
	        System.out.println("Retrieved the Web Server!! " +webServer);
		 }else{
			 System.out.println("Web Server not available!! ");
		 }
		*/
	

	    
	    //endpoint.stop();

	    //server.stop();
	}
	
	public void stop() {  
		weld.shutdown();
		 //ShutdownManager shutdownManager = container.instance().select(ShutdownManager.class).get();
	     // shutdownManager.shutdown();

		/*try {
			lifecycle.stopApplication(null);
		} catch (Exception e1) {
			e1.printStackTrace();
		}*/

	}


}
