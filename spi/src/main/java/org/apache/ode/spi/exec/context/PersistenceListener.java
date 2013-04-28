package org.apache.ode.spi.exec.context;

import java.util.ArrayList;
import java.util.List;

//import javax.persistence.PostLoad;
//import javax.persistence.PrePersist;

public class PersistenceListener {

	//@PrePersist
	/*protected void store(Variable variable) {
		ArrayList<Memory> memoryBlocks = new ArrayList<Memory>();
		memoryBlocks.add(variable.getType());
		memoryBlocks.add(variable.getScope());
		variable.setMemoryBlocks(memoryBlocks);
	}

	//@PostLoad
	protected void load(Variable variable) {
		List<Memory> memoryBlocks = variable.getMemoryBlocks();
		variable.setType((Type) memoryBlocks.get(0));
		variable.setScope((Scope) memoryBlocks.get(1));
		variable.setMemoryBlocks(null);
	}*/
}
