package org.apache.ode.runtime.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.cache.Cache.Entry;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteFileSystem;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.igfs.IgfsFile;
import org.apache.ignite.igfs.IgfsPath;
import org.apache.ode.spi.deployment.PathScanner;
import org.apache.ode.spi.tenant.Tenant;
import org.h2.expression.Wildcard;

@Dependent
public class PathScannerImpl implements PathScanner {

	@Inject
	private Ignite ignite;

	private Set<List<String>> includes = new HashSet<>();
	private Set<List<String>> excludes = new HashSet<>();
	private Set<Filter> filters = new HashSet<>();

	@Override
	public void setIncludes(String... paths) {
		for (String path : paths) {
			includes.add(Arrays.asList(path.split("/")));
		}

	}

	@Override
	public void setExcludes(String... paths) {
		for (String path : paths) {
			excludes.add(Arrays.asList(path.split("/")));
		}
	}

	@Override
	public void setFilters(Filter... filter) {
		Collections.addAll(filters, filter);
	}

	@Override
	public Set<IgfsPath> scan(IgfsPath baseDir) {
		Set<IgfsPath> results = new HashSet<>();
		IgniteFileSystem repositoryFS = ignite.fileSystem(Tenant.REPOSITORY_FILE_SYSTEM);

		LinkedList<IgfsFile> paths = new LinkedList<>();
		paths.add(repositoryFS.info(baseDir));

		while (!paths.isEmpty()) {
			IgfsFile parent = paths.poll();

			for (IgfsFile child : repositoryFS.listFiles(parent.path())) {
				if (child.isDirectory() && include(child)) {
					paths.push(child);
				} else if (child.isFile() && include(child)) {
					results.add(child.path());
				}
			}

		}

		return results;
	}

	private boolean include(IgfsFile file) {
		if (matches(file, includes) && !matches(file, excludes)) {
			return true;
		}
		return false;

	}

	private boolean matches(IgfsFile file, Set<List<String>> patterns) {
		List<String> components = file.path().components();
		components = components.subList(2, components.size());
		next: for (List<String> matchPath : patterns) {
			LinkedList<String> matchQueue = new LinkedList<>(matchPath);

			if (file.isDirectory()) {
				matchQueue.removeLast();
			}

			for (String component : components) {

				String match = matchQueue.peek();
				if ("**".contentEquals(match)) {
					if (matchQueue.size() > 1) {
						String secondMatch = reformatGlobExpr(matchQueue.get(1));
						if (component.matches(secondMatch)) {
							matchQueue.poll();
							matchQueue.poll();
							continue;
						}
					}
				}
				match = reformatGlobExpr(matchQueue.poll());
				if (!component.matches(match)) {
					continue next;
				}
			}
			return true;

		}
		return false;

	}

	String reformatGlobExpr(String expr) {
		expr = expr.replaceAll("\\.", "\\\\.");
		expr = expr.replaceAll("\\*", "\\.\\*");
		return expr;
	}

	@Override
	public Filter contentTypeFilter(String... contentType) {
		Set<String> fileExtensions = new HashSet();
		IgniteCache<BinaryObject, BinaryObject> configCache = ignite.cache(Tenant.TENANT_CACHE_NAME).withKeepBinary();
		SqlQuery<BinaryObject, BinaryObject> query = new SqlQuery<>("Configuration", "type = ?");
		List<Entry<BinaryObject, BinaryObject>> contentTypeEntries = configCache.query(query.setArgs("ode:contentType")).getAll();
		for (Entry<BinaryObject, BinaryObject> entry : contentTypeEntries) {
			String[] fileExts = entry.getValue().field("fileExtensions");
			for (String fileExt : fileExts) {
				fileExtensions.add(fileExt);
			}
		}

		return new ContentTypeFilter(fileExtensions);
	}

	private class ContentTypeFilter implements Filter {

		private final Set<String> fileExtensions;

		private ContentTypeFilter(Set<String> fileExtensions) {
			this.fileExtensions = fileExtensions;
		}

		@Override
		public boolean include(IgfsPath path) {
			for (String fileExt : fileExtensions) {
				if (path.name().endsWith(fileExt)) {
					return true;
				}
			}
			return false;
		}

	}

}
