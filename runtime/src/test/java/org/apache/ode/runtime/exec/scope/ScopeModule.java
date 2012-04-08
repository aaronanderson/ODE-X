package org.apache.ode.runtime.exec.scope;

import java.util.HashSet;
import java.util.Set;

import org.apache.ode.runtime.exec.platform.ScopeContext.ExecutableScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.InstructionScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.ProcessScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.ProgramScopeContext;
import org.apache.ode.runtime.exec.platform.ScopeContext.ThreadScopeContext;
import org.apache.ode.runtime.exec.scope.ScopeContextImpl.GuiceScopeImpl;
import org.apache.ode.runtime.exec.scope.ScopeContextImpl.ThreadLocalState;
import org.apache.ode.spi.exec.ExecutableScope;
import org.apache.ode.spi.exec.InstructionScope;
import org.apache.ode.spi.exec.ProcessScope;
import org.apache.ode.spi.exec.ProgramScope;
import org.apache.ode.spi.exec.ThreadScope;

import com.google.inject.AbstractModule;

public class ScopeModule extends AbstractModule {
	protected void configure() {
		bindScope(ProgramScope.class, new ProgramGuiceScopeImpl());
		bind(ProgramScopeContext.class).to(ProgramScopeContextImpl.class);
		bindScope(ProcessScope.class, new ProcessGuiceScopeImpl());
		bind(ProcessScopeContext.class).to(ProcessScopeContextImpl.class);
		bindScope(ThreadScope.class, new ThreadGuiceScopeImpl());
		bind(ThreadScopeContext.class).to(ThreadScopeContextImpl.class);
		bindScope(InstructionScope.class, new InstructionGuiceScopeImpl());
		bind(InstructionScopeContext.class).to(InstructionScopeContextImpl.class);
		bindScope(ExecutableScope.class, new ExecutableGuiceScopeImpl());
		bind(ExecutableScopeContext.class).to(ExecutableScopeContextImpl.class);
	}

	public static class ProgramScopeContextImpl extends ScopeContextImpl implements ProgramScopeContext {
		static ThreadLocal<ThreadLocalState> programState = new ThreadLocal<ThreadLocalState>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return programState;
		}

	}

	public static class ProgramGuiceScopeImpl extends GuiceScopeImpl {
		public static Set<Class<?>> SINGLETONS = new HashSet<Class<?>>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return ProgramScopeContextImpl.programState;
		}

		@Override
		public Set<Class<?>> getSingletons() {
			return SINGLETONS;
		}

		@Override
		public boolean isSingleton(Class<?> clazz) {
			ProgramScope scope = clazz.getAnnotation(ProgramScope.class);
			if (scope!=null){
				return scope.singleton();
			}
			return false;
		}
	}

	public static class ProcessScopeContextImpl extends ScopeContextImpl implements ProcessScopeContext {
		static ThreadLocal<ThreadLocalState> processState = new ThreadLocal<ThreadLocalState>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return processState;
		}

	}

	public static class ProcessGuiceScopeImpl extends GuiceScopeImpl {
		public static Set<Class<?>> SINGLETONS = new HashSet<Class<?>>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return ProcessScopeContextImpl.processState;
		}

		@Override
		public Set<Class<?>> getSingletons() {
			return SINGLETONS;
		}
		
		@Override
		public boolean isSingleton(Class<?> clazz) {
			ProcessScope scope = clazz.getAnnotation(ProcessScope.class);
			if (scope!=null){
				return scope.singleton();
			}
			return false;
		}
	}

	public static class ThreadScopeContextImpl extends ScopeContextImpl implements ThreadScopeContext {
		static ThreadLocal<ThreadLocalState> threadState = new ThreadLocal<ThreadLocalState>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return threadState;
		}

	}

	public static class ThreadGuiceScopeImpl extends GuiceScopeImpl {
		public static Set<Class<?>> SINGLETONS = new HashSet<Class<?>>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return ThreadScopeContextImpl.threadState;
		}

		@Override
		public Set<Class<?>> getSingletons() {
			return SINGLETONS;
		}
		
		@Override
		public boolean isSingleton(Class<?> clazz) {
			ThreadScope scope = clazz.getAnnotation(ThreadScope.class);
			if (scope!=null){
				return scope.singleton();
			}
			return false;
		}
	}

	public static class InstructionScopeContextImpl extends ScopeContextImpl implements InstructionScopeContext {
		static ThreadLocal<ThreadLocalState> instructionState = new ThreadLocal<ThreadLocalState>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return instructionState;
		}

	}

	public static class InstructionGuiceScopeImpl extends GuiceScopeImpl {
		public static Set<Class<?>> SINGLETONS = new HashSet<Class<?>>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return InstructionScopeContextImpl.instructionState;
		}

		@Override
		public Set<Class<?>> getSingletons() {
			return SINGLETONS;
		}
		
		@Override
		public boolean isSingleton(Class<?> clazz) {
			InstructionScope scope = clazz.getAnnotation(InstructionScope.class);
			if (scope!=null){
				return scope.singleton();
			}
			return false;
		}
	}
	
	public static class ExecutableScopeContextImpl extends ScopeContextImpl implements ExecutableScopeContext {
		static ThreadLocal<ThreadLocalState> executableState = new ThreadLocal<ThreadLocalState>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return executableState;
		}

	}

	public static class ExecutableGuiceScopeImpl extends GuiceScopeImpl {
		public static Set<Class<?>> SINGLETONS = new HashSet<Class<?>>();

		@Override
		public ThreadLocal<ThreadLocalState> getThreadLocal() {
			return ExecutableScopeContextImpl.executableState;
		}

		@Override
		public Set<Class<?>> getSingletons() {
			return SINGLETONS;
		}
		
		@Override
		public boolean isSingleton(Class<?> clazz) {
			ExecutableScope scope = clazz.getAnnotation(ExecutableScope.class);
			if (scope!=null){
				return scope.singleton();
			}
			return false;
		}
	}


}
