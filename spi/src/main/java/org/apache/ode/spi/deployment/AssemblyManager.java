package org.apache.ode.spi.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.ignite.igfs.IgfsPath;
import org.apache.ode.spi.deployment.Assembly.AssemblyException;

public interface AssemblyManager {
	public static final String SERVICE_NAME = "urn:org:apache:ode:assembly";
	
	public static final IgfsPath ASSEMBLY_DIR = new IgfsPath("/assemblies");
	

	public <C> void create(URI type, URI reference, C config, Entry... files) throws AssemblyException;

	public <C> void update(URI reference, C file) throws AssemblyException;

	public void update(URI reference, Entry... files) throws AssemblyException;

	public void delete(URI reference) throws AssemblyException;

	public void deploy(URI reference) throws AssemblyException;

	public void undeploy(URI reference) throws AssemblyException;

	public void createAlias(String alias, URI reference) throws AssemblyException;

	public void deleteAlias(String alias) throws AssemblyException;

	public URI alias(String alias) throws AssemblyException;

	public static final class Entry {

		private final String path;
		private final URL location;

		public Entry(Path location) throws IOException {
			this.path = location.getFileName().toString();
			this.location = location.toUri().toURL();
		}

		public Entry(String path, Path location) throws IOException {
			this.path = path;
			this.location = location.toUri().toURL();
		}

		public Entry(URL location) throws URISyntaxException {
			this.path = Paths.get(location.toURI()).getFileName().toString();
			this.location = location;
		}

		public Entry(String path, URL location) {
			this.path = path;
			this.location = location;
		}

		public String getPath() {
			return path;
		}

		public URL getLocation() {
			return location;
		}

	}

}
