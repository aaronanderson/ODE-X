package org.apache.ode.spi.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Deployment<C> {

	protected C config;

	public List<Entry> files = new ArrayList<>();

	public <T > List<Entry> files() {
		return files;
	}

	public C config() {
		return config;
	}

	public static class DeploymentBuilder<B extends DeploymentBuilder<?, D, C>, D extends Deployment<C>, C> {

		protected D deployment;

		protected DeploymentBuilder() {
			this.deployment = (D) new Deployment();
		}

		public B file(Entry entry) {
			deployment.files.add(entry);
			return (B) this;
		}

		public B config(C config) {
			deployment.config = config;
			return (B) this;
		}

		public D build() {
			return deployment;
		}

	}

	public static class Entry {

		private final String path;
		private final InputSupplier input;

		public Entry(String path, InputSupplier input) throws IOException {
			this.path = path;
			this.input = input;
		}

		public String path() {
			return path;
		}

		public Deployment.InputSupplier input() {
			return input;
		}

	}

	public static final class FileEntry extends Entry {

		public FileEntry(Path location) throws IOException {
			super(location.getFileName().toString(), () -> Files.newInputStream(location));
		}

		public FileEntry(String path, Path location) throws IOException {
			super(path, () -> Files.newInputStream(location));
		}

	}

	public static final class URLEntry extends Entry {

		public URLEntry(URL location) throws URISyntaxException, IOException {
			super(Paths.get(location.toURI()).getFileName().toString(), () -> location.openStream());
		}

		public URLEntry(String path, URL location) throws IOException {
			super(path, () -> location.openStream());
		}

	}

	@FunctionalInterface
	public static interface InputSupplier {

		InputStream open() throws IOException;
	}

}