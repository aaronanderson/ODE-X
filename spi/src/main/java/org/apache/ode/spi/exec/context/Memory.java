package org.apache.ode.spi.exec.context;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
/*
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
*/
/**
 * The purpose of this class is to provided a common method for persisting JAXB execution context instances, especially using JPA.
 * A goal of this framework is to provide an extensible framework so that new Memory types can be added in the future.
 * Clearly having a JPA table for every JAXB class is prohibitive to extensibility. Instead, we will introduce a generic Memory
 * base class that all JAXB classes will inherit from which will provide a byte array for persistence. Each JAXB class will be 
 * responsible for loading/storing it's state into the byte array. This will involve incorporating additional persistence logic 
 * but since we didn't want to use the JPA one table per class property to column mapping anyway this is acceptable. Additionally
 * This Memory class allows for associating other Memory instances to itself in the form of MemoryBlocks outside the XSD model. This provides
 * a means to create advanced XSD hierarchies/Memory models (variables, arrays, types, etc) and have them persisted and restored. 
 * 
 *
 */
/*@Entity
@Table(name = "MEMORY")
@DiscriminatorColumn(name = "TYPE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("MEMORY")*/
public class Memory extends org.apache.ode.spi.exec.context.xml.Memory implements Serializable {

	//These properties are added through inheritance for unified persistence purposes only and are not considered part of the actual XSD data model
	
	/*Thought about this a while but being able to store element attribute and values in generic columns is probably better than
	trying to serialize everything in a byte array. Reasons being:
	
	1. Less conversion overhead. Easier to convert most attribute/values to string than to parse XML value with indeterminate schema and possibly mixing attribute values with element values
	2. Attributes may be queried. Investigate enabling indexes
	3. If persistence/loading is performed correctly all persistence fields will be nulled out reducing memory profile
	4. reduce number of memory object needed and only use them when they can be reused.
	
	
	Five was selected as a good number since it will fit most scenarios and if more than five are
	needed the element can be decomposed into multiple memory objects.
	*/
	
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
	
	//This is really only used for memory cleanup so it doesn't make sense to polute the XSD model with it
	protected ExecutionContext execContext;

	
	
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
	//@OrderColumn(name="INDEX")//important for array access
	//@JoinTable(name = "MEMORY_BLOCK", joinColumns = @JoinColumn(name = "BLOCK_ADDR", referencedColumnName = "PHYS_ADDR"), inverseJoinColumns = @JoinColumn(name = "PHYS_ADDR", referencedColumnName = "PHYS_ADDR"))
	public List<Memory> getMemoryBlocks() {
		return memoryBlocks;
	}

	public void setMemoryBlocks(List<Memory> memoryBlocks) {
		this.memoryBlocks = memoryBlocks;
	}
	
	
	//@OneToOne
	//@JoinColumn(name = "ECTX_ADDR", referencedColumnName = "ECTX_ADDR", nullable=false)
	public ExecutionContext getExecContext() {
		return execContext;
	}

	public void setExecContext(ExecutionContext execContext) {
		this.execContext = execContext;
	}

	@Override
	//@Column(name = "VIRT_ADDR")
	public String getVirtualAddr() {
		return super.getVirtualAddr();
	}

	@Override
	//@Id
	//@GeneratedValue
	//@Column(name = "PHYS_ADDR")
	public long getPhysicalAddr() {
		return super.getPhysicalAddr();
	}

}
