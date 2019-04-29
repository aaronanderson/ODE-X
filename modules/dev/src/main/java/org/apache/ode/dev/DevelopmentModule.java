package org.apache.ode.dev;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ode.spi.tenant.Module;
import org.apache.ode.spi.tenant.Tenant;
import org.apache.ode.spi.tenant.Module.Id;

@Id(DevelopmentModule.SCXML_MODULE_ID)
public class DevelopmentModule implements Module {

	public static final String SCXML_MODULE_ID = "org:apache:ode:scxml";


	@Enable
	public void enable(Ignite ignite) {

	}

	@Disable
	public void disable(Ignite ignite) {
	}


}
