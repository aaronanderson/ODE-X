package org.ode.data.memory.repo;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.xml.bind.annotation.XmlRegistry;

import org.apache.ode.data.memory.repo.xml.ObjectFactory;
import org.apache.ode.data.memory.repo.xml.Repository;

@XmlRegistry
public class RepoObjectFactory extends ObjectFactory {

	@Inject
	Provider<FileRepository> provider;

	@Override
	public Repository createRepository() {
		return provider.get();
	}

}
