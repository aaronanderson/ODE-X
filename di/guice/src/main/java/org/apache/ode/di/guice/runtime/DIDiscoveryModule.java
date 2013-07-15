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
import java.util.logging.Logger;

import org.apache.ode.di.guice.core.DIScannerModule;
import org.apache.ode.di.guice.core.DIScannerModule.GuiceAnnotationProcessor;
import org.apache.ode.spi.di.ComponentAnnotationScanner;
import org.apache.ode.spi.di.ComponentAnnotationScanner.ComponentModel;
import org.apache.ode.spi.di.ComponentAnnotationScanner.Components;
import org.apache.ode.spi.di.InstructionAnnotationScanner;
import org.apache.ode.spi.di.InstructionAnnotationScanner.InstructionModel;
import org.apache.ode.spi.di.InstructionAnnotationScanner.Instructions;
import org.apache.ode.spi.di.OperationAnnotationScanner;
import org.apache.ode.spi.di.OperationAnnotationScanner.OperationModel;
import org.apache.ode.spi.di.OperationAnnotationScanner.Operations;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class DIDiscoveryModule extends AbstractModule {

	public static Logger log = Logger.getLogger(DIDiscoveryModule.class.getName());

	protected void configure() {
		GuiceAnnotationProcessor<ComponentModel> cap = new GuiceAnnotationProcessor<ComponentModel>(new ComponentAnnotationScanner());
		bind(new TypeLiteral<Map<Class<?>, ComponentModel>>(){}).annotatedWith(Components.class).toInstance(cap.getModels());
		GuiceAnnotationProcessor<InstructionModel> iap = new GuiceAnnotationProcessor<InstructionModel>(new InstructionAnnotationScanner());
		bind(new TypeLiteral<Map<Class<?>, InstructionModel>>(){}).annotatedWith(Instructions.class).toInstance(iap.getModels());
		GuiceAnnotationProcessor<OperationModel> oap = new GuiceAnnotationProcessor<OperationModel>(new OperationAnnotationScanner());
		bind(new TypeLiteral<Map<Class<?>, OperationModel>>(){}).annotatedWith(Operations.class).toInstance(oap.getModels());
		
		install(new DIScannerModule(cap,iap,oap));
		
	}

}
