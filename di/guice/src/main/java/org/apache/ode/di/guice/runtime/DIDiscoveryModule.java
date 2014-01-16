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
package org.apache.ode.di.guice.runtime;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.ode.di.guice.core.DIScannerModule;
import org.apache.ode.di.guice.core.DIScannerModule.GuiceAnnotationClassProcessor;
import org.apache.ode.di.guice.core.DIScannerModule.GuiceAnnotationMapProcessor;
import org.apache.ode.di.guice.core.DIScannerModule.GuiceAnnotationSetProcessor;
import org.apache.ode.spi.di.CommandAnnotationScanner;
import org.apache.ode.spi.di.CommandAnnotationScanner.CommandModel;
import org.apache.ode.spi.di.CommandAnnotationScanner.Commands;
import org.apache.ode.spi.di.ComponentAnnotationScanner;
import org.apache.ode.spi.di.ComponentAnnotationScanner.ComponentModel;
import org.apache.ode.spi.di.ComponentAnnotationScanner.Components;
import org.apache.ode.spi.di.DispatchAnnotationScanner;
import org.apache.ode.spi.di.DispatchAnnotationScanner.DispatcherModel;
import org.apache.ode.spi.di.DispatchAnnotationScanner.Dispatches;
import org.apache.ode.spi.di.InstructionAnnotationScanner;
import org.apache.ode.spi.di.InstructionAnnotationScanner.InstructionModel;
import org.apache.ode.spi.di.InstructionAnnotationScanner.Instructions;
import org.apache.ode.spi.di.NodeStatusAnnotationScanner;
import org.apache.ode.spi.di.NodeStatusAnnotationScanner.NodeStatusModel;
import org.apache.ode.spi.di.NodeStatusAnnotationScanner.NodeStatuses;
import org.apache.ode.spi.di.OperationAnnotationScanner;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.Operations;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class DIDiscoveryModule extends AbstractModule {

	public static Logger log = Logger.getLogger(DIDiscoveryModule.class.getName());

	protected void configure() {
		GuiceAnnotationClassProcessor<NodeStatusModel> nsap = new GuiceAnnotationClassProcessor<NodeStatusModel>(new NodeStatusAnnotationScanner());
		bind(new TypeLiteral<Map<Class<?>, NodeStatusModel>>(){}).annotatedWith(NodeStatuses.class).toInstance(nsap.getModels());
		GuiceAnnotationClassProcessor<ComponentModel> cap = new GuiceAnnotationClassProcessor<ComponentModel>(new ComponentAnnotationScanner());
		bind(new TypeLiteral<Map<Class<?>, ComponentModel>>(){}).annotatedWith(Components.class).toInstance(cap.getModels());
		GuiceAnnotationClassProcessor<InstructionModel> iap = new GuiceAnnotationClassProcessor<InstructionModel>(new InstructionAnnotationScanner());
		bind(new TypeLiteral<Map<Class<?>, InstructionModel>>(){}).annotatedWith(Instructions.class).toInstance(iap.getModels());
		
		GuiceAnnotationMapProcessor<QName, OperationModel> oap = new GuiceAnnotationMapProcessor<QName, OperationModel>(new OperationAnnotationScanner());		
		bind(new TypeLiteral<Map<QName, OperationModel>>(){}).annotatedWith(Operations.class).toInstance(oap.getModels());
		
		GuiceAnnotationMapProcessor<QName, CommandModel> cmap = new GuiceAnnotationMapProcessor<QName, CommandModel>(new CommandAnnotationScanner());		
		bind(new TypeLiteral<Map<QName, CommandModel>>(){}).annotatedWith(Commands.class).toInstance(cmap.getModels());
		
		GuiceAnnotationSetProcessor<DispatcherModel> dset = new GuiceAnnotationSetProcessor<DispatcherModel>(new DispatchAnnotationScanner());		
		bind(new TypeLiteral<Set<DispatcherModel>>(){}).annotatedWith(Dispatches.class).toInstance(dset.getModels());
		
		install(new DIScannerModule(nsap,cap,iap,oap));
		
	}

}
