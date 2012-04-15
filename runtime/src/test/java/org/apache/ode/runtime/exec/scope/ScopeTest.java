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
package org.apache.ode.runtime.exec.scope;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.ode.runtime.exec.platform.ScopeContext.ExecutableScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.InstructionScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.ProcessScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.ProgramScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.ThreadScopeContext;
import org.apache.ode.runtime.exec.scope.ScopeModule.ProgramGuiceScopeImpl;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.InstructionScope;
import org.apache.ode.spi.exec.ProcessScope;
import org.apache.ode.spi.exec.ProgramScope;
import org.apache.ode.spi.exec.ThreadScope;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.mycila.inject.jsr250.Jsr250;
import com.mycila.inject.jsr250.Jsr250Injector;

public class ScopeTest {
	private static final Logger log = Logger.getLogger(ScopeTest.class.getName());
	private static Jsr250Injector injector;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ProgramGuiceScopeImpl.SINGLETONS.add(ProgramSingletonObject.class);
		injector = Jsr250.createInjector(new ScopeModule(), new ScopeTestModule());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ProgramGuiceScopeImpl.SINGLETONS.clear();
		injector.destroy();
	}

	public static class ScopeTestModule extends AbstractModule {
		@Override
		protected void configure() {

		}
	}

	@ProgramScope
	public static class ProgramObject {
		boolean started = false;
		boolean stopped = false;

		@Inject
		ProgramSingletonObject singleton;

		@Inject
		SharedProgramObject shared;

		@Inject
		NamedProgramObject named;

		@PostConstruct
		public void init() {
			started = true;
		}

		@PreDestroy
		public void destroy() {
			stopped = true;
		}

	}

	public static class ProgramSingletonObject {

	}

	@ProgramScope(singleton = true)
	public static class SharedProgramObject {

	}

	@ProgramScope
	@Named("Program")
	public static class NamedProgramObject {

	}

	@Test
	public void programTest() throws Exception {
		ProgramScopeContext psc1 = injector.getInstance(ProgramScopeContext.class);
		assertNotNull(psc1);
		ProgramScopeContext psc2 = injector.getInstance(ProgramScopeContext.class);
		assertNotNull(psc2);
		assertFalse(psc1.equals(psc2));
		psc1.create();
		ProgramObject p1;
		try {
			psc1.begin();
			p1 = psc1.newInstance(ProgramObject.class);
			assertNotNull(p1);
			assertTrue(p1.started);
			assertNotNull(p1.shared);
			ProgramObject p2 = psc1.newInstance(ProgramObject.class);
			assertNotNull(p2);
			assertNotSame(p1, p2);
			assertSame(p1.shared, p2.shared);
			//assertSame(p1.singleton, p2.singleton);
			assertSame(p1.named, p2.named);

		} finally {
			psc1.end();
		}
		psc2.create();
		ProgramObject p2;
		try {
			psc2.begin();
			p2 = psc2.newInstance(ProgramObject.class);
			assertNotNull(p2);
			assertNotSame(p1, p2);
			assertNotSame(p1.shared, p2.shared);
			assertNotSame(p1.named, p2.named);
		} finally {
			psc2.end();
		}
		psc2.destroy();
		psc1.destroy();
		assertTrue(p1.stopped);

	}

	@ProcessScope
	public static class ProcessObject {
		boolean started = false;
		boolean stopped = false;

		@Inject
		SharedProgramObject prgShared;

		@Inject
		SharedProcessObject shared;

		@Inject
		NamedProcessObject named;

		@PostConstruct
		public void init() {
			started = true;
		}

		@PreDestroy
		public void destroy() {
			stopped = true;
		}

	}

	@ProcessScope(singleton = true)
	public static class SharedProcessObject {

	}

	@ProcessScope
	@Named("Process")
	public static class NamedProcessObject {

	}

	@Test
	public void processTest() throws Exception {
		ProgramScopeContext prgsc = injector.getInstance(ProgramScopeContext.class);
		assertNotNull(prgsc);
		prgsc.create();
		SharedProgramObject prg1;
		try {
			prgsc.begin();
			prg1 = prgsc.newInstance(SharedProgramObject.class);
			assertNotNull(prg1);
			ProcessScopeContext prcsc = injector.getInstance(ProcessScopeContext.class);
			assertNotNull(prcsc);
			prcsc.create();
			ProcessObject prc1;
			try {
				prcsc.begin();
				prc1 = prcsc.newInstance(ProcessObject.class);
				assertTrue(prc1.started);
				assertNotNull(prc1.shared);
				ProcessObject prc2 = prcsc.newInstance(ProcessObject.class);
				assertNotNull(prc2);
				assertNotSame(prc1, prc2);
				assertSame(prc1.shared, prc2.shared);
				assertSame(prc1.named, prc2.named);
				assertSame(prg1, prc1.prgShared);
			} finally {
				prcsc.end();
			}
			prcsc.destroy();
			assertTrue(prc1.stopped);
		} finally {
			prgsc.end();
		}
		prgsc.destroy();

	}

	@ThreadScope
	public static class ThreadObject {
		boolean started = false;
		boolean stopped = false;

		@Inject
		SharedProgramObject prgShared;

		@Inject
		SharedProcessObject prcShared;

		@Inject
		SharedThreadObject shared;

		@Inject
		NamedThreadObject named;

		@PostConstruct
		public void init() {
			started = true;
		}

		@PreDestroy
		public void destroy() {
			stopped = true;
		}

	}

	@ThreadScope(singleton = true)
	public static class SharedThreadObject {

	}

	@ThreadScope
	@Named("Thread")
	public static class NamedThreadObject {

	}

	@Test
	public void threadTest() throws Exception {
		ProgramScopeContext prgsc = injector.getInstance(ProgramScopeContext.class);
		assertNotNull(prgsc);
		prgsc.create();
		SharedProgramObject prg1;
		try {
			prgsc.begin();
			prg1 = prgsc.newInstance(SharedProgramObject.class);
			assertNotNull(prg1);
			ProcessScopeContext prcsc = injector.getInstance(ProcessScopeContext.class);
			assertNotNull(prcsc);
			prcsc.create();
			SharedProcessObject prc1;
			try {
				prcsc.begin();
				prc1 = prcsc.newInstance(SharedProcessObject.class);
				ThreadScopeContext thrsc = injector.getInstance(ThreadScopeContext.class);
				assertNotNull(thrsc);
				thrsc.create();
				ThreadObject thr1;
				try {
					thrsc.begin();
					thr1 = thrsc.newInstance(ThreadObject.class);
					assertTrue(thr1.started);
					assertNotNull(thr1.shared);
					ThreadObject thr2 = thrsc.newInstance(ThreadObject.class);
					assertNotNull(thr2);
					assertNotSame(thr1, thr2);
					assertSame(thr1.shared, thr2.shared);
					assertSame(thr1.named, thr2.named);
					assertSame(prg1, thr1.prgShared);
					assertSame(prc1, thr1.prcShared);
				} finally {
					thrsc.end();
				}
				thrsc.destroy();
				assertTrue(thr1.stopped);
			} finally {
				prcsc.end();
			}
			prcsc.destroy();
		} finally {
			prgsc.end();
		}
		prgsc.destroy();

	}

	@InstructionScope
	public static class InstructionObject {
		boolean started = false;
		boolean stopped = false;

		@Inject
		SharedProgramObject prgShared;

		@Inject
		SharedProcessObject prcShared;

		@Inject
		SharedThreadObject thrShared;

		@Inject
		SharedInstructionObject shared;

		@Inject
		NamedInstructionObject named;

		@PostConstruct
		public void init() {
			started = true;
		}

		@PreDestroy
		public void destroy() {
			stopped = true;
		}

	}

	@InstructionScope(singleton = true)
	public static class SharedInstructionObject {

	}

	@InstructionScope
	@Named("Instruction")
	public static class NamedInstructionObject {

	}

	@Test
	public void instructionTest() throws Exception {
		ProgramScopeContext prgsc = injector.getInstance(ProgramScopeContext.class);
		assertNotNull(prgsc);
		prgsc.create();
		SharedProgramObject prg1;
		try {
			prgsc.begin();
			prg1 = prgsc.newInstance(SharedProgramObject.class);
			assertNotNull(prg1);
			ProcessScopeContext prcsc = injector.getInstance(ProcessScopeContext.class);
			assertNotNull(prcsc);
			prcsc.create();
			SharedProcessObject prc1;
			try {
				prcsc.begin();
				prc1 = prcsc.newInstance(SharedProcessObject.class);
				ThreadScopeContext thrsc = injector.getInstance(ThreadScopeContext.class);
				assertNotNull(thrsc);
				thrsc.create();
				SharedThreadObject thr1;
				try {
					thrsc.begin();
					thr1 = thrsc.newInstance(SharedThreadObject.class);
					InstructionScopeContext inssc = injector.getInstance(InstructionScopeContext.class);
					assertNotNull(inssc);
					inssc.create();
					InstructionObject ins1;
					try {
						inssc.begin();
						ins1 = inssc.newInstance(InstructionObject.class);
						assertTrue(ins1.started);
						assertNotNull(ins1.shared);
						InstructionObject ins2 = inssc.newInstance(InstructionObject.class);
						assertNotNull(ins2);
						assertNotSame(ins1, ins2);
						assertSame(ins1.shared, ins2.shared);
						assertSame(ins2.named, ins2.named);
						assertSame(prg1, ins1.prgShared);
						assertSame(prc1, ins1.prcShared);
						assertSame(thr1, ins1.thrShared);
					} finally {
						inssc.end();
					}
					inssc.destroy();
					assertTrue(ins1.stopped);
				} finally {
					thrsc.end();
				}
				thrsc.destroy();
			} finally {
				prcsc.end();
			}
			prcsc.destroy();
		} finally {
			prgsc.end();
		}
		prgsc.destroy();

	}

	@ExecutableScope
	public static class ExecutableObject {
		boolean started = false;
		boolean stopped = false;

		@Inject
		Provider<InstructionObject> insProvider;

		@PostConstruct
		public void init() {
			started = true;
		}

		@PreDestroy
		public void destroy() {
			stopped = true;
		}

	}

	@Test
	public void executableTest() throws Exception {
		ExecutableScopeContext exsc = injector.getInstance(ExecutableScopeContext.class);
		assertNotNull(exsc);
		exsc.create();
		ExecutableObject ex1;
		try {
			exsc.begin();
			ex1 = exsc.newInstance(ExecutableObject.class);
		} finally {
			exsc.end();
		}

		ProgramScopeContext prgsc = injector.getInstance(ProgramScopeContext.class);
		assertNotNull(prgsc);
		prgsc.create();
		SharedProgramObject prg1;
		try {
			prgsc.begin();
			prg1 = prgsc.newInstance(SharedProgramObject.class);
			assertNotNull(prg1);
			ProcessScopeContext prcsc = injector.getInstance(ProcessScopeContext.class);
			assertNotNull(prcsc);
			prcsc.create();
			SharedProcessObject prc1;
			try {
				prcsc.begin();
				prc1 = prcsc.newInstance(SharedProcessObject.class);
				ThreadScopeContext thrsc = injector.getInstance(ThreadScopeContext.class);
				assertNotNull(thrsc);
				thrsc.create();
				SharedThreadObject thr1;
				try {
					thrsc.begin();
					thr1 = thrsc.newInstance(SharedThreadObject.class);
					InstructionScopeContext inssc = injector.getInstance(InstructionScopeContext.class);
					assertNotNull(inssc);
					inssc.create();
					InstructionObject ins1;
					try {
						inssc.begin();
						try {
							exsc.begin();
							ins1 = ex1.insProvider.get();
							assertTrue(ins1.started);
							assertNotNull(ins1.shared);
							assertSame(prg1, ins1.prgShared);
							assertSame(prc1, ins1.prcShared);
							assertSame(thr1, ins1.thrShared);
						} finally {
							exsc.end();
						}
						exsc.destroy();
						assertTrue(ex1.stopped);
					} finally {
						inssc.end();
					}
					inssc.destroy();
					assertTrue(ins1.stopped);
				} finally {
					thrsc.end();
				}
				thrsc.destroy();
			} finally {
				prcsc.end();
			}
			prcsc.destroy();
		} finally {
			prgsc.end();
		}
		prgsc.destroy();

	}

}
