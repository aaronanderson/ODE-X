package org.apache.ode.runtime.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ode.spi.exec.xml.InsAdd;
import org.apache.ode.spi.exec.xml.InsAddRef;
import org.apache.ode.spi.exec.xml.Instruction;
import org.apache.ode.spi.exec.xml.InstructionAddress;

public class IndexedInstructionAddress extends InstructionAddress {

	public static final Pattern INSTRUCTION_ADDRESS = Pattern.compile("b(\\d+)i(\\d+)");

	public short bIndex;
	public short iIndex;

	public Instruction ref;

	IndexedInstructionAddress(String addr) {
		super(addr);
	}

	static IndexedInstructionAddress unmarshal(String addr, Map<String, IndexedInstructionAddress> instructions) throws Exception {
		IndexedInstructionAddress iaddr = instructions.get(addr);
		if (iaddr == null) {
			iaddr = new IndexedInstructionAddress(addr);
			Matcher m = INSTRUCTION_ADDRESS.matcher(addr);
			if (m.find()) {
				iaddr.bIndex = Short.parseShort(m.group(1));
				iaddr.iIndex = Short.parseShort(m.group(2));
			} else {
				throw new Exception(String.format("invalid instruction format", addr));
			}
			instructions.put(addr, iaddr);
		}
		return iaddr;

	}

	static class IndexedInsAddAdapter extends InsAddAdapter {
		Map<String, IndexedInstructionAddress> instructions = new HashMap<String, IndexedInstructionAddress>();

		public IndexedInsAddAdapter(Map<String, IndexedInstructionAddress> instructions) {
			this.instructions = instructions;
		}

		@Override
		public InsAdd unmarshal(String addr) throws Exception {
			return IndexedInstructionAddress.unmarshal(addr, instructions);
		}

	}

	static class IndexedInsAddRefAdapter extends InsAddRefAdapter {
		Map<String, IndexedInstructionAddress> instructions = new HashMap<String, IndexedInstructionAddress>();

		public IndexedInsAddRefAdapter(Map<String, IndexedInstructionAddress> instructions) {
			this.instructions = instructions;
		}

		@Override
		public InsAddRef unmarshal(String addr) throws Exception {
			return IndexedInstructionAddress.unmarshal(addr, instructions);
		}

	}
}