package org.apache.ode.spi.exec.context;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
/*
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import jpatest.xml.State;
*/
//@Entity
//@Table(name = "EXEC_CONTEXT")
//@DiscriminatorColumn(name = "TYPE")
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)*/
public abstract class ExecutionContext extends org.apache.ode.spi.exec.context.xml.ExecutionContext implements Serializable {

	protected String attr1;
	protected String attr2;
	protected String attr3;
	protected String attr4;
	protected String attr5;

	//general purpose storage if attribute storage is insufficient
	protected byte[] content;
	
	//Create and Modify times. Modify Time can be used for @Version
	protected Calendar createTime;
	protected Calendar modifyTime;
	
	//Optional lock value that can be used by a persistence implementation to lock accross nodes
	protected String lockInfo;

	protected List<Memory> memoryBlocks;

	//protected List<ExecutionContextLink> eCtxLinks;

	//@Column(name = "ATTR1")
	public String getAttr1() {
		return attr1;
	}

	public void setAttr1(String attr1) {
		this.attr1 = attr1;
	}

	//@Column(name = "ATTR2")
	public String getAttr2() {
		return attr2;
	}

	public void setAttr2(String attr2) {
		this.attr2 = attr2;
	}

	//@Column(name = "ATTR3")
	public String getAttr3() {
		return attr3;
	}

	public void setAttr3(String attr3) {
		this.attr3 = attr3;
	}

	//@Column(name = "ATTR4")
	public String getAttr4() {
		return attr4;
	}

	public void setAttr4(String attr4) {
		this.attr4 = attr4;
	}

	//@Column(name = "ATTR5")
	public String getAttr5() {
		return attr5;
	}

	public void setAttr5(String attr5) {
		this.attr5 = attr5;
	}
	
	public void setCreateTime(Calendar createTime) {
		this.createTime = createTime;
	}
	
	public Calendar getCreateTime() {
		return createTime;
	}
	
	public void setModifyTime(Calendar modifyTime) {
		this.modifyTime = modifyTime;
	}
	
	public Calendar getModifyTime() {
		return modifyTime;
	}
	
	public void setLockInfo(String lockInfo) {
		this.lockInfo = lockInfo;
	}
	
	public String getLockInfo() {
		return lockInfo;
	}

	//@Column(name = "CONTENT")
	//@Lob
	//@Basic(fetch = FetchType.LAZY)
	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	//@OneToMany
	//@OrderColumn(name = "INDEX")
	//important for array access
	//@JoinTable(name = "ECTX_MEMORY_BLOCK", joinColumns = @JoinColumn(name = "BLOCK_ADDR", referencedColumnName = "ECTX_ADDR"), inverseJoinColumns = @JoinColumn(name = "PHYS_ADDR", referencedColumnName = "PHYS_ADDR"))
	public List<Memory> getMemoryBlocks() {
		return memoryBlocks;
	}

	public void setMemoryBlocks(List<Memory> memoryBlocks) {
		this.memoryBlocks = memoryBlocks;
	}

	//@OneToMany(cascade=CascadeType.ALL) 
	//@JoinColumns({
	 //    @JoinColumn(name = "ECTX_ADDR", referencedColumnName = "ECTX_ADDR"),
	 //    @JoinColumn(name = "LINK_ADDR", referencedColumnName = "ECTX_ADDR")
	//}/*
	//inverseJoinColumns={ 
	//		@JoinColumn(name = "ECTX_ADDR", referencedColumnName = "ECTX_ADDR"),
	//		@JoinColumn(name = "ECTX_ADDR", referencedColumnName = "LINK_ADDR")
	//}*/)
	public List<ExecutionContextLink> getECtxInLinks() {
		return (List)inLink;
	}

	public void setECtxInLinks(List<ExecutionContextLink> eCtxLinks) {
		inLink=(List)eCtxLinks;
	}
	
	public List<ExecutionContextLink> getECtxOutLinks() {
		return (List)outLink;
	}

	public void setECtxOutLinks(List<ExecutionContextLink> eCtxLinks) {
		outLink=(List)eCtxLinks;
	}

	/*
	@Override
	//@Id
	//@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="ECTX_SEQ")
    //@SequenceGenerator(name="ECTX_SEQ", sequenceName="ECTX_SEQ", allocationSize=10)
	//@Column(name = "ECTX_ADDR")
	public long getECtxAddr() {
		return super.getECtxAddr();
	}

	@Override
	//@Column(name = "NAME")
	public String getName() {
		return super.getName();
	}

	@Override
	//@Column(name = "STATE")
	public State getState() {
		return super.getState();
	}

	@Override
	//@Transient
	public List<jpatest.xml.ExecutionContextLink> getExecutionContextLink() {
		// TODO Auto-generated method stub
		return super.getExecutionContextLink();
	}*/

}
