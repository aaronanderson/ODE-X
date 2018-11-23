package org.apache.ode.spi.cli;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandRequest implements Serializable {

	private String entity;
	private String name;
	private List<Parameter> parameters = new LinkedList<>();

	public String entity() {
		return entity;
	}

	public CommandRequest entity(String entity) {
		this.entity = entity;
		return this;
	}

	public String name() {
		return name;
	}

	public CommandRequest name(String name) {
		this.name = name;
		return this;
	}

	public List<Parameter> parameters() {
		return parameters;
	}

	public CommandRequest parameters(List<Parameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public CommandRequest parameters(Parameter... parameters) {
		this.parameters = Stream.of(parameters).collect(Collectors.toList());
		return this;
	}

	public abstract static class Parameter<T extends Parameter> implements Serializable {
		private String name;

		public String name() {
			return name;
		}

		public T name(String name) {
			this.name = name;
			return (T) this;
		}

	}

	public static class StringParameter extends Parameter<StringParameter> {
		private String value;

		public String value() {
			return value;
		}

		public StringParameter value(String value) {
			this.value = value;
			return this;
		}

	}

	public static class NumberParameter extends Parameter<NumberParameter> {
		private Long value;

		public Long value() {
			return value;
		}

		public NumberParameter value(Long value) {
			this.value = value;
			return this;
		}
	}

	public static class FileParameter extends Parameter<FileParameter> {
		private String fileName;
		private byte[] fileContents;

		public String fileName() {
			return fileName;
		}

		public void fileName(String fileName) {
			this.fileName = fileName;
		}

		public byte[] fileContents() {
			return fileContents;
		}

		public FileParameter fileContents(byte[] fileContents) {
			this.fileContents = fileContents;
			return this;
		}

	}

}
