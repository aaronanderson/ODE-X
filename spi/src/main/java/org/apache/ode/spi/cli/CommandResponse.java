package org.apache.ode.spi.cli;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandResponse implements Serializable {

	private Status status = Status.OK;
	private String message;
	private List<FileResult> fileResults = new LinkedList<>();

	public Status status() {
		return status;
	}

	public CommandResponse status(Status status) {
		this.status = status;
		return this;
	}

	public String message() {
		return message;
	}

	public CommandResponse message(String message) {
		this.message = message;
		return this;
	}

	public List<FileResult> fileResults() {
		return fileResults;
	}

	public CommandResponse fileResults(List<FileResult> fileResults) {
		this.fileResults = fileResults;
		return this;
	}

	public CommandResponse setFileResults(FileResult... fileResults) {
		this.fileResults = Stream.of(fileResults).collect(Collectors.toList());
		return this;
	}

	public static enum Status {
		OK, ERROR;
	}

	public static class FileResult implements Serializable {
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

		public FileResult fileContents(byte[] fileContents) {
			this.fileContents = fileContents;
			return this;
		}

	}

}
