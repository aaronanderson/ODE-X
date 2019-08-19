package org.apache.ode.spi.deployment;

import java.util.Set;

import org.apache.ignite.igfs.IgfsPath;

public interface PathScanner {

	void setIncludes(String... paths);

	void setExcludes(String... paths);

	void setFilters(Filter... filters);

	Filter contentTypeFilter(String... contentType);

	Set<IgfsPath> scan(IgfsPath baseDir);

	@FunctionalInterface
	public static interface Filter {

		boolean include(IgfsPath path);

	}

}
