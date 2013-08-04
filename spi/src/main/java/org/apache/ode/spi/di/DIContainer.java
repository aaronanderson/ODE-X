package org.apache.ode.spi.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface DIContainer {

	<T> T getInstance(Class<T> clazz);

	<T> T getInstance(Class<T> clazz, Annotation qualifier);

	<T> T getInstance(TypeLiteral<T> type);

	<T> T getInstance(TypeLiteral<T> type, Annotation qualifier);

	//http://blog.vityuk.com/2011/03/java-generics-and-reflection.html
	//Since Java doesn't have reified generics this is needed to capture generic types. Inspired from Guice and CDI TypeLiteral 
	//which would be facaded by this class
	public static class TypeLiteral<T> {

		private Type type;

		public TypeLiteral() {
			type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		}

		public Type getType() {
			return type;
		}

	}

}
