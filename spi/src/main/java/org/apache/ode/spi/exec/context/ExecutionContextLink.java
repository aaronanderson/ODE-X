package org.apache.ode.spi.exec.context;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.ode.spi.exec.context.xml.Axis;

/*
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
*/
//import jpatest.ExecutionContextLink.ExecutionContextLinkPK;
//import jpatest.xml.Axis;

//@Entity
//@IdClass(value=ExecutionContextLinkPK.class)
//@Table(name = "ECTX_LINK")
public class ExecutionContextLink extends org.apache.ode.spi.exec.context.xml.ExecutionContextLink implements Serializable {

	protected int index;

	/*
	@Override
	//@Id
	//@Column(name = "ECTX_ADDR")
	public long getECtxAddr() {
		return eCtxAddr;
	}
	
	@Override
	//@Id
	//@Column(name = "LINK_ADDR")
	public long getLinkAddr() {
		return linkAddr;
	}*/

	//@OrderColumn(name = "INDEX")
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	/*
	@Override
	//@Column(name = "AXIS")
	public Axis getAxis() {
		return axis;
	}

	@Override
	//@Column(name = "RELATION")
	public String getRelation() {
		return relation;
	}*/

	public static class ExecutionContextLinkPK implements Serializable {

		private long linkAddr;
		private long eCtxAddr;

		public ExecutionContextLinkPK() {
			// Your class must have a no-arq constructor
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ExecutionContextLinkPK) {
				ExecutionContextLinkPK executionContextLinkPK = (ExecutionContextLinkPK) obj;

				if (executionContextLinkPK.getLinkAddr() != linkAddr) {
					return false;
				}

				if (executionContextLinkPK.getECtxAddr() != eCtxAddr) {
					return false;
				}

				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(new long[] { linkAddr, eCtxAddr });
		}

		public long getLinkAddr() {
			return linkAddr;
		}

		public void setLinkAddr(long value) {
			this.linkAddr = value;
		}

		public long getECtxAddr() {
			return eCtxAddr;
		}

		public void setECtxAddr(long value) {
			this.eCtxAddr = value;
		}
	}

}
