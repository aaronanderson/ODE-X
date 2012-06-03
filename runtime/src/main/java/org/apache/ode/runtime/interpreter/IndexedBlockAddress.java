package org.apache.ode.runtime.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ode.spi.exec.xml.BlcAdd;
import org.apache.ode.spi.exec.xml.BlcAddRef;
import org.apache.ode.spi.exec.xml.BlockAddress;

public class IndexedBlockAddress extends BlockAddress {

	public static final Pattern BLOCK_ADDRESS = Pattern.compile("b(\\d+)");

	public short bIndex;
	//Block ref;
	public IndexedInstructionAddress[] addresses;

	IndexedBlockAddress(String addr) {
		super(addr);
	}

	static IndexedBlockAddress unmarshal(String addr, Map<String, IndexedBlockAddress> blocks) throws Exception {
		IndexedBlockAddress baddr = blocks.get(addr);
		if (baddr == null) {
			baddr = new IndexedBlockAddress(addr);
			Matcher m = BLOCK_ADDRESS.matcher(addr);
			if (m.find()) {
				baddr.bIndex = Short.parseShort(m.group(1));
			} else {
				throw new Exception(String.format("invalid block format", addr));
			}
			blocks.put(addr, baddr);
		}
		return baddr;

	}

	static class IndexedBlcAddAdapter extends BlcAddAdapter {
		Map<String, IndexedBlockAddress> blocks = new HashMap<String, IndexedBlockAddress>();

		IndexedBlcAddAdapter(Map<String, IndexedBlockAddress> blocks) {
			this.blocks = blocks;
		}

		@Override
		public BlcAdd unmarshal(String addr) throws Exception {
			return IndexedBlockAddress.unmarshal(addr, blocks);
		}

	}

	static class IndexedBlcAddRefAdapter extends BlcAddRefAdapter {
		Map<String, IndexedBlockAddress> blocks = new HashMap<String, IndexedBlockAddress>();

		IndexedBlcAddRefAdapter(Map<String, IndexedBlockAddress> blocks) {
			this.blocks = blocks;
		}

		@Override
		public BlcAddRef unmarshal(String addr) throws Exception {
			return IndexedBlockAddress.unmarshal(addr, blocks);
		}
	}

}