package org.apache.ode.spi.exec.platform;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.util.Map;

import javax.xml.namespace.QName;

@Retention(RUNTIME)
@Target(METHOD)
public @interface Dispatch {

	Filter[] filters() default {};

	//void dispatch(Queue<Token> tokens);
	//void dispatch(Token token);
	//Queue<CommandToken> dispatch(Map<QName, ?> environment, Queue<CommandToken> execStack);
	//TODO test pipes and stacks of incoming parameters

	public static interface Token {
		public TokenType type();
	}
	
	public static class Pipe implements Token {
		public TokenType type() {
			return TokenType.PIPE;
		}
	}
	public static class BeginSequence implements Token {
		public TokenType type() {
			return TokenType.BEGIN_SEQUENCE;
		}
	}

	public static class EndSequence implements Token {
		public TokenType type() {
			return TokenType.END_SEQUENCE;
		}
	}

	public static class BeginParallel implements Token {
		public TokenType type() {
			return TokenType.BEGIN_PARALLEL;
		}
	}

	public static class EndPrallel implements Token {
		public TokenType type() {
			return TokenType.END_PARALLEL;
		}
	}

	public static class Environment implements Token {
		private Map<QName, ?> environment;

		Environment(Map<QName, ?> environment) {
			this.environment = environment;
		}

		public TokenType type() {
			return TokenType.ENVIRONMENT;
		}
	}

	public static class Operation implements Token {
		private QName name;
		private Object[] parameters;
		private Object[] resolvedParameters;
		private MethodHandle resolvedMethodHandle;
		private boolean isResolved;

		public Operation(QName name, Object... parameters) {
			this.name = name;
			this.parameters = parameters;
		}

		public TokenType type() {
			return TokenType.OPERATION;
		}
	}

	public static class Command extends Operation {
		public Command(QName name, Object... parameters) {
			super(name, parameters);
		}

		public TokenType type() {
			return TokenType.COMMAND;
		}
	}

	public static class Block extends Operation {
		public Block(QName name, Object... parameters) {
			super(name, parameters);
		}

		public TokenType type() {
			return TokenType.BLOCK;
		}
	}

	public static class Abort extends Operation {
		public Abort(QName name, Object... parameters) {
			super(name, parameters);
		}

		public TokenType type() {
			return TokenType.ABORT;
		}
	}

	public static enum TokenType {
		PIPE, BEGIN_SEQUENCE, END_SEQUENCE, BEGIN_PARALLEL, END_PARALLEL, ENVIRONMENT, COMMAND, OPERATION, BLOCK, ABORT;
	}

	@Retention(RUNTIME)
	@Target(TYPE)
	public static @interface Dispatcher {

	}

	@Retention(RUNTIME)
	@Target(METHOD)
	public static @interface Filter {

		public String namespace() default "";

		public String name() default "";

		public Class<?> token() default Token.class;

		//public boolean 
	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface CommandMap {

	}

	@Retention(RUNTIME)
	@Target(PARAMETER)
	public static @interface OperationMap {

	}

}
