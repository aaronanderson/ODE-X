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
package org.apache.ode.runtime.exec.executable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.ode.runtime.exec.JAXBRuntimeUtil;
import org.apache.ode.runtime.exec.executable.TestCtxObjectFactory.TestCtxObjectFactoryImpl;
import org.apache.ode.runtime.exec.executable.TestObjectFactory.TestObjectFactoryImpl;
import org.apache.ode.runtime.exec.modules.ScopeModule;
import org.apache.ode.runtime.exec.platform.ExecutableObjectFactoryImpl;
import org.apache.ode.runtime.exec.platform.ExecutionContextObjectFactoryImpl;
import org.apache.ode.runtime.exec.platform.ScopeContext.ExecutableScopeContext;
import org.apache.ode.runtime.interpreter.IndexedExecutable;
import org.apache.ode.spi.exec.Component.InstructionSet;
import org.apache.ode.spi.exec.ExecutableObjectFactory;
import org.apache.ode.spi.exec.instruction.ExecutionContextObjectFactory;
import org.apache.ode.spi.exec.executable.xml.Block;
import org.apache.ode.spi.exec.executable.xml.Executable;

import com.google.inject.AbstractModule;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

public abstract class ExecTestBase {
	//protected static final Logger log = Logger.getLogger(ExecTestBase.class.getName());
	protected static Block block;
	protected static IndexedExecutable eIndex;
	protected static Set<InstructionSet> set;
	protected static Jsr250Injector injector;
	protected static JAXBContext ectx;
	protected static JAXBContext ecctx;
	protected static Schema eschema;
	protected static Schema ectxschema;
	protected static ExecutableScopeContext esc;
	static {
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			eschema = schemaFactory.newSchema(new Source[] { new StreamSource(cl.getResourceAsStream("META-INF/xsd/executable.xsd")),
					new StreamSource(cl.getResourceAsStream("META-INF/xsd/test.xsd")) });
			ectxschema = schemaFactory.newSchema(new Source[] { new StreamSource(cl.getResourceAsStream("META-INF/xsd/execution-context.xsd")),
					new StreamSource(cl.getResourceAsStream("META-INF/xsd/test-context.xsd")) });
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

	public static void setup(String executablePath, AbstractModule... modules) throws Exception {
		set = new HashSet<InstructionSet>();
		set.add(new InstructionSet(null, "org.apache.ode.runtime.exec.test.xml", TestObjectFactoryImpl.class, "org.apache.ode.runtime.ectx.test.xml",
				TestCtxObjectFactoryImpl.class));
		ectx = JAXBRuntimeUtil.executableJAXBContextByPath(set);
		ecctx = JAXBRuntimeUtil.executionContextJAXBContextByPath(set);

		Unmarshaller eum = ectx.createUnmarshaller();
		eIndex = IndexedExecutable.configure(eum);
		eum.setSchema(eschema);
		AbstractModule[] allModules = new AbstractModule[modules.length + 2];
		allModules[0] = new BaseExecTestModule();
		allModules[1] = new ScopeModule();
		System.arraycopy(modules, 0, allModules, 2, modules.length);
		injector = Jsr250.createInjector(allModules);
		esc = injector.getInstance(ExecutableScopeContext.class);
		esc.create();
		try {
			esc.begin();
			TestObjectFactory testExecutableOF = injector.getInstance(TestObjectFactory.class);
			ExecutableObjectFactory execExecutableOF = injector.getInstance(ExecutableObjectFactory.class);
			JAXBRuntimeUtil.setObjectFactories(eum, new Object[] { execExecutableOF, testExecutableOF });
			Object o = eum.unmarshal(ExecTestBase.class.getResourceAsStream(executablePath));
			assertTrue(o instanceof JAXBElement);
			o = ((JAXBElement<?>) o).getValue();
			assertTrue(o instanceof Executable);
			Executable e = (Executable) o;
			assertEquals(1, e.getBlocks().size());
			block = e.getBlocks().get(0);
			assertTrue(block.getInstructions().size() > 0);
		} finally {
			esc.end();
		}
	}

	public static class BaseExecTestModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(TestObjectFactory.class).to(TestObjectFactoryImpl.class);
			bind(TestCtxObjectFactory.class).to(TestCtxObjectFactoryImpl.class);
			bind(ExecutableObjectFactory.class).to(ExecutableObjectFactoryImpl.class);
			bind(ExecutionContextObjectFactory.class).to(ExecutionContextObjectFactoryImpl.class);
		}
	}

	public static void teardown() throws Exception {
		esc.destroy();
		injector.destroy();
	}

}
